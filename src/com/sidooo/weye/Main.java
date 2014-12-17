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
                System.out.println("-----------------");
                System.out.println("执行结果如下所示:");
                System.out.println("-----------------");
                System.out.println(" 学号" + "\t" + " 姓名");
                System.out.println("-----------------");
                String name = null;
                if (rs.next()) {
                    int logcount = rs.getInt(0);
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

        String xmlPath = "/etc/weye/web.xml";
        File xmlFile = new File(xmlPath);
        if (!xmlFile.exists() || xmlFile.isDirectory()) {
            logger.error("Can't find web.xml");
            return;
        }



        try {
            PropertyConfigurator.configure("/etc/weye/log4j.properties");
            logger.info("Load log4j Properties Succeed.");
        } catch (Exception e) {
            logger.error("Load log4j Properties Failed.", e);
            return;
        }

        String serverXmlPath = "/etc/weye/server.xml";
        File serverXmlFile = new File(serverXmlPath);
        if (!serverXmlFile.exists() || serverXmlFile.isDirectory()) {
            logger.error("Can't find server.xml");
            return;
        }

        try {
            testLogDatabase();
        } catch (Exception e) {
            logger.error("Connect Log Database Fail.", e);
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
            logger.error("Connect Web Database Failed.", e);
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
            logger.error("weye error", e);
        } finally {
            logger.warn("weye exit.");
        }


	}

}
