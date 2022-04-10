package com.estatetrader.entity;

import java.util.Map;

/**
 * Created by steven on 19/06/2017.
 */
public class ExtensionCallerInfo {

    public String subsystem;

    /**
     * extension token 过期时间， 单位ms
     */
    public long   expire;

    /**
     * 原始用户id， 使用时需要与user token中的uid一致
     */
    public long   uid;

    /**
     * 拓展用户id
     */
    public long   eid;

    /**
     * 应用id
     */
    public int    aid;

    /**
     * 拓展用户角色
     */
    public String role;

    /**
     * 其他可以用于extension auto wired 的参数
     */
    public Map<String, String> parameters;
}
