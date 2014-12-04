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
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static List<Crawl> crawlList = new ArrayList<Crawl>();
    private static List<WebHost> webList = new ArrayList<WebHost>();

	public static void usage() {
		System.err.println("usage: weye.jar");
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
        } catch (Exception e) {
            logger.error("Load log4j Properties Failed.");
            logger.error(e.getStackTrace().toString());
            return;
        }

        try {
            UrlDatabase.init("10.1.1.2", 27017, "webdb" );
        } catch (Exception e) {
            logger.error("Connect Web Database Failed.");
            return;
        }

        logger.error("Load web.xml.");
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
                        String confType = conf.nodeName();
                        if ("browse".equals(confType)) {
                            Crawl crawl = Crawl.createInstance(CrawlType.BROWSE, conf);
                            crawlList.add(crawl);
                            new Thread(crawl).start();
                        } else if ("query".equals(confType)) {
                            Crawl crawl = Crawl.createInstance(CrawlType.QUERY, conf);
                            crawlList.add(crawl);
                            new Thread(crawl).start();
                        } else {

                        }
                    }
                }
            }

            while(true) {
                Thread.sleep(99999999);
            }
        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
        } finally {
            logger.warn("weye exit.");
        }


	}

}
