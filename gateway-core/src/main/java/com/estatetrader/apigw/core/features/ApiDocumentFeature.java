package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.*;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.document.*;
import com.estatetrader.gateway.StructTypePathPioneer;
import com.estatetrader.generic.*;
import com.estatetrader.typetree.*;
import com.estatetrader.apigw.core.models.ApiAwareTypePathPioneer;
import com.estatetrader.apigw.core.utils.ClassUtil;
import com.estatetrader.define.ApiParameterEncryptionMethod;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.util.Lambda;
import com.estatetrader.util.RawString;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.apigw.core.models.TypeSpanApiMetadata;
import com.estatetrader.apigw.core.models.inject.ApiInjectAwareTypePathPioneer;
import com.estatetrader.apigw.core.phases.parsing.SchemaProcessor;
import com.estatetrader.responseEntity.Response;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface ApiDocumentFeature {

    @Extension
    class ApiDocumentGenerator implements SchemaProcessor {
        @Override
        public void process(ApiSchema schema) {
            new DocumentGeneratingHandler(schema).generate();
        }
    }

    class DocumentGeneratingHandler {

        final ApiSchema schema;
        final ApiDocument document;
        final Set<Integer> designCodes;

        public DocumentGeneratingHandler(ApiSchema schema) {
            this.schema = schema;
            this.document = schema.document;
            this.designCodes = new HashSet<>();
        }

        public void generate() {
            document.apis = new LinkedList<>();
            document.codes = new LinkedList<>();
            document.commonParams = getSystemParamInfo(schema);
            generateCommonStruct(GenericTypes.of(Response.class));

            List<ApiMethodInfo> apiList = Lambda.orderBy(schema.getApiInfoList(), m -> m.methodName);

            for (ApiMethodInfo api : apiList) {
                MethodInfo methodInfo;
                try {
                    methodInfo = generateForApi(api);
                } catch (Exception e) {
                    throw new IllegalApiDefinitionException(String.format(
                        "generate document for API %s failed, owner = %s, jar = %s: %s",
                        api.methodName, api.owner, api.jarFileSimpleName, e.getMessage()), e);
                }
                document.apis.add(methodInfo);
            }

            generateForErrorCode(designCodes);
        }

        void generateCommonStruct(GenericType type) {
            TypePathPioneer pathPioneer = new StructTypePathPioneer();
            TypeTree typeTree = new TypeTree(type, pathPioneer);
            typeTree.visit(new StructGeneratingVisitor(document::addTypeStruct));
        }

        private void generateForErrorCode(Set<Integer> designCode) {
            for (AbstractReturnCode rc : ErrorCodeFeature.parseCodesInClass(ApiReturnCode.class)) {
                CodeInfo c = new CodeInfo();
                c.code = rc.getCode();
                c.desc = rc.getDesc();
                c.name = rc.getName();
                c.service = rc.getService();
                if (c.code > 0) {
                    c.exposedToClient = designCode.contains(c.code);
                } else if (c.desc == null) {
                    c.desc = c.name;
                    c.exposedToClient = false;
                } else {
                    c.exposedToClient = true;
                }
                document.codes.add(c);
            }
        }

        MethodInfo generateForApi(ApiMethodInfo api) {
            MethodInfoGenerator mg = new MethodInfoGenerator(api);
            mg.generate();
            return mg.methodInfo;
        }

        private class MethodInfoGenerator {
            final ApiMethodInfo api;
            final MethodInfo methodInfo;

            public MethodInfoGenerator(ApiMethodInfo api) {
                this.api = api;
                this.methodInfo = new MethodInfo();
            }

            void generate() {
                methodInfo.origin = "api-gateway";
                methodInfo.description = api.description;
                methodInfo.detail = api.detail;
                methodInfo.groupName = api.groupName;
                methodInfo.methodName = api.methodName;
                if (api.exportParams != null) {
                    methodInfo.exportParams = new ArrayList<>();
                    for (Map.Entry<String, Class<? extends ServiceInjectable.InjectionData>> entry : api.exportParams.entrySet()) {
                        methodInfo.exportParams.add(entry.getKey() + "-" + entry.getValue().getName());
                    }
                }

                methodInfo.securityLevel = api.securityLevel.name();
                methodInfo.groupOwner = api.groupOwner;
                methodInfo.methodOwner = api.owner;
                methodInfo.encryptionOnly = api.encryptionOnly;
                methodInfo.needVerify = api.needVerifySignature;
                methodInfo.subsystem = api.subSystem;
                methodInfo.jarFile = api.jarFileSimpleName;
                methodInfo.exampleValue = api.exampleValue;
                methodInfo.state = api.state.name();

                if (api.errors != null) {
                    methodInfo.errorCodes = new ArrayList<>(api.errors.length);
                    for (int code : api.errors) {
                        if (code <= 0) continue;
                        List<AbstractReturnCode> list = schema.returnCodes.get(code);
                        if (list == null) {
                            throw new IllegalApiDefinitionException("illegal error code " + code + " in " + api.methodName);
                        }

                        for (AbstractReturnCode rc : list) {
                            CodeInfo c = new CodeInfo();
                            c.code = code;
                            c.desc = rc.getDesc();
                            c.name = rc.getName();
                            // API声明的所有code均为design code
                            c.exposedToClient = true;
                            designCodes.add(c.code);
                            methodInfo.errorCodes.add(c);
                        }
                    }
                }

                generateForReturnType();
                generateForParameters();
            }

            void generateForReturnType() {
                StructTypeResolver typeResolver = new StructTypeResolver();
                TypePathPioneer pathPioneer = new ApiInjectAwareTypePathPioneer(typeResolver, api, schema);
                TypeTree typeTree = new TypeTree(api.responseWrapper.responseType(), pathPioneer);
                methodInfo.returnType = typeTree.visit(new StructGeneratingVisitor(document::addTypeStruct));
            }

            void generateForParameters() {
                List<ParameterInfo> parameterInfoList = new ArrayList<>(api.parameterInfos.length);
                for (ApiParameterInfo parameter : api.parameterInfos) {
                    if (parameter.isAutowired || parameter.serverOnly) continue;
                    parameterInfoList.add(generateForParameter(parameter));
                }
                Set<String> sequences = new HashSet<>();
                for (ParameterInfo pi : parameterInfoList) {
                    if (pi.sequence != null && !pi.sequence.isEmpty()) {
                        if (!sequences.add(pi.sequence)) {
                            throw new IllegalApiDefinitionException("duplicate sequence value " + pi.sequence + " found in api " + api.methodName);
                        }
                    }
                }

                methodInfo.parameters = parameterInfoList;
            }

            ParameterInfo generateForParameter(ApiParameterInfo parameter) {
                ParameterInfo b = new ParameterInfo();
                b.defaultValue = parameter.defaultValueInText;
                b.sequence = parameter.sequence;
                b.required = parameter.isRequired;
                if (parameter.injectable != null) {
                    if (parameter.isAutowired) {
                        b.injectOnly = true;
                        // 如果一个参数是自动注入的,那么必然是非必填的。实际上,除了serverInjection的自动注入参数(例如uid,did)
                        // 其他自动注入参数都不暴露给客户端
                        b.required = false;
                    }
                    b.serviceInjection = parameter.injectable.getName() + "-" + parameter.injectable.getDataType().getName();
                }
                if (parameter.encryptionMethod != null &&
                    parameter.encryptionMethod != ApiParameterEncryptionMethod.NONE) {
                    b.encryptionMethod = parameter.encryptionMethod.name();
                }
                b.name = parameter.name;
                b.description = parameter.description;

                if (parameter.verifyEnumType != null) {
                    b.description = getEnumDescription(parameter.verifyEnumType, b.description);
                    if (b.description.isEmpty()) {
                        b.description = parameter.description;
                    }
                } else {
                    Class<?> enumDefClass = extractEnumDef(parameter.type);
                    if (enumDefClass != null) {
                        b.description = getEnumDescription(enumDefClass, b.description);
                    }
                }

                if (parameter.verifyRegex != null) {
                    b.verifyMsg = parameter.verifyMsg;
                    b.verifyRegex = parameter.verifyRegex.pattern();
                }
                if (parameter.fileUploadInfo != null) {
                    b.fileUploadInfo = "folder: " + parameter.fileUploadInfo.folderName;
                }
                b.exampleValue = parameter.exampleValue;

                b.type = generateForParameterType(api, parameter);
                return b;
            }

            GenericTypeInfo generateForParameterType(ApiMethodInfo api, ApiParameterInfo parameter) {
                TypePathPioneer pathPioneer = new ApiAwareTypePathPioneer(new StructTypeResolver(), api);
                TypeTree typeTree = new TypeTree(parameter.type, pathPioneer);
                return typeTree.visit(new StructGeneratingVisitor(document::addTypeStruct));
            }
        }
    }

    /**
     * 获取系统级参数
     */
    static List<CommonParameterInfo> getSystemParamInfo(ApiSchema apiSchema) {
        return apiSchema.commonParameterInfoMap.values()
            .stream()
            .map(cp -> {
                CommonParameterInfo info = new CommonParameterInfo();
                info.name = cp.getName();
                info.desc = cp.getDesc();
                info.fromClient = cp.isFromClient();
                info.injectable = cp.isInjectable();
                return info;
            }).collect(Collectors.toList());
    }

    class StructCacheKey {
        final String api;
        final GenericType type;

        public StructCacheKey(String api, GenericType type) {
            this.api = api;
            this.type = type;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof StructCacheKey)) return false;

            StructCacheKey that = (StructCacheKey) object;

            if (!Objects.equals(api, that.api)) return false;
            if (!Objects.equals(type, that.type)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = api != null ? api.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }

    class StructGeneratingVisitor implements TypeTreeVisitor<GenericTypeInfo> {

        final Consumer<TypeStruct> structCollector;
        final Map<StructCacheKey, TypeStruct> structCache;

        public StructGeneratingVisitor(Consumer<TypeStruct> structCollector) {
            this.structCollector = structCollector;
            this.structCache = new HashMap<>();
        }

        @Override
        public GenericTypeInfo visit(TypePath path, VisitorContext<GenericTypeInfo> context) {
            return parseEndType(path, context);
        }

        @Override
        public GenericTypeInfo report(TypePath path, TypePathMultiWayMove move, List<GenericTypeInfo> reports) {
            if (move instanceof RecordTypeMove) {
                processStructType(path, (RecordTypeMove)move, reports);
                // 外层暂时不使用其返回值，为了解决兼容性问题，这里临时返回null
                return null;
            } else if (move instanceof UnionTypeMove) {
                return constructUnionType((UnionTypeMove) move, reports);
            } else {
                throw new IllegalArgumentException("unsupported move " + move);
            }
        }

        private void processStructType(TypePath path, RecordTypeMove move, List<GenericTypeInfo> fieldTypes) {
            List<FieldSpan> spans = move.fieldSpans();
            if (spans.size() != fieldTypes.size()) {
                throw new IllegalStateException("bug");
            }
            List<FieldInfo> fields = new ArrayList<>(spans.size());
            for (int i = 0; i < spans.size(); i++) {
                FieldInfo field = generateStructField(spans.get(i), fieldTypes.get(i));
                fields.add(field);
            }

            StaticType declaredType = ((StaticType) path.endType()).getDeclaredType();
            Class<?> recordType = declaredType.getRawType();

            TypeStruct struct = structCache.get(structCacheKey(path));
            if (struct.name != null) {
                throw new IllegalStateException("bug");
            }
            struct.name = getStructName(path);
            struct.groupName = getStructGroupName(path);

            Description desc = recordType.getAnnotation(Description.class);
            if (desc != null && !desc.value().isEmpty()) {
                struct.description = desc.value();
            }

            struct.fields = fields;
            if (declaredType instanceof ParameterizedType) {
                TypeVariable[] tvs = ((ParameterizedType) declaredType).getTypeParameters();
                List<String> vars = new ArrayList<>(tvs.length);
                for (TypeVariable tv : tvs) {
                    vars.add(tv.getName());
                }
                struct.typeVars = vars;
            }

            structCollector.accept(struct);
        }

        private GenericTypeInfo constructUnionType(UnionTypeMove move, List<GenericTypeInfo> possibleTypes) {
            List<PossibleSpan> possibleSpans = move.possibleSpans();
            if (possibleSpans.size() != possibleTypes.size()) {
                throw new IllegalStateException("unbalanced");
            }
            Map<String, GenericTypeInfo> possibleTypeMap = new LinkedHashMap<>(possibleSpans.size());
            for (int i = 0; i < possibleSpans.size(); i++) {
                PossibleSpan span = possibleSpans.get(i);
                GenericTypeInfo possibleType = possibleTypes.get(i);
                String typeKey = getPossibleTypeKey(span);
                GenericTypeInfo previous = possibleTypeMap.put(typeKey, possibleType);
                if (previous != null) {
                    throw new IllegalApiDefinitionException("the type key " + typeKey + " of " + possibleType + " is the same with "
                        + previous + " in the union type " + span.getUnionType());
                }
            }
            return GenericTypeInfo.unionType(possibleTypeMap);
        }

        private GenericTypeInfo parseEndType(TypePath path, VisitorContext<GenericTypeInfo> context) {
            GenericType type = path.endType();

            if (type.equals(RawString.class) ||
                type.equals(String.class)) {
                return GenericTypeInfo.stringType();
            }
            if (type.equals(Date.class)) {
                return GenericTypeInfo.normalType("date");
            }
            if (type.equals(Boolean.class)) {
                return GenericTypeInfo.normalType("boolean");
            }
            if (type.equals(Byte.class)) {
                return GenericTypeInfo.normalType("byte");
            }
            if (type.equals(Integer.class)) {
                return GenericTypeInfo.normalType("int");
            }
            if (type.equals(Long.class)) {
                return GenericTypeInfo.normalType("long");
            }

            if (type instanceof ClassType) {
                ClassType classType = (ClassType) type;
                if (classType.isPrimitive()) {
                    return GenericTypeInfo.normalType(classType.getSimpleName());
                }
                if (classType.isEnum()) {
                    return GenericTypeInfo.stringType();
                }
            }

            if (type instanceof CollectionLikeType) {
                GenericTypeInfo et = context.visitChildren();
                return GenericTypeInfo.listType(et);
            }

            if (type instanceof MapType) {
                GenericTypeInfo keyType = context.visit(((MapType) type).getKeyType());
                GenericTypeInfo valueType = context.visitChildren();
                return GenericTypeInfo.mapType(keyType, valueType);
            }

            if (type instanceof UnionType) {
                return context.visitChildren();
            }

            if (type instanceof TypeVariable) {
                return GenericTypeInfo.typeVariable(((TypeVariable) type).getName());
            }

            if (type instanceof StaticType) {
                StaticType staticType = (StaticType) type;
                if (context.typeResolver().isRecordType(staticType)) {
                    return parseStructType(path, context);
                } else {
                    throw new IllegalApiDefinitionException("unsupported struct " + type);
                }
            }

            throw new IllegalApiDefinitionException("unsupported type " + type);
        }

        GenericTypeInfo parseStructType(TypePath path, VisitorContext<GenericTypeInfo> context) {
            StaticType type = (StaticType) path.endType();

            generateStruct(path, context);
            String structName = getStructName(path);

            if (type instanceof ParameterizedType) {
                GenericType[] typeArgs = ((ParameterizedType) type).getTypeArguments();
                List<GenericTypeInfo> typeArgInfos = new ArrayList<>(typeArgs.length);
                for (GenericType ta : typeArgs) {
                    GenericTypeInfo typeArgInfo = context.visit(ta);
                    typeArgInfos.add(typeArgInfo);
                }
                return GenericTypeInfo.parameterizedType(structName, typeArgInfos);
            } else {
                return GenericTypeInfo.normalType(structName);
            }
        }

        private void generateStruct(TypePath path, VisitorContext<GenericTypeInfo> context) {
            StaticType recordType = (StaticType) path.endType();
            StaticType declaredType = recordType.getDeclaredType();
            if (!recordType.equals(declaredType)) {
                context.visit(declaredType);
                return;
            }

            StructCacheKey cacheKey = structCacheKey(path);
            TypeStruct cached = structCache.get(cacheKey);
            if (cached == null) {
                structCache.put(cacheKey, new TypeStruct());
                context.visitChildren();
            }
        }

        private FieldInfo generateStructField(FieldSpan span, GenericTypeInfo fieldTypeInfo) {
            GenericField field = span.getField();
            GenericType fieldType = span.getEndType();

            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.name = field.getName();
            fieldInfo.type = fieldTypeInfo;

            Description fd = field.getAnnotation(Description.class);
            if (fd == null || fd.value().isEmpty()) {
                throw new IllegalApiDefinitionException("no @Description is presented on the field " + field);
            }
            fieldInfo.desc = fd.value();

            EnumDef ed = field.getAnnotation(EnumDef.class);
            if (ed != null && onlyComposedBy(fieldType, String.class)) {
                fieldInfo.desc = getEnumDescription(ed.value(), fieldInfo.desc);
            } else {
                Class<?> enumDefClass = extractEnumDef(fieldType);
                if (enumDefClass != null) {
                    fieldInfo.desc = getEnumDescription(enumDefClass, fieldInfo.desc);
                }
            }

            if (onlyComposedBy(fieldType, Date.class)) {
                fieldInfo.desc += " 时间格式为POSIX time的毫秒数";
            }

            Deprecated dep = field.getAnnotation(Deprecated.class);
            if (dep != null) {
                fieldInfo.deprecated = true;
            }
            ExampleValue ev = field.getAnnotation(ExampleValue.class);
            if (ev != null && !ev.value().isEmpty()) {
                fieldInfo.exampleValue = ev.value();
            }

            return fieldInfo;
        }

        private StructCacheKey structCacheKey(TypePath path) {
            StaticType declaredType = ((StaticType) path.endType()).getDeclaredType();
            TypeSpanApiMetadata metadata = path.metadataOf(TypeSpanApiMetadata.class);
            ApiMethodInfo currentMethod = metadata != null ? metadata.getApiMethod() : null;
            String currentMethodName = currentMethod != null ? currentMethod.methodName : null;
            return new StructCacheKey(currentMethodName, declaredType);
        }

        private boolean onlyComposedBy(GenericType type, Class<?> baseType) {
            return type.equals(baseType)
                || type instanceof CollectionLikeType
                && onlyComposedBy(((CollectionLikeType) type).getElementType(), baseType);
        }
    }

    static String getPossibleTypeKey(PossibleSpan span) {
        return span.getEndType().getRawType().getSimpleName();
    }

    static String getStructGroupName(TypePath path) {
        StaticType declaredType = ((StaticType) path.endType()).getDeclaredType();
        return getEntityGroup(currentMethodInfo(path), declaredType.getRawType());
    }

    static String getStructName(TypePath path) {
        StaticType declaredType = ((StaticType) path.endType()).getDeclaredType();
        return getEntityName(currentMethodInfo(path), declaredType.getRawType());
    }

    static ApiMethodInfo currentMethodInfo(TypePath path) {
        TypeSpanApiMetadata metadata = path.metadataOf(TypeSpanApiMetadata.class);
        return metadata != null ? metadata.getApiMethod() : null;
    }

    static String getEntityGroup(ApiMethodInfo currentMethod, Class<?> structType) {
        if (structType.getAnnotation(GlobalEntityGroup.class) != null) {
            return null;
        }
        EntityGroup entityGroup = structType.getAnnotation(EntityGroup.class);
        if (entityGroup != null && !entityGroup.value().isEmpty()) {
            return entityGroup.value();
        }
        return currentMethod != null ? currentMethod.groupName : null;
    }

    static String getEntityName(ApiMethodInfo currentMethod, Class<?> structType) {
        String group = getEntityGroup(currentMethod, structType);
        if (group == null) {
            return "Api_" + getSimpleName(structType);
        }
        return "Api_" + group.toUpperCase() + "_" + getSimpleName(structType);
    }

    static String getSimpleName(Class<?> type){
        if (type.isMemberClass()) {
            String name = type.getName();
            int index = name.lastIndexOf(".");
            return name.substring(index + 1).replace("$", "_");
        }

        return type.getSimpleName();
    }

    static Class<?> extractEnumDef(GenericType type) {
        if (type instanceof ClassType && ((ClassType) type).isEnum()) {
            return ((ClassType) type).getRawType();
        }
        if (type instanceof CollectionLikeType) {
            return extractEnumDef(((CollectionLikeType) type).getElementType());
        }
        return null;
    }

    static String getEnumDescription(Class<?> type, String description) {
        HashMap<String, HashMap<String, String>> enumName$lo_desc = new HashMap<>();
        HashSet<String> loSet = new HashSet<>();
        Pattern regex = Pattern.compile("\\n\\s{0,4}([a-zA-Z]{2}-[a-zA-Z]{2}):([^\\n]+)", Pattern.CASE_INSENSITIVE);
        HashMap<String, String> descMap = new HashMap<>();
        Matcher m = regex.matcher("\nzh-cn:" + description);
        while (m.find()) {
            descMap.put(m.group(1).toLowerCase(), m.group(2));
        }
        for (Field ef : type.getDeclaredFields()) {
            if (ClassUtil.isConstField(ef) && ef.getType() == type) {
                Description efAnnotation = ef.getAnnotation(Description.class);
                if (efAnnotation != null) {
                    HashMap<String, String> map = new HashMap<>();
                    enumName$lo_desc.put(ef.getName(), map);
                    String source = "\nzh-cn:" + efAnnotation.value();
                    Matcher matcher = regex.matcher(source);
                    while (matcher.find()) {
                        map.put(matcher.group(1).toLowerCase(), matcher.group(2));
                        loSet.add(matcher.group(1).toLowerCase());
                    }
                } else {
                    throw new IllegalApiDefinitionException("@Description is missing for the const " + ef.getName() +
                        " in enum " + type.getName());
                }
            }
        }
        String[] fieldArray = new String[enumName$lo_desc.size()];
        enumName$lo_desc.keySet().toArray(fieldArray);
        Arrays.sort(fieldArray, String::compareTo);
        StringBuilder sb = new StringBuilder();
        if (loSet.contains("zh-cn")) {
            if (descMap.containsKey("zh-cn")) {
                sb.append(descMap.get("zh-cn"));
                sb.append(" ");
            }
            for (String name : fieldArray) {
                sb.append(name);
                sb.append(" ");
                sb.append(enumName$lo_desc.get(name).get("zh-cn"));
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            loSet.remove("zh-cn");
        }
        for (String lo : loSet) {
            sb.append("\n");
            sb.append(lo);
            sb.append(":");
            if (descMap.containsKey(lo)) {
                sb.append(descMap.get(lo));
                sb.append(" ");
            }
            for (String name : fieldArray) {
                sb.append(name);
                sb.append(" ");
                sb.append(enumName$lo_desc.get(name).get(lo));
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }
}
