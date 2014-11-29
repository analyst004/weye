package com.sidooo.weye;

import java.io.FileInputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawl implements Runnable {
	
	protected Logger logger = null;
	protected CrawlStatus status = CrawlStatus.CLOSED;
	protected Element conf = null;
    private int succCount = 0;               //爬取成功数量
    private int failCount = 0;               //爬去失败数量
    protected String webDomain;
    private long executeTick = 0;           //爬虫工作总时间
    private Thread thread = null;

	protected Crawl(Element conf){
	    this.conf = conf;
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

    public Logger getLogger()
    {
        return logger;
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void run() {

        System.out.println("OK");
    }

    protected CrawlRequest createListRequest() throws Exception{

        CrawlRequest request = new CrawlRequest();
        request.setHost(this.getHost());

        Element childNode = conf.select("list").first();
        if (childNode.hasAttr("method")) {
            String method = childNode.attr("method");
            request.setHttpMethod(method);
        }

        if (childNode.hasAttr("path")) {
            String path = childNode.attr("path");
            request.setPath(path);
        }

        if (childNode.hasAttr("pagecount")) {
            request.attr("pagecount", childNode.attr("pagecount"));
        }

        Elements childs = childNode.children();
        for(Element child : childs) {
            if (child.nodeName().equals("string")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addStringParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("query")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addQueryParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("form")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addFormParameter(paramName, paramValue);
            }
        }

        return request;
    }

    protected CrawlRequest createPageRequest() {

        CrawlRequest request = new CrawlRequest();

        request.setHost(this.getHost());

        Element childNode = conf.select("page").first();
        if (childNode.hasAttr("method")) {
            request.setHttpMethod(childNode.attr("method"));
        }

        if (childNode.hasAttr("path")) {
            request.setPath(childNode.attr("path"));
        }

        Elements childs = childNode.children();
        for(Element child : childs) {
            if (child.nodeName().equals("string")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addStringParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("query")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addQueryParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("form")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addFormParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("select")) {

                String selectMethod = child.attr("method");
                String selectKey = child.attr("key");
                String selectIndex = child.attr("index");
                String selectAttribute = child.attr("attribute");

                request.addSelect(selectMethod, selectKey, selectIndex, selectAttribute);
            }
        }

        return request;
    }

    protected CrawlRequest createItemRequest() {

        CrawlRequest request = new CrawlRequest();

        request.setHost(this.getHost());

        Element childNode = conf.select("item").first();
        if (childNode.hasAttr("method")) {
            request.setHttpMethod(childNode.attr("method"));
        }

        if (childNode.hasAttr("path")) {
            request.setPath(childNode.attr("path"));
        }

        Elements childs = childNode.children();
        for(Element child : childs) {
            if (child.nodeName().equals("string")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addStringParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("query")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addQueryParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("form")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addFormParameter(paramName, paramValue);
            }

        }

        return request;
    }

    protected CrawlRequest createEntryRequest() {

        CrawlRequest request = new CrawlRequest();

        request.setHost(this.getHost());

        Element childNode = conf.select("entry").first();
        if (childNode.hasAttr("method")) {
            request.setHttpMethod(childNode.attr("method"));
        }

        if (childNode.hasAttr("path")) {
            request.setPath(childNode.attr("path"));
        }

        Elements childs = childNode.children();
        for(Element child : childs) {
            if (child.nodeName().equals("string")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addStringParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("query")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addQueryParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("form")) {
                String paramName = child.attr("name");
                String paramValue = child.text();
                request.addFormParameter(paramName, paramValue);
            }

            if (child.nodeName().equals("select")) {

                String selectMethod = child.attr("method");
                String selectKey = child.attr("key");
                String selectIndex = child.attr("index");
                String selectAttribute = child.attr("attribute");

                request.addSelect(selectMethod, selectKey, selectIndex, selectAttribute);
            }



        }

        return request;
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
        System.out.println("Running Crawl!");
        status = CrawlStatus.RUNNING;
        try {
            CrawlRequest webList = createListRequest();
            webList.exec();
            int pageCount = Integer.parseInt(webList.attr("pagecount"));
            for(int i=0; i<pageCount; i++) {
                CrawlRequest webPage = createPageRequest();
                webPage.attr("pageid", i+"");
                webPage.exec();
                String[] items = webPage.select();
                for( String item : items) {
                    CrawlRequest webItem = createItemRequest();
                    webItem.attr("itemid", item);
                    if (UrlDatabase.exist(webItem)) {
                        webItem.exec();
                        UrlDatabase.write(webItem);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            status = CrawlStatus.CLOSED;
        }
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
            CrawlRequest webQuery = createEntryRequest();
            webQuery.exec();
            String[] items = webQuery.select();
            for (String item : items) {
                CrawlRequest webItem = createItemRequest();
                webItem.attr("itemid", item);
                if (UrlDatabase.exist(webItem)) {
                    webItem.exec();
                    UrlDatabase.write(webItem);
                }
            }
        } catch (Exception e) {

        } finally {
            status = CrawlStatus.CLOSED;
        }
    }

}
