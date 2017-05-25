package com.infomaximum.rocksdb.core.objectsource.utils.index;

import com.infomaximum.rocksdb.core.objectsource.utils.structentity.StructEntity;
import com.infomaximum.rocksdb.core.struct.DomainObject;
import com.infomaximum.rocksdb.utils.TypeConvertRocksdb;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by kris on 24.05.17.
 */
public class IndexUtils {

    public static int calcHashValue(Object value) {
        String string;
        if (value==null) {
            string = "";
        } else if (value instanceof DomainObject) {
            string = String.valueOf( ((DomainObject) value).getId() );
        } else {
            string = value.toString();
        }
        return Math.abs(hash(string.getBytes(TypeConvertRocksdb.ROCKSDB_CHARSET), 1));
    }

    public static int calcHashValues(Object[] values) {
        StringBuilder sBuilder = new StringBuilder();
        for (Object value: values) {
            if (value==null) continue;
            if (value instanceof DomainObject) {
                sBuilder.append(((DomainObject) value).getId());
            } else {
                sBuilder.append(value.toString());
            }
        }

        return Math.abs(hash(sBuilder.toString().getBytes(TypeConvertRocksdb.ROCKSDB_CHARSET), 1));
    }


    private static int hash(byte[] data, int seed) {
        int m = 0x5bd1e995;
        int r = 24;

        int h = seed ^ data.length;

        int len = data.length;
        int len_4 = len >> 2;

        for (int i = 0; i < len_4; i++) {
            int i_4 = i << 2;
            int k = data[i_4 + 3];
            k = k << 8;
            k = k | (data[i_4 + 2] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 1] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 0] & 0xff);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        int len_m = len_4 << 2;
        int left = len - len_m;

        if (left != 0) {
            if (left >= 3) {
                h ^= (int) data[len - 3] << 16;
            }
            if (left >= 2) {
                h ^= (int) data[len - 2] << 8;
            }
            if (left >= 1) {
                h ^= (int) data[len - 1];
            }

            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }


}