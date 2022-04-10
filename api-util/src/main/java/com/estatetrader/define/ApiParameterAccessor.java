package com.estatetrader.define;

import com.estatetrader.core.GatewayException;

public interface ApiParameterAccessor {
    Object getParameter(String name) throws GatewayException;
    String getCommonParameter(String name) throws GatewayException;
}
