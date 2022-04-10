package com.estatetrader.define;

import com.estatetrader.entity.CallerInfo;

public enum SecurityType {
    //Test(-1),
    /**
     * 无认证, 用户无关的接口. eg. 无任何安全风险的接口
     * 验证要素
     * 1. 设备签名
     */
    Anonym(0x00),

    /**
     * 用户认证，已认证类用户（已绑定手机号）
     * 验证要素
     * 1. 设备签名
     * 2. 用户token
     */
    User(0x01),

    /**
     * 内部用户认证，
     * 验证要素
     * 1. 设备签名
     * 2. 用户token
     * 3. 内网ip
     */
    InternalUser(0x02),

    /**
     * 三方系统集成接口. eg. 微信支付回调
     * service自己做验证
     */
    Integrated(0x04),

    /**
     * 用户认证，
     * 验证要素
     * 1. 设备签名
     * 2. 设备token
     */
    RegisteredDevice(0x08),

    /**
     * 已废弃
     * @deprecated 请使用@{{@link #User}}
     */
    @Deprecated
    SeceretUserToken(0x10),

    /**
     * 内网环境验证
     * 验证要素
     * 1. 内网ip
     */
    Internal(0x20),

    /**
     * 子系统用户认证. eg. 供应商系统用户
     * 验证要素
     * 1. 设备签名
     * 2. 用户token
     * 3. 用户角色
     * @deprecated 请使用@{{@link #AuthorizedUser}}
     */
    @Deprecated
    SubSystem(0x40),

    /**
     * 已授权用户，只允许通过了权限验证的用户
     * 验证要素
     * 1. 设备签名
     * 2. 用户token
     * 3. 用户角色
     */
    AuthorizedUser(0x80),

    /**
     * 尚未认证的登录用户，相比于User级别来说，此权限级别允许那些尚未认证（绑定手机号）的用户访问
     */
    UnidentifiedUser(0x100),

    /**
     * 进行了三方登录授权绑定的用户，即{@link CallerInfo#partnerBindId}不为0
     */
    PartnerBoundUser(0x200);

    private final int code;

    /**
     * @param code
     *            security16进制编码
     */
    SecurityType(int code) {
        this.code = code;
    }

    /**
     * 检查auth权限是否包含当前权限
     */
    public boolean check(int auth) {
        return (auth & code) == code;
    }
    
    /**
     * 检查auth权限是否包含当前权限
     */
    public boolean check(SecurityType auth) {
        return (auth.code & code) == code;
    }
    
    /**
     * 在auth权限的基础上增加当前权限
     */
    public int authorize(int auth) {
        return auth | this.code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 判断auth权限是否为空
     */
    public static boolean isNone(int auth) {
        return auth == 0;
    }

    /**
     * User(0x01), InternalUser(0x02), RegisteredDevice(0x08), ApiSubSystem(0x40), AuthorizedUser(0x80)
     * @param auth
     * @return
     */
    public static boolean requireToken(int auth) {
        return (auth & (RegisteredDevice.code | UnidentifiedUser.code | User.code | InternalUser.code
            | SubSystem.code | AuthorizedUser.code)) > 0;
    }

    /**
     * UnidentifiedUser(0x100), User(0x01), InternalUser(0x02), ApiSubSystem(0x40), AuthorizedUser(0x80);
     */
    public static boolean requireUnidentifiedUser(int auth) {
        return (auth & (
            UnidentifiedUser.code | User.code | AuthorizedUser.code | InternalUser.code | SubSystem.code)) > 0;
    }

    /**
     * User(0x01), InternalUser(0x02), ApiSubSystem(0x40), AuthorizedUser(0x80);
     */
    public static boolean requireIdentifiedUserToken(int auth) {
        return (auth & (User.code | AuthorizedUser.code | InternalUser.code | SubSystem.code)) > 0;
    }

    /**
     * PartnerBoundUser(0x200);
     */
    public static boolean requirePartnerToken(int auth) {
        return (auth & PartnerBoundUser.code) > 0;
    }
}
