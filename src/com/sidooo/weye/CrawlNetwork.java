package com.sidooo.weye;

public enum CrawlNetwork {

	DIRECT("DIRECT"),
	PROXY("PROXY");
	
	private String type;
	
	private CrawlNetwork(String type) {
		this.type = type;
	}
	
	public String toString() {
		return this.type;
	}
	
	public static CrawlNetwork fromValue(String v) {
		return valueOf(v);
	}
}
