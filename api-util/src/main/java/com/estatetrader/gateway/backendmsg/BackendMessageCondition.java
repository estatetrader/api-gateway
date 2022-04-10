package com.estatetrader.gateway.backendmsg;

import java.io.Serializable;
import java.util.List;

/**
 * 一些场景下，我们希望发送给某个用户/设备的消息仅在这个用户/设备在特定的请求下才能读取到
 *
 * 为了满足这种需求，后端在发送消息时可以选择性指定一个filter，在filter中约束能够读取这条消息的条件
 */
public class BackendMessageCondition implements Serializable {
    /**
     * 若不为null并且不为空，表示仅将消息发送给指定的一组appId
     */
    public List<Integer> onlyAppIds;

    /**
     * 若不为null且不为空，表示仅在发起特定API请求时读取消息
     */
    public List<String> onlyApis;

    /**
     * 若不为null且不为空，则表示仅在发起的API调用不在指定的API黑名单中才读取消息，用于屏蔽某些API请求的消息读取过程
     */
    public List<String> ignoredApis;

    @Override
    public String toString() {
        return "BackendMessageCondition{" +
                "onlyAppIds=" + onlyAppIds +
                ", onlyApis=" + onlyApis +
                ", ignoredApis=" + ignoredApis +
                '}';
    }
}
