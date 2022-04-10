package com.estatetrader.core;

public class IllegalConfigException extends RuntimeException {
    public IllegalConfigException(String configName, String detailedMessage) {
        super("config " + configName + " is invalid" + (detailedMessage != null ? ": " + detailedMessage : ""));
    }
}
