package com.sidooo.weye;

/**
 * Created with IntelliJ IDEA.
 * User: kimzhang
 * Date: 14-11-18
 * Time: 下午5:20
 * To change this template use File | Settings | File Templates.
 */
public class WebHost {

    private  String name;
    private  String domain;

    public WebHost(String name, String domain) {
        this.name = name;
        this.domain = domain;
    }

    public String getName() {
        return this.name;
    }

    public String getDomain() {
        return this.domain;
    }
}
