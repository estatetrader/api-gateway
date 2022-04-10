package com.estatetrader.entity;

public class ApiReturnCode extends AbstractReturnCode {

    public static final int _C_NO_ASSIGN = Integer.MIN_VALUE;
    public static final AbstractReturnCode NO_ASSIGN = new ApiReturnCode("unassigned", _C_NO_ASSIGN);

    public static final int _C_SUCCESS = 0;
    public static final AbstractReturnCode SUCCESS = new ApiReturnCode("success", _C_SUCCESS);

    public static final int _C_UNKNOWN_ERROR = -100;
    public static final AbstractReturnCode UNKNOWN_ERROR = new ApiReturnCode("unknown error", _C_UNKNOWN_ERROR);

    /**
     * 内部服务异常, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_INTERNAL_SERVER_ERROR = -101;
    public static final AbstractReturnCode INTERNAL_SERVER_ERROR = new ApiReturnCode(_C_INTERNAL_SERVER_ERROR, UNKNOWN_ERROR);

    /**
     * 内部序列化异常, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_SERIALIZE_FAILED = -102;
    public static final AbstractReturnCode SERIALIZE_FAILED = new ApiReturnCode(_C_SERIALIZE_FAILED, UNKNOWN_ERROR);

//    /**
//     * ip受限, 对外显示为UNKNOWN_ERROR
//     */
//    private static final int _C_IP_DENIED = -103;
//    public static final AbstractReturnCode IP_DENIED = new ApiReturnCode(_C_IP_DENIED, UNKNOWN_ERROR);

    /**
     * 严重错误, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_FATAL_ERROR = -104;
    public static final AbstractReturnCode FATAL_ERROR = new ApiReturnCode(_C_FATAL_ERROR, UNKNOWN_ERROR);

    /**
     * 网络访问失败, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_WEB_ACCESS_FAILED = -105;
    public static final AbstractReturnCode WEB_ACCESS_FAILED = new ApiReturnCode(_C_WEB_ACCESS_FAILED, UNKNOWN_ERROR);

    /**
     * security service调用异常, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_SECURITY_SERVICE_ERROR = -106;
    public static final AbstractReturnCode SECURITY_SERVICE_ERROR = new ApiReturnCode(_C_SECURITY_SERVICE_ERROR, UNKNOWN_ERROR);

    /**
     * dubbo服务找不到, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_DUBBO_SERVICE_NOTFOUND_ERROR = -107;
    public static final AbstractReturnCode DUBBO_SERVICE_NOTFOUND_ERROR = new ApiReturnCode(_C_DUBBO_SERVICE_NOTFOUND_ERROR, UNKNOWN_ERROR);

    /**
     * dubbo服务调用超时, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_DUBBO_SERVICE_TIMEOUT_ERROR = -108;
    public static final AbstractReturnCode DUBBO_SERVICE_TIMEOUT_ERROR = new ApiReturnCode(_C_DUBBO_SERVICE_TIMEOUT_ERROR, UNKNOWN_ERROR);

    /**
     * dubbo服务异常, 对外显示为UNKNOWN_ERROR
     */
    private static final int _C_DUBBO_SERVICE_ERROR = -109;
    public static final AbstractReturnCode DUBBO_SERVICE_ERROR = new ApiReturnCode(_C_DUBBO_SERVICE_ERROR, ApiReturnCode.UNKNOWN_ERROR);

    public static final int _C_UNKNOWN_METHOD = -120;
    public static final AbstractReturnCode UNKNOWN_METHOD = new ApiReturnCode("unknown method", _C_UNKNOWN_METHOD);

    /**
     * 请求解析异常, mt参数中接口依赖信息解析失败
     */
    public static final int                _C_UNKNOWN_DEPENDENT_METHOD = -121;
    public static final AbstractReturnCode UNKNOWN_DEPENDENT_METHOD    = new ApiReturnCode(_C_UNKNOWN_DEPENDENT_METHOD, UNKNOWN_METHOD);

    /**
     * 依赖的接口执行出错，导致本接口无法执行
     */
    public static final int                 _C_DEPENDENT_API_FAILURE = -122;
    public static final AbstractReturnCode DEPENDENT_API_FAILURE = new ApiReturnCode(_C_DEPENDENT_API_FAILURE, UNKNOWN_ERROR);

    /**
     * Mock service提供的结果无效
     */
    public static final int                 _C_ILLEGAL_MOCK_RESULT = -123;
    public static final AbstractReturnCode ILLEGAL_MOCK_RESULT = new ApiReturnCode(_C_ILLEGAL_MOCK_RESULT, UNKNOWN_ERROR);

    /**
     * 找不到Mock数据（由mock-service提供方抛出）
     */
    public static final int                 _C_MOCK_DATA_NOT_FOUND = -124;
    public static final AbstractReturnCode MOCK_DATA_NOT_FOUND = new ApiReturnCode("mock data not find", _C_MOCK_DATA_NOT_FOUND);

    public static final int _C_PARAMETER_ERROR = -140;
    public static final AbstractReturnCode PARAMETER_ERROR = new ApiReturnCode("parameter error", _C_PARAMETER_ERROR);

    public static final int _C_DEVICE_ID_IS_MISSING = -150;
    public static final AbstractReturnCode DEVICE_ID_IS_MISSING = new ApiReturnCode("device id not found", _C_DEVICE_ID_IS_MISSING);

    /**
     * 密文参数解密失败
     */
    public static final int _C_PARAMETER_DECRYPT_ERROR = -141;
    public static final AbstractReturnCode PARAMETER_DECRYPT_ERROR = new ApiReturnCode(_C_PARAMETER_DECRYPT_ERROR, PARAMETER_ERROR);

    public static final int _C_ACCESS_DENIED = -160;
    public static final AbstractReturnCode ACCESS_DENIED = new ApiReturnCode("access denied", _C_ACCESS_DENIED);

    /**
     * 用户身份验证失败, 对外显示为ACCESS_DENIED
     */
    public static final int _C_USER_CHECK_FAILED = -161;
    public static final AbstractReturnCode USER_CHECK_FAILED = new ApiReturnCode(_C_USER_CHECK_FAILED, ACCESS_DENIED);

    /**
     * 凭据内的app_id和通用参数不一致, 对外显示为ACCESS_DENIED
     */
    public static final int _C_APP_ID_MISMATCH = -162;
    public static final AbstractReturnCode APP_ID_MISMATCH = new ApiReturnCode(_C_APP_ID_MISMATCH, ACCESS_DENIED);

    /**
     * 凭据内的did和通用参数不一致, 对外显示为ACCESS_DENIED
     */
    public static final int _C_DEVICE_ID_MISMATCH = -163;
    public static final AbstractReturnCode DEVICE_ID_MISMATCH = new ApiReturnCode(_C_DEVICE_ID_MISMATCH, ACCESS_DENIED);

    /**
     * 访问令牌无法解析,设备信息不足, 对外显示为ACCESS_DENIED
     */
    public static final int _C_UNKNOWN_TOKEN_DENIED = -164;
    public static final AbstractReturnCode UNKNOWN_TOKEN_DENIED = new ApiReturnCode(_C_UNKNOWN_TOKEN_DENIED, ACCESS_DENIED);

    /**
     * encryptionOnly接口不接受来自非安全通道的访问, 对外显示为ACCESS_DENIED
     */
    public static final int _C_UNKNOWN_ENCRYPTION_DENIED = -165;
    public static final AbstractReturnCode UNKNOWN_ENCRYPTION_DENIED = new ApiReturnCode(_C_UNKNOWN_ENCRYPTION_DENIED, ACCESS_DENIED);

    /**
     * risk manager 返回要阻止相关调用, 对外显示为ACCESS_DENIED
     */
    public static final int _C_RISK_CONTROL_DENIED = -166;
    public static final AbstractReturnCode RISK_CONTROL_DENIED = new ApiReturnCode("access denied", _C_RISK_CONTROL_DENIED);

    /**
     * client ip 不在白名单
     */
    public static final int _C_CLIENT_IP_DENIED = -167;
    public static final AbstractReturnCode CLIENT_IP_DENIED = new ApiReturnCode(_C_CLIENT_IP_DENIED, ACCESS_DENIED);

    /**
     * user id 在用户黑名单
     */
    public static final int _C_USER_BLACKLIST_DENIED = -168;
    public static final AbstractReturnCode USER_BLACKLIST_DENIED = new ApiReturnCode(_C_USER_BLACKLIST_DENIED, RISK_CONTROL_DENIED);

    /**
     * device id 在设备黑名单
     */
    public static final int _C_DEVICE_BLACKLIST_DENIED = -169;
    public static final AbstractReturnCode DEVICE_BLACKLIST_DENIED = new ApiReturnCode(_C_DEVICE_BLACKLIST_DENIED, RISK_CONTROL_DENIED);

    /**
     * ip id 在 ip 黑名单
     */
    public static final int _C_IP_BLACKLIST_DENIED = -170;
    public static final AbstractReturnCode IP_BLACKLIST_DENIED = new ApiReturnCode(_C_IP_BLACKLIST_DENIED, RISK_CONTROL_DENIED);

    /**
     * 手机号码 在手机号码前缀黑名单
     */
    public static final int _C_PHONE_PREFIX_BLACKLIST_DENIED = -171;
    public static final AbstractReturnCode PHONE_PREFIX_BLACKLIST_DENIED = new ApiReturnCode(_C_PHONE_PREFIX_BLACKLIST_DENIED, RISK_CONTROL_DENIED);


    public static final int _C_USER_TOKEN_SIGNATURE_ERROR = -180;
    public static final AbstractReturnCode USER_TOKEN_SIGNATURE_ERROR = new ApiReturnCode("signature error", _C_USER_TOKEN_SIGNATURE_ERROR);

    public static final int _C_DEVICE_TOKEN_SIGNATURE_ERROR = -181;
    public static final AbstractReturnCode DEVICE_TOKEN_SIGNATURE_ERROR = new ApiReturnCode("signature error", _C_DEVICE_TOKEN_SIGNATURE_ERROR);

    public static final int _C_UNKNOWN_SIGNATURE_ERROR = -182;
    public static final AbstractReturnCode UNKNOWN_SIGNATURE_ERROR = new ApiReturnCode("signature error", _C_UNKNOWN_SIGNATURE_ERROR);

    public static final int _C_STATIC_SALT_SIGNATURE_ERROR = -183;
    /**
     * 签名错误-静态盐不匹配
     */
    public static final AbstractReturnCode STATIC_SALT_SIGNATURE_ERROR = new ApiReturnCode(_C_STATIC_SALT_SIGNATURE_ERROR, UNKNOWN_SIGNATURE_ERROR);

    public static final int _C_DYNAMIC_SALT_SIGNATURE_ERROR = -184;
    /**
     * 签名错误-需要使用动态盐（客户端在应该使用动态时使用了静态盐）
     */
    public static final AbstractReturnCode DYNAMIC_SALT_SIGNATURE_ERROR = new ApiReturnCode(_C_DYNAMIC_SALT_SIGNATURE_ERROR, DEVICE_TOKEN_SIGNATURE_ERROR);

    public static final int _C_UNKNOWN_THIRD_PARTY_SIGNATURE_ERROR = -186;
    /**
     * 签名错误-未知的third party
     */
    public static final AbstractReturnCode UNKNOWN_THIRD_PARTY_SIGNATURE_ERROR = new ApiReturnCode(_C_UNKNOWN_THIRD_PARTY_SIGNATURE_ERROR, UNKNOWN_SIGNATURE_ERROR);

    public static final int _C_THIRD_PARTY_API_SIGNATURE_ERROR = -187;
    /**
     * 签名错误-third party未定义API
     */
    public static final AbstractReturnCode THIRD_PARTY_API_SIGNATURE_ERROR = new ApiReturnCode(_C_THIRD_PARTY_API_SIGNATURE_ERROR, UNKNOWN_SIGNATURE_ERROR);

    public static final int _C_THIRD_PARTY_RSA_SIGNATURE_ERROR = -188;
    /**
     * 签名错误-third party的rsa不匹配
     */
    public static final AbstractReturnCode THIRD_PARTY_RSA_SIGNATURE_ERROR = new ApiReturnCode(_C_THIRD_PARTY_RSA_SIGNATURE_ERROR, UNKNOWN_SIGNATURE_ERROR);

    public static final int _C_ILLEGAL_MULTIAPI_ASSEMBLY = -190;
    public static final AbstractReturnCode ILLEGAL_MULTIAPI_ASSEMBLY = new ApiReturnCode("unsupported method group", _C_ILLEGAL_MULTIAPI_ASSEMBLY);

    /**
     * RawString返回类型不允许进行多接口同时调用
     */
    public static final int _C_ILLEGAL_MUTLI_RAWSTRING_RT = -191;
    public static final AbstractReturnCode ILLEGAL_MUTLI_RAWSTRING_RT = new ApiReturnCode(_C_ILLEGAL_MUTLI_RAWSTRING_RT,
        ILLEGAL_MULTIAPI_ASSEMBLY);

    /**
     * Integrated级别接口不允许进行组合访问
     */
    public static final int _C_ILLEGAL_MUTLI_INTEGRATED_API_ACCESS = -192;
    public static final AbstractReturnCode ILLEGAL_MUTLI_INTEGRATED_API_ACCESS = new ApiReturnCode(_C_ILLEGAL_MUTLI_INTEGRATED_API_ACCESS,
        ILLEGAL_MULTIAPI_ASSEMBLY);

    public static final int _C_REQUEST_PARSE_ERROR = -200;
    public static final AbstractReturnCode REQUEST_PARSE_ERROR = new ApiReturnCode("parse error", _C_REQUEST_PARSE_ERROR);

    public static final int _C_API_UPGRADE = -220;
    public static final AbstractReturnCode API_UPGRADE = new ApiReturnCode("api upgraded", _C_API_UPGRADE);

    public static final int _C_APPID_NOT_EXIST = -280;
    public static final AbstractReturnCode APPID_NOT_EXIST = new ApiReturnCode("app not found", _C_APPID_NOT_EXIST);

    /*
     * 为了兼容客户端代码，-360和-361的错误代码不能变化，但是原本-300需要转换为-360，因此将-360和-361定义提前到这里
     */

    public static final int _C_USER_TOKEN_ERROR = -360;
    public static final AbstractReturnCode USER_TOKEN_ERROR = new ApiReturnCode("user token error", _C_USER_TOKEN_ERROR);

    public static final int _C_DEVICE_TOKEN_ERROR = -361;
    public static final AbstractReturnCode DEVICE_TOKEN_ERROR = new ApiReturnCode("device token error", _C_DEVICE_TOKEN_ERROR);

    public static final int _C_EXTENSION_TOKEN_ERROR = -362;
    public static final AbstractReturnCode EXTENSION_TOKEN_ERROR = new ApiReturnCode("extension token error", _C_EXTENSION_TOKEN_ERROR);

    /**
     * 表示此次API请求需要未认证类或以上token，用于提醒客户端申请未认证类token（ntk）
     */
    public static final int _C_UNIDENTIFIED_USER_TOKEN_ERROR = -363;
    public static final AbstractReturnCode UNIDENTIFIED_USER_TOKEN_ERROR = new ApiReturnCode("unidentified user token error", _C_UNIDENTIFIED_USER_TOKEN_ERROR);

    /**
     * 表示此次请求的某个API需要三方登录授权绑定信息，客户端在收到此错误后需要引导用户进行相应的三方登录授权
     */
    public static final int _C_PARTNER_TOKEN_ERROR = -364;
    public static final AbstractReturnCode PARTNER_TOKEN_ERROR = new ApiReturnCode("partner token error", _C_PARTNER_TOKEN_ERROR);

    public static final int _C_USER_TOKEN_EXPIRE = -300;
    public static final AbstractReturnCode USER_TOKEN_EXPIRE = new ApiReturnCode(_C_USER_TOKEN_EXPIRE, USER_TOKEN_ERROR);

    public static final int _C_TOKEN_IN_USER_TOKEN_EXPIRED_LIST = -301;
    /**
     * token在user-token 过期列表中，指示这个用户的所有token都需要强制失效
     */
    public static final AbstractReturnCode TOKEN_IN_USER_TOKEN_EXPIRED_LIST = new ApiReturnCode(_C_TOKEN_IN_USER_TOKEN_EXPIRED_LIST, USER_TOKEN_ERROR);

    public static final int _C_SINGLE_DEVICE_ERROR = -310;
    /**
     * 用于实现单设备登录，即账号仅在一个设备上登录。如果同一设备在其他设备上登录成功，则原设备上使用老的token的客户端请求均会被此错误码拒绝
     */
    public static final AbstractReturnCode SINGLE_DEVICE_ERROR = new ApiReturnCode("single device error", _C_SINGLE_DEVICE_ERROR);

    public static final int _C_NO_TRUSTED_DEVICE = -320;
    public static final AbstractReturnCode NO_TRUSTED_DEVICE = new ApiReturnCode("untrusted device", _C_NO_TRUSTED_DEVICE);

    public static final int _C_NO_ACTIVE_DEVICE = -340;
    public static final AbstractReturnCode NO_ACTIVE_DEVICE = new ApiReturnCode("inactive device", _C_NO_ACTIVE_DEVICE);

    public static final int _C_MISSING_EXTENSION_TOKEN = -370;
    /**
     * 如果客户端请求的某个API需要etk，而客户端没有提供，则报告此错误
     */
    public static final AbstractReturnCode MISSING_EXTENSION_TOKEN = new ApiReturnCode(_C_MISSING_EXTENSION_TOKEN, EXTENSION_TOKEN_ERROR);

    public static final int _C_APP_ID_NOT_IN_ETK_ISSUER = -371;
    public static final AbstractReturnCode APP_ID_NOT_IN_ETK_ISSUER = new ApiReturnCode("app not supported in etk issuer", _C_APP_ID_NOT_IN_ETK_ISSUER);

    public static final int _C_EXTENSION_TOKEN_EXPIRE = -372;
    /**
     * 扩展凭据已过期
     */
    public static final AbstractReturnCode EXTENSION_TOKEN_EXPIRE = new ApiReturnCode(_C_EXTENSION_TOKEN_EXPIRE, EXTENSION_TOKEN_ERROR);

    public static final int _C_EXTENSION_INVALID_SUBSYSTEM = -373;
    /**
     * 扩展凭据中的子系统信息不合法
     */
    public static final AbstractReturnCode EXTENSION_INVALID_SUBSYSTEM = new ApiReturnCode(_C_EXTENSION_INVALID_SUBSYSTEM, EXTENSION_TOKEN_ERROR);

    public static final int _C_EXTENSION_INVALID_SIGNATURE = -374;
    /**
     * 扩展凭据签名不合法
     */
    public static final AbstractReturnCode EXTENSION_INVALID_SIGNATURE = new ApiReturnCode(_C_EXTENSION_INVALID_SIGNATURE, EXTENSION_TOKEN_ERROR);

    public static final int _C_EXTENSION_UID_MISMATCH = -375;
    /**
     * 扩展凭据中的uid和用户凭据中的uid不匹配
     */
    public static final AbstractReturnCode EXTENSION_UID_MISMATCH = new ApiReturnCode(_C_EXTENSION_UID_MISMATCH, EXTENSION_TOKEN_ERROR);

    public static final int _C_EXTENSION_SUBSYSTEM_MISMATCH = -376;
    /**
     * 扩展凭据中的子系统和设备（用户）凭据中的不匹配
     */
    public static final AbstractReturnCode EXTENSION_SUBSYSTEM_MISMATCH = new ApiReturnCode(_C_EXTENSION_SUBSYSTEM_MISMATCH, EXTENSION_TOKEN_ERROR);

    public static final int _C_EXTENSION_APP_ID_MISMATCH = -377;
    /**
     * 扩展凭据中的app id和设备（用户）凭据中的不匹配
     */
    public static final AbstractReturnCode EXTENSION_APP_ID_MISMATCH = new ApiReturnCode(_C_EXTENSION_APP_ID_MISMATCH, EXTENSION_TOKEN_ERROR);

    public static final int _C_MISSING_EXTENSION_PARAM = -378;
    /**
     * 扩展凭据中缺少此次请求的API中的某个必需注入的参数
     */
    public static final AbstractReturnCode MISSING_EXTENSION_PARAM = new ApiReturnCode(_C_MISSING_EXTENSION_PARAM, EXTENSION_TOKEN_ERROR);

    public static final int _C_EXTENSION_TOKEN_MISSING_TOKEN = -379;
    /**
     * 扩展凭据中缺少此次请求的API中的某个必需注入的参数
     */
    public static final AbstractReturnCode EXTENSION_TOKEN_MISSING_TOKEN = new ApiReturnCode(_C_EXTENSION_TOKEN_MISSING_TOKEN, EXTENSION_TOKEN_ERROR);

    public static final int _C_UPLOAD_FILE_TOO_LARGE = -380;
    public static final AbstractReturnCode UPLOAD_FILE_TOO_LARGE = new ApiReturnCode("upload file too large", _C_UPLOAD_FILE_TOO_LARGE);

    public static final int _C_UPLOAD_FILE_NAME_ERROR = -390;
    public static final AbstractReturnCode UPLOAD_FILE_NAME_ERROR = new ApiReturnCode("invalid file name", _C_UPLOAD_FILE_NAME_ERROR);

    public static final int _C_UPLOAD_FILE_QUANTITY_LIMIT_EXCEEDED = -391;
    public static final AbstractReturnCode UPLOAD_FILE_QUANTITY_LIMIT_EXCEEDED = new ApiReturnCode("tool many files", _C_UPLOAD_FILE_QUANTITY_LIMIT_EXCEEDED);

    public static final int _C_UPLOAD_FILE_VOD_ERROR = -392;
    public static final AbstractReturnCode UPLOAD_FILE_VOD_ERROR = new ApiReturnCode("upload error to vod", _C_UPLOAD_FILE_VOD_ERROR);

    public static final int _C_UPLOAD_FILE_CONTENT_TYPE_ERROR = -393;
    public static final AbstractReturnCode UPLOAD_FILE_CONTENT_TYPE_ERROR = new ApiReturnCode("unsupported file content type", _C_UPLOAD_FILE_CONTENT_TYPE_ERROR);

    public static final int _C_UPLOAD_ENCODE_TYPE_ERROR = -394;
    public static final AbstractReturnCode UPLOAD_ENCODE_TYPE_ERROR = new ApiReturnCode("unsupported file encode type", _C_UPLOAD_ENCODE_TYPE_ERROR);

    public static final int _C_UPLOAD_FILE_QINIU_ERROR = -395;
    public static final AbstractReturnCode UPLOAD_FILE_QINIU_ERROR = new ApiReturnCode("upload error to qiniu", _C_UPLOAD_FILE_QINIU_ERROR);

    public static final int _C_PERMISSION_DENIED = -400;
    public static final AbstractReturnCode PERMISSION_DENIED = new ApiReturnCode("permission denied", _C_PERMISSION_DENIED);

    public static final int _C_NON_VISITOR = -401;
    public static final AbstractReturnCode NON_VISITOR = new ApiReturnCode(_C_NON_VISITOR, PERMISSION_DENIED);

    public static final int _C_SUBSYSTEM_UNMATCH = -402;
    public static final AbstractReturnCode SUBSYSTEM_UNMATCH = new ApiReturnCode(_C_SUBSYSTEM_UNMATCH, PERMISSION_DENIED);

    public static final int _C_PERMISSION_CHECK_FAILED = -403;
    /**
     * 指示当前用户所属的角色（role）没有权限访问当前请求中的某个API
     * 在子系统权限树下，每个API都定义了一组能够访问它的角色集合
     * 如果该子系统树和该API均存在，但是角色集合没有包含当前用户的role，则返回-403
     */
    public static final AbstractReturnCode PERMISSION_CHECK_FAILED = new ApiReturnCode(_C_PERMISSION_CHECK_FAILED, PERMISSION_DENIED);

    public static final int _C_PERMISSION_UNASSIGNED = -404;
    /**
     * 授权校验时，如果访问的目标API没有在凭据中指定的子系统对应的权限树中注册，则报告此错误
     */
    public static final AbstractReturnCode PERMISSION_UNASSIGNED = new ApiReturnCode(_C_PERMISSION_UNASSIGNED, PERMISSION_DENIED);

    public static final int _C_SUBSYSTEM_NOT_FOUND = -405;
    /**
     * 授权校验时，如果凭据中的子系统未注册到权限树中，则报告此错误码
     */
    public static final AbstractReturnCode SUBSYSTEM_NOT_FOUND = new ApiReturnCode(_C_SUBSYSTEM_NOT_FOUND, PERMISSION_DENIED);

    public static final int _C_TOKEN_MISSING_SUBSYSTEM = -406;
    public static final AbstractReturnCode TOKEN_MISSING_SUBSYSTEM = new ApiReturnCode(_C_TOKEN_MISSING_SUBSYSTEM, PERMISSION_DENIED);

    public static final int _C_REQUIRE_VERIFY_CODE = -444;
    public static final AbstractReturnCode REQUIRE_VERIFY_CODE = new ApiReturnCode("request verify code required", _C_REQUIRE_VERIFY_CODE);

    public static final int _C_REQUIRE_VERIFY_CODE_BY_USER_ID = -445;
    public static final AbstractReturnCode REQUIRE_VERIFY_CODE_BY_USER_ID = new ApiReturnCode(_C_REQUIRE_VERIFY_CODE_BY_USER_ID, REQUIRE_VERIFY_CODE);

    public static final int _C_REQUIRE_VERIFY_CODE_BY_DEVICE_ID = -446;
    public static final AbstractReturnCode REQUIRE_VERIFY_CODE_BY_DEVICE_ID = new ApiReturnCode(_C_REQUIRE_VERIFY_CODE_BY_DEVICE_ID, REQUIRE_VERIFY_CODE);

    public static final int _C_REQUIRE_VERIFY_CODE_BY_CLIEN_IP = -447;
    public static final AbstractReturnCode REQUIRE_VERIFY_CODE_BY_CLIEN_IP = new ApiReturnCode(_C_REQUIRE_VERIFY_CODE_BY_CLIEN_IP, REQUIRE_VERIFY_CODE);

    public static final int _C_REQUIRE_VERIFY_CODE_BY_PHONE_PREFIX = -448;
    public static final AbstractReturnCode REQUIRE_VERIFY_CODE_BY_PHONE_PREFIX = new ApiReturnCode(_C_REQUIRE_VERIFY_CODE_BY_PHONE_PREFIX, REQUIRE_VERIFY_CODE);

    public ApiReturnCode(String desc, int code) {
        super(desc, code);
    }

    public ApiReturnCode(int code, AbstractReturnCode display) {
        super(code, display);
    }

    public ApiReturnCode(int code, int displayCode, String desc) {
        super(code, new ApiReturnCode(desc, displayCode));
    }
}
