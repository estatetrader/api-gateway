package com.estatetrader.gateway.backendmsg;

import java.io.Serializable;

/**
 * 处于队列中的后台消息
 */
public class QueuedBackendMessage implements Serializable {
    /**
     * 消息的唯一ID，由业务系统给出，用于实现消息的幂等发送
     */
    public String messageKey;
    /**
     * 消息体，描述了要发送的消息的具体内容，须使用强类型化的序列化工具
     */
    public BackendMessageBody body;
    /**
     * 消息的发送条件，这些条件将在网关校验
     */
    public BackendMessageCondition condition;
    /**
     * 时间戳，表示消息将在指定的时间后过期
     */
    public long expiredAt;

    @Override
    public String toString() {
        return "QueuedBackendMessage{" +
                "messageKey='" + messageKey + '\'' +
                ", body=" + body +
                ", condition=" + condition +
                ", expiredAt=" + expiredAt +
                '}';
    }
}
