package com.infomaximum.database.core.sequence;

import com.infomaximum.database.utils.TypeConvert;
import com.infomaximum.rocksdb.struct.RocksDataBase;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kris on 22.04.17.
 */
public class ManagerSequence {

    private static final String COLUMN_FAMILY_SEQUENCE="system.sequence";

    private final RocksDataBase rocksDataBase;
    private final Map<String, Sequence> sequences;
    private final ColumnFamilyHandle columnFamilyHandle;

    public ManagerSequence(RocksDataBase rocksDataBase) throws RocksDBException {
        this.rocksDataBase = rocksDataBase;
        this.columnFamilyHandle = rocksDataBase.getColumnFamilyHandle(COLUMN_FAMILY_SEQUENCE);

        this.sequences = new HashMap<String, Sequence>();
    }

    public Sequence getSequence(String sequenceName) throws RocksDBException {
        Sequence sequence = sequences.get(sequenceName);
        if (sequence==null) {
            synchronized (sequences) {
                sequence = sequences.get(sequenceName);
                if (sequence==null) {
                    sequence = new Sequence(rocksDataBase, columnFamilyHandle, TypeConvert.pack(sequenceName));
                    sequences.put(sequenceName, sequence);
                }
            }
        }
        return sequence;
    }
}