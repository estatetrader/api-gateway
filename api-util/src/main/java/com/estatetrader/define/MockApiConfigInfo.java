package com.estatetrader.define;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.ExampleValue;
import com.estatetrader.responseEntity.BackendMessageResp;

import java.io.Serializable;
import java.util.List;

/**
 * mock相关的信息
 */
@Description("mockApi相关配置信息")
public class MockApiConfigInfo implements Serializable {

    @ExampleValue("[]")
    @Description("需要mock的api列表")
    public List<String> apisToMock;

    @ExampleValue("[]")
    @Description("需要录制的api列表")
    public List<String> apisToScribe;

    @Description("本次API调用时需要返回给客户端的backend-message列表，用于实现对backend-message的mock功能")
    @ExampleValue("[]")
    public List<BackendMessageResp> backendMessages;
}
