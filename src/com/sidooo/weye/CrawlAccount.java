package com.sidooo.weye;

public enum CrawlAccount {

	RANDOM("RANDOM"),
	FIXED("FIXED");
	
	private String type;
	
	private CrawlAccount(String type) {
		this.type = type;
	}
	
	public String toString() {
		return this.type;
	}
	
	public static CrawlAccount fromValue(String v) {
		return valueOf(v);
	}
}
