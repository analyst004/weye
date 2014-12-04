package com.sidooo.weye;

import com.mongodb.BasicDBObject;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: kimzhang
 * Date: 14-11-16
 * Time: 下午3:21
 * To change this template use File | Settings | File Templates.
 */
public class UrlDatabaseTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void test1() throws  Exception
    {
        UrlDatabase.init("10.1.1.2", 27017, "test");
        for (int  i= 1; i<=10; i++) {

            WebItem request = new WebItem();
            request.setHost("http://test.com");
            request.setPath("/test"+i);


            UrlDatabase.write("test", request);

        }
    }
}
