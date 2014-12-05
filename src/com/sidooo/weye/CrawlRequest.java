package com.sidooo.weye;

import net.sf.json.JSONObject;
import org.apache.http.*;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import sun.misc.IOUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WebSelect {
    private String method = null;
    private String key = null;
    private String index = null;
    private String attribute = null;

    public WebSelect(String method, String key,String index, String attribute) {
        this.method = method;
        this.key = key;
        this.index = index;
        this.attribute = attribute;
    }

    public String getMethod() {
        return method;
    }

    public String getKey() {
        return key;
    }

    public Integer getIndex() {
        if (index != null) {
            return Integer.parseInt(index);
        } else {
            return null;
        }
    }

    public String getAttribute() {
        return attribute;
    }
}

/**
 * Created with IntelliJ IDEA.
 * User: kimzhang
 * Date: 14-11-17
 * Time: 下午2:03
 * To change this template use File | Settings | File Templates.
 */
public class CrawlRequest {

    protected String httpMehtod = "";

    protected String host = "";
    protected String path = "";
    private StringWriter stream = new StringWriter();
    public Logger logger;
    //返回的HTML文本
    protected String html = "";

    // GET方式填充的参数
    protected Map<String, String> stringParams = new HashMap<String, String>();
    protected Map<String, String> queryParams = new HashMap<String,String>();
    // Form参数
    protected Map<String, String> formParams = new HashMap<String,String>();
    protected List<WebSelect> webSelects = new ArrayList<WebSelect>();

    protected Map<String,String> cookies = new HashMap<String,String>();
    //内置变量
    protected Map<String, String> variables = new HashMap<String, String>();
    protected String referer = null;

    protected String name = null;

    private DateTime date = new DateTime();

    public  CrawlRequest() {
        UUID requestId = UUID.randomUUID();
        logger = Logger.getLogger(String.valueOf(requestId));
        WriterAppender appender = new WriterAppender(new SimpleLayout(), stream);
        appender.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5.5p %m%n"));
        logger.addAppender(appender);
    }

    public void setHttpMethod(String method) {
        this.httpMehtod = method;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public  void setPath(String path) {
        this.path = path;
    }

    public String getHtml() {
        return html;
    }

    public String getLog() {
        return stream.getBuffer().toString();
    }

    public String getPath() {
        String ret = path;
        if (variables.containsKey("pageid")) {
            ret = ret.replace("%pageid%", variables.get("pageid"));
        }

        if (variables.containsKey("itemid")) {
            ret = ret.replace("%itemid%", variables.get("itemid"));
        }

        return ret;
    }

    public String getUrl() {
        return host + getPath();
    }

    public DateTime getDate() {
        return this.date;
    }

    public  Map<String,String> getStringParams() {
        Map<String,String> result = new HashMap<String, String>();
        Set<String> keys = stringParams.keySet();
        for (String key : keys) {
            String value = stringParams.get(key);
            if (value.charAt(0) == '$') {
                value = variables.get(value.substring(1));
            }
            String text = key +"=" + value;
            result.put(key, value);
        }
        return result;
    }

    public Map<String, String> getQueryParams() {
        Map<String,String> result = new HashMap<String, String>();
        Set<String> keys = queryParams.keySet();
        for (String key : keys) {
            String value = queryParams.get(key);
            if (value.charAt(0) == '$') {
                value = variables.get(value.substring(1));
            }
            String text = key +"=" + value;
            result.put(key, value);
        }
        return result;
    }

    public Map<String,String> getFormParams() {
        Map<String,String> result = new HashMap<String, String>();
        Set<String> keys = formParams.keySet();
        for (String key : keys) {
            String value = formParams.get(key);
            if (value.charAt(0) == '$') {
                value = variables.get(value.substring(1));
            }
            String text = key +"=" + value;
            result.put(key, value);
        }
        return result;
    }

    public void addSelect(String method, String key, String index, String attribute) {
        WebSelect select = new WebSelect(method, key, index, attribute);
        this.webSelects.add(select);
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String attr(String name) {
        return variables.get(name);
    }

    public void attr(String name, String value) {
        variables.put(name, value);
    }

    public void addFormParameter(String name, String value) {
        formParams.put(name, value);
    }

    public void addQueryParameter(String name, String value) {
        queryParams.put(name, value);
    }

    public void addStringParameter(String name, String value) {
        stringParams.put(name, value);
    }

    public String getMd5(){
        try {
            MessageDigest hash = MessageDigest.getInstance("MD5");
            hash.update(host.toLowerCase().getBytes());
            hash.update(getPath().toLowerCase().getBytes());
            Set<String> keys = stringParams.keySet();
            for (String key : keys) {
                String value = stringParams.get(key);
                if (value.charAt(0) == '$') {
                    value = variables.get(value.substring(1));
                }
                String text = key +"=" + value;
                hash.update(text.getBytes());
            }
            keys = formParams.keySet();
            for(String key : keys) {
                String value = formParams.get(key);
                if (value.charAt(0) == '$') {
                    value = variables.get(value.substring(1));
                }
                String text = key +"=" + value;
                hash.update(text.getBytes());
            }
            keys = queryParams.keySet();
            for (String key: keys) {
                String value = queryParams.get(key);
                if (value.charAt(0) == '$') {
                    value = variables.get(value.substring(1));
                }
                String text = key +"=" + value;
                hash.update(text.getBytes());
            }

            byte[] array = hash.digest();
            StringBuffer buffer = new StringBuffer();
            for(int i=0; i < array.length; i++)
            {
                buffer.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return buffer.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    protected String[] xml(String[] texts, String key, Integer index, String attribute) {

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

    protected String[] jquery(String[] texts, String key, Integer index, String attribute) {

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

    protected String[] json(String[] texts, String key, Integer index, String attribute)	{

        List<String> result = new ArrayList<String>();

        for(String text:texts) {
            JSONObject json = JSONObject.fromObject(text);
            if (".".equals(key)) {
                result.add(text);
            } else {
                String value = json.getString(key);
                result.add(value);
            }
        }
        return result.toArray(new String[0]);
    }

    protected String[] regular(String[] texts, String key, Integer index, String attribute) {

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

    protected boolean fetch() throws Exception {

        // prepare http request
        HttpRequestBase http = null;
        if ("get".equals(httpMehtod)) {
            URL url = new URL(host);
            URIBuilder uri = new URIBuilder();
            uri.setScheme(url.getProtocol());
            uri.setHost(url.getHost());
            uri.setPort(url.getPort());
            uri.setPath(getPath());
            Set<String> paramNames = stringParams.keySet();
            for (String paramName:paramNames) {
                String paramValue = stringParams.get(paramName);
                if (paramValue.charAt(0) == '$') {
                    paramValue = variables.get(paramValue.substring(1));
                }
                uri.setParameter(paramName, paramValue);
            }
            http = new HttpGet(uri.build());
        } else if ("post".equals(httpMehtod)) {
            List<NameValuePair> form = new ArrayList<NameValuePair>();
            Set<String> paramNames = formParams.keySet();
            for (String paramName:paramNames) {
                String paramValue = formParams.get(paramName);
                if (paramValue.charAt(0) == '$') {
                    paramValue = variables.get(paramValue.substring(1));
                }
                form.add(new BasicNameValuePair(paramName, paramValue));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
            HttpPost post = new HttpPost(getUrl());
            post.setEntity(entity);
            http = post;
        } else {
            throw new IllegalArgumentException("invalid method:" + httpMehtod);
        }

        if (referer != null) {
            http.addHeader("Referer", referer);
        }

//		HttpEntity entity = null;
//		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, "UTF-8");
        //httpMethod.addRequestHeader("ContentType","application/x-www-form-urlencoded;charset=GBK");

        //prepare http cookies
        CloseableHttpClient client;
        if (cookies.size() > 0) {
            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();
            http.setConfig(config);

            Set<String> cookieNames = cookies.keySet();
            //CookieStore cookieStore = new BasicCookieStore();
            for(String cookieName:cookieNames) {
                String cookieValue = cookies.get(cookieName);
                BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);
               // cookieStore.addCookie(cookie);
            }
            //client = HttpClients.custom().setDefaultCookieStore(cookieStore);
            client = HttpClients.createDefault();
        } else {
            client = HttpClients.createDefault();
        }

        CloseableHttpResponse response = null;
        try {
            response = client.execute(http);
            int retCode = response.getStatusLine().getStatusCode();
            if (retCode >= 300) {
                logger.warn(response.getStatusLine().getReasonPhrase());
                return false;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                logger.warn("Response contains no content.");
                return false;
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
                out.append(line);
            }
            this.html = out.toString();


            //this.html = new String(this.html.getBytes(charset), "UTF-8");

            cookies.clear();
            HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator("Set-Cookie"));
            while(it.hasNext()) {
                HeaderElement element = it.nextElement();
                cookies.put(element.getName(), element.getValue());
            }


        } catch(Exception e) {
            logger.fatal(e.getStackTrace().toString());
            return false;
        } finally {
            if (response != null)
                response.close();
        }
        logger.info("fetch Web Item succeed.");
        return true;

    }


}

class WebList extends CrawlRequest
{
    public static WebList createInstance(String host, Element conf) throws Exception{

        WebList request = new WebList();
        request.setHost(host);

        Element childNode = conf.select("list").first();
        request.logger.info(childNode.toString());
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

        request.logger.info("Parse Web List Configuration Succecced.");

        return request;
    }

    public int exec() {

        try {
            if (fetch() ) {
                // 分析页面， 获取页面总数
                int pageCount = analysisPageCount(this.html);
                this.attr("pagecount", Integer.toString(pageCount));
                return pageCount;
            } else {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }


    }

    private int analysisPageCount(String html) {
        Document doc = Jsoup.parse(html);
        Element element = doc.getElementsContainingOwnText("尾页").first();
        if (element != null ) {
            //存在“尾页”的标签
            if (element.hasAttr("onclick")) {
                //<a href="javascript:void(0);" onclick="gotoPage(29170)">尾页</a>
                String onclick = element.attr("onclick");
                Pattern pattern = Pattern.compile("^\\w+\\((\\d+)\\)",Pattern.CASE_INSENSITIVE);
                Matcher match = pattern.matcher(onclick);
                if (match.find()) {
                    String text = match.group(1);
                    int pageCount = Integer.parseInt(text);
                    return pageCount;
                }
            }

            if (element.hasAttr("href"))  {
                //<a href="index_7654.htm">尾页</a>
                String href = element.attr("href");
                Pattern pattern = Pattern.compile("^index_(\\d+).htm$", Pattern.CASE_INSENSITIVE);
                Matcher match = pattern.matcher(href);
                if (match.find()) {
                    String text = match.group(1);
                    int pageCount = Integer.parseInt(text);
                    return pageCount;
                }
            }

        }  else {

            Elements elements = doc.select("script");
            for( Element script : elements) {

                //<script>createPageHTML(7655,1,"index","htm");</script>
                String text = script.html();
                if (text.indexOf("createPageHTML")>= 0 ) {
                    String[] items = text.split("\\W");
                    int maxValue = 0;
                    for(String item : items) {
                        int value = 0;
                        try {
                            value = Integer.parseInt(item);
                        } catch (Exception e){
                            value = 0;
                        }

                        if (value > maxValue) {
                            maxValue = value;
                        }
                    }

                    if (maxValue > 0) {
                        return maxValue;
                    }
                }
            }

        }

        return 0;

    }

}

class  WebPage extends  CrawlRequest
{

    public static WebPage createInstance(String host, Element conf) {

        WebPage request = new WebPage();

        request.setHost(host);

        Element childNode = conf.select("page").first();
        request.logger.info(childNode.toString());

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
                String selectIndex = null;
                if (child.hasAttr("index")) {
                    selectIndex = child.attr("index");
                }

                String selectAttribute = null;
                if (child.hasAttr("attribute")) {
                    selectAttribute = child.attr("attribute");
                }
                request.addSelect(selectMethod, selectKey, selectIndex, selectAttribute);
            }
        }
        request.logger.info("Parse Web Page Configuration Succecced.");
        return request;
    }

    public  String[] exec(int pageId) {
        try {
            this.attr("pageid", Integer.toString(pageId));
            if (fetch() ) {
                return select(this.html);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String[] select(String html) {

        String[] matches = new String[]{html};

        for(WebSelect select : webSelects) {

            String method = select.getMethod();
            String key = select.getKey();
            Integer index = select.getIndex();
            String attribute = select.getAttribute();

            if (method == null) {
                return null;
            } else if ("jquery".equals(method)) {
                matches = jquery(matches, key, index, attribute);
            } else if ("json".equals(method)){
                matches = json(matches, key, index, attribute);
            } else if ("regular".equals(method)) {
                matches = regular(matches, key, index, attribute);
            } else if ("xml".equals(method)) {
                matches = xml(matches, key, index, attribute);
            } else {
                return null;
            }
        }

        return matches;
    }

}

class WebItem extends  CrawlRequest
{
    public static WebItem createInstance(String host, Element conf) throws Exception {

        WebItem request = new WebItem();

        request.setHost(host);

        Element childNode = conf.select("item").first();
        request.logger.info(childNode.toString());
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

        request.logger.info("Parse Web Item Configuration Succecced.");
        return request;
    }

    public void exec(String itemId) {
        try {
            logger.info("itemid = "+itemId);
            this.attr("itemid", itemId);
            fetch();
        } catch (Exception e) {
            return;
        }
    }
}

class WebEntry extends CrawlRequest
{

    public static WebEntry createInstance(String host, Element conf) throws Exception{

        WebEntry request = new WebEntry();

        request.setHost(host);

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

    public String[] exec() {
        try {
            fetch();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}