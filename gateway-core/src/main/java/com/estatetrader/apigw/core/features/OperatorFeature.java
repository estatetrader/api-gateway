package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.request.RequestFinished;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.support.CookieSupport;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.ConstField;
import com.estatetrader.define.CookieName;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * API操作人相关，不建议使用
 */
public interface OperatorFeature {

    @Extension
    class ParameterParserImpl implements RequestStarted.ParameterParser {
        @Override
        public void parse(ApiContext context) {
            context.operator = context.getRequest().getParameter(CommonParameter.operator);
        }
    }

    @Extension
    class CookieParserImpl implements RequestStarted.CookieParser {

        static Logger logger = LoggerFactory.getLogger(OperatorFeature.class);

        @Override
        public void parse(ApiContext context) {
            String cookie = context.request.getCookieValue(context.appId + CookieName.operatorToken);
            try {
                if (context.operator == null && cookie != null && !cookie.isEmpty()) {
                    context.operator = URLDecoder.decode(cookie, StandardCharsets.UTF_8.name());
                }
            } catch (UnsupportedEncodingException e) {
                logger.error("failed to parse operator cookie: " + cookie, e);
                context.clearOperatorToken = true;
            }
        }
    }

    @Extension
    class AutowiredParameterValueProviderImpl implements CallStarted.AutowiredParameterValueProvider {
        /**
         * 注入可注入的参数
         * <p>
         * 根据call和info判断你是否能够为当前参数提供注入，并将最终注入结果作为返回值返回出去
         * 如果你不能为当前参数提供注入，则调用next.go()将机会留给其他注入器
         *
         * @param call    当前待注入的API call
         * @param info    当前待注入的参数信息
         * @param context 请求上下文
         * @param next    如果你需要其他注入器提供注入，则调用next.go()
         * @return 参数最终要注入的值
         * @throws GatewayException 抛出错误码
         */
        @Override
        public String autowire(ApiMethodCall call,
                               ApiParameterInfo info,
                               ApiContext context,
                               Next<String, GatewayException> next) throws GatewayException {
            if (CommonParameter.operator.equals(info.name)) {
                return context.operator;
            }
            return next.go();
        }
    }

    @Extension
    class NotificationProcessorImpl implements CallResultReceived.NotificationProcessor, CookieSupport {
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
        public void process(String name, String value,
                            ApiContext context,
                            ApiMethodCall call,
                            Next.NoResult<IOException> next) throws IOException {

            if (!ConstField.SET_COOKIE_OPERATOR.equals(name)) {
                next.go();
                return;
            }

            if (value != null && value.length() > 0) {
                dispatchCookie(context.getResponse(),
                    context.appId + CookieName.operatorToken,
                    URLEncoder.encode(value, "UTF-8"), c -> {

                        c.setMaxAge(3600 * 24 * 30);
                        c.setHttpOnly(true);
                    });
            }
        }
    }

    @Extension
    class CookieDispatcherImpl implements RequestFinished.CookieDispatcher, CookieSupport {
        @Override
        public void dispatch(ApiContext context) {
            if (context.clearOperatorToken) {
                // 删除 cookie 标志位
                dispatchCookie(context.getResponse(), context.appId + CookieName.operatorToken, c -> {
                    c.setMaxAge(0);
                    c.setHttpOnly(true);
                });
            }
        }
    }
}
