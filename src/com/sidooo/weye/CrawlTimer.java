package com.sidooo.weye;

public enum CrawlTimer {
	
	ONCE("ONCE"),
	EVERYDAY("EVERYDAY"),
	WEEKLY("WEEKLY"),
	MONTHLY("MONTHLY");
	
	private final String code;
	
	private CrawlTimer(String code) {
		this.code = code;
	}
	
	public String toString() {
		return this.code;
	}
	
	public static CrawlTimer fromValue(String v) {
		return valueOf(v);
	}
}
