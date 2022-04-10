package com.estatetrader.gateway.backendmsg;

public class PolledBackendMessage {
    public final String key;
    public final BackendMessageBody body;

    public PolledBackendMessage(String key, BackendMessageBody body) {
        this.key = key;
        this.body = body;
    }
}
