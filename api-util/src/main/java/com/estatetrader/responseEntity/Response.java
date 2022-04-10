package com.estatetrader.responseEntity;

import java.io.Serializable;
import java.util.List;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

/**
 * Created by rendong on 14-5-2.
 */
@Description("接口返回值状态节点")
@GlobalEntityGroup
public class Response implements Serializable {
    @Description("当前服务端时间")
    public long systime;

    @Description("调用返回值")
    public int code;

    @Description("调用标识符")
    public String cid;

    @Description("API调用状态，code的信息请参考ApiCode定义文件")
    public List<CallState> stateList;

    @Description("后台消息，表示后端在当前请求以及当前请求之前发送给此客户端的信息。" +
        "包括上次API请求以来后端异步向此客户端发送的信息和此次API请求链路中各服务同步返回给此客户端的旁路信息")
    public List<BackendMessageResp> backendMessages;

    @Description("续签后的新user token，此字段不为空意味着本次请求所使用的token已过期但续签成功，请使用它来替换你之前保存的token")
    public String newUserToken;

    @Description("续签后的新user token的过期时间")
    public long newUserTokenExpire;

    @Description("是否需要清除utk并重新申请")
    public boolean needRenewUserToken;

    @Description("是否需要清除etk并重新申请")
    public boolean needRenewExtensionToken;
}
