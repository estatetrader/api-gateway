package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.ApiParameter;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.apigw.core.utils.RsaHelper;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.ApiParameterEncryptionMethod;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.util.AesHelper;
import com.estatetrader.util.Base64Util;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * API参数加解密传输相关的功能
 */
public interface EncryptedParameterFeature {
    @Extension
    class ApiParameterExtraInfoParserImpl implements ParsingClass.ApiParameterExtraInfoParser {
        @Override
        public void parse(Class<?> clazz,
                          Method method,
                          ApiMethodInfo apiInfo,
                          Parameter parameter,
                          ApiParameter apiParameter,
                          ApiParameterInfo pInfo) {

            pInfo.encryptionMethod = apiParameter.encryptionMethod();
        }
    }

    @Extension(after = SecurityFeature.class /*需要在token解析后进行*/)
    class ContextProcessorImpl implements RequestStarted.ContextProcessor {

        private final Logger logger = LoggerFactory.getLogger(EncryptedParameterFeature.class);
        private final RsaHelper defaultRsaHelper;
        private final AesHelper defaultAesHelper;

        public ContextProcessorImpl(@Value("${com.estatetrader.apigw.rsaPublic}") String rsaPublic,
                                    @Value("${com.estatetrader.apigw.rsaPrivate}") String rsaPrivate,
                                    @Value("${gateway.default-parameter-encryption-key}")
                                        String defaultParameterEncryptionPassword) {
            this.defaultRsaHelper = new RsaHelper(rsaPublic, rsaPrivate);
            this.defaultAesHelper = new AesHelper(Base64Util.decode(defaultParameterEncryptionPassword), null);
        }

        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) {
            processEncryptedParams(context);
        }

        private void processEncryptedParams(ApiContext context) {
            AesHelper aesHelper = null;

            for (ApiMethodCall call : context.apiCalls) {
                for (int i = 0; i < call.method.parameterInfos.length; i++) {
                    ApiParameterInfo p = call.method.parameterInfos[i];
                    if (p.encryptionMethod == null || p.encryptionMethod == ApiParameterEncryptionMethod.NONE) {
                        continue;
                    }

                    String encrypted = call.parameters[i];
                    if (encrypted == null) {
                        continue;
                    }

                    String decrypted;
                    if (p.encryptionMethod == ApiParameterEncryptionMethod.AES) {
                        if (aesHelper == null) {
                            aesHelper = context.caller != null ? new AesHelper(context.caller.key, null) :
                                defaultAesHelper;
                        }

                        try {
                            decrypted = aesHelper.decrypt(encrypted);
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("failed to decrypt " + encrypted + " by aes", e);
                            }
                            // 我们推迟了解密失败的报错，考虑如下场景：
                            // 1. 客户端拥有的token和device secret不一致，这时应让验签逻辑去处理
                            // 2. 客户端拥有一个网关无法解析的token，这时应报告token缺失错误
                            // 因此我们这里仅记录是否有解密报错的情况，在安全验证和签名验证通过后再报告错误
                            context.parameterDecryptionFailure = true;
                            return;
                        }
                    } else if (p.encryptionMethod == ApiParameterEncryptionMethod.RSA) {
                        try {
                            decrypted = defaultRsaHelper.decrypt(encrypted);
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("failed to decrypt " + encrypted + " by rsa", e);
                            }
                            // 同上
                            context.parameterDecryptionFailure = true;
                            return;
                        }

                        if (decrypted == null) {
                            // 同上
                            context.parameterDecryptionFailure = true;
                            return;
                        }
                    } else {
                        throw new IllegalStateException("invalid encryption method " + p.encryptionMethod);
                    }

                    call.parameters[i] = decrypted;
                }
            }
        }
    }

    @Extension(after = {
        // 客户端拥有一个网关无法解析的token或客户端在没有token的情况下调用了非匿名API，这时应报告token缺失错误
        SecurityFeature.class,
        // 客户端拥有的token和device secret不一致，这时应让验签逻辑去处理，报告签名错误
        RequestSignatureFeature.class
    })
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {
        @Override
        public void verify(ApiContext context) throws GatewayException {
            if (context.parameterDecryptionFailure) {
                throw new GatewayException(ApiReturnCode.PARAMETER_DECRYPT_ERROR);
            }
        }
    }
}
