package com.estatetrader.apigw.load.shipped;

import com.estatetrader.entity.AbstractReturnCode;

public class ShippedReturnCode extends AbstractReturnCode {

    public static final int _C_API_DOCUMENT_GENERATION_ERROR = 1001;

    public static final ShippedReturnCode API_DOCUMENT_GENERATION_ERROR = new ShippedReturnCode("API文档生成失败",
        _C_API_DOCUMENT_GENERATION_ERROR);

    public static final int _C_API_NOT_FOUND = 1002;

    public static final ShippedReturnCode API_NOT_FOUND = new ShippedReturnCode("指定的API不存在",
        _C_API_NOT_FOUND);

    /**
     * 初始化一个对外暴露的ReturnCode(用于客户端异常处理)
     */
    public ShippedReturnCode(String desc, int code) {
        super(desc, code);
    }
}
