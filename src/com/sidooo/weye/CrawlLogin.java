package com.sidooo.weye;

public enum CrawlLogin {

	SSO("SSO"),
	NONE("NONE");
	
	private final String code;
	
	private CrawlLogin(String code) {
		this.code = code;
	}
	
	public String toString() {
		return this.code;
	}
	
	public static CrawlLogin fromValue(String v) {
		return valueOf(v);
	}
	
}
