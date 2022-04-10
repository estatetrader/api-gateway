package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.define.ConstField;

import java.io.IOException;

public interface HttpETagFeature {

    @Extension
    class NotificationProcessorImpl implements CallResultReceived.NotificationProcessor {

        static final String HEADER_ETAG = "ETag";

        /**
         * 处理来自后端服务器返回的旁路信息
         * 如果你对某个信息不感兴趣，请调用next.go()让其他处理器处理
         *
         * @param name    旁路信息的名称
         * @param value   信息的内容
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param next    调用next.go()让其他旁路信息处理器处理
         * @throws IOException 抛出异常
         */
        @Override
        public void process(String name,
                            String value,
                            ApiContext context,
                            ApiMethodCall call,
                            Next.NoResult<IOException> next) throws IOException {

            if (ConstField.NOTIFICATION_ETAG.equals(name)) {
                context.response.setHeader(HEADER_ETAG, value);
                return;
            }

            next.go();
        }
    }
}
