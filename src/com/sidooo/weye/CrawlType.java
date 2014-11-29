package com.sidooo.weye;

/**
 * Created with IntelliJ IDEA.
 * User: kimzhang
 * Date: 14-11-18
 * Time: 下午4:56
 * To change this template use File | Settings | File Templates.
 */
public enum CrawlType {

    QUERY("QUERY"),
    BROWSE("BROWSE");

    private final String code;

    private CrawlType(String code) {
        this.code = code;
    }

    public String toString() {
        return this.code;
    }

    public static CrawlType fromValue(String v) {
        return valueOf(v);
    }
}
