package com.infomaximum.database.maintenance;

import com.infomaximum.database.core.schema.Schema;
import com.infomaximum.database.core.schema.StructEntity;
import com.infomaximum.database.datasource.DataSource;
import com.infomaximum.database.exeption.DatabaseException;
import com.infomaximum.database.exeption.InconsistentDatabaseException;

import java.util.*;
import java.util.stream.Collectors;

/*
 Не потоко безопасный класс
 */
public class SchemaService {

    private final DataSource dataSource;

    private boolean isCreationMode = false;

    private String namespace;
    private String namespacePrefix;
    private Schema schema;

    public SchemaService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SchemaService setCreationMode(boolean value) {
        this.isCreationMode = value;
        return this;
    }

    public SchemaService setNamespace(String namespace) {
        this.namespace = namespace;
        this.namespacePrefix = namespace + StructEntity.NAMESPACE_SEPARATOR;
        return this;
    }

    public SchemaService setSchema(Schema schema) {
        this.schema = schema;
        return this;
    }

    public void execute() throws DatabaseException {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException();
        }

        validateConsistentNames();

        DomainService domainService = new DomainService(dataSource)
                .setCreationMode(isCreationMode);
        for (StructEntity domain : schema.getDomains()) {
            domainService.execute(domain);
        }

        validateUnknownColumnFamilies();
    }

    private void validateConsistentNames() throws InconsistentDatabaseException {
        Set<String> processedNames = new HashSet<>();
        for (StructEntity domain : schema.getDomains()) {
            if (processedNames.contains(domain.getColumnFamily())) {
                throw new InconsistentDatabaseException("Column family " + domain.getColumnFamily() + " into " + domain.getObjectClass() + " already exists.");
            }

            if (!domain.getColumnFamily().startsWith(namespacePrefix)) {
                throw new InconsistentDatabaseException("Namespace " + namespace + " is not consistent with " + domain.getObjectClass());
            }

            processedNames.add(domain.getColumnFamily());
        }
    }

    private void validateUnknownColumnFamilies() throws InconsistentDatabaseException {
        Set<String> columnFamilies = Arrays.stream(dataSource.getColumnFamilies())
                .filter(s -> s.startsWith(namespacePrefix))
                .collect(Collectors.toSet());
        for (StructEntity domain : schema.getDomains()) {
            DomainService.removeDomainColumnFamiliesFrom(columnFamilies, domain);
        }

        if (!columnFamilies.isEmpty()) {
            throw new InconsistentDatabaseException("Namespace " + namespace + " contains unknown column families " + String.join(", ", columnFamilies) + ".");
        }
    }
}