package com.estatetrader.core;

public class ApiNotFoundException extends RuntimeException {
    public ApiNotFoundException(String methodName) {
        super("api " + methodName + " is not found");
    }
}
