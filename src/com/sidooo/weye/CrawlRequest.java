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

    public String getHost() {
        return this.host;
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






}


class WebList
{
    private String html;

    public  WebList(String html) {
        this.html = html;
    }

    public int pageCount() {
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

class  WebPage
{
    private String html;
    private List<WebSelect> selectors;

    public WebPage(String html) {
        this.html = html;
    }

    public  void addSelector(String method, String key, String index, String attribute) {
        WebSelect selector = new WebSelect(method, key, index, attribute);
        selectors.add(selector);
    }

    public String[] items() {

        String[] matches = new String[]{html};

        for(WebSelect select : selectors) {

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


}

class WebItem
{
    private String html;
    private DateTime date;

    public WebItem(String html) {
        this.html = html;
        this.date = new DateTime();
    }

    public Date date() {
        return new Date(date.getMillis());
    }

    public String html() {
        return this.html;
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


}