package com.estatetrader.rule.expire;

/**
 * 表示过期原因
 */
public class ExpireReason {
    /**
     * 过期原因的类型，用于界定返回给客户端的错误码
     */
    public final ExpireReasonType type;
    /**
     * 额外返回给客户端的信息
     */
    public final String message;
    /**
     * 在报告错误之前，是否尝试renew token，仅在renew失败后向客户端报告错误
     */
    public final boolean tryToRenew;

    public ExpireReason(ExpireReasonType type) {
        this(type, null);
    }

    public ExpireReason(ExpireReasonType type, String message) {
        this(type, message, false);
    }

    public ExpireReason(ExpireReasonType type, String message, boolean tryToRenew) {
        this.type = type;
        this.message = message;
        this.tryToRenew = tryToRenew;
    }

    public static ExpireReason EXPIRED = new ExpireReason(ExpireReasonType.EXPIRED);
}
