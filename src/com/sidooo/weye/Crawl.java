package com.sidooo.weye;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
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
    private int failCount = 0;               //爬取失败数量
    protected String webDomain;
    private long executeTick = 0;           //爬虫工作总时间
    private Thread thread = null;
    private boolean isEnabled = false;
    protected  DateTime lastRunTime = null;
    private CloseableHttpClient client = null;
    private HttpClientContext context = null;
    //内置变量
    protected Map<String, String> variables = new HashMap<String, String>();

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

        client = HttpClients.createDefault();
        context = HttpClientContext.create();
	}

    public static Crawl createInstance(Element conf) {
        if ("browse".equals(conf.nodeName())) {
            return new BrowseCrawl(conf);
        } else if ("query".equals(conf.nodeName())) {
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
        // 时间间隔24小时
        return minutes >= 1440;
    }

    protected String fetch(Element target) throws Exception {

        String httpMethod = null;
        if (target.hasAttr("method")) {
            httpMethod = target.attr("method");
        }

        String path = null;
        if (target.hasAttr("path")) {
            path = target.attr("path");
            if (variables.containsKey("pageid")) {
                path = path.replace("%pageid%", variables.get("pageid"));
            }

            if (variables.containsKey("itemid")) {
                path = path.replace("%itemid%", variables.get("itemid"));
            }
        }

        // prepare http request
        HttpRequestBase http = null;
        if ("get".equals(httpMethod)) {
            URL url = new URL(this.getHost());
            URIBuilder uri = new URIBuilder();
            uri.setScheme(url.getProtocol());
            uri.setHost(url.getHost());
            uri.setPort(url.getPort());
            uri.setPath(path);
            Elements strings = target.select("string");
            for (Element string : strings) {
                String paramName = string.attr("name");
                String paramValue = string.text();
                if (paramValue.charAt(0) == '$') {
                    paramValue = variables.get(paramValue.substring(1));
                }
                uri.setParameter(paramName, paramValue);
            }
            http = new HttpGet(uri.build());
        } else if ("post".equals(httpMethod)) {
            List<NameValuePair> form = new ArrayList<NameValuePair>();
            Elements params = target.select("form");
            for (Element param : params) {
                String paramName = param.attr("name");
                String paramValue = param.text();
                if (paramValue.charAt(0) == '$') {
                    paramValue = variables.get(paramValue.substring(1));
                }
                form.add(new BasicNameValuePair(paramName, paramValue));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
            HttpPost post = new HttpPost(getHost() + path);
            post.setEntity(entity);
            http = post;
        } else {
            throw new IllegalArgumentException("invalid method:" + httpMethod);
        }

//        if (referer != null) {
//            http.addHeader("Referer", referer);
//        }

//		HttpEntity entity = null;
//		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, "UTF-8");
        //httpMethod.addRequestHeader("ContentType","application/x-www-form-urlencoded;charset=GBK");

        //prepare http cookies
//        if (cookies.size() > 0) {
//            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();
//            http.setConfig(config);
//
//            Set<String> cookieNames = cookies.keySet();
//            //CookieStore cookieStore = new BasicCookieStore();
//            for(String cookieName:cookieNames) {
//                String cookieValue = cookies.get(cookieName);
//                BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);
//                // cookieStore.addCookie(cookie);
//            }
//            //client = HttpClients.custom().setDefaultCookieStore(cookieStore);
//            client = HttpClients.createDefault();
//        } else {
//            client = HttpClients.createDefault();
//        }

        Elements cookieItems = target.select("cookie");
        CookieStore cookieStore = context.getCookieStore();
        if (cookieStore != null) {
            for(Element cookieItem : cookieItems) {
                String cookieName = cookieItem.attr("name");
                cookieName = URLEncoder.encode(cookieName, "utf-8");
                String cookieValue = cookieItem.text();
                cookieValue = URLEncoder.encode(cookieValue, "utf-8");
                BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);
                cookieStore.addCookie(cookie);
            }
        }

        CloseableHttpResponse response = null;
        try {

            response = client.execute(http, context);
            int retCode = response.getStatusLine().getStatusCode();
            if (retCode >= 300) {
                logger.warn(response.getStatusLine().getReasonPhrase());
                return null;
            }



            HttpEntity entity = response.getEntity();
            if (entity == null) {
                logger.warn("Response contains no content.");
                return null;
            }

            ContentType contentType = ContentType.get(entity);
            Charset charset = contentType.getCharset();
            if (charset == null) {
                charset = Charset.defaultCharset();
            }

            //解决中文乱码
            InputStreamReader isr = new InputStreamReader(entity.getContent(), charset);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line+"\r\n");
            }
            return new String(out.toString().getBytes(), "UTF-8");


            //this.html = new String(this.html.getBytes(charset), "UTF-8");

//            cookies.clear();
//            HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator("Set-Cookie"));
//            while(it.hasNext()) {
//                HeaderElement element = it.nextElement();
//                cookies.put(element.getName(), element.getValue());
//            }

        } finally {
            if (response != null)
                response.close();
        }
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

    private void login() throws Exception {
        Element target = conf.select("login").first();
        fetch(target);
    }

    private WebList getList() throws Exception {
        Element target = conf.select("list").first();
        String html = fetch(target);
        return new WebList(target, html);
    }

    private WebPage getPage(int pageId) throws Exception  {
        variables.put("pageid",Integer.toString(pageId));
        Element target = conf.select("page").first();
        String html = fetch(target);
        WebPage page = new WebPage(html);
        Elements selectors = target.select("select");
        for(Element selector : selectors) {

            String selectMethod = selector.attr("method");
            String selectKey = selector.attr("key");
            String selectIndex = null;
            if (selector.hasAttr("index")) {
                selectIndex = selector.attr("index");
            }

            String selectAttribute = null;
            if (selector.hasAttr("attribute")) {
                selectAttribute = selector.attr("attribute");
            }

            page.addSelector(selectMethod, selectKey, selectIndex, selectAttribute);
        }
        return page;
    }

    private WebItem getItem(String itemId) throws Exception {
        variables.put("itemid", itemId);
        Element target = conf.select("item").first();
        String html = fetch(target);
        return new WebItem(html);
    }

    private void saveItem(WebItem item) throws Exception {
        Element target = conf.select("item").first();

        //replace variables
        String text = target.toString();
        text.replaceAll("\t", "");
        text.replaceAll("\n", "");
        text.replaceAll("\r", "");
        if (variables.containsKey("pageid")) {
            text = text.replace("%pageid%", variables.get("pageid"));
            text = text.replace("$pageid", variables.get("pageid"));
        }

        if (variables.containsKey("itemid")) {
            text = text.replace("%itemid%", variables.get("itemid"));
            text = text.replace("$itemid", variables.get("itemid"));
        }

        UrlDatabase.write(this.getId(), text, item);
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

            try {
                login();
                logger.info("Web Login Succeed");
            } catch (Exception e) {
                logger.fatal("Web Login Fail.", e);
                status = CrawlStatus.CLOSED;
                return;
            }

            //获取列表入口， 计算出总页数
            WebList list = null;
            try {
                list = getList();
            } catch (Exception e) {
                logger.fatal("Fetch Web List Error", e);
                status = CrawlStatus.CLOSED;
                return;
            }

            //分页获取
            int pageFailCount = 0;
            boolean needContinue = true;
            int pageCount = list.pageCount();
            logger.info("Web List has "+pageCount+" pages");
            for(int i = 1; i <= pageCount; i++) {

                if (!needContinue) {
                    break;
                }
                WebPage page = null;
                try {
                    page = getPage(i);
                    logger.info("Fetch Web Page "+i+" Succeed.");
                    pageFailCount = 0;
                } catch (Exception e) {
                    logger.warn("Fetch Web Page Error.", e);
                    pageFailCount += 1;
                    if (pageFailCount > 4) {
                        //如果连续5个页面获取失败， 则不再继续尝试获取剩余内容
                        break;
                    } else {
                        continue;
                    }
                }

                String[] itemids = page.items();
                for (String itemid : itemids) {

//                  try {
//                      DateTime now = new DateTime();
//                      DateTime fetchTime = UrlDatabase.exist(this.getId(), webItem);
//                      if (fetchTime != null) {
//                          int hours = Hours.hoursBetween(fetchTime, now).getHours();
//                          if (hours < 72) {
//                              //距离上次采集时间未超过72小时， 不进行更新
//                              //列表后面的内容也不需要继续获取了
//                              needContinue = false;
//                              break;
//                          }
//                      }
//                  } catch (Exception e) {
//                      logger.warn("Get Web Item Last update time failed.");
//                      logger.warn(e.getStackTrace().toString());
//                  }

                    WebItem item = null;
                    try {
                        item = getItem(itemid);
                    } catch (Exception e) {
                        logger.warn("Get Web Item Fail.", e);
                        continue;
                    }

                    try {
                        saveItem(item);
                    } catch (Exception e) {
                        logger.warn("Save Web Item error.", e);
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
