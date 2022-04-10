package com.estatetrader.define;

import com.estatetrader.annotation.DefineCommonParameter;

public final class CommonParameter {
    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "user token 代表访问者身份,完成用户登入流程后获取")
    public static final String token = "_tk";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "device token 代表访问设备的身份,完成设备注册流程后获取")
    public static final String deviceToken = "_dtk";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "extension token 代表子系统给该用户的授权身份")
    public static final String extensionToken = "_etk";

    @DefineCommonParameter(fromClient = true, injectable = false,
        desc = "method 请求的资源名")
    public static final String method = "_mt";

    @DefineCommonParameter(fromClient = true, injectable = false,
        desc = "signature 参数字符串签名")
    public static final String signature = "_sig";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "application id 应用编号")
    public static final String applicationId = "_aid";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "business id 业务流水号, 用于做幂等判断, 风控等. 支持通过Cookie注入获取url中的值")
    public static final String businessId = "_bid";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "call id 客户端调用编号. 支持通过Cookie注入获取url中的值")
    public static final String callId = "_cid";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "device id 设备标示符")
    public static final String deviceId = "_did";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "源设备编号，用于多平台跳转中的最后一级平台的设备编号")
    public static final String originDeviceId = "_origin_did";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "user id 用户标示符")
    public static final String userId = "_uid";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "三方平台授权登录的bindId")
    public static final String partnerBindId = "_partner_bind_id";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "extension user id 子系统授权用户标示符")
    public static final String extensionUserId = "_eid";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "如果已经登录，则返回userId，否则返回deviceId，表示对当前访问者的一个最近似描述，可用于一致性哈希的参数")
    public static final String userIdOrDeviceId = "_uid_or_did";

    /**
     * 业务子系统主账号id
     * @deprecated 此字段不能很好描述实际用到的数据的含义，因此不建议使用，未来网关会关闭对其支持
     * 请使用@ExtensionAutowired
     */
    @Deprecated
    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "subSystem main id 业务子系统主账号id，已废弃，请使用@ExtensionAutowired")
    public static final String subSystemMainId = "_ssmid";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "client ip 用户ip")
    public static final String clientIp = "_cip";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "version code 客户端数字版本号. 支持通过Cookie注入获取url中的值")
    public static final String versionCode = "_vc";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "version 客户端版本名.")
    public static final String versionName = "_vn";

    @DefineCommonParameter(fromClient = true, injectable = false,
        desc = "signature method 签名算法 hmac,md5,sha1,rsa,ecc")
    public static final String signatureMethod = "_sm";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "token中的用户手机号")
    public static final String phoneNumber = "_pn";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "第三方集成的身份标识(第三方集成情景下使用)")
    public static final String thirdPartyId = "_tpid";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "cookie注入 支持在url中使用该参数")
    public static final String cookie = "_cookie";

    public static final String extensionAutowiredPrefix = "_extension_autowired__";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "extension token params注入")
    public static final String extensionParamsAutowired = "_extension_autowired_params";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "user agent注入")
    public static final String userAgent = "_userAgent";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "当前站点host")
    public static final String host = "_host";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "request 时间戳")
    public static final String timestamp = "_ts";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "操作人")
    public static final String operator = "_operator";

    /**
     * 用于内部参数注入的标识符，用于指示在第三方集成的场景下网关向后台传递整个post表单
     */
    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "用于内部参数注入的标识符，用于指示在第三方集成的场景下网关向后台传递整个post表单")
    public static final String postBody = "_pb";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "服务端注入")
    public static final String serviceInjection = "_si";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "用户级别")
    public static final String userLevel = "_ulv";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "折扣等级")
    public static final String discountLevel = "_dlv";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "客户端操作标识符")
    public static final String actionId = "_action_id";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "采购商ID")
    public static final String purchaserId = "_purchaser_id";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "可选API列表")
    public static final String _opt = "_opt";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "STORE ID")
    public static final String storeId = "_store_id";

    @DefineCommonParameter(fromClient = false, injectable = true,
        desc = "role id")
    public static final String roleId = "_role_id";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "referer")
    public static final String referer = "_referer";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "子系统通用参数")
    public static final String subsystemParams = "_scp";

    public static final String requestHeaderInjectPrefix = "_request_header__";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "逗号分隔的合法API名称，由客户端提供，用于指示网关当前请求中哪些API需要使用Mock数据，仅用于测试环境")
    public static final String apisToMock = "_apis_to_mock";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "页面实例编号，用于标识每次页面刷新")
    public static final String pageViewId = "_pvid";

    @DefineCommonParameter(fromClient = true, injectable = true,
        desc = "客户端网络状态描述，用于客户端调试等")
    public static final String clientNetworkInfo = "_client_network_info";
}
