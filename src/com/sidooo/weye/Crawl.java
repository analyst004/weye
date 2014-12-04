package com.sidooo.weye;

import java.io.FileInputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;
import org.joda.time.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.datatype.Duration;

public class Crawl implements Runnable {
	
	protected Logger logger = null;
	protected CrawlStatus status = CrawlStatus.CLOSED;
	protected Element conf = null;
    private int succCount = 0;               //爬取成功数量
    private int failCount = 0;               //爬去失败数量
    protected String webDomain;
    private long executeTick = 0;           //爬虫工作总时间
    private Thread thread = null;
    private boolean isEnabled = false;
    protected  DateTime lastRunTime = null;

	protected Crawl(Element conf){
	    this.conf = conf;

        if (conf.hasAttr("enable")) {
            if ("true".equals(conf.attr("enable"))) {
                isEnabled = true;
            } else {
                isEnabled = false;
            }
        } else {
            isEnabled = false;
        }
        if (conf.hasAttr("id")) {
            logger =  Logger.getLogger(conf.attr("id"));
        } else {
            logger = Logger.getLogger("UnkownCrawl");
        }

	}


    public static Crawl createInstance(CrawlType type, Element conf) {
        if (type == CrawlType.BROWSE) {
            return new BrowseCrawl(conf);
        } else if (type == CrawlType.QUERY){
            return new QueryCrawl(conf);
        } else {
            return null;
        }
    }

    public String getId() {
        return conf.attr("id");
    }

    public String getName() {
        return conf.attr("name");
    }

    public  String getHost() {
        return conf.attr("host");
    }

    public int getCount() {
        return succCount + failCount;
    }

    public CrawlStatus getStatus() {
        return status;
    }

    public void enable(boolean value) {
        isEnabled = value;
    }

    public boolean enable() {
        return isEnabled;
    }

    protected boolean isNeedUpdate() {

        if (lastRunTime == null) {
            return true;
        }
        int minutes = Minutes.minutesBetween(lastRunTime, new DateTime()).getMinutes();
        return minutes >= 60;
    }

    @Override
    public void run() {


    }







}




class BrowseCrawl extends Crawl  {

//    public void run() {
//        WebList list;
//        int pageCount = list.getPageCount();
//        for (int i=1; i<pageCount; i++) {
//            WebPage page = list.getPage(i);
//            WebItem[] items = page.getItems();
//            for (WebItem item : items) {
//                String format = item.getFormat();
//                String data = item.getData();
//                saveCloud(format, data);
//            }
//        }
//    }
    public  BrowseCrawl(Element conf) {
        super(conf);
    }

    @Override
    public void run() {

        logger.info("BrowseCrawl "+ this.getId()+" is running");
        status = CrawlStatus.RUNNING;

        while(true) {

            if (!enable()) {
                logger.warn("Crawl is disabled");
                break;
            }

            if (!isNeedUpdate()) {
                try {
                    Thread.sleep(60000);
                } catch (Exception e) {
                    break;
                }
                continue;
            }

            lastRunTime = new DateTime();

            //获取列表入口， 计算出总页数
            int pageCount = 0;
            try {
                WebList list = WebList.createInstance(this.getHost(), this.conf);
                pageCount = list.exec();
                logger.info("Web List has "+pageCount+" pages");
            } catch (Exception e) {
                logger.fatal("Fetch Web List Error");
                logger.fatal(e.getStackTrace().toString());
                status = CrawlStatus.CLOSED;
                return;
            }

            //分页获取
            int pageFailCount = 0;
            boolean needContinue = true;
            for(int i=1; i<pageCount; i++) {

                if (!needContinue) {
                    break;
                }
                WebPage page = null;
                try {
                    page = WebPage.createInstance(this.getHost(), this.conf);
                } catch (Exception e) {
                    logger.fatal("Parse Web Page Configuration Failed.");
                    logger.fatal(e.getStackTrace().toString());
                    needContinue = false;
                    continue;
                }

                String[] items = null;
                try {
                    items = page.exec(i);
                    logger.info("Fetch Web Page "+i+" Succeed.");
                    pageFailCount = 0;
                } catch (Exception e) {
                    logger.warn("Fetch Web Page Error.");
                    logger.warn(e.getStackTrace().toString());
                    pageFailCount += 1;
                    if (pageFailCount > 4) {
                        //如果连续5个页面获取失败， 则不再继续尝试获取剩余内容
                        break;
                    } else {
                        continue;
                    }
                }

                for( String item : items) {

                    WebItem webItem = null;
                    try {
                        webItem = WebItem.createInstance(this.getHost(), this.conf);
                    } catch (Exception e) {
                        logger.fatal("Parse Web Item Configuration Failed.");
                        logger.fatal(e.getStackTrace().toString());
                        needContinue = false;
                        break;
                    }

                    try {
                        DateTime now = new DateTime();
                        DateTime fetchTime = UrlDatabase.exist(this.getId(), webItem);
                        if (fetchTime != null) {
                            int hours = Hours.hoursBetween(fetchTime, now).getHours();
                            if (hours < 72) {
                                //距离上次采集时间未超过72小时， 不进行更新
                                //列表后面的内容也不需要继续获取了
                                needContinue = false;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Get Web Item Last update time failed.");
                        logger.warn(e.getStackTrace().toString());
                    }

                    try {
                        webItem.exec(item);
                        logger.info("Fetch Web Item "+item+" Succeed.");
                    } catch (Exception e) {
                        logger.warn("Fetch Web Item Error.");
                        logger.warn(e.getStackTrace().toString());
                        continue;
                    }

                    try {
                        UrlDatabase.write(this.getId(), webItem);
                    } catch (Exception e) {
                        logger.warn("Save Web Item error.");
                        logger.warn(e.getStackTrace().toString());
                    }

                }
            }

        }
        status = CrawlStatus.CLOSED;
        logger.info("BrowseCrawl "+ this.getId()+" has exited");

    }
}



class QueryCrawl extends Crawl {

    public QueryCrawl(Element conf) {
        super(conf);
    }

    @Override
    public void run() {

        status = CrawlStatus.RUNNING;
        try {
//            WebEntry webEntry = createEntryRequest();
//            String[] items = webEntry.exec();
//            for (String item : items) {
//                WebItem webItem = createItemRequest();
//                if (UrlDatabase.exist(webItem)) {
//                    webItem.exec(item);
//                    UrlDatabase.write(getName(), webItem);
//                }
//            }
        } catch (Exception e) {

        } finally {
            status = CrawlStatus.CLOSED;
        }
    }

}
