package com.infomaximum.database.utils;

import com.infomaximum.database.core.schema.EntityField;
import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.exeption.DataSourceDatabaseException;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class IndexUtils {

    public static boolean toLongCastable(Class<?> type) {
        return type != String.class;
    }

    public static void setHashValues(final List<EntityField> sortedFields, final Map<EntityField, Object> values, long[] destination) {
        for (int i = 0; i < sortedFields.size(); ++i) {
            EntityField field = sortedFields.get(i);
            destination[i] = buildHash(field.getType(), values.get(field));
        }
    }

    public static void setHashValues(final List<EntityField> sortedFields, final DomainObject object, long[] destination) throws DataSourceDatabaseException {
        for (int i = 0; i < sortedFields.size(); ++i) {
            EntityField field = sortedFields.get(i);
            destination[i] = buildHash(field.getType(), object.get(field.getClass(), field.getName()));
        }
    }

    public static long buildHash(Class<?> type, Object value) {
        if (value == null) {
            return 0;
        }

        if (type == Long.class) {
            return ((Long) value).longValue();
        } else if (type == String.class) {
            return hash(TypeConvert.pack(((String) value).toLowerCase()));
        } else if (type == Integer.class) {
            return ((Integer) value).longValue();
        } else if (type == Boolean.class) {
            return ((Boolean) value) ? 1 : 0;
        } else if (type == Date.class) {
            return ((Date)value).getTime();
        } else {
            throw new IllegalArgumentException("Unsupported type " + type + " for hashing.");
        }
    }

    public static boolean equals(Class<?> clazz, Object left, Object right) {
        if (left == null) {
            return right == null;
        }

        if (clazz == String.class) {
            return ((String)left).equalsIgnoreCase((String)right);
        }

        return left.equals(right);
    }

    /**
     * http://www.azillionmonkeys.com/qed/hash.html
     * @param data
     * @return
     */
    private static long hash(final byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }

        int len = data.length;
        long hash = len;
        long tmp;
        int rem;

        rem = len & 3;
        len >>>= 2;

        /* Main loop */
        int pos = 0;
        for (; len > 0; --len) {
            hash += (data[pos++] | (data[pos++] << 8));
            tmp = ((data[pos++] | (data[pos++] << 8)) << 11) ^ hash;
            hash = ((hash << 16) ^ tmp);
            hash += (hash >>> 11);
        }

        /* Handle end cases */
        switch (rem) {
            case 3:
                hash += (data[pos++] | (data[pos++] << 8));
                hash ^= (hash << 16);
                hash ^= (data[pos++] << 18);
                hash += (hash >>> 11);
                break;
            case 2:
                hash += (data[pos++] | (data[pos++] << 8));
                hash ^= (hash << 11);
                hash += (hash >>> 17);
                break;
            case 1:
                hash += data[pos++];
                hash ^= (hash << 10);
                hash += (hash >>> 1);
                break;
        }

        /* Force "avalanching" of final 127 bits */
        hash ^= (hash << 3);
        hash += (hash >>> 5);
        hash ^= (hash << 4);
        hash += (hash >>> 17);
        hash ^= (hash << 25);
        hash += (hash >>> 6);

        return hash & 0xFFFFFFFF;
    }
}
