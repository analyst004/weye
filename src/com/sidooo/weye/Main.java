package com.sidooo.weye;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static List<Crawl> crawlList = new ArrayList<Crawl>();
    private static List<WebHost> webList = new ArrayList<WebHost>();

	public static void usage() {
		System.err.println("usage: weye.jar");
	}

    private static void testLogDatabase() throws Exception {

        //驱动名称
        String driver = "com.mysql.jdbc.Driver";

        // URL指向要访问的数据库名scutcs
        String url = "jdbc:mysql://10.1.1.2:3306/logdb";

        // MySQL配置时的用户名
        String user = "root";

        // Java连接MySQL配置时的密码
        String password = "";

        Connection conn = null;
        ResultSet rs = null;
        try {
            // 加载驱动程序
            Class.forName(driver);

            // 连续数据库
            conn = DriverManager.getConnection(url, user, password);

            if(!conn.isClosed())  {

                // statement用来执行SQL语句
                Statement statement = conn.createStatement();

                // 要执行的SQL语句
                String sql = "select COUNT(*) from loginfo";

                rs = statement.executeQuery(sql);

                String name = null;
                if (rs.next()) {
                    long logcount = rs.getLong(1);
                }
            }
        } finally {

            if (rs != null) {
                rs.close();
            }

            if (conn != null) {
                conn.close();
            }
        }
    }

	
	public static void main(String[] args) {

        Logger logger = Logger.getRootLogger();

        String xmlPath = null;
        String serverXmlPath = null;
        String logXmlPath = null;

        String os = System.getProperty ("os.name");
        if (os.indexOf("Win") >= 0) {
            xmlPath = "conf/web.xml";
            serverXmlPath = "conf/server.xml";
            logXmlPath = "conf/log4j.properties";
            System.setProperty("LogPath", "log");
        } else {
            xmlPath = "/etc/weye/web.xml";
            serverXmlPath = "/etc/weye/server.xml";
            logXmlPath = "/etc/weye/log4j.properties";
            System.setProperty("LogPath", "/var/log");
        }
        File xmlFile = new File(xmlPath);
        if (!xmlFile.exists() || xmlFile.isDirectory()) {
            logger.error("Can't find web.xml, File not found");
            return;
        }



        try {
            PropertyConfigurator.configure(logXmlPath);
            logger.info("Load log4j Properties Succeed.");
        } catch (Exception e) {
            logger.error("Load log4j Properties Failed." +  e.toString());
            return;
        }


        File serverXmlFile = new File(serverXmlPath);
        if (!serverXmlFile.exists() || serverXmlFile.isDirectory()) {
            logger.error("Can't find server.xml, File not found.");
            return;
        }

        try {
            testLogDatabase();
        } catch (Exception e) {
            logger.error("Connect Log Database Fail." + e.toString());
            return;
        }

        try {
            Document doc = Jsoup.parse(serverXmlFile, "UTF-8");
            Element server = doc.select("server").first();
            String ip = server.attr("ip");
            String port = server.attr("port");
            String dbname = server.attr("database");
            UrlDatabase.init(ip, Integer.parseInt(port), dbname);
        } catch (Exception e) {
            logger.error("Connect Web Database Failed:" + e.toString());
            return;
        }

        logger.info("Load web.xml.");
        try {
            Document doc = Jsoup.parse(xmlFile,"UTF-8");

            Elements webs = doc.select("web");
            for (Element web : webs) {
                String nodeName = web.nodeName();
                if ("web".equals(nodeName)) {
                    String name = web.attr("name");
                    String host = web.attr("host");
                    webList.add(new WebHost(name, host));

                    Elements confs = web.children();
                    for (Element conf : confs) {
                        if (!conf.hasAttr("id")) {
                            continue;
                        }

                        String confType = conf.nodeName();
                        if ("browse".equals(confType)) {
                            Crawl crawl = Crawl.createInstance(conf);
                            crawlList.add(crawl);
                            new Thread(crawl).start();
                        } else if ("query".equals(confType)) {
//                            Crawl crawl = Crawl.createInstance(CrawlType.QUERY, conf);
//                            crawlList.add(crawl);
//                            new Thread(crawl).start();
                        } else {
                            continue;
                        }
                    }
                }
            }

            while(true) {
                Thread.sleep(99999999);
            }
        } catch (Exception e) {
            logger.error("weye error:" + e.toString());
        } finally {
            logger.warn("weye exit.");
        }


	}

}
