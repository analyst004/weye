package com.sidooo.weye;

public enum CrawlStatus {

    RUNNING("RUNNING"),
    CLOSED("CLOSED"),
    PENDING("PENDING"),
    IDLE("IDLE");

    private String type;

    private CrawlStatus(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }

    public static CrawlStatus fromValue(String v) {
        return valueOf(v);
    }
}