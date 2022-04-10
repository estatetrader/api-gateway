package com.estatetrader.responseEntity;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

import java.io.Serializable;

@Description("后台发送给前端的信息，独立于标准API返回值之外的旁路信息，用于实现API无关的通用业务框架，例如发放积分/优惠券等")
@GlobalEntityGroup
public final class BackendMessageResp implements Serializable {
    @Description("发送给客户端的信息内容，内容的格式由前后端根据type的不同进行约定")
    public String content;

    @Description("发送给客户端的信息类型，前后端应协商确定系统中有哪些合法的信息类型，以及每种信息类型对应的content格式。" +
        "信息类型应在整个系统中唯一，并具有确定的含义。客户端应忽略其无法处理的消息（或汇报相应日志，但不应报错）")
    public String type;

    @Description("信息所属的服务，作为type的命名空间，由后端决定。前端可根据不同的服务对type的语义进行更进一步的区分")
    public String service;

    /**
     * 消息的静默时间，单位毫秒，客户端应在此字段指定的时间内，忽略同type/service的消息
     *
     * 基于性能上的考虑，当后端向所有客户端广播一条消息时，我们没法精确保证每个客户端只读一次。
     * 理想情况下，在有广播消息时，对于每个客户端，只有它的第一次请求能够读取此消息，
     * 但是标记此广播消息已被此设备读取对于网关来说难度比较大（CPU时间、内存占用等问题），
     * 所以，当有广播消息时，每个客户端在每次请求时都会读取到。
     *
     * 这种做法会导致客户端收到重复的后台信息，为了解决这个问题，我们引入了静默周期（quietPeriod）
     *
     * 表示在第一次收到此信息后，在quietPeriod毫秒内如果收到同type/service的消息，客户端应直接丢弃该消息
     */
    @Description("消息的静默时间，单位毫秒，客户端应在此字段指定的时间内，忽略同type/service的消息")
    public int quietPeriod;

    public BackendMessageResp() {}

    public BackendMessageResp(String content, String type, String service, int quietPeriod) {
        this.content = content;
        this.type = type;
        this.service = service;
        this.quietPeriod = quietPeriod;
    }
}