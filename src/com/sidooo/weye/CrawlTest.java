package com.sidooo.weye;

import static org.junit.Assert.*;

import java.io.File;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CrawlTest { 

    private  Document doc;
	@Before
	public void setUp() throws Exception {
        UrlDatabase.init("10.1.1.5", 27017, "test" );
        File xmlFile = new File("conf/web.xml");
        doc = Jsoup.parse(xmlFile,"UTF-8");

        System.setProperty("LogPath", "log");
        PropertyConfigurator.configure("conf/log4j_for_test.properties");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test1() throws Exception {
        Element browse = doc.select("browse[id=WA110001G]").first();
        Crawl crawl = Crawl.createInstance(browse);
        crawl.run();
	}

    @Test
    public void test2() throws Exception {
        Element browse = doc.select("browse[id=WA110004G]").first();
        Crawl crawl = Crawl.createInstance(browse);
        crawl.run();
    }

    @Test
    public void test3() throws Exception {
        Element browse = doc.select("browse[id=WA110043G]").first();
        Crawl crawl = Crawl.createInstance(browse);
        crawl.run();
    }

    @Test
    public void test4() throws Exception {
        Element browse = doc.select("browse[id=WA120001G]").first();
        Crawl crawl = Crawl.createInstance(browse);
        crawl.run();
    }

    @Test
    public void test5() throws Exception {
        Element browse = doc.select("browse[id=WA110002G]").first();
        Crawl crawl = Crawl.createInstance(browse);
        crawl.run();
    }

}
