package com.estatetrader.define;

public class IllegalApiDefinitionException extends RuntimeException {
    public IllegalApiDefinitionException(String message, Throwable t) {
        super(message, t);
    }

    public IllegalApiDefinitionException(Throwable t) {
        super(t);
    }

    public IllegalApiDefinitionException(String message) {
        super(message);
    }
}
