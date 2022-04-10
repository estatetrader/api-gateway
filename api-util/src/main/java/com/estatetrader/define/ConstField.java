package com.estatetrader.define;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ConstField {
    private ConstField() {
    }

    /**
     * utf-8
     *
     * @deprecated 请使用 StandardCharsets.UTF_8
     */
    @Deprecated
    public static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * utf-8
     *
     * @deprecated 请使用 StandardCharsets.UTF_8.name()
     */
    @Deprecated
    public static final String UTF8_STRING = StandardCharsets.UTF_8.name();

    public static final String REQUEST_ERROR_CODE_EXT = "com.estatetrader.REQUEST_ERROR_CODE_EXT";
    public static final String SET_COOKIE_TOKEN = "com.estatetrader.SET_COOKIE_TOKEN";
    public static final String SET_COOKIE_STOKEN = "com.estatetrader.SET_COOKIE_STOKEN";
    public static final String SET_COOKIE_TTOKEN = "com.estatetrader.SET_COOKIE_TTOKEN";
    public static final String SET_COOKIE_ETOKEN = "com.estatetrader.SET_COOKIE_ETOKEN";
    public static final String SET_COOKIE_OPERATOR = "com.estatetrader.SET_COOKIE_OPERATOR";
    public static final String REDIRECT_TO = "com.estatetrader.REDIRECT_TO";
    public static final String BACKEND_MESSAGE = "com.estatetrader.BACKEND_MESSAGE";
    public static final String SERVICE_LOG = "com.estatetrader.SERVICE_LOG";
    public static final String NOTIFICATION_ETAG = "com.estatetrader.ETAG";
    public static final String SERVICE_PARAM_EXPORT_PREFIX = "com.estatetrader.SERVICE_PARAM_EXPORT_";
}