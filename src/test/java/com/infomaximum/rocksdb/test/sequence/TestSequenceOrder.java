package com.infomaximum.rocksdb.test.sequence;

import com.infomaximum.rocksdb.RocksDataTest;
import com.infomaximum.rocksdb.builder.RocksdbBuilder;
import com.infomaximum.rocksdb.sequence.Sequence;
import com.infomaximum.rocksdb.struct.RocksDataBase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kris on 22.04.17.
 */
public class TestSequenceOrder extends RocksDataTest {

    private final static Logger log = LoggerFactory.getLogger(TestSequenceOrder.class);

    @Test
    public void run() throws Exception {
        String sequenceName = "sdfuisii";

        RocksDataBase rocksDataBase = new RocksdbBuilder()
                .withPath(pathDataBase)
                .build();

        Sequence sequence = rocksDataBase.getSequence(sequenceName);
        for (int i=1; i < 1000000; i++) {
            long id = sequence.next();
            Assert.assertEquals(i, id);
        }

        rocksDataBase.destroy();
    }

}