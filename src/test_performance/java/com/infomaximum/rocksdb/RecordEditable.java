package com.infomaximum.rocksdb;

import com.infomaximum.database.domainobject.DomainObjectEditable;

public class RecordEditable extends RecordReadable implements DomainObjectEditable {

    public RecordEditable(long id) {
        super(id);
    }

    void setString1(String value) {
        set(FIELD_STRING_1, value);
    }
}
