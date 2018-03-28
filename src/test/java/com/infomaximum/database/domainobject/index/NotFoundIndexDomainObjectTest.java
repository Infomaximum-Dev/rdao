package com.infomaximum.database.domainobject.index;

import com.infomaximum.database.domainobject.filter.IndexFilter;
import com.infomaximum.database.exception.runtime.NotFoundIndexException;
import com.infomaximum.domain.ExchangeFolderReadable;
import com.infomaximum.database.domainobject.ExchangeFolderDataTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kris on 22.04.17.
 */
public class NotFoundIndexDomainObjectTest extends ExchangeFolderDataTest {

    @Test
    public void run() throws Exception {
        try {
            domainObjectSource.find(ExchangeFolderReadable.class, new IndexFilter("uuid", ""));
            Assert.fail();
        } catch (NotFoundIndexException ignore) {}

        try {
            domainObjectSource.find(ExchangeFolderReadable.class, new IndexFilter(ExchangeFolderReadable.FIELD_UUID, "")
                .appendField(ExchangeFolderReadable.FIELD_SYNC_DATE, ""));
            Assert.fail();
        } catch (NotFoundIndexException ignore) {}
    }
}