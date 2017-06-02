package com.infomaximum.rocksdb.utils;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by kris on 23.03.17.
 */
public class TypeConvertRocksdb {

    public static final Charset ROCKSDB_CHARSET = Charset.forName("UTF-8");

    public static String getString(byte[] value){
        if (value==null) {
            return null;
        } else {
            return new String(value, TypeConvertRocksdb.ROCKSDB_CHARSET);
        }
    }

    public static Integer getInteger(byte[] value){
        if (value==null) {
            return null;
        } else {
            return Ints.fromByteArray(value);
        }
    }

    public static Long getLong(byte[] value){
        if (value==null) {
            return null;
        } else {
            return Longs.fromByteArray(value);
        }
    }

    public static Boolean getBoolean(byte[] value){
        if (value==null) {
            return null;
        } else {
            return (value[0]==(byte)1);
        }
    }

    public static Date getDate(byte[] value){
        if (value==null) {
            return null;
        } else {
            return new Date(getLong(value));
        }
    }


    public static byte[] pack(String value){
        return value.getBytes(TypeConvertRocksdb.ROCKSDB_CHARSET);
    }

    public static byte[] pack(int value){
        return Ints.toByteArray(value);
    }

    public static byte[] pack(long value){
        return Longs.toByteArray(value);
    }

    public static byte[] pack(boolean value){
        return new byte[] { (value)? (byte)1 :(byte)0 };
    }

    public static byte[] pack(Date value){
        return pack(value.getTime());
    }


    public static Object get(Class<?> type, byte[] value){
        if (type==String.class) {
            return getString(value);
        } else if (type == Long.class || type == long.class) {
            return getLong(value);
        } else if (type == Integer.class || type == int.class) {
            return getInteger(value);
        } else if (type == Boolean.class || type == boolean.class) {
            return getBoolean(value);
        } else if (type == byte[].class) {
            return value;
        } else if (type == Date.class) {
            return getDate(value);
        } else {
            throw new RuntimeException("Not support type: " + type);
        }
    }

    public static byte[] packObject(Class<?> type, Object value){
        if (type==String.class) {
            return pack((String) value);
        } else if (type == Long.class || type == long.class) {
            return pack((Long) value);
        } else if (type == Integer.class || type == int.class) {
            return pack((Integer) value);
        } else if (type == Boolean.class || type == boolean.class) {
            return pack((Boolean) value);
        } else if (type == byte[].class) {
            return (byte[]) value;
        } else if (type == Date.class) {
            return pack((Date)value);
        } else {
            throw new RuntimeException("Not support type: " + type);
        }
    }
}
