package com.estatetrader.apigw.core.support;

import com.estatetrader.apigw.core.contracts.GatewayCookie;
import com.estatetrader.apigw.core.contracts.GatewayResponse;

import java.util.function.Consumer;

public interface CookieSupport {

    default void dispatchCookie(GatewayResponse response, String name, Consumer<GatewayCookie> settings) {
        dispatchCookie(response, name, "", settings);
    }

    default void dispatchCookie(GatewayResponse response, String name, String value, Consumer<GatewayCookie> settings) {
        response.setCookie(name, value, cookie -> {
            cookie.setPath("/");
            if (settings != null) {
                settings.accept(cookie);
            }
        });
    }
}
