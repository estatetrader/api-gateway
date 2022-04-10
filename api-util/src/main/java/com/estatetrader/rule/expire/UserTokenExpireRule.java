package com.estatetrader.rule.expire;

/**
 * user token的过期判断规则
 *
 * 仅在字段不为null时，那个字段对应的规则才会生效
 *
 * 如果多个字段的值不为null，那么token必须同时满足这些规则才被判定为失效
 *
 * note: 注意，所有字段的规则之间是且的关系，不是或的关系
 *
 * 过期规则仅针对特定用户，目标用户的userId添加规则时指定，userId必须大于0
 *
 * 可以给一个用户添加多条过期规则，这些规则之间是或的关系，网关按照规则的顺序依次判断，
 * 在遇到第一个满足条件的规则时判定token过期，并使用此规则给定的reason决定错误码
 */
public class UserTokenExpireRule {
    /**
     * 在规则命中时，网关应采取的措施，续签token，还是报告客户端错误
     */
    private ExpireReason reason;

    /**
     * 根据token的签发时间是否早于给定时间判断是否过期
     * 任何规则都需要提供有效的beforeTime
     * 要求：0 < beforeTime <= now
     */
    private long beforeTime;
    /**
     * 根据token的appId判断是否过期
     */
    private Integer appId;
    /**
     * 根据token的subsystem判断是否过期
     */
    private String subsystem;
    /**
     * 根据token的角色判断是否过期
     */
    private String role;
    /**
     * 根据特定token进行判断是否过期
     */
    private String token;

    public ExpireReason getReason() {
        return reason;
    }

    public void setReason(ExpireReason reason) {
        this.reason = reason;
    }

    public long getBeforeTime() {
        return beforeTime;
    }

    public void setBeforeTime(long beforeTime) {
        this.beforeTime = beforeTime;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public void setSubsystem(String subsystem) {
        this.subsystem = subsystem;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
