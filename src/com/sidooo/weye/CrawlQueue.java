package com.sidooo.weye;

import redis.clients.jedis.Jedis;

import java.util.AbstractQueue;

/**
 * Created with IntelliJ IDEA.
 * User: kimzhang
 * Date: 14-11-17
 * Time: 下午1:51
 * To change this template use File | Settings | File Templates.
 */
public class CrawlQueue {

    private static Jedis redis;
    private static String host;

    public static void init(String host)
    {
        redis = new Jedis(host);
    }

    public static void push(String crawlName, CrawlRequest quest)
    {
        String stream = quest.toString();
        redis.rpush(crawlName, stream);
    }

    public static CrawlRequest poll(String crawlName) throws Exception
    {
       // String stream = redis.lpop(crawlName);
        //return new CrawlRequest(stream);
        return null;
    }

}
