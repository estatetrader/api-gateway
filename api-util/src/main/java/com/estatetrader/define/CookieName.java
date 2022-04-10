package com.estatetrader.define;

/**
 * Created by steven on 29/03/2017.
 */
public class CookieName {

    /**
     * device id 设备标示符, 存储在cookie中的名字
     */
    public static final String deviceId = "__did";

    /**
     * user token 代表访问者身份,完成用户登入流程后获取
     */
    public static final String token = "__tk";

    /**
     * temp token 用户的临时身份, 完成oauth后获取
     */
    public static final String ttoken = "__ttk";

    /**
     * 用于提示客户端当前temp token是否存在
     */
    public static final String tempTokenExist = "__tct";

    /**
     * extension token 拓展的用户身份
     */
    public static final String etoken = "__etk";

    /**
     * 用于提示客户端当前extension token是否存在
     */
    public static final String extTokenExist = "__ect";

    /**
     * 用于提示客户端当前token是否存在
     */
    public static final String cookieExist = "__ct";

    /**
     * 当前用户的名称（签发token时的用户名）
     */
    public static final String operatorToken = "__operator";
}
