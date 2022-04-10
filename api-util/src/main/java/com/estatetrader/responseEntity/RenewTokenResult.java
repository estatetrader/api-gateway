package com.estatetrader.responseEntity;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

import java.io.Serializable;

@Description("token续签结果")
@GlobalEntityGroup
public class RenewTokenResult implements Serializable {

    @Description("续签后的新token")
    public String newToken;

    @Description("新token的过期时间，对于客户端而言，在这个时间点之后，新token将不可用（过期并且不可续签）")
    public long expire;
}
