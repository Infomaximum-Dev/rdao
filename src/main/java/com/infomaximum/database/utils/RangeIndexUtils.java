package com.infomaximum.database.utils;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.provider.DBIterator;
import com.infomaximum.database.provider.DBTransaction;
import com.infomaximum.database.provider.KeyPattern;
import com.infomaximum.database.provider.KeyValue;
import com.infomaximum.database.schema.RangeIndex;
import com.infomaximum.database.utils.key.RangeIndexKey;

import java.util.ArrayList;

public class RangeIndexUtils {

    public static void insertIndexedRange(RangeIndex index, RangeIndexKey key, Object beginValue, Object endValue, DBTransaction transaction) throws DatabaseException {
        if (!isIndexedRange(beginValue, endValue)) {
            return;
        }

        final long totalBegin = IntervalIndexUtils.castToLong(beginValue);
        final long totalEnd = IntervalIndexUtils.castToLong(endValue);
        IntervalIndexUtils.checkInterval(totalBegin, totalEnd);

        key.setBeginRangeValue(totalBegin);

        try (DBIterator iterator = transaction.createIterator(index.columnFamily)) {
            // разобьем уже существующие интервалы по началу вставляемого
            final KeyPattern pattern = RangeIndexKey.buildLeftBorder(key.getHashedValues(), totalBegin);
            KeyValue keyValue = seek(iterator, pattern, totalBegin);
            for (; keyValue != null; keyValue = stepForward(iterator, pattern)) {
                long begin = RangeIndexKey.unpackIndexedValue(keyValue.getKey());
                if (begin < totalBegin) {
                    if (!RangeIndexKey.unpackEndOfRange(keyValue.getKey())) {
                        RangeIndexKey.setIndexedValue(totalBegin, keyValue.getKey());
                        transaction.put(index.columnFamily, keyValue.getKey(), TypeConvert.EMPTY_BYTE_ARRAY);
                    }
                } else if (begin > totalBegin) {
                    break;
                }
            }

            // вставим начало вставляемого интервала
            key.setIndexedValue(totalBegin);
            key.setEndOfRange(false);
            transaction.put(index.columnFamily, key.pack(), TypeConvert.EMPTY_BYTE_ARRAY);

            ArrayList<byte[]> prevBeginKeys = new ArrayList<>();
            // разобьем вставляемый интервал по уже существующим
            long prevBegin = totalBegin;
            for (; keyValue != null; keyValue = stepForward(iterator, pattern)) {
                if (key.getId() == RangeIndexKey.unpackId(keyValue.getKey())) {
                    continue;
                }

                long begin = RangeIndexKey.unpackIndexedValue(keyValue.getKey());
                if (prevBegin != begin) {
                    if (begin == totalEnd) {
                        prevBeginKeys.clear();
                        break;
                    } else if (begin > totalEnd) {
                        break;
                    }

                    key.setIndexedValue(begin);
                    key.setEndOfRange(false);
                    transaction.put(index.columnFamily, key.pack(), TypeConvert.EMPTY_BYTE_ARRAY);

                    prevBegin = begin;
                    prevBeginKeys.clear();
                }

                if (!RangeIndexKey.unpackEndOfRange(keyValue.getKey())) {
                    prevBeginKeys.add(keyValue.getKey());
                }
            }

            // разобьем уже существующие интервалы по концу вставляемого
            for (byte[] beginKey : prevBeginKeys) {
                RangeIndexKey.setIndexedValue(totalEnd, beginKey);
                transaction.put(index.columnFamily, beginKey, TypeConvert.EMPTY_BYTE_ARRAY);
            }

            // вставим конец вставляемого интервала
            key.setIndexedValue(totalEnd);
            key.setEndOfRange(true);
            transaction.put(index.columnFamily, key.pack(), TypeConvert.EMPTY_BYTE_ARRAY);
        }
    }

    public static void removeIndexedRange(RangeIndex index, RangeIndexKey key, Object beginValue, Object endValue, DBTransaction transaction) throws DatabaseException {
        if (!isIndexedRange(beginValue, endValue)) {
            return;
        }

        final long begin = IntervalIndexUtils.castToLong(beginValue);
        final long end = IntervalIndexUtils.castToLong(endValue);
        IntervalIndexUtils.checkInterval(begin, end);

        try (DBIterator iterator = transaction.createIterator(index.columnFamily)) {
            KeyValue keyValue = iterator.seek(RangeIndexKey.buildLeftBorder(key.getHashedValues(), begin));
            while (keyValue != null) {
                if (RangeIndexKey.unpackId(keyValue.getKey()) == key.getId()) {
                    transaction.delete(index.columnFamily, keyValue.getKey());
                }

                if (RangeIndexKey.unpackEndOfRange(keyValue.getKey())) {
                    break;
                }
                keyValue = iterator.next();
            }
        }
    }

    private static boolean isIndexedRange(Object beginValue, Object endValue) {
        return beginValue != null && endValue != null;
    }

    private static KeyValue stepForward(DBIterator iterator, KeyPattern pattern) throws DatabaseException {
        KeyValue keyValue = iterator.step(DBIterator.StepDirection.FORWARD);
        if (keyValue == null || pattern.match(keyValue.getKey()) != KeyPattern.MATCH_RESULT_SUCCESS) {
            return null;
        }

        return keyValue;
    }

    public static KeyValue seek(DBIterator indexIterator, KeyPattern pattern, long filterBeginValue) throws DatabaseException {
        KeyValue res = indexIterator.seek(pattern);
        if  (res == null) {
            return null;
        }

        long begin = RangeIndexKey.unpackIndexedValue(res.getKey());
        if (begin != filterBeginValue) {
            res = indexIterator.step(DBIterator.StepDirection.BACKWARD);
            if (res == null) {
                return indexIterator.seek(null);
            }
            begin = RangeIndexKey.unpackIndexedValue(res.getKey());
        }

        do {
            if (pattern.match(res.getKey()) != KeyPattern.MATCH_RESULT_SUCCESS ||
                    RangeIndexKey.unpackEndOfRange(res.getKey()) ||
                    begin != RangeIndexKey.unpackIndexedValue(res.getKey())) {
                return indexIterator.step(DBIterator.StepDirection.FORWARD);
            }

            res = indexIterator.step(DBIterator.StepDirection.BACKWARD);
        } while (res != null);

        return indexIterator.seek(null);
    }
}