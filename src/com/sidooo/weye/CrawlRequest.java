package com.sidooo.weye;

import com.sun.xml.internal.bind.v2.runtime.RuntimeUtil;
import net.sf.json.JSONObject;
import org.apache.http.Consts;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.NameValuePair;
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
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WebSelect {
    private String method;
    private String key;
    private String index;
    private String attribute;

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

    public String getIndex() {
        return index;
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

    private String httpMehtod = "";

    private String host = "";
    private String path = "";
    private String log = "";
    //返回的HTML文本
    private String html = "";
    // GET方式填充的参数
    private Map<String, String> stringParams = new HashMap<String, String>();
    private Map<String, String> queryParams = new HashMap<String,String>();
    // Form参数
    private Map<String, String> formParams = new HashMap<String,String>();
    private List<WebSelect> webSelects = new ArrayList<WebSelect>();

    private Map<String,String> cookies = new HashMap<String,String>();
//    //内置变量
    private Map<String, String> variables = new HashMap<String, String>();
    private String referer = "";

    public  CrawlRequest() {
        System.out.println("CrawlRequest inited");
    }


    public CrawlRequest(String stream) {

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
        return log;
    }

    public void addSelect(String method, String key, String index, String attribute) {
        WebSelect select = new WebSelect(method, key, index, attribute);
//        this.webSelects.add(select);
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String attr(String name) {
//        return variables.get(name);
        return "";
    }

    public void attr(String name, String value) {
//        variables.put(name, value);
    }

    public void addFormParameter(String name, String value) {
//        formParams.put(name, value);
    }

    public void addQueryParameter(String name, String value) {
//        queryParams.put(name, value);
    }

    public void addStringParameter(String name, String value) {
//        stringParams.put(name, value);
    }

    public String getMd5(){
        try {
            MessageDigest hash = MessageDigest.getInstance("MD5");
            byte[] array = hash.digest(host.toLowerCase().getBytes());
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

        return result.toArray(new String[0]);
    }

    private String[] json(String[] texts, String key, Integer index, String attribute)	{

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
    public String[] select() {

        String[] matches = new String[]{this.html};

//        for(WebSelect select : webSelects) {
//
//            String method = select.getMethod();
//            String key = select.getKey();
//            Integer index = Integer.parseInt(select.getIndex());
//            String attribute = select.getAttribute();
//
//            if (method == null) {
//                return null;
//            } else if ("jquery".equals(method)) {
//                matches = jquery(matches, key, index, attribute);
//            } else if ("json".equals(method)){
//                matches = json(matches, key, index, attribute);
//            } else if ("regular".equals(method)) {
//                matches = regular(matches, key, index, attribute);
//            } else if ("xml".equals(method)) {
//                matches = xml(matches, key, index, attribute);
//            } else {
//                return null;
//            }
//        }

        return matches;
    }

    public void exec() throws Exception {

        // prepare http request
        HttpRequestBase http = null;
        if ("get".equals(httpMehtod)) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http");
            uri.setHost(host);
            uri.setPath(path);
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
            HttpPost post = new HttpPost(host+path);
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
            if (retCode != 200) {
                return;
            }
            this.html = EntityUtils.toString(response.getEntity());
            cookies.clear();
            HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator("Set-Cookie"));
            while(it.hasNext()) {
                HeaderElement element = it.nextElement();
                cookies.put(element.getName(), element.getValue());
            }
        } catch(Exception e) {
            return;
        } finally {
            if (response != null)
                response.close();
        }

    }
}