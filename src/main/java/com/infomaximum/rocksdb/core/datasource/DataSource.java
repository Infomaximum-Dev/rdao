package com.infomaximum.rocksdb.core.datasource;

import com.infomaximum.rocksdb.transaction.Transaction;
import com.infomaximum.rocksdb.transaction.engine.EngineTransaction;
import org.rocksdb.RocksDBException;

import java.util.Map;

/**
 * Created by user on 19.04.2017.
 */
public interface DataSource {

    public Transaction createTransaction();

    public long nextId(String columnFamily) throws RocksDBException;

    public Map<String, byte[]> load(String columnFamily, long id, boolean isWriteLock) throws RocksDBException;

}
