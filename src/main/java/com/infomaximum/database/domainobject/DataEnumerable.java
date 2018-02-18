package com.infomaximum.database.domainobject;

import com.infomaximum.database.core.iterator.*;
import com.infomaximum.database.core.schema.EntityField;
import com.infomaximum.database.core.schema.Schema;
import com.infomaximum.database.datasource.DataSource;
import com.infomaximum.database.datasource.KeyPattern;
import com.infomaximum.database.datasource.KeyValue;
import com.infomaximum.database.domainobject.filter.*;
import com.infomaximum.database.domainobject.key.FieldKey;
import com.infomaximum.database.exception.DataSourceDatabaseException;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.exception.UnexpectedEndObjectException;
import com.infomaximum.database.exception.runtime.IllegalTypeException;
import com.infomaximum.database.utils.TypeConvert;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public abstract class DataEnumerable {

    public static class NextState {

        private long nextId = -1;

        private boolean isEmpty() {
            return nextId == -1;
        }

        private void clear() {
            nextId = -1;
        }
    }

    protected final DataSource dataSource;

    DataEnumerable(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public abstract <T, U extends DomainObject> T getValue(final EntityField field, U object) throws DataSourceDatabaseException;
    public abstract long createIterator(String columnFamily) throws DataSourceDatabaseException;

    public KeyValue seek(long iteratorId, final KeyPattern pattern) throws DataSourceDatabaseException {
        return dataSource.seek(iteratorId, pattern);
    }

    public KeyValue next(long iteratorId) throws DataSourceDatabaseException {
        return dataSource.next(iteratorId);
    }

    public KeyValue step(long iteratorId, DataSource.StepDirection direction) throws DataSourceDatabaseException {
        return dataSource.step(iteratorId, direction);
    }

    public void closeIterator(long iteratorId) {
        dataSource.closeIterator(iteratorId);
    }

    public <T extends DomainObject> T get(final Class<T> clazz, long id, final Set<String> loadingFields) throws DataSourceDatabaseException {
        final String columnFamily = Schema.getEntity(clazz).getColumnFamily();

        long iteratorId = createIterator(columnFamily);
        try {
            return seekObject(DomainObject.getConstructor(clazz), loadingFields, iteratorId, FieldKey.buildKeyPattern(id, loadingFields),null);
        } finally {
            dataSource.closeIterator(iteratorId);
        }
    }

    public <T extends DomainObject> T get(final Class<T> clazz, long id) throws DataSourceDatabaseException {
        return get(clazz, id, Collections.emptySet());
    }

    public <T extends DomainObject> IteratorEntity<T> find(final Class<T> clazz, Filter filter, final Set<String> loadingFields) throws DatabaseException {
        if (filter instanceof EmptyFilter) {
            return new AllIterator<>(this, clazz, loadingFields);
        } else if (filter instanceof IndexFilter) {
            return new IndexIterator<>(this, clazz, loadingFields, (IndexFilter)filter);
        } else if (filter instanceof PrefixIndexFilter) {
            return new PrefixIndexIterator<>( this, clazz, loadingFields, (PrefixIndexFilter)filter);
        } else if (filter instanceof IntervalIndexFilter) {
            return new IntervalIndexIterator<>(this, clazz, loadingFields, (IntervalIndexFilter) filter);
        }
        throw new IllegalArgumentException("Unknown filter type " + filter.getClass());
    }

    public <T extends DomainObject> IteratorEntity<T> find(final Class<T> clazz, Filter filter) throws DatabaseException {
        return find(clazz, filter, Collections.emptySet());
    }

    public <T extends DomainObject> T buildDomainObject(final Constructor<T> constructor, long id, Collection<String> preInitializedFields) {
        T obj = buildDomainObject(constructor, id);
        for (String field : preInitializedFields) {
            obj._setLoadedField(field, null);
        }
        return obj;
    }

    private <T extends DomainObject> T buildDomainObject(final Constructor<T> constructor, long id) {
        try {
            T domainObject = constructor.newInstance(id);
            domainObject._setDataSource(this);
            return domainObject;
        } catch (ReflectiveOperationException e) {
            throw new IllegalTypeException(e);
        }
    }

    public <T extends DomainObject> T nextObject(final Constructor<T> constructor, Collection<String> preInitializedFields,
                                                 long iteratorId, NextState state) throws DataSourceDatabaseException {
        if (state.isEmpty()) {
            return null;
        }

        T obj = buildDomainObject(constructor, state.nextId, preInitializedFields);
        state.clear();
        readObject(obj, iteratorId, state);
        return obj;
    }

    public <T extends DomainObject> T seekObject(final Constructor<T> constructor, Collection<String> preInitializedFields,
                                                 long iteratorId, KeyPattern pattern, NextState state) throws DataSourceDatabaseException {
        KeyValue keyValue = seek(iteratorId, pattern);
        if (keyValue == null) {
            return null;
        }

        FieldKey key = FieldKey.unpack(keyValue.getKey());
        if (!key.isBeginningObject()) {
            return null;
        }

        T obj = buildDomainObject(constructor, key.getId(), preInitializedFields);
        readObject(obj, iteratorId, state);
        return obj;
    }

    private <T extends DomainObject> void readObject(T obj, long iteratorId, NextState state) throws DataSourceDatabaseException {
        KeyValue keyValue;
        FieldKey key;
        while ((keyValue = next(iteratorId)) != null) {
            key = FieldKey.unpack(keyValue.getKey());
            if (key.getId() != obj.getId()) {
                if (!key.isBeginningObject()) {
                    throw new UnexpectedEndObjectException(obj.getId(), key);
                }
                if (state != null) {
                    state.nextId = key.getId();
                }
                break;
            }
            EntityField field = obj.getStructEntity().getField(key.getFieldName());
            obj._setLoadedField(key.getFieldName(), TypeConvert.unpack(field.getType(), keyValue.getValue(), field.getConverter()));
        }
    }
}
