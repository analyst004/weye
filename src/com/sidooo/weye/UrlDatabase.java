package com.sidooo.weye;

import com.mongodb.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: kimzhang
 * Date: 14-11-15
 * Time: 下午5:03
 * To change this template use File | Settings | File Templates.
 */
public class UrlDatabase {

    private static Mongo client;
    private static String host;
    private static int port;
    private static String dbname;

    public static void init(String host, int port, String dbname) throws  Exception{
        host = host;
        port = port;
        dbname = dbname;
        client = new Mongo(host, port);
    }

    public static void write(CrawlRequest request)
    {
        String md5 = request.getMd5();
        DBCollection coll = client.getDB("urldb").getCollection("completed");
        BasicDBObject doc = new BasicDBObject("md5", md5);

        coll.insert(doc);
        coll.drop();
    }

    public static boolean exist(CrawlRequest request)
    {
        String md5 = request.getMd5();
        DBCollection coll;
        coll = client.getDB("urldb").getCollection("completed");

        BasicDBObject query = new BasicDBObject();
        query.put("md5", md5);
        DBCursor cursor = coll.find(query);
        boolean ret = cursor.hasNext();
        coll.drop();
        return ret;
    }
}
