package com.estatetrader.entity;

import com.estatetrader.define.SecurityType;
import com.estatetrader.define.CommonParameter;

import java.io.Serializable;

/**
 * 调用者信息，包括设备信息和用户信息(已登录)的一部分
 *
 * @author rendong
 */
public class CallerInfo implements Serializable {
    /**
     * token version for backward compliance
     */
    public short version;
    public int    appid;
    public int    securityLevel;
    public long   expire;
    public long   deviceId;
    public long   uid;
    /**
     * 设备身份公钥
     */
    public byte[] key;
    /**
     * 通过动态密码验证的phone number
     */
    public String phoneNumber;
    public String oauthid;
    /**
     * 用户角色
     */
    public String role;
    /**
     * 子系统
     */
    public String subsystem;
    /**
     * 业务子系统主账号id
     */
    public long   subSystemMainId = Long.MIN_VALUE;
    /**
     * the time window in milliseconds used to indicate how long time after this token expired,
     * the gateway cannot use this token to renew a new token
     */
    public long renewWindow;

    /**
     * 凭据的签发时间，在签发时自动获取当前时间，无须主动赋值
     */
    public long createdTime;

    /**
     * 为true表示拥有此token的用户在签发此token时已认证（例如已绑定手机号）
     * 此字段仅在 uid > 0 时有意义
     */
    public boolean identified = true;

    /**
     * 表示客户端在使用三方平台授权登录时，在本系统内存储的相关账号绑定表的记录的ID。
     *
     * 用于标识token为ptk（partner token）
     *
     * token归类（从上往下依次升级）：
     * 1. dtk: deviceId != 0
     * 2. ptk: deviceId != 0 && partnerBindId != 0
     * 3. utk: deviceId != 0 && userId != 0
     *
     * 当userId为0时，此值为0表示无任何客户端的登录信息（未进行三方授权登录），
     * 否则，非0的partnerBindId表示客户端已进行三方授权登录，服务器已记录三方授权账号（即bind），其值为该bind表中记录的ID；
     * 当userId为非0时，此值为非0表示一个客户端最之前进行了三方授权登录和手机号绑定操作，并在最后一次登录时使用了三方账号登录；
     * 但在userId为非0而partnerBindId为0并不代表该账号没有与之关联的三方登录信息，只能说明最后一次登录是直接使用了手机号。
     *
     * 用户系统下发token时，需要同时考虑当前登录是否携带了三方登录信息和手机号绑定情况，并根据情况主动给出partnerBindId和userId的值。
     * 例如，客户端在使用微信登录时，用户系统应根据客户端提供的union-id查找三方账号绑定表以确定partnerBindId和user表，
     * 以确定对应的userId（如果存在）。
     *
     * 如果客户端仅使用了手机号验证码登录，未提供任何三方平台的账号信息，则只应在token中设置userId，而将partnerBindId置0。
     *
     * 一个典型性的微信小程序客户端的首次三方登录过程如下：
     * 1. 客户端首次访问系统，生成did并且注册设备，以获得dtk和device-secret
     * 2. 客户端执行静默登录逻辑，访问微信服务器，获取三方登录信息凭据（access code），并将该code发送给用户系统
     * 3. 用户系统收到code之后再次访问微信服务器，换取unionId，该unionId唯一标识了打开当前小程序客户端的微信账号
     * 4. 用户系统使用unionId在三方账号绑定表中搜索该unionId对应的bindId
     * 5. 目前还bindId不存在，所以我们构造一条绑定记录用于记录该unionId，并将bindId设置为新的绑定纪录的ID
     * 6. 构造一个ptk，其中ptk.partnerBindId设置为上步骤获取到的bindId，并将其返回给客户端
     * 7. 客户端将ptk存储到本地存储中，并在以后的每一次API调用时使用通用参数_tk携带该ptk（直到客户端获得了级别更高的utk）
     *
     * 该客户端在第二次打开时（尚未绑定手机号），会有如下步骤：
     * 1. 客户端如果发现本地ptk/utk不存在，则执行静默登录逻辑，从微信服务器获取access code，并调用静默登录接口
     * 2. 用户系统将收到的access code换为unionId，并搜索三方账号绑定表，查找该unionId对应的bindId
     * 3. 此时，bindId已存在，构造一个ptk并将ptk.partnerBindId设置为bindId，返回给客户端
     * 4. 客户端收到ptk，并将其存储在本地存储中
     * 5. 客户端访问了user级别的API，或用户主动登录，则引导用户输入手机号和验证码或提示进行手机号授权，
     *    并正式调用用户登录API（此时客户端仍然需要携带ptk）
     * 6. 用户系统根据登录时提供的手机号创建账号，获得userId，并更新三方绑定表，将其与ptk.partnerBindId进行关联
     * 7. 用户系统构造一个utk，其中包含刚刚获得的userId和ptk中的partnerBindId，并返回给客户端
     * 8. 客户端收到utk，对其进行存储，并在后续API调用时使用_tk通用参数携带该utk
     *
     * 该客户端在后续打开时（已绑定手机号），会有如下步骤：
     * 1. 客户端如果发现本地ptk/utk不存在执行静默登录逻辑，从微信服务器获取access code，并调用静默登录接口
     * 2. 用户系统使用access code换取unionId
     * 3. 用户系统使用unionId搜索三方账号绑定表，查找对应的bindId
     * 4. 此时，bindId已经存在，则继续查找用户表，获得userId
     * 5. 用户系统构造一个utk，其中包含查询得到的bindId和userId，并返回给客户端
     * 6. 客户端收到utk，对其进行存储，并在后续API调用时使用_tk通用参数携带该utk
     *
     * 后续，如果app登录时使用了同一个微信账号，则用户系统也应在签发utk时设置userId和partnerBindId；
     * 如果该app登录时没有使用微信账号，而是直接使用了手机号，则不得在utk中包含partnerBindId
     *
     * 关于ptk的过期和自动续签
     * 1. ptk不支持过期
     * 2. ptk不支持自动续签
     * 3. utk在进行自动续签时，需要检查partnerBindId与userId的映射关系
     *
     * 关于请求签名
     * 1. ptk在请求签名设计中的角色与utk相当
     * 2. 在收到-180时自动清除ptk和utk，并考虑自动进行静默登录
     *
     * 关于API权限级别与注入：
     * 1. 业务方可使用 {@link CommonParameter#partnerBindId} 注入 ptk/utk中的partnerBindId
     * 2. 如果API必须要注入一个非0的partnerBindId，则须将权限级别声明为{@link SecurityType#PartnerBoundUser}
     * 3. 当客户端使用partnerBindId为0的token（dtk或者partnerBindId=0的utk）访问{@link SecurityType#PartnerBoundUser}
     *    权限级别的API时，网关会拒绝请求并报告-364错误{@link ApiReturnCode#PARTNER_TOKEN_ERROR}
     * 4. 客户端在收到-364错误码后需要引导用户进行三方登录授权
     */
    public long partnerBindId;
}
