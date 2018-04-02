package com.infomaximum.database.maintenance;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.database.domainobject.filter.EmptyFilter;
import com.infomaximum.database.domainobject.iterator.IteratorEntity;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.exception.ForeignDependencyException;
import com.infomaximum.database.exception.InconsistentDatabaseException;
import com.infomaximum.database.provider.DBIterator;
import com.infomaximum.database.provider.DBProvider;
import com.infomaximum.database.provider.DBTransaction;
import com.infomaximum.database.schema.*;
import com.infomaximum.database.utils.IndexUtils;
import com.infomaximum.database.utils.PrefixIndexUtils;
import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.database.utils.key.FieldKey;
import com.infomaximum.database.utils.key.IndexKey;
import com.infomaximum.database.utils.key.IntervalIndexKey;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class DomainService {

    @FunctionalInterface
    private interface ModifierCreator {

        void apply(final DomainObject obj, DBTransaction transaction) throws DatabaseException;
    }

    @FunctionalInterface
    private interface IndexAction {
        void apply() throws DatabaseException;
    }

    private final DBProvider dbProvider;

    private ChangeMode changeMode = ChangeMode.NONE;
    private boolean isValidationMode = false;

    private StructEntity domain;
    private boolean existsData = false;

    public DomainService(DBProvider dbProvider) {
        this.dbProvider = dbProvider;
    }

    public DomainService setChangeMode(ChangeMode value) {
        this.changeMode = value;
        return this;
    }

    public DomainService setValidationMode(boolean value) {
        this.isValidationMode = value;
        return this;
    }

    public DomainService setDomain(StructEntity value) {
        this.domain = value;
        return this;
    }

    public void execute() throws DatabaseException {
        final String dataColumnFamily = domain.getColumnFamily();

        if (!dbProvider.containsSequence(dataColumnFamily)) {
            if (changeMode == ChangeMode.CREATION) {
                dbProvider.createSequence(dataColumnFamily);
            } else if (isValidationMode) {
                throw new InconsistentDatabaseException("Sequence " + dataColumnFamily + " not found.");
            }
        }

        existsData = ensureColumnFamily(dataColumnFamily);

        for (EntityIndex index : domain.getIndexes()) {
            ensureIndex(index, () -> doIndex(index));
        }

        for (EntityPrefixIndex index : domain.getPrefixIndexes()) {
            ensureIndex(index, () -> doPrefixIndex(index));
        }

        for (EntityIntervalIndex index : domain.getIntervalIndexes()) {
            ensureIndex(index, () -> doIntervalIndex(index));
        }

        if (changeMode == ChangeMode.REMOVAL) {
            remove();
        }

        validate();
    }

    static void removeDomainColumnFamiliesFrom(Set<String> columnFamilies, final StructEntity domain) {
        columnFamilies.remove(domain.getColumnFamily());

        for (EntityIndex index : domain.getIndexes()) {
            columnFamilies.remove(index.columnFamily);
        }

        for (EntityPrefixIndex index : domain.getPrefixIndexes()) {
            columnFamilies.remove(index.columnFamily);
        }

        for (EntityIntervalIndex index : domain.getIntervalIndexes()) {
            columnFamilies.remove(index.columnFamily);
        }
    }

    private void remove() throws DatabaseException {
        for (String columnFamily : getColumnFamilies()) {
            dbProvider.dropColumnFamily(columnFamily);
        }
    }

    private void validate() throws DatabaseException {
        if (!isValidationMode) {
            return;
        }

        validateUnknownColumnFamilies();

        if (changeMode != ChangeMode.REMOVAL) {
            validateIntegrity();
        }
    }

    private <T extends BaseIndex> void ensureIndex(T index, IndexAction indexAction) throws DatabaseException {
        final boolean existsIndexedValues = ensureColumnFamily(index.columnFamily);

        if (existsData) {
            if (existsIndexedValues || changeMode != ChangeMode.CREATION) {
                return;
            }

            indexAction.apply();
        } else if (existsIndexedValues && isValidationMode) {
            throw new InconsistentDatabaseException("Index " + index.columnFamily + " is not empty, but " + domain.getColumnFamily() + " is empty.");
        }
    }

    private void doIndex(EntityIndex index) throws DatabaseException {
        final Set<String> indexingFields = index.sortedFields.stream().map(EntityField::getName).collect(Collectors.toSet());
        final IndexKey indexKey = new IndexKey(0, new long[index.sortedFields.size()]);

        indexData(indexingFields, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            IndexUtils.setHashValues(index.sortedFields, obj, indexKey.getFieldValues());

            transaction.put(index.columnFamily, indexKey.pack(), TypeConvert.EMPTY_BYTE_ARRAY);
        });
    }

    private void doPrefixIndex(EntityPrefixIndex index) throws DatabaseException {
        final Set<String> indexingFields = index.sortedFields.stream().map(EntityField::getName).collect(Collectors.toSet());
        final SortedSet<String> lexemes = PrefixIndexUtils.buildSortedSet();

        indexData(indexingFields, (obj, transaction) -> {
            lexemes.clear();
            for (EntityField field : index.sortedFields) {
                PrefixIndexUtils.splitIndexingTextIntoLexemes(obj.get(field.getName()), lexemes);
            }
            PrefixIndexUtils.insertIndexedLexemes(index, obj.getId(), lexemes, transaction);
        });
    }

    private void doIntervalIndex(EntityIntervalIndex index) throws DatabaseException {
        final Set<String> indexingFields = index.sortedFields.stream().map(EntityField::getName).collect(Collectors.toSet());
        final List<EntityField> hashedFields = index.getHashedFields();
        final EntityField indexedField = index.getIndexedField();
        final IntervalIndexKey indexKey = new IntervalIndexKey(0, new long[hashedFields.size()]);

        indexData(indexingFields, (obj, transaction) -> {
            indexKey.setId(obj.getId());
            IndexUtils.setHashValues(hashedFields, obj, indexKey.getHashedValues());
            indexKey.setIndexedValue(obj.get(indexedField.getName()));

            transaction.put(index.columnFamily, indexKey.pack(), TypeConvert.EMPTY_BYTE_ARRAY);
        });
    }

    private Set<String> getColumnFamilies() throws DatabaseException {
        final String namespacePrefix = domain.getColumnFamily() + StructEntity.NAMESPACE_SEPARATOR;
        Set<String> result = Arrays.stream(dbProvider.getColumnFamilies())
                .filter(s -> s.startsWith(namespacePrefix))
                .collect(Collectors.toSet());
        result.add(domain.getColumnFamily());
        return result;
    }

    private void validateUnknownColumnFamilies() throws DatabaseException {
        Set<String> columnFamilies = getColumnFamilies();
        removeDomainColumnFamiliesFrom(columnFamilies, domain);
        if (!columnFamilies.isEmpty()) {
            throw new InconsistentDatabaseException(domain.getObjectClass() + " contains unknown column families " + String.join(", ", columnFamilies) + ".");
        }
    }

    private boolean ensureColumnFamily(String columnFamily) throws DatabaseException {
        if (dbProvider.containsColumnFamily(columnFamily)) {
            return existsKeys(columnFamily);
        }

        if (changeMode == ChangeMode.CREATION) {
            dbProvider.createColumnFamily(columnFamily);
        } else if (isValidationMode) {
            throw new InconsistentDatabaseException("Column family " + columnFamily + " not found.");
        }
        return false;
    }

    private void indexData(Set<String> loadingFields, ModifierCreator recordCreator) throws DatabaseException {
        DomainObjectSource domainObjectSource = new DomainObjectSource(dbProvider);
        try (DBTransaction transaction = dbProvider.beginTransaction();
             IteratorEntity<? extends DomainObject> iter = domainObjectSource.find(domain.getObjectClass(), EmptyFilter.INSTANCE, loadingFields)) {
            while (iter.hasNext()) {
                recordCreator.apply(iter.next(), transaction);
            }

            transaction.commit();
        }
    }

    private boolean existsKeys(String columnFamily) throws DatabaseException {
        try (DBIterator i = dbProvider.createIterator(columnFamily)) {
            return i.seek(null) != null;
        }
    }

    private void validateIntegrity() throws DatabaseException {
        if (!existsData) {
            return;
        }

        List<EntityField> foreignFields = domain.getFields()
                .stream()
                .filter(EntityField::isForeign)
                .collect(Collectors.toList());

        if (foreignFields.isEmpty()) {
            return;
        }

        Set<String> fieldNames = foreignFields
                .stream()
                .map(EntityField::getName)
                .collect(Collectors.toSet());

        FieldKey fieldKey = new FieldKey(0);

        DomainObjectSource domainObjectSource = new DomainObjectSource(dbProvider);
        try (IteratorEntity<? extends DomainObject> iter = domainObjectSource.find(domain.getObjectClass(), EmptyFilter.INSTANCE, fieldNames)) {
            while (iter.hasNext()) {
                DomainObject obj = iter.next();

                for (EntityField field : foreignFields) {
                    Long value = obj.get(field.getName());
                    if (value == null) {
                        continue;
                    }

                    fieldKey.setId(value);
                    if (dbProvider.getValue(field.getForeignDependency().getColumnFamily(), fieldKey.pack()) == null) {
                        throw new ForeignDependencyException(obj.getId(), domain.getObjectClass(), field, value);
                    }
                }
            }
        }
    }
}
