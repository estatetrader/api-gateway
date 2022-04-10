package com.estatetrader.gateway.backendmsg;

import java.io.Serializable;

/**
 * 后台消息
 */
public class BackendMessageBody implements Serializable {
    /**
     * 消息类型，用于协助客户端如何处理消息体
     */
    public String type;

    /**
     * 消息内容，具体格式由type决定，
     * 为了允许网关介入content的序列化过程（例如实现page link功能），我们需要将content声明为Object类型
     */
    public Serializable content;

    /**
     * 消息所属的服务，可以作为messageType的命名空间
     */
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
    public int quietPeriod;

    @Override
    public String toString() {
        return "BackendMessageBody{" +
                "type='" + type + '\'' +
                ", content=" + content +
                ", service='" + service + '\'' +
                ", quietPeriod=" + quietPeriod +
                '}';
    }
}
