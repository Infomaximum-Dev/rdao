package com.infomaximum.rocksdb.domain;

import com.infomaximum.database.core.anotation.Entity;
import com.infomaximum.database.core.anotation.Field;
import com.infomaximum.database.core.anotation.Index;
import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectReadable;
import com.infomaximum.database.exeption.DatabaseException;
import com.infomaximum.rocksdb.domain.type.FormatType;

/**
 * Created by user on 19.04.2017.
 */
@Entity(
        name = "com.infomaximum.StoreFile",
        fields = {
                @Field(name = StoreFileReadable.FIELD_FILE_NAME, type = String.class),
                @Field(name = StoreFileReadable.FIELD_CONTENT_TYPE, type = String.class),
                @Field(name = StoreFileReadable.FIELD_SIZE, type = Long.class),
                @Field(name = StoreFileReadable.FIELD_SINGLE, type = Boolean.class),
                @Field(name = StoreFileReadable.FIELD_FORMAT, type = FormatType.class)
        },
        indexes = {
                @Index(fields = {StoreFileReadable.FIELD_SIZE}),
                @Index(fields = {StoreFileReadable.FIELD_SIZE, StoreFileReadable.FIELD_FILE_NAME})
        }
)
public class StoreFileReadable extends DomainObject implements DomainObjectReadable {

    public final static String FIELD_FILE_NAME="file_name";
    public final static String FIELD_CONTENT_TYPE="content_type";
    public final static String FIELD_SIZE="size";
    public final static String FIELD_SINGLE="single";
    public final static String FIELD_FORMAT="format";



    public StoreFileReadable(long id) {
        super(id);
    }


    public String getFileName() throws DatabaseException {
        return getString(FIELD_FILE_NAME);
    }

    public String getContentType() throws DatabaseException {
        return getString(FIELD_CONTENT_TYPE);
    }

    public long getSize() throws DatabaseException {
        return getLong(FIELD_SIZE);
    }

    public boolean isSingle() throws DatabaseException {
        return getBoolean(FIELD_SINGLE);
    }


    public FormatType getFormat() throws DatabaseException {
        return getEnum(FormatType.class, FIELD_FORMAT);
    }

}