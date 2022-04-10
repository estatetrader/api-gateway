package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.utils.EccHelper;
import com.estatetrader.apigw.core.utils.RsaHelper;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.responseEntity.RenewTokenResult;
import com.estatetrader.util.Base64Util;
import com.estatetrader.util.HexStringUtil;
import com.estatetrader.util.Md5Util;
import com.estatetrader.util.SHAUtil;
import com.estatetrader.apigw.core.models.SignatureType;
import com.estatetrader.apigw.core.models.ApiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * 请求签名校验
 */
public interface RequestSignatureFeature {

    @Component
    class Config {
        @SuppressWarnings("FieldMayBeFinal")
        @Value("${gateway.enable.request-signature:true}")
        private boolean enableRequestSignature = true;
    }

    // 签名验证要早于基础权限验证，但是由于客户端代码没有完全准备好，不能很好维护tk的状态，所以此处临时要求签名晚于基础权限验证
    @Extension(after = SecurityFeature.class)
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {

        private final Extensions<SignatureVerifier> verifiers;
        private final VerificationProcessor processor;
        private final Config config;

        public RequestVerifierImpl(Extensions<SignatureVerifier> verifiers,
                                   Extensions<VerificationProcessor> processor,
                                   Config config) {
            this.verifiers = verifiers;
            this.processor = processor.singleton();
            this.config = config;
        }

        @Override
        public void verify(ApiContext context) throws GatewayException {
            if (!config.enableRequestSignature) {
                return;
            }
            GatewayRequest request = context.request;
            // 拼装被签名参数列表
            String requestText = processor.getRequestText(request.getParameterNames(), request::getParameter);
            // 客户端传入的签名
            String signature = request.getParameter(CommonParameter.signature);
            SignatureType signatureType = verifiers
                .chain(SignatureVerifier::verify, context, requestText, signature)
                .go();

            context.signatureType = signatureType.getDescription();
        }
    }

    interface SignatureVerifier {

        /**
         * 验证请求中携带的签名是否合法，并在不合法时抛出签名错误相关的异常
         *
         * @param context 请求上下文
         * @param requestText 本次请求中需要校验的内容
         * @param signature 客户端提供的签名
         * @param next 如果需要下一个verifier执行校验，请调用 next.go()
         *
         * @return 在校验成功后返回此次校验使用的签名类型
         */
        SignatureType verify(ApiContext context,
                             String requestText,
                             String signature,
                             Next<SignatureType, GatewayException> next) throws GatewayException;
    }

    @Extension(first = true)
    class SignatureWhitelist implements SignatureVerifier {

        private static final String DEBUG_AGENT = "client.tester";

        private final Logger logger = LoggerFactory.getLogger(RequestSignatureFeature.class);

        private final boolean debugAgentEnabled;

        public SignatureWhitelist(@Value("${gateway.signature.debug-agent-enabled:false}") boolean debugAgentEnabled) {
            this.debugAgentEnabled = debugAgentEnabled;
        }

        /**
         * 验证请求中携带的签名是否合法，并在不合法时抛出签名错误相关的异常
         *
         * @param context     请求上下文
         * @param requestText 本次请求中需要校验的内容
         * @param signature   客户端提供的签名
         * @param next        如果需要下一个verifier执行校验，请调用 next.go()
         */
        @Override
        public SignatureType verify(ApiContext context,
                                    String requestText,
                                    String signature,
                                    Next<SignatureType, GatewayException> next) throws GatewayException {
            if (debugAgentEnabled &&
                context.agent != null &&
                context.agent.contains(DEBUG_AGENT)) {

                logger.debug("check signature is skipped by tester user agent {}", context.agent);
                return SignatureType.NONE;
            }

            if (context.fromInternalEnvironment) {
                logger.debug("check signature is skipped by internal environment");
                return SignatureType.NONE;
            }

            if (SecurityType.Internal.check(context.requiredSecurity)) {
                //internal接口不做签名验证
                logger.debug("check signature is skipped by internal API");
                return SignatureType.NONE;
            }

            CallerInfo caller = context.caller;

            //noinspection
            if (caller != null && (SecurityType.InternalUser.check(caller.securityLevel) || caller.appid == 10)) {
                //boss 用户目前没有验签直接跳过
                logger.warn("check signature is skipped by old BOSS");
                return SignatureType.NONE;
            }

            // 执行校验
            return next.go();
        }
    }

    @Extension(last = true)
    class SignatureVerifierImpl implements SignatureVerifier {

        private final VerificationProcessor processor;

        public SignatureVerifierImpl(Extensions<VerificationProcessor> processor) {
            this.processor = processor.singleton();
        }

        /**
         * 验证请求中携带的签名是否合法，并在不合法时抛出签名错误相关的异常
         *
         * @param context     请求上下文
         * @param requestText 本次请求中需要校验的内容
         * @param signature   客户端提供的签名
         * @param next        如果需要下一个verifier执行校验，请调用 next.go()
         */
        @Override
        public SignatureType verify(ApiContext context,
                                    String requestText,
                                    String signature,
                                    Next<SignatureType, GatewayException> next) throws GatewayException {
            String sm = context.getRequest().getParameter(CommonParameter.signatureMethod);
            boolean allAnon = SecurityType.isNone(context.requiredSecurity);

            VerificationContext verificationContext = new VerificationContext(
                context.appId,
                context.token,
                context.caller,
                context.deviceCaller,
                context
            );

            return processor.verifySignature(requestText, signature, sm, allAnon, verificationContext);
        }
    }

    class VerificationContext {
        @SuppressWarnings("WeakerAccess")
        public final int appId;
        public final String token;
        public final CallerInfo caller;
        @SuppressWarnings("WeakerAccess")
        public final CallerInfo deviceCaller;
        @SuppressWarnings("WeakerAccess")
        public final ApiContext apiContext;

        @SuppressWarnings("WeakerAccess")
        public VerificationContext(int appId,
                                   String token,
                                   CallerInfo caller,
                                   CallerInfo deviceCaller) {
            this(appId, token, caller, deviceCaller, null);
        }

        @SuppressWarnings("WeakerAccess")
        public VerificationContext(int appId,
                                   String token,
                                   CallerInfo caller,
                                   CallerInfo deviceCaller,
                                   ApiContext apiContext) {
            this.appId = appId;
            this.token = token;
            this.caller = caller;
            this.deviceCaller = deviceCaller;
            this.apiContext = apiContext;
        }
    }

    interface VerificationProcessor {

        /**
         * 计算用于计算请求签名输入源的请求文本
         *
         * @param parameterNames 请求中包含的参数列表
         * @param parameterValues 请求中每个参数对应的参数值
         * @return 返回用于请求签名的请求文本
         */
        String getRequestText(Iterable<String> parameterNames, Function<String, String> parameterValues);

        /**
         * 验证请求中携带的签名是否合法，并在不合法时抛出签名错误相关的异常
         * @param requestText 本次请求中需要校验的内容
         * @param signature 客户端提供的签名
         * @param signatureMethod 客户端计算签名值时使用的算法
         * @param allowStaticSalt 是否允许使用静态盐（仅在本次请求的API均为匿名API时为true）
         * @param context 签名核对的上下文信息，包括token/appId等信息
         *
         * @return 在校验成功后返回此次校验使用的签名类型
         *
         * @throws GatewayException 在签名不匹配时抛出相应的错误码
         */
        SignatureType verifySignature(String requestText,
                           String signature,
                           String signatureMethod,
                           boolean allowStaticSalt,
                           VerificationContext context) throws GatewayException;
    }

    @Extension
    class VerificationProcessorImpl implements VerificationProcessor {

        private final Logger logger = LoggerFactory.getLogger(RequestSignatureFeature.class);

        private final String staticSignPwd;
        private final SecurityFeature.Config securityConfig;

        public VerificationProcessorImpl(@Value("${gateway.static-signature-password}")
                                                             String staticSignPwd,
                                         SecurityFeature.Config securityConfig) {
            this.staticSignPwd = staticSignPwd;
            this.securityConfig = securityConfig;
        }

        /**
         * 计算用于计算请求签名输入源的请求文本
         *
         * @param parameterNames 请求中包含的参数列表
         * @param parameterValues 请求中每个参数对应的参数值
         * @return 返回用于请求签名的请求文本
         */
        @Override
        public String getRequestText(Iterable<String> parameterNames, Function<String, String> parameterValues) {
            StringBuilder sb = new StringBuilder(128);

            List<String> list = new ArrayList<>(10);
            for (String key : parameterNames) {
                list.add(key);
            }

            // 参数排序
            list.sort(String::compareTo);

            // 拼装被签名参数列表
            for (String key : list) {
                if (CommonParameter.signature.equals(key)) {
                    continue;
                }
                sb.append(key);
                sb.append("=" );
                sb.append(parameterValues.apply(key));
            }
            return sb.toString();
        }

        /**
         * 验证请求中携带的签名是否合法，并在不合法时抛出签名错误相关的异常
         * @param requestText 本次请求中需要校验的内容
         * @param signature 客户端提供的签名
         * @param signatureMethod 客户端计算签名值时使用的算法
         * @param allowStaticSalt 是否允许使用静态盐（仅在本次请求的API均为匿名API时为true）
         * @param context 签名核对的上下文信息，包括token/appId等信息
         *
         * @throws GatewayException 在签名不匹配时抛出相应的错误码
         */
        @Override
        public SignatureType verifySignature(String requestText,
                           String signature,
                           String signatureMethod,
                           boolean allowStaticSalt,
                           VerificationContext context) throws GatewayException {

            logger.debug("check signature request text {} against {}", requestText, signature);

            if (signature == null || signature.isEmpty()) {
                throw new GatewayException(ApiReturnCode.UNKNOWN_SIGNATURE_ERROR);
            }

            // 1. 优先使用动态盐
            if (context.caller != null &&
                checkSignatureByDynamicSalt(requestText, signature, signatureMethod, context.caller.key)) {
                // 使用动态盐校验成功
                // 这里要求：客户端如果请求中传递了token，则其应该使用动态盐进行签名
                return SignatureType.DYNAMIC_SALT;
            }

            // TODO 删除此兼容性代码（和iOS确认后删除该兼容性代码）
            // 1.1 使用设备凭据中的动态盐
            if (context.deviceCaller != null && context.appId == 3 &&
                checkSignatureByDynamicSalt(requestText, signature, signatureMethod, context.deviceCaller.key)) {
                // 兼容某些客户端app，它们由于特殊原因导致了user token和device secret不一致，所以尝试使用device token验证
                logger.warn("invalid utk key found, currently we use dtk to pass the request signature");

                if (context.caller != null && context.apiContext != null && !context.apiContext.userTokenRenewed) {
                    // dtk的key通过了签名验证而utk没有，为了协助客户端恢复，我们使用dtk中的key取代utk中的key
                    context.caller.key = context.deviceCaller.key;

                    RenewTokenResult renewResult = new RenewTokenResult();
                    renewResult.expire = context.caller.expire + context.caller.renewWindow;
                    renewResult.newToken = securityConfig.getAesTokenHelper().generateStringUserToken(context.caller);

                    // 构造一个新的utk，让客户端更新
                    context.apiContext.newUserTokenResult = renewResult;

                    logger.info("renew user token {} using the key from device token to {}",
                        context.token, renewResult.newToken);
                }

                return SignatureType.DYNAMIC_SALT;
            }

            // 2. 对于全匿名API的请求，可以降级为静态盐
            if (allowStaticSalt && checkSignatureByStaticSalt(requestText, signature, signatureMethod)) {
                /*
                 * 使用静态盐校验成功
                 * 当切仅当客户端没有可用的token（包括dtk和utk）并且本次请求的所有API均为匿名时，才应该使用静态盐
                 *
                 * 这里我们兼容了客户端的已有逻辑，即如果请求均为匿名时，客户端可以自由选择动态盐和静态盐，但推荐使用动态盐
                 * 例外：在特殊情况下，客户端存在合法的utk/dtk但是却缺失device secret/csrf token，
                 * 在这种情况下我们允许客户端使用静态盐去访问匿名API
                 */
                if (context.caller != null) {
                    // 如果客户端拥有有效token，则应使用动态盐，这里我们做临时兼容

                    logger.warn("client should use dynamic salt signature");

                    // TODO: 在所有客户端接入新验签逻辑之后，取消下面的注释，用于强制使用新签名逻辑
                    // throw new GatewayException(ApiReturnCode.DYNAMIC_SALT_SIGNATURE_ERROR);
                }
                return SignatureType.STATIC_SALT;
            }

            // 3. 分析、推测验签出错的原因
            if (checkSignatureByStaticSalt(requestText, signature, signatureMethod)) {
                // 静态盐校验通过，但是由于非匿名API的存在，所有最终仍然是签名不匹配
                throw new GatewayException(ApiReturnCode.DYNAMIC_SALT_SIGNATURE_ERROR);
            } else if (context.caller != null || context.token != null) {

                /* token存在，我们需要使用动态盐，但是动态盐有两种，一种来源于user token，一种来源于device token
                 * 我们使用不同的错误码来指示客户端清理相应的token，因为验签错误可能是token与secret不一致，
                 * 清理相应token并从服务端重新获取新的token可以解决这个问题
                 *
                 * 注意：由于历史原因，某些客户端会在全匿名API时使用静态盐（而不是在token存在时使用动态盐）
                 * 这种现象我们不在下面的错误码中区分。换句话说，即使客户端使用的是静态盐（错误的静态盐或其他原因），
                 * 我们也会提示它是utk或者dtk不匹配。
                 *
                 * 在token解析失败时，我们仍然报告-180/-181，这样可以可以协助客户端删除有问题的tk
                 */

                if (context.caller != null && context.caller.uid > 0 ||
                    context.token != null && (
                        context.token.startsWith("utk_") || context.token.startsWith("ntk_"))) {
                    // user token (utk/ntk)

                    throw new GatewayException(ApiReturnCode.USER_TOKEN_SIGNATURE_ERROR);
                } else {
                    throw new GatewayException(ApiReturnCode.DEVICE_TOKEN_SIGNATURE_ERROR);
                }
            } else {
                /* 在此分支中，签名错误可能由以下原因造成的：
                 * 1. 客户端本地没有dtk/utk，但是却使用了动态盐签名策略，可以通过清理device secret解决
                 * 2. 拼接请求文本的算法不正确，将null/undefined等无法传递给网关的参数拼接进请求文本（无法修复）
                 * 3. 使用了_tk的http header方式传递token，但是在某些网络中这种非标准header被过滤（无法修复）
                 * 为了协助客户端尽可能从不一致困境中走出来，网关此时提示客户端-181，以期待客户端清理动态盐和did
                 */
                throw new GatewayException(ApiReturnCode.DEVICE_TOKEN_SIGNATURE_ERROR);
            }
        }

        private boolean checkSignatureByStaticSalt(String requestText, String sig, String sm) {
            byte[] bytes = (requestText + staticSignPwd).getBytes(StandardCharsets.UTF_8);

            if ("md5".equalsIgnoreCase(sm)) {
                byte[] expect = HexStringUtil.toByteArray(sig);
                byte[] actual = Md5Util.compute(bytes);
                return Arrays.equals(expect, actual);
            } else {// 默认使用sha1
                byte[] expect;
                try {
                    expect = Base64Util.decode(sig);
                } catch (IllegalArgumentException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("failed to base64 decode " + sig, e);
                    }

                    return false;
                }

                byte[] actual = SHAUtil.computeSHA1(bytes);
                return Arrays.equals(expect, actual);
            }
        }

        private boolean checkSignatureByDynamicSalt(String requestText, String sig, String sm, byte[] key) {

            if ("rsa".equalsIgnoreCase(sm)) {
                // RSA 配合 base64 编码的签名 用于app端

                byte[] bytes = requestText.getBytes(StandardCharsets.UTF_8);
                return RsaHelper.verify(Base64Util.decode(sig), bytes, key);
            }

            if ("md5".equalsIgnoreCase(sm)) {
                // MD5 配合 hex 编码的签名 用于web端

                String text = requestText + HexStringUtil.toHexString(key);
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                return Arrays.equals(HexStringUtil.toByteArray(sig), Md5Util.compute(bytes));
            }

            if ("sha1".equalsIgnoreCase(sm)) {
                // SHA1 配合 base64 编码的签名 用于app端

                String text = requestText + HexStringUtil.toHexString(key);
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

                return Arrays.equals(Base64Util.decode(sig), SHAUtil.computeSHA1(bytes));
            }

            // ECC 配合 base64 编码的签名 用于app端
            // 默认ECC
            return EccHelper.verify(Base64Util.decode(sig), requestText.getBytes(StandardCharsets.UTF_8), key);
        }
    }
}
