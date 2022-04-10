package com.estatetrader.apigw.core.phases.parsing;

import com.estatetrader.apigw.core.models.ApiSchema;

/**
 * 用于解析基本信息
 */
public interface CommonInfoParser {
    /**
     * 解析基本信息
     * @param schema 用于接收解析结果
     */
    void parse(ApiSchema schema);
}
