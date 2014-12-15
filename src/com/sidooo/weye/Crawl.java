package com.sidooo.weye;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayway.jsonpath.JsonPath;
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
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
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
    protected CloseableHttpClient client = null;
    protected HttpClientContext context = null;
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

    protected String replaceVariables(String input) {
        String output = input;
        Set<String> varNames = variables.keySet();
        for(String varName : varNames) {
            output = output.replace("%"+varName+"%", variables.get(varName));
        }

        CookieStore store = this.context.getCookieStore();
        if (store != null) {
            List<Cookie> cookies = store.getCookies();
            for(Cookie cookie : cookies) {
                String name = cookie.getName();
                String value = cookie.getValue();
                output = output.replace("$"+name+"$", value);
            }
        }

        return output;
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

    protected void reset() {
        client = HttpClients.createDefault();
        context = HttpClientContext.create();
    }

    protected String fetch(Element target) throws Exception {

        String httpMethod = null;
        if (target.hasAttr("method")) {
            httpMethod = target.attr("method");
        }

        String path = null;
        if (target.hasAttr("path")) {
            path = target.attr("path");
            path = replaceVariables(path);
        }

        // prepare http request
        HttpRequestBase http = null;
        if ("get".equals(httpMethod)) {
            URIBuilder uri = new URIBuilder();
            if (path.charAt(0) != '/') {
                //绝对路径
                URL url = new URL(path);
                uri.setScheme(url.getProtocol());
                uri.setHost(url.getHost());
                uri.setPort(url.getPort());
                uri.setPath(url.getPath());
            } else {
                URL url = new URL(this.getHost());
                uri.setScheme(url.getProtocol());
                uri.setHost(url.getHost());
                uri.setPort(url.getPort());
                uri.setPath(path);
            }

            Elements strings = target.select("string");
            for (Element string : strings) {
                String paramName = string.attr("name");
                String paramValue = string.ownText();
                if (paramValue.length()>0) {
                        paramValue = replaceVariables(paramValue);
                } else {
                    //没有参数的具体值, 可能是动态值
                    Elements scripts = string.select("javascript");
                    for(Element script : scripts ) {
                        String jsFileName = script.attr("file");
                        String jsText = script.text();
                        jsText = replaceVariables(jsText);
                        ScriptEngineManager sem = new ScriptEngineManager();
                        ScriptEngine se = sem.getEngineByName("javascript");
                        se.eval(new FileReader(new File("js/"+jsFileName)));

                        Object t = se.eval(jsText);
                        paramValue = t.toString();
                    }

                }
                uri.setParameter(paramName, paramValue);
            }
            http = new HttpGet(uri.build());
        } else if ("post".equals(httpMethod)) {
            List<NameValuePair> form = new ArrayList<NameValuePair>();
            Elements params = target.select("form");
            for (Element param : params) {
                String paramName = param.attr("name");
                String paramValue = param.ownText();
                if (paramValue.length()>0) {
                    paramValue = replaceVariables(paramValue);
                } else {
                    //没有参数的具体值, 可能是动态值
                    Elements scripts = param.select("javascript");
                    for(Element script : scripts ) {
                        String jsFileName = script.attr("file");
                        String jsText = script.text();
                        jsText = replaceVariables(jsText);
                        ScriptEngineManager sem = new ScriptEngineManager();
                        ScriptEngine se = sem.getEngineByName("javascript");
                        se.eval(new FileReader(new File("js/"+jsFileName)));

                        Object t = se.eval(jsText);
                        paramValue = t.toString();
                    }
                }
                paramValue = URLEncoder.encode(paramValue, "utf-8");
                form.add(new BasicNameValuePair(paramName, paramValue));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
            HttpPost post = null;
            if (path.charAt(0) != '/') {
                post = new HttpPost(path);
            } else {
                post = new HttpPost(getHost() + path);
            }
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
                throw new HttpException("RetCode = "+retCode);
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                logger.warn("Response contains no content.");
                throw new NoHttpResponseException("Response contains no content.");
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

    protected String[] select(String[] input, String method, String key, Integer index, String attribute) {
        String[] matches = null;

        if (method == null) {
            return null;
        } else if ("jquery".equals(method)) {
            matches = jquery(input, key, index, attribute);
        } else if ("json".equals(method)){
            matches = json(input, key, index, attribute);
        } else if ("regular".equals(method)) {
            matches = regular(input, key, index, attribute);
        } else if ("xml".equals(method)) {
            matches = xml(input, key, index, attribute);
        } else {
            return null;
        }

        return matches;
    }

    private String[] xml(String[] texts, String key, Integer index, String attribute) {

        List<String> result = new ArrayList<String>();

        for(String text : texts)  {
            Document doc = Jsoup.parse(text, "", Parser.xmlParser());
            Elements elements = doc.select(key);
            if (elements == null || elements.size() <= 0) {
                continue;
            }

            if (index != null) {
                Element element = elements.get(index.intValue());
                if (element == null) {
                    continue;
                }

                if (attribute != null) {

                    if ("text".equals(attribute)) {
                        result.add(element.text());
                    } else {
                        result.add(element.attr(attribute));
                    }
                } else {

                    result.add(element.html());
                }
            } else {
                for(Element element : elements) {
                    if (attribute != null) {
                        result.add(element.attr(attribute));
                    } else {
                        result.add(element.outerHtml());
                    }
                }
            }
        }

        return result.toArray(new String[0]);
    }

    private String[] jquery(String[] texts, String key, Integer index, String attribute) {

        List<String> result = new ArrayList<String>();

        for(String text:texts) {
            Document doc = Jsoup.parse(text);

            Elements elements = null;
            if (key == null) {
                elements = doc.children();
            } else {
                elements = doc.select(key);
            }

            if (elements == null || elements.size() <= 0) {
                continue;
            }


            if (index != null) {
                Element element = elements.get(index.intValue());
                if (element == null) {
                    continue;
                }

                if (attribute != null) {
                    if ("text".equals(attribute)) {
                        result.add(element.text());
                    } else {
                        result.add(element.attr(attribute));
                    }
                } else {
                    result.add(element.outerHtml());
                }
            } else {
                for(Element element : elements) {
                    if (attribute != null) {
                        if ("text".equals(attribute)) {
                            result.add(element.text());
                        } else {
                            result.add(element.attr(attribute));
                        }

                    } else {
                        result.add(element.outerHtml());
                    }
                }
            }
        }

        return  new HashSet<String>(result).toArray(new String[0]);
    }

    private String[] json(String[] texts, String key, Integer index, String attribute)	{

        List<String> result = new ArrayList<String>();

        for(String text:texts) {
            result = JsonPath.read(text, key);
        }
        return result.toArray(new String[0]);
    }

    private String[] regular(String[] texts, String key, Integer index, String attribute) {

        List<String> result = new ArrayList<String>();

        for (String text : texts) {
            Pattern pattern = Pattern.compile(key);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                if (index != null) {
                    if (index.intValue() > (matcher.groupCount() - 1) ) {
                        continue;
                    }
                    result.add(matcher.group(index.intValue()+1));
                } else {
                    int groupCount = matcher.groupCount();
                    for (int i=1; i<=groupCount; i++) {
                        result.add(matcher.group(i));
                    }
                }
            } else {
                continue;
            }
        }

        return result.toArray(new String[0]);
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

    private void account() throws Exception {
        Element account = conf.select("account").first();
        if (account != null) {
            String username = account.attr("username");
            String password = account.attr("password");
            variables.put("username", username);
            variables.put("password", password);
        }
    }

    private boolean login() throws Exception {
        Elements targets = conf.select("login");
        for (Element target : targets) {
            String response = fetch(target);
            Elements selectors = target.select("select");
            String[] results = new String[]{response};
            for(Element selector : selectors) {

                String selectMethod = selector.attr("method");
                String selectKey = null;
                if (selector.hasAttr("key")) {
                    selectKey = selector.attr("key");
                } else {
                    selectKey = selector.text();
                }

                Integer selectIndex = null;
                if (selector.hasAttr("index")) {
                    selectIndex = Integer.parseInt(selector.attr("index"));
                }

                String selectAttribute = null;
                if (selector.hasAttr("attribute")) {
                    selectAttribute = selector.attr("attribute");
                }

                results = select(results, selectMethod, selectKey, selectIndex, selectAttribute);
                if (selector.hasAttr("name")) {
                    String selectName = selector.attr("name");
                    if (results.length > 1) {
                        for(int i=0; i<results.length; i++) {
                            variables.put(selectName+"["+i+"]", results[i]);
                        }
                    } else if (results.length == 1) {
                        variables.put(selectName, results[0]);
                    } else {

                    }
                }
            }

            Elements checks = target.select("check");
            boolean checkResult = true;
            for(Element check : checks) {
                String varName = check.attr("var");
                if (!variables.containsKey(varName)) {
                    checkResult = false;
                    break;
                }

                String actureValue = variables.get(varName);
                String expectValue = check.text();
                if (!actureValue.equals(expectValue)) {
                    checkResult = false;
                    break;
                }
            }

            if (!checkResult) {
                return false;
            }
        }

        return true;
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
        if (selectors.size() > 0) {
            String[] results = new String[]{html};
            for(Element selector : selectors) {

                String selectMethod = selector.attr("method");
                String selectKey = selector.attr("key");
                Integer selectIndex = null;
                if (selector.hasAttr("index")) {
                    selectIndex = Integer.parseInt(selector.attr("index"));
                }

                String selectAttribute = null;
                if (selector.hasAttr("attribute")) {
                    selectAttribute = selector.attr("attribute");
                }

                results = select(results, selectMethod, selectKey, selectIndex, selectAttribute);
            }
            page.addItems(results);
        }

        return page;
    }

    private void checkPagePeriod() throws Exception {
        Element target = conf.select("page").first();
        if (target.hasAttr("period")) {
            Integer period = Integer.parseInt(target.attr("period"));
            Thread.sleep(period * 1000);
        }
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
        text = text.replaceAll("[\\\t\\\n\\\r]", "");
        //text.replaceAll("\\r\\n", "");
        text = replaceVariables(text);

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

            reset();

            try {
                account();
            } catch (Exception e) {
                logger.fatal("Get Account Error.", e);
                status = CrawlStatus.CLOSED;
                return;
            }

            try {
                if (login()) {
                    logger.info("Web Login Succeed");
                } else {
                    logger.info("Web Login Fail.");
                    break;
                }
            } catch (Exception e) {
                logger.fatal("Web Login Error.", e);
                status = CrawlStatus.CLOSED;
                return;
            }

            //获取列表入口， 计算出总页数
            WebList list = null;
            int listFailCount = 0;
            while (listFailCount <= 3) {
                try {
                    list = getList();
                    logger.info("Get Web List Succeed.");
                    break;
                } catch (SocketTimeoutException e) {
                    listFailCount++;
                    logger.warn("Get Web List Timeout, Retry "+listFailCount);
                    //暂停10秒
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e2) {
                        logger.warn("Sleep Fail", e2);
                    }
                    continue;
                } catch (Exception e) {
                    logger.fatal("Fetch Web List Error", e);
                    status = CrawlStatus.CLOSED;
                    return;
                }
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

                if (i>1) {
                    //检查页面获取间隔时间
                    try {
                        checkPagePeriod();
                    } catch (Exception e) {
                        logger.warn("Check Period Error", e);
                    }
                }

                WebPage page = null;
                try {
                    page = getPage(i);
                    logger.info("Fetch Web Page "+i+" Succeed.");
                    pageFailCount = 0;
                } catch (SocketTimeoutException e) {
                    try {
                        page = getPage(i);
                        logger.info("Retry Fetch Web Page "+i+" Succeed.");
                        pageFailCount = 0;
                    } catch (Exception e2) {
                        logger.warn("Retry Fetch Web Page Error.", e2);
                        pageFailCount += 1;
                        if (pageFailCount > 4) {
                            //如果连续5个页面获取失败， 则不再继续尝试获取剩余内容
                            break;
                        } else {
                            continue;
                        }
                    }

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
                        logger.info("Get Web Item Succeed, Item: " + itemid);
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
