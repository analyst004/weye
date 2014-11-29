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
	
	private String testId = "175d4c81-2ae5-44f8-8426-df5b0fa0b413";
			
	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {

        String testXml =
           "<web  name=\"最高人民法院\" index=\"http://www.court.gov.cn\">" +
                "<browse name=\"全国法院失信被执行人名单(自然人)\" host=\"http://shixin.court.gov.cn\">" +
                    "<list method=\"get\"  path=\"/personMore.do\"/>" +
                    "<page method=\"post\" path=\"/personMore.do\">" +
                        "<form name=\"currentPage\">$pageid</form>" +
                        "<select method=\"jquery\" key=\"table.Resultlist a[title]\" attribute=\"id\"></select>" +
                    "</page>" +
                    "<item method=\"get\" path=\"/detail\">" +
                        "<string name=\"id\">$itemid</string>" +
                    "</item>" +
                "</browse>" +
           "</web>";

        Document doc = Jsoup.parse(testXml);
        Element conf = doc.select("web").first().child(0);

        Crawl crawl = Crawl.createInstance(CrawlType.BROWSE, conf);
		
		assertEquals(crawl.getStatus(), CrawlStatus.CLOSED);
		
		//crawl.start();
        new Thread(crawl).start();

        Thread.sleep(5000);

//        assertEquals(crawl.getStatus(), CrawlStatus.RUNNING);
	}
	
	
}
