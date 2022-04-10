package com.estatetrader.apigw.core.phases.parsing;

import com.alibaba.fastjson.JSON;
import com.estatetrader.annotation.*;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.define.*;
import com.estatetrader.generic.*;
import com.estatetrader.typetree.*;
import com.estatetrader.core.ParameterConverter;
import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.util.Lambda;
import com.estatetrader.util.RawString;
import com.estatetrader.apigw.core.models.ApiMethodType;
import com.estatetrader.apigw.core.models.SimpleServiceInstance;
import com.estatetrader.apigw.core.models.ApiGroupInfo;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ParsingClass {

    interface ParseClassHandler {
        void parseApi(Class<?> clazz, ServiceInstance serviceInstance, String jarFile, List<ApiMethodInfo> infoList);
    }

    @Extension(first = true)
    class ParseClassHandlerImpl implements ParseClassHandler {

        private static final Pattern GROUP_NAME_PATTERN = Pattern.compile("^\\w+$");
        private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("^\\w+\\.\\w+$");

        private final Extensions<ParseClassBriefHandler> parseClassBriefHandlers;
        private final Extensions<ParseMethodHandler> parseMethodHandlers;
        private final boolean openApiOnly;

        public ParseClassHandlerImpl(Extensions<ParseClassBriefHandler> parseClassBriefHandlers,
                                     Extensions<ParseMethodHandler> parseMethodHandlers,
                                     @Value("${com.estatetrader.openApi.onlyGenerateOpenApiDocument:false}") boolean openApiOnly) {
            this.parseClassBriefHandlers = parseClassBriefHandlers;
            this.parseMethodHandlers = parseMethodHandlers;
            this.openApiOnly = openApiOnly;
        }

        @Override
        public void parseApi(Class<?> clazz,
                             ServiceInstance serviceInstance,
                             String jarFile,
                             List<ApiMethodInfo> infoList) {

            ApiGroupInfo groupInfo = new ApiGroupInfo();
            parseClassBriefHandlers.forEach(h -> h.parseClassBrief(clazz, groupInfo));

            boolean found = false;
            for (Method mInfo : clazz.getMethods()) {
                HttpApi api = mInfo.getAnnotation(HttpApi.class);
                if (api == null) continue;

                ApiMethodInfo apiInfo = new ApiMethodInfo();
                apiInfo.groupInfo = groupInfo;
                apiInfo.jarFile = jarFile;
                if (jarFile != null) {
                    apiInfo.jarFileSimpleName = new File(jarFile).getName();
                }
                apiInfo.apiMethodType = ApiMethodType.DUBBO;
                apiInfo.groupName = groupInfo.name;
                apiInfo.methodName = api.name();
                apiInfo.owner = api.owner();
                apiInfo.groupOwner = apiInfo.groupInfo.owner;
                apiInfo.recordResult = api.recordResult();

                if (!GROUP_NAME_PATTERN.matcher(apiInfo.groupName).matches()) {
                    throw new IllegalApiDefinitionException("group name " + apiInfo.groupName +
                        " does not match pattern " + GROUP_NAME_PATTERN);
                }
                if (!METHOD_NAME_PATTERN.matcher(apiInfo.methodName).matches()) {
                    throw new IllegalApiDefinitionException("method name " + apiInfo.methodName +
                        " does not match pattern " + METHOD_NAME_PATTERN);
                }

                try {
                    parseMethodHandlers.forEach(h -> h.parseMethodBrief(clazz, mInfo, apiInfo, serviceInstance));
                    parseMethodHandlers.forEach(h -> h.parseMethodParameters(clazz, mInfo, apiInfo));
                    parseMethodHandlers.forEach(h -> h.parseMethodReturnType(clazz, mInfo, apiInfo));
                } catch (IllegalApiDefinitionException e) {
                    String error = "Illegal api definition for " + mInfo.getName() + " in jar " +
                        apiInfo.jarFileSimpleName + ": " + e.getMessage();
                    throw new IllegalApiDefinitionException(error, e);
                }

                if (openApiOnly) {
                    // 忽略非open-api
                    if (apiInfo.needVerifySignature && SecurityType.Integrated.check(apiInfo.securityLevel)) {
                        infoList.add(apiInfo);
                    }
                } else {
                    infoList.add(apiInfo);
                }

                found = true;
            }

            if (!found) {
                throw new IllegalApiDefinitionException("[API] api method not found. class:" + clazz.getName());
            }

            ApiMockInterfaceImpl amii = clazz.getAnnotation(ApiMockInterfaceImpl.class);
            if (amii != null) {
                for (ApiMethodInfo info : infoList) {
                    info.mocked = true;
                }
            }
        }
    }

    interface ParseClassBriefHandler {
        void parseClassBrief(Class<?> clazz, ApiGroupInfo groupInfo);
    }

    @Extension(first = true)
    class ParseClassBriefHandlerImpl implements ParseClassBriefHandler {

        @Override
        public void parseClassBrief(Class<?> clazz, ApiGroupInfo groupInfo) {
            ApiGroup groupAnnotation = clazz.getAnnotation(ApiGroup.class);
            if (groupAnnotation == null) throw new IllegalApiDefinitionException("class " + clazz + " is not annotated by @ApiGroup");

            groupInfo.name = groupAnnotation.name();
            groupInfo.owner = groupAnnotation.owner();
        }
    }

    interface ParseMethodHandler {
        void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo apiInfo, ServiceInstance serviceInstance);
        default void parseMethodParameters(Class<?> clazz, Method method, ApiMethodInfo apiInfo) {}
        default void parseMethodReturnType(Class<?> clazz, Method method, ApiMethodInfo apiInfo) {}
    }

    @Extension(first = true)
    class ParseMethodHandlerImpl implements ParseMethodHandler {

        private final Extensions<ParseParameterHandler> parseParameterHandlers;
        private final Extensions<ParseReturnTypeHandler> parseReturnTypeHandlers;

        public ParseMethodHandlerImpl(Extensions<ParseParameterHandler> parseParameterHandlers,
                                      Extensions<ParseReturnTypeHandler> parseReturnTypeHandlers) {
            this.parseParameterHandlers = parseParameterHandlers;
            this.parseReturnTypeHandlers = parseReturnTypeHandlers;
        }

        @Override
        public void parseMethodBrief(Class<?> clazz, Method method,
                                     ApiMethodInfo apiInfo,
                                     ServiceInstance instance) {
            HttpApi api = method.getAnnotation(HttpApi.class);
            if (api == null) throw new IllegalApiDefinitionException("method " + method.getName() +
                " is not annotated by @HttpApi");

            ApiShortCircuit asc = method.getAnnotation(ApiShortCircuit.class);
            if (asc != null) {
                apiInfo.staticMockValue = Lambda.newInstance(asc.value());
                apiInfo.mocked = true;
                if (!method.getReturnType().isInstance(apiInfo.staticMockValue)) {
                    throw new IllegalApiDefinitionException("short circuit data type error " +
                        clazz.getName() + " " + api.name());
                }
            }
            EncryptTransfer et = method.getAnnotation(EncryptTransfer.class);
            if (et != null) {
                apiInfo.encryptionOnly = et.encryptionOnly();
            }

            apiInfo.description = api.desc();
            apiInfo.detail = api.detail();
            if (api.name().indexOf('.') < 0) {
                throw new IllegalApiDefinitionException("invalid method name, no . found in " + api.name());
            }

            apiInfo.proxyMethodInfo = method;
            if (instance == null) {
                if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                    try {
                        apiInfo.serviceInstance = new SimpleServiceInstance(clazz.newInstance());
                    } catch (Exception e) {
                        throw new IllegalApiDefinitionException("服务实例化失败" + clazz.getName(), e);
                    }
                } else {
                    throw new IllegalApiDefinitionException("服务实例不存在" + clazz.getName());
                }
            } else {
                apiInfo.serviceInstance = instance;
            }

            apiInfo.dubboInterface = clazz;
            apiInfo.needVerifySignature = api.needVerify();
            apiInfo.needVerifyCode = api.needVerifyCode();

            apiInfo.state = api.state();
        }

        @Override
        public void parseMethodParameters(Class<?> clazz, Method method, ApiMethodInfo apiInfo) {
            Parameter[] parameters = method.getParameters();
            ApiParameterInfo[] pInfos = new ApiParameterInfo[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                ApiParameterInfo pInfo = new ApiParameterInfo();
                pInfo.nativeName = parameter.getName();
                parseParameterHandlers.forEach(h -> h.parseParameter(clazz, method, apiInfo, parameter, pInfo));
                pInfos[i] = pInfo;
            }
            apiInfo.parameterInfos = pInfos;
            if (hasDuplicateParam(pInfos)) {
                throw new IllegalApiDefinitionException("duplicate param , groupName: " + apiInfo.groupName + ", methodName: " + apiInfo.methodName);
            }
        }

        @Override
        public void parseMethodReturnType(Class<?> clazz, Method method, ApiMethodInfo apiInfo) {
            parseReturnTypeHandlers.forEach(h -> h.parseReturnType(clazz, method, apiInfo));
        }

        /**
         * 检查接口参数定义中是否有重复的参数命名
         *
         * @param pInfos 参数信息
         * @return true:存在重复的参数名,false:无重复参数
         */
        private boolean hasDuplicateParam(ApiParameterInfo[] pInfos) {
            HashSet<String> hs = new HashSet<>();
            if (pInfos != null) {
                for (ApiParameterInfo pInfo : pInfos) {
                    if (!hs.contains(pInfo.name)) {
                        hs.add(pInfo.name);
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    interface ParseParameterHandler {
        void parseParameter(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo);
    }

    @Extension(first = true)
    class ParseParameterHandlerImpl implements ParseParameterHandler {

        private final Extensions<ParameterInfoParser> parameterInfoParsers;
        private final StructTypeResolver typeResolver;

        public ParseParameterHandlerImpl(Extensions<ParameterInfoParser> parameterInfoParsers) {
            this.parameterInfoParsers = parameterInfoParsers;
            this.typeResolver = new StructTypeResolver();
        }

        @Override
        public void parseParameter(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo) {
            GenericType parameterType = typeResolver.concreteType(GenericTypes.parameterType(parameter));
            pInfo.type = parameterType;
            pInfo.converter = ParameterConverter.getConverter(parameterType);

            boolean parsed = false;
            for (ParameterInfoParser parser : parameterInfoParsers) {
                if (parser.parse(clazz, method, apiInfo, parameter, pInfo)) {
                    parsed = true;
                    break;
                }
            }

            if (!parsed) {
                throw new IllegalApiDefinitionException("api参数 " + pInfo.name + " 未被标记 " + apiInfo.methodName + "  " + clazz.getName());
            }

            if (!pInfo.isRequired) {
                if (pInfo.defaultValueInText == null) {
                    pInfo.defaultValueInText = ParameterInfoParser.parseDefaultValue(null, pInfo);
                }

                if (pInfo.defaultValue == null && pInfo.defaultValueInText != null) {
                    pInfo.defaultValue = pInfo.converter.convert(pInfo.defaultValueInText);
                }
            }

            // 对参数类型进行检查
            if (!pInfo.isAutowired) {
                TypePathPioneer typePathPioneer = new DefaultTypePathPioneer(typeResolver);
                TypeTree paramTypeTree = new TypeTree(pInfo.type, typePathPioneer);
                paramTypeTree.visit(new ParameterTypeVerifier());
            }
        }
    }

    interface ParameterInfoParser {
        boolean parse(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo);

        /**
         * 为参数获取合法的默认值
         */
        static String parseDefaultValue(String defaultValue, ApiParameterInfo pInfo) {
            GenericType type = pInfo.type;
            if (type instanceof ClassType && ((ClassType) type).isPrimitive()) {
                if ((defaultValue == null || defaultValue.length() == 0)) {
                    if (type.equals(boolean.class)) {
                        return "false";
                    } else {
                        return "0";//未设置默认值,0
                    }
                }

                try {
                    if (type.equals(boolean.class)) {
                        return defaultValue;// no need to check
                    } else if (type.equals(byte.class)) {
                        return String.valueOf(Byte.parseByte(defaultValue));
                    } else if (type.equals(short.class)) {
                        return String.valueOf(Short.parseShort(defaultValue));
                    } else if (type.equals(char.class) || type.equals(int.class)) {
                        return String.valueOf(Integer.parseInt(defaultValue));
                    } else if (type.equals(long.class)) {
                        return String.valueOf(Long.parseLong(defaultValue));
                    } else if (type.equals(float.class)) {
                        return String.valueOf(Float.parseFloat(defaultValue));
                    } else if (type.equals(double.class)) {
                        return String.valueOf(Double.parseDouble(defaultValue));
                    } else {
                        return defaultValue;
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalApiDefinitionException("invalid format of " + defaultValue +
                        " to be as the default value of " + pInfo.type);
                }
            } else if (type instanceof ClassType && ((ClassType) type).isEnum()) {
                if (defaultValue != null) {
                    if (defaultValue.length() == 0) {
                        return null;
                    } else {
                        //noinspection unchecked,rawtypes
                        return String.valueOf(Enum.valueOf((Class)((ClassType) type).getRawType(), defaultValue));
                    }
                } else {
                    return null;
                }
            } else if (type instanceof CollectionLikeType && ((CollectionLikeType) type).getElementType().equals(String.class)) {
                if (defaultValue == null || defaultValue.length() == 0) {
                    return null;//未设置String的默认值,默认值为null
                } else if (pInfo.verifyEnumType != null && pInfo.verifyEnumType != EnumNull.class) {
                    List<String> sa = JSON.parseArray(defaultValue, String.class);
                    for (String s : sa) {
                        //noinspection unchecked
                        Enum.valueOf(pInfo.verifyEnumType, s);
                    }
                    return defaultValue;
                } else {
                    return defaultValue;
                }
            } else if (!type.equals(String.class)) {
                if (defaultValue != null) {
                    if (defaultValue.length() == 0) {
                        return null;//结构化入参默认值为null
                    } else {
                        JSON.parseObject(defaultValue, type.toReflectType());//测试结构化参数
                        return defaultValue;
                    }
                } else {
                    return null;
                }
            } else {
                if (defaultValue == null || defaultValue.length() == 0) {
                    return null;//未设置String的默认值,默认值为null
                } else if (pInfo.verifyEnumType != null && pInfo.verifyEnumType != EnumNull.class) {
                    //noinspection unchecked
                    Enum.valueOf(pInfo.verifyEnumType, defaultValue);
                    return defaultValue;
                } else {
                    return defaultValue;
                }
            }
        }
    }

    @Extension(first = true)
    class ApiParameterInfoParser implements ParameterInfoParser {

        private final Extensions<ApiParameterExtraInfoParser> extraInfoParsers;

        public ApiParameterInfoParser(Extensions<ApiParameterExtraInfoParser> extraInfoParsers) {
            this.extraInfoParsers = extraInfoParsers;
        }

        @Override
        public boolean parse(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo) {
            ApiParameter p = parameter.getAnnotation(ApiParameter.class);
            if (p == null) {
                return false;
            }

            pInfo.serverOnly = false;
            pInfo.description = p.desc();
            if (p.sequence().trim().length() > 0) {
                pInfo.sequence = p.sequence();
            }
            pInfo.ignoreForSecurity = p.ignoreForSecurity();
            pInfo.name = p.name();
            pInfo.isRequired = p.required();
            if (p.enumDef() != EnumNull.class) {
                pInfo.verifyEnumType = p.enumDef();
            }
            if (p.verifyRegex().length() == 0) {
                pInfo.verifyRegex = null;
                pInfo.verifyMsg = null;
            } else {
                pInfo.verifyRegex = Pattern.compile(p.verifyRegex());
                pInfo.verifyMsg = p.verifyMsg();
            }
            try {
                if (pInfo.isRequired) {
                    pInfo.defaultValueInText = null;
                } else {
                    String text = ParameterInfoParser.parseDefaultValue(p.defaultValue(), pInfo);
                    pInfo.defaultValueInText = text;
                    if (text != null) {
                        pInfo.defaultValue = pInfo.converter.convert(text);
                    }
                }
            } catch (Exception e) {
                throw new IllegalApiDefinitionException(
                    "parse default value failed. " + pInfo.name + "  " + apiInfo.methodName + "  " + clazz.getName(), e);
            }

            for (ApiParameterExtraInfoParser parser : extraInfoParsers) {
                parser.parse(clazz, method, apiInfo, parameter, p, pInfo);
            }

            return true;
        }
    }

    interface ApiParameterExtraInfoParser {
        void parse(Class<?> clazz,
                   Method method,
                   ApiMethodInfo apiInfo,
                   Parameter parameter,
                   ApiParameter apiParameter,
                   ApiParameterInfo pInfo);
    }

    @Extension
    class ApiAutowiredParameterInfoParser implements ParameterInfoParser {
        @Override
        public boolean parse(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo) {
            ApiAutowired p = parameter.getAnnotation(ApiAutowired.class);
            if (p == null) {
                return false;
            }

            pInfo.serverOnly = true;
            pInfo.name = p.value();
            pInfo.isRequired = false;
            pInfo.isAutowired = true;

            return true;
        }
    }

    @Extension
    class ApiCookieAutowiredParameterInfoParser implements ParameterInfoParser {
        @Override
        public boolean parse(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo) {
            ApiCookieAutowired p = parameter.getAnnotation(ApiCookieAutowired.class);
            if (p == null) {
                return false;
            }

            if (p.value().length == 0) {
                throw new IllegalApiDefinitionException("cookie名不能为空 " + apiInfo.methodName + "  " + clazz.getName());
            }
            pInfo.serverOnly = true;
            pInfo.name = CommonParameter.cookie;
            pInfo.names = p.value();
            pInfo.isAutowired = true;

            return true;
        }
    }

    interface ParseReturnTypeHandler {
        void parseReturnType(Class<?> clazz, Method mInfo, ApiMethodInfo apiInfo);
    }

    @Extension(first = true)
    class ParseReturnTypeHandlerImpl implements ParseReturnTypeHandler {

        final StructTypeResolver typeResolver = new StructTypeResolver();

        /**
         * 解析返回结果,为每个api设定对应的serializer
         *
         * @param mInfo   反射获得的方法信息
         * @param apiInfo api信息,保存了接口对应的处理实例、serializer等信息
         */
        @Override
        public void parseReturnType(Class<?> clazz, Method mInfo, ApiMethodInfo apiInfo) {
            GenericType returnType = typeResolver.concreteReturnType(mInfo);
            apiInfo.returnType = returnType;
            apiInfo.responseWrapper = ResponseWrapper.getResponseWrapper(returnType);

            if (returnType.equals(RawString.class)) {
                return;
            }

            // 对返回值类型进行检查，检查返回值注入后的类型
            TypePathPioneer typePathPioneer = new DefaultTypePathPioneer(typeResolver);
            TypeTree returnTypeTree = new TypeTree(returnType, typePathPioneer);
            returnTypeTree.visit(new ReturnTypeVerifier());
        }
    }

    abstract class StructTypeVerifier implements TypeTreeVisitor<Void> {

        private static final Logger LOGGER = LoggerFactory.getLogger(ParsingClass.class);

        private static final Class<?>[] ALLOWED_MAP_KEY_TYPES = {
            String.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class
        };

        @Override
        public Void visit(TypePath path, VisitorContext<Void> context) {
            if (path.inCircle()) {
                return null;
            }
            visitEndType(path, context);
            return null;
        }

        @Override
        public Void report(TypePath path, TypePathMultiWayMove move, List<Void> reports) {
            if (move instanceof RecordTypeMove) {
                RecordTypeMove recordTypeMove = (RecordTypeMove) move;
                List<FieldSpan> spans = recordTypeMove.fieldSpans();
                if (spans.isEmpty()) {
                    LOGGER.warn("no field found in the record type {}", path.endType());
//                    throw new IllegalApiDefinitionException("no field found in the record type " + path.endType());
                }

                for (FieldSpan span : spans) {
                    GenericField field = span.getField();
                    Description fieldDesc = field.getAnnotation(Description.class);
                    if (fieldDesc == null || fieldDesc.value().isEmpty()) {
                        throw new IllegalApiDefinitionException("@Description is required for field " + field);
                    }
                }
            }
            if (move instanceof MapTypeMove) {
                MapType mapType = ((MapTypeMove) move).mapType();
                GenericType keyType = mapType.getKeyType();
                if (Arrays.stream(ALLOWED_MAP_KEY_TYPES).noneMatch(keyType::equals)) {
                    throw new IllegalApiDefinitionException("type " + keyType + " is not supported as the key type of the Map type " + mapType);
                }
            }
            return null;
        }

        protected void visitEndType(TypePath path, VisitorContext<Void> context) {
            GenericType type = path.endType();
            if (type instanceof CollectionLikeType || type instanceof UnionType || type instanceof MapType) {
                context.visitChildren();
                return;
            }

            if (type instanceof StaticType) {
                StaticType staticType = (StaticType) type;
                if (isAcceptableBasicType(staticType)) {
                    return;
                }
                if (staticType instanceof ClassType && ((ClassType) staticType).isEnum()) {
                    context.visitChildren();
                    return;
                }
                if (context.typeResolver().isRecordType(staticType)) {
                    visitRecordType(path, context);
                    return;
                }
            }

            throw new IllegalApiDefinitionException("unsupported type " + type);
        }

        protected abstract boolean isAcceptableBasicType(StaticType type);

        protected void visitRecordType(TypePath path, VisitorContext<Void> context) {
            StaticType recordType = (StaticType) path.endType();
            if (recordType.equals(GenericTypes.OBJECT_TYPE)) {
                throw new IllegalApiDefinitionException("Object is not allowed here");
            }
            if (!GenericTypes.of(Serializable.class).isAssignableFrom(recordType)) {
                throw new IllegalApiDefinitionException("the type " + recordType
                    + " should implement the Serializable interface");
            }

            Description structDesc = recordType.getRawType().getAnnotation(Description.class);
            if (structDesc == null || structDesc.value().isEmpty()) {
                LOGGER.warn("@Description is required for type {}", recordType);
//                throw new IllegalApiDefinitionException("@Description is required for type " + recordType);
            }
            context.visitChildren();
            if (hasEntityGroup(recordType)) {
                RecordTypeResolver typeResolver = context.typeResolver();
                for (GenericField field : typeResolver.recordFields(recordType.getDeclaredType())) {
                    checkEntityGroup(field, typeResolver);
                }
            }
        }

        private void checkEntityGroup(GenericField field, RecordTypeResolver typeResolver) {
            List<GenericType> typesWithoutEntityGroup = field.getResolvedType().visit((type, childrenReports) -> {
                if (type instanceof StaticType) {
                    StaticType staticType = (StaticType) type;
                    if (typeResolver.isRecordType(staticType) && !hasEntityGroup(staticType)) {
                        return Stream
                            .concat(Stream.of(type), childrenReports.stream().flatMap(List::stream))
                            .collect(Collectors.toList());
                    }
                }
                return childrenReports
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            });

            if (typesWithoutEntityGroup.isEmpty()) {
                return;
            }

            String typesInText = typesWithoutEntityGroup.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

            throw new IllegalApiDefinitionException("The type[s] " + typesInText + " must be annotated with "
                + "@EntityGroup to specify its group name, since it is referred by the field " + field.getName()
                + " of type " + field.getDeclaringClass() + ", which is annotated with @EntityGroup.");
        }

        private boolean hasEntityGroup(StaticType recordType) {
            GlobalEntityGroup globalEntityGroup = recordType.getRawType().getAnnotation(GlobalEntityGroup.class);
            EntityGroup entityGroup = recordType.getRawType().getAnnotation(EntityGroup.class);
            return globalEntityGroup != null || entityGroup != null && !entityGroup.value().isEmpty();
        }
    }

    class ParameterTypeVerifier extends StructTypeVerifier {
        private static final Class<?>[] ALLOWED_BASE_TYPES = {
            String.class,
            boolean.class,
            byte.class,
            short.class,
            int.class,
            long.class,
            Boolean.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class
        };

        @Override
        protected boolean isAcceptableBasicType(StaticType type) {
            return Arrays.stream(ALLOWED_BASE_TYPES).anyMatch(t -> t.equals(type.getRawType()));
        }
    }

    class ReturnTypeVerifier extends StructTypeVerifier {
        private static final Class<?>[] ALLOWED_BASE_TYPES = {
            void.class,
            String.class,
            boolean.class,
            byte.class,
            short.class,
            int.class,
            long.class,
            Datum.class,
            Boolean.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Boolean.class
        };

        @Override
        protected boolean isAcceptableBasicType(StaticType type) {
            return Arrays.asList(ALLOWED_BASE_TYPES).contains(type.getRawType());
        }
    }
}
