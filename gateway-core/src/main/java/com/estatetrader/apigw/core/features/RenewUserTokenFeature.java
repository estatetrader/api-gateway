package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.RenewUserTokenProvider;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.phases.parsing.ApiRegister;
import com.estatetrader.apigw.core.phases.parsing.ApiVerifier;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.ApiOpenState;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.phases.executing.request.RequestFinished;
import com.estatetrader.responseEntity.RenewTokenResult;
import com.estatetrader.responseEntity.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 续签用户凭据
 */
public interface RenewUserTokenFeature {

    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {

        @Override
        public void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo apiInfo, ServiceInstance serviceInstance) {
            if (method.getAnnotation(RenewUserTokenProvider.class) != null) {
                apiInfo.renewUserTokenService = true;
                apiInfo.recordResult = true;
            }
        }
    }

    @Extension
    class ApiInfoRegisterImpl implements ApiRegister {

        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            if (info.state != ApiOpenState.OPEN && info.state != ApiOpenState.DEPRECATED) {
                return;
            }

            if (info.renewUserTokenService) {
                if (schema.renewUserTokenService != null) {
                    throw new IllegalApiDefinitionException("duplicate renew user token service found. " +
                        "it is already defined in " + schema.renewUserTokenService.jarFileSimpleName);
                }

                schema.renewUserTokenService = info;
            }
        }
    }

    @Extension
    class ApiVerifierImpl implements ApiVerifier {
        @Override
        public void verify(ApiMethodInfo info, ApiSchema schema) {
            if (!info.renewUserTokenService) {
                return;
            }

            boolean hasTokenParam = false;
            for (ApiParameterInfo pInfo : info.parameterInfos) {
                if (!pInfo.isAutowired) {
                    throw new IllegalApiDefinitionException("renewUserToken API must only have autowired parameters");
                }
                if (CommonParameter.token.equals(pInfo.name)) {
                    hasTokenParam = true;
                }
            }

            if (!hasTokenParam) {
                throw new IllegalApiDefinitionException("renewUserToken API must have an autowired parameter " +
                    "with name _token");
            }

            if (!info.returnType.equals(RenewTokenResult.class)) {
                throw new IllegalApiDefinitionException("the return type of renewUserToken API " +
                    "must be of type RenewTokenResult");
            }
        }
    }

    @Extension(before = UserTokenExpireFeature.class)
    class AsyncTokenProcessorImpl implements SecurityFeature.AsyncTokenProcessor {

        private final Extensions<RenewUserTokenOperator> renewUserTokenProviders;

        public AsyncTokenProcessorImpl(Extensions<RenewUserTokenOperator> renewUserTokenProviders) {
            this.renewUserTokenProviders = renewUserTokenProviders;
        }

        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) throws GatewayException {
            long current = context.startTime;

            if (context.caller == null || context.caller.uid <= 0) {
                // dtk永不过期，所以无需renew，ptk不支持续期
                return;
            }

            if (context.caller.expire < current && // token已过期
                context.caller.expire + context.caller.renewWindow >= current ) { // 尚在可续签期限之内
                renewUserTokenProviders.chain(RenewUserTokenOperator::renew, context, pipeline).go();
            }
        }
    }

    interface RenewUserTokenOperator {
        void renew(ApiContext context,
                   WorkflowPipeline pipeline,
                   Next.NoResult<GatewayException> next) throws GatewayException;
    }

    @Extension(last = true)
    class DefaultRenewUserTokenOperator implements RenewUserTokenOperator {

        static final Logger logger = LoggerFactory.getLogger(RenewUserTokenFeature.class);

        private final SecurityFeature.Config securityConfig;

        public DefaultRenewUserTokenOperator(SecurityFeature.Config securityConfig) {
            this.securityConfig = securityConfig;
        }

        @Override
        public void renew(ApiContext context,
                          WorkflowPipeline pipeline,
                          Next.NoResult<GatewayException> next) {

            if (context.apiSchema.renewUserTokenService == null) {
                return;
            }

            if (context.userTokenRenewed) {
                return; // 避免重复renew
            }

            context.userTokenRenewed = true;

            context.startApiCall(pipeline, context.apiSchema.renewUserTokenService, (result, code) -> {
                if (code != 0) {
                    logger.warn("auto renew token failed deal to api calling code {}", code);
                    return;
                }

                RenewTokenResult newUserTokenResult = (RenewTokenResult) result;
                if (newUserTokenResult != null &&
                    newUserTokenResult.newToken != null &&
                    !newUserTokenResult.newToken.isEmpty()) {

                    // finally we got the renewed token, save it for client
                    CallerInfo newCaller = securityConfig.getAesTokenHelper().parseToken(newUserTokenResult.newToken);

                    if (newCaller == null) {
                        // 如果新的utk不合法，我们继续保留之前的utk
                        logger.warn("invalid renew token: {}", newUserTokenResult.newToken);
                    } else {
                        context.caller = newCaller;
                        context.newUserTokenResult = newUserTokenResult;
                    }
                } else {
                    logger.warn("auto renew token failed, renew token result is empty");
                }
            });
        }
    }

    @Extension
    class ResponseGeneratorImpl implements RequestFinished.ResponseGenerator {
        @Override
        public void process(ApiContext context, AbstractReturnCode code, List<ApiMethodCall> calls, Response response) {
            if (context.newUserTokenResult != null) {
                response.newUserToken = context.newUserTokenResult.newToken;
                response.newUserTokenExpire = context.newUserTokenResult.expire;
            }
        }
    }
}
