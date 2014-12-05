package com.sidooo.weye;

import static org.junit.Assert.*;

import java.io.File;

import junit.framework.TestCase;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CrawlTest { 
	
	@Before
	public void setUp() throws Exception {
        UrlDatabase.init("10.1.1.2", 27017, "test" );
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test1() throws Exception {

        File xmlFile = new File("conf/web.xml");
        Document doc = Jsoup.parse(xmlFile,"UTF-8");
        Element browse = doc.select("browse[id=WA110001G]").first();
        Element web = browse.parent();

        Crawl crawl = Crawl.createInstance(CrawlType.BROWSE, browse);
        crawl.run();
	}

    @Test
    public void test2() throws Exception {

        File xmlFile = new File("conf/web.xml");
        Document doc = Jsoup.parse(xmlFile,"UTF-8");
        Element browse = doc.select("browse[id=WA110004G]").first();
        Element web = browse.parent();

        Crawl crawl = Crawl.createInstance(CrawlType.BROWSE, browse);
        crawl.run();
    }
}
