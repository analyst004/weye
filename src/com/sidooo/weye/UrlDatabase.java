package com.sidooo.weye;

import com.mongodb.*;
import org.joda.time.DateTime;

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

    public static void write(String crawlId, WebItem item) throws Exception
    {
        DBCollection coll = getCollection(crawlId);


        BasicDBObject doc = new BasicDBObject();
        doc.put("md5", item.getMd5());
        doc.put("url", item.getUrl());
        doc.put("date", item.getDate());

        BasicDBList string = new BasicDBList();
        Map<String,String> params = item.getStringParams();
        Set<String> keys = params.keySet();
        for(String key:keys) {
            String value = params.get(key);
            string.add(new BasicDBObject(key, value));
        }
        doc.put("string", string);

        BasicDBList query = new BasicDBList();
        params = item.getQueryParams();
        keys = params.keySet();
        for(String key : keys) {
            String value = params.get(key);
            query.add(new BasicDBObject(key,value));
        }
        doc.put("query", query);

        BasicDBList form = new BasicDBList();
        params = item.getFormParams();
        keys = params.keySet();
        for(String key : keys) {
            String value = params.get(key);
            form.add(new BasicDBObject(key,value));
        }
        doc.put("form", form);

        doc.put("log", item.getLog());
        doc.put("text",  item.getHtml());
        coll.insert(doc);
        coll.drop();
    }

    public static DateTime exist(String crawlId, WebItem item) throws Exception
    {
        DBCollection coll = getCollection(crawlId);
        try {
            BasicDBObject query = new BasicDBObject();
            query.put("md5", item.getMd5());
            DBCursor cursor = coll.find(query).sort(new BasicDBObject("date", -1));
            if (!cursor.hasNext()) {
                return null;
            } else {
                DBObject last = cursor.next();
                DateTime fetchTime = new DateTime(last.toMap().get("date"));
                return fetchTime;
            }
        } finally {
            coll.drop();
        }
    }
}
