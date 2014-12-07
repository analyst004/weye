package com.sidooo.weye;

import com.mongodb.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.jsoup.nodes.Element;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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
    private static DB db;

    public static void init(String host, int port, String dbname) throws  Exception{
        host = host;
        port = port;
        dbname = dbname;


        MongoOptions options = new MongoOptions();
        options.autoConnectRetry = true;
        options.connectionsPerHost = 200;
        options.socketTimeout = 2000;
        options.socketKeepAlive = true;

        client = new Mongo(new ServerAddress(host, port), options);
        db = client.getDB(dbname);
        if (db == null) {
            throw new ClassNotFoundException(dbname + " not found");
        }
    }

    private static DBCollection getCollection(String crawlId) throws Exception{
        if (!db.collectionExists(crawlId)) {
            DBObject options = BasicDBObjectBuilder.start().add("capped", false).get();
            return db.createCollection(crawlId, options);
        } else {
            return db.getCollection(crawlId);
        }
    }

    public static void write(String crawlId, String target, WebItem item) throws Exception
    {
        DBCollection coll = getCollection(crawlId);

        MessageDigest hash = MessageDigest.getInstance("MD5");
        hash.update(target.toLowerCase().getBytes());
        byte[] array = hash.digest();
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i < array.length; i++)
        {
            buffer.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
        }
        String md5 = buffer.toString();

        BasicDBObject doc = new BasicDBObject();
        doc.put("md5", md5);
        //DateTimeFormatter parser = ISODateTimeFormat.dateTime();
        doc.put("date", item.date());
        doc.put("target", target);
        doc.put("text",  item.html());
        coll.insert(doc, WriteConcern.SAFE);
    }

//    public static DateTime exist(String crawlId, WebItem item) throws Exception
//    {
//        DBCollection coll = getCollection(crawlId);
//        try {
//            BasicDBObject query = new BasicDBObject();
//            query.put("md5", item.getMd5());
//            DBCursor cursor = coll.find(query).sort(new BasicDBObject("date", -1));
//            if (!cursor.hasNext()) {
//                return null;
//            } else {
//                DBObject last = cursor.next();
//                DateTime fetchTime = new DateTime(last.toMap().get("date"));
//                return fetchTime;
//            }
//        } finally {
//            coll.drop();
//        }
//    }
}
