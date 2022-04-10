package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.DesignedErrorCode;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.define.ConstField;
import com.estatetrader.entity.*;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.phases.executing.access.CallFinished;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.parsing.ApiRegister;
import com.estatetrader.apigw.core.phases.parsing.ApiVerifier;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.apigw.core.utils.ClassUtil;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.util.Lambda;
import com.estatetrader.annotation.ApiGroup;
import com.estatetrader.annotation.HttpApi;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 错误码相关功能
 */
public interface ErrorCodeFeature {

    static List<AbstractReturnCode> parseCodesInClass(Class<?> clazz) {
        List<AbstractReturnCode> codes = new ArrayList<>();

        for (Field f : clazz.getDeclaredFields()) {
            if (ClassUtil.isConstField(f) &&
                AbstractReturnCode.class.isAssignableFrom(f.getType())) {

                AbstractReturnCode code;
                try {
                    code = (AbstractReturnCode)f.get(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }

                code.setName(f.getName());
                codes.add(code);
            }
        }

        return codes;
    }

    @Extension
    class ParseClassBriefHandlerImpl implements ParsingClass.ParseClassBriefHandler {

        @Override
        public void parseClassBrief(Class<?> clazz, ApiGroupInfo groupInfo) {
            ApiGroup groupAnnotation = clazz.getAnnotation(ApiGroup.class);

            groupInfo.minCode = groupAnnotation.minCode();
            groupInfo.maxCode = groupAnnotation.maxCode();
            groupInfo.codes = new ArrayList<>();

            for (AbstractReturnCode code : parseCodesInClass(groupAnnotation.codeDefine())) {
                if (code.getCode() < groupInfo.minCode || code.getCode() >= groupInfo.maxCode) {
                    throw new RuntimeException(
                        "code " + code.getName() + " which value is " + code.getCode() + " " +
                            "not in the scope [" + groupInfo.minCode + "," + groupInfo.maxCode + ") " +
                            "in " + groupInfo.name);
                }

                code.setService(groupInfo.name);
                groupInfo.codes.add(code);
            }
        }
    }

    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {

        @Override
        public void parseMethodBrief(Class<?> clazz,
                                     Method method,
                                     ApiMethodInfo apiInfo,
                                     ServiceInstance serviceInstance) {

            HttpApi api = method.getAnnotation(HttpApi.class);
            DesignedErrorCode errors = method.getAnnotation(DesignedErrorCode.class);
            if (errors == null) return;
            int[] es = errors.value();
            if (es.length == 0) return;

            // 避免重复定义error code 以及避免暴露隐藏code
            HashSet<Integer> set = new HashSet<>();
            for (int i : es) {
                if (!set.add(i)) {
                    throw new IllegalApiDefinitionException("duplicate error code " + i +
                        " in " + clazz.getName() + " " + api.name());
                }
            }

            // error code 排序
            Arrays.sort(es);
            apiInfo.errors = es;
        }
    }

    @Extension
    class ApiRegisterImpl implements ApiRegister {

        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            if (info.groupInfo == null || info.groupInfo.codes == null) {
                return;
            }

            for (AbstractReturnCode code : info.groupInfo.codes) {
                List<AbstractReturnCode> list = schema.returnCodes.computeIfAbsent(code.getCode(),
                    key -> new ArrayList<>());

                if (!list.contains(code)) {
                    list.add(code);
                }
            }
        }
    }

    @Extension
    class ApiVerifierImpl implements ApiVerifier {
        @Override
        public void verify(ApiMethodInfo info, ApiSchema schema) {
            if (info.errors == null) return;

            for (int code : info.errors) {
                if (code <= 0) continue;

                List<AbstractReturnCode> list = schema.returnCodes.get(code);
                if (list == null) {
                    throw new IllegalApiDefinitionException("code " + code + " is not defined");
                }

                if (Lambda.all(list, x -> x.getDisplay() != x)) {
                    throw new IllegalApiDefinitionException("cannot use a shadow code as a designed code " + code);
                }
            }
        }
    }

    @Extension
    class NotificationProcessorImpl implements CallResultReceived.NotificationProcessor {

        private final Logger logger = LoggerFactory.getLogger(ErrorCodeFeature.class);

        /**
         * 处理来自后端服务器返回的旁路信息
         * 如果你对某个信息不感兴趣，请调用next.go()让其他处理器处理
         *
         * @param name    旁路信息的名称
         * @param value   信息的内容
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param next    调用next.go()让其他旁路信息处理器处理
         */
        @Override
        public void process(String name,
                            String value,
                            ApiContext context,
                            ApiMethodCall call,
                            Next.NoResult<IOException> next) throws IOException {
            if (!ConstField.REQUEST_ERROR_CODE_EXT.equals(name)) {
                next.go();
                return;
            }

            try {
                int c = Integer.parseInt(value);
                context.requestErrorCode = new ApiReturnCode("RequestErrorCodeFromServer", c);
            } catch (NumberFormatException e) {
                logger.warn("service return an illegal code " + value, e);
            }
        }
    }

    @Extension
    class CallExceptionHandlerImpl implements CallFinished.CallExceptionHandler {

        private final Logger logger = LoggerFactory.getLogger(ErrorCodeFeature.class);
        private final Extensions<ReturnCodeTransformer> returnCodeTransformers;

        public CallExceptionHandlerImpl(Extensions<ReturnCodeTransformer> returnCodeTransformers) {
            this.returnCodeTransformers = returnCodeTransformers;
        }

        @Override
        public void handle(ApiContext context,
                           ApiMethodCall call,
                           Throwable throwable,
                           Next.NoResult<RuntimeException> next) {

            AbstractReturnCode code = exceptionToCode(throwable);

            AbstractReturnCode finalCode = returnCodeTransformers
                .chain(ReturnCodeTransformer::transform, context, call, code).go();

            call.setCode(finalCode);
        }

        private AbstractReturnCode exceptionToCode(Throwable throwable) {
            if (throwable instanceof GatewayException) {
                GatewayException ge = (GatewayException) throwable;
                if (ge.getCause() != null && logger.isWarnEnabled()) {
                    logger.warn("gateway exception: " + ge.getMessage(), ge);
                }

                return ge.getCode();
            }

            if (throwable instanceof ReturnCodeException) {
                ReturnCodeException rce = (ReturnCodeException) throwable;
                if (rce.getCode() == ApiReturnCode.PARAMETER_ERROR) {
                    logger.warn("servlet catch an api error. " + rce.getMessage(), rce);
                } else {
                    logger.warn("servlet catch an api error.", rce);
                }

                return rce.getCode();
            }

            if (throwable instanceof ServiceException) {
                ServiceException se = (ServiceException) throwable;
                logger.warn("service exception. code:" + se.getCode() + " msg:" + se.getMsg(), se);
                return new ApiReturnCode(
                    se.getCode(),
                    se.getDisplayCode(),
                    se.getCode() == se.getDisplayCode() ? se.getMsg() : se.getDescription()
                );
            }

            if (throwable instanceof ServiceRuntimeException) {
                ServiceRuntimeException se = (ServiceRuntimeException) throwable;
                logger.warn("service exception. code:" + se.getCode() + " msg:" + se.getMsg(), se);
                return new ApiReturnCode(
                    se.getCode(),
                    se.getDisplayCode(),
                    se.getCode() == se.getDisplayCode() ? se.getMsg() : se.getDescription()
                );
            }

            if (throwable.getCause() instanceof ServiceException) {
                ServiceException se = (ServiceException) throwable.getCause();
                logger.warn("service exception. code:" + se.getCode() + " msg:" + se.getMsg(), se);
                return new ApiReturnCode(
                    se.getCode(),
                    se.getDisplayCode(),
                    se.getCode() == se.getDisplayCode() ? se.getMsg() : se.getDescription()
                );
            }

            if (throwable instanceof TimeoutException ||
                throwable.getCause() instanceof TimeoutException) {
                logger.warn("dubbo timeout.", throwable);
                return ApiReturnCode.DUBBO_SERVICE_TIMEOUT_ERROR;
            }

            if (throwable instanceof RpcException ||
                throwable.getCause() instanceof RpcException) {
                // TODO: refine this code, change dubbo source to add error code when this RpcException init
                if (throwable.getMessage() == null ||
                    throwable.getMessage().lastIndexOf("The service using threads greater than") < 0) {

                    logger.warn("dubbo exception.", throwable);
                }

                return ApiReturnCode.DUBBO_SERVICE_NOTFOUND_ERROR;
            }

            logger.error("internal error.", throwable);
            return ApiReturnCode.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 错误码转换器
     */
    interface ReturnCodeTransformer {
        /**
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param code    待转换的错误码
         * @param next    如果无法转换，则调用next.got让其他转换器转换
         * @return 返回转换后的code
         */
        AbstractReturnCode transform(ApiContext context,
                                     ApiMethodCall call,
                                     AbstractReturnCode code,
                                     Next<AbstractReturnCode, RuntimeException> next);
    }

    @Extension(last = true)
    class DefaultReturnCodeTransformer implements ReturnCodeTransformer {
        /**
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param code    待转换的错误码
         * @param next    如果无法转换，则调用next.got让其他转换器转换
         * @return 返回转换后的code
         */
        @Override
        public AbstractReturnCode transform(ApiContext context,
                                            ApiMethodCall call,
                                            AbstractReturnCode code,
                                            Next<AbstractReturnCode, RuntimeException> next) {
            int display = code.getDisplay().getCode();

            if (display <= 0) {
                return code;
            }

            if (call.method.errors != null &&
                Arrays.binarySearch(call.method.errors, display) >= 0) {
                return code;
            }

            return ApiReturnCode.UNKNOWN_ERROR;
        }
    }
}
