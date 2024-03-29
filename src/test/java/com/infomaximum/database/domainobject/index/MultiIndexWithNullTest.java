package com.infomaximum.database.domainobject.index;

import com.infomaximum.database.domainobject.iterator.IteratorEntity;
import com.infomaximum.database.domainobject.filter.HashFilter;
import com.infomaximum.domain.StoreFileEditable;
import com.infomaximum.domain.StoreFileReadable;
import com.infomaximum.database.domainobject.StoreFileDataTest;
import org.junit.Assert;
import org.junit.Test;

public class MultiIndexWithNullTest extends StoreFileDataTest {

    @Test
    public void findByComboIndex() throws Exception {
        final int recordCount = 100;

        domainObjectSource.executeTransactional(transaction -> {
            for (long size = 0; size < recordCount; size++) {
                StoreFileEditable storeFile = transaction.create(StoreFileEditable.class);
                storeFile.setSize(size);
                storeFile.setFileName(null);
                transaction.save(storeFile);
            }
        });

        for (long size = 0; size < recordCount; size++) {
            try (IteratorEntity<StoreFileReadable> i = domainObjectSource.find(StoreFileReadable.class,
                    new HashFilter(StoreFileReadable.FIELD_SIZE, size).appendField(StoreFileReadable.FIELD_FILE_NAME, null))) {
                Assert.assertTrue(i.hasNext());

                StoreFileReadable storeFileReadable = i.next();

                Assert.assertEquals(size, storeFileReadable.getSize());
                Assert.assertEquals(null, storeFileReadable.getFileName());

                Assert.assertFalse(i.hasNext());
            }
        }
    }
}
