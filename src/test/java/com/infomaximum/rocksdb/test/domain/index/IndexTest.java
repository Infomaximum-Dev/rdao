package com.infomaximum.rocksdb.test.domain.index;

import com.infomaximum.database.core.iterator.IteratorEntity;
import com.infomaximum.database.domainobject.filter.IndexFilter;
import com.infomaximum.rocksdb.domain.StoreFileEditable;
import com.infomaximum.rocksdb.domain.StoreFileReadable;
import com.infomaximum.rocksdb.test.StoreFileDataTest;
import org.junit.Assert;
import org.junit.Test;

public class IndexTest extends StoreFileDataTest {

    @Test
    public void findByIndex() throws Exception {
        final int recordCount = 100;

        domainObjectSource.executeTransactional(transaction -> {
            for (int size = 0; size < recordCount; size++) {
                StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
                storeFile.setSize(size);
                transaction.save(storeFile);
            }
        });

        for (long size = 0; size < recordCount; size++) {
            try (IteratorEntity<StoreFileReadable> i = domainObjectSource.find(StoreFileReadable.class, new IndexFilter(StoreFileReadable.FIELD_SIZE, size))) {
                Assert.assertTrue(i.hasNext());
                Assert.assertEquals(size, i.next().getSize());
                Assert.assertFalse(i.hasNext());
            }
        }
    }

    @Test
    public void findByPartialUpdatedMultiIndex() throws Exception {
        final int recordCount = 100;
        final String fileName = "file_name";

        // insert new objects
        domainObjectSource.executeTransactional(transaction -> {
            for (int i = 0; i < recordCount; i++) {
                StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
                storeFile.setSize(i);
                storeFile.setFileName(fileName);
                transaction.save(storeFile);
            }
        });

        // update part of multi-indexed object
        domainObjectSource.executeTransactional(transaction -> {
            for (int i = 0; i < recordCount; i++) {
                StoreFileEditable storeFile = transaction.get(StoreFileEditable.class, i + 1);
                storeFile.setSize(i + 2 * recordCount);
                transaction.save(storeFile);
            }
        });

        // find
        for (long size = (0 + 2 * recordCount); size < (recordCount + 2 * recordCount); size++) {
            try (IteratorEntity<StoreFileReadable> i = domainObjectSource.find(StoreFileReadable.class, new IndexFilter(StoreFileReadable.FIELD_SIZE, size))) {
                Assert.assertTrue(i.hasNext());
                Assert.assertEquals(size, i.next().getSize());
                Assert.assertFalse(i.hasNext());
            }

            IndexFilter filter = new IndexFilter(StoreFileReadable.FIELD_SIZE, size).appendField(StoreFileReadable.FIELD_FILE_NAME, fileName);
            try (IteratorEntity<StoreFileReadable> i = domainObjectSource.find(StoreFileReadable.class, filter)) {
                Assert.assertTrue(i.hasNext());
                Assert.assertEquals(size, i.next().getSize());
                Assert.assertFalse(i.hasNext());
            }
        }
    }
}
