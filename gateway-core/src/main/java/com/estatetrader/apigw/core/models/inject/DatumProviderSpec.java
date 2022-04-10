package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.annotation.*;
import com.estatetrader.define.Datum;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.annotation.inject.DatumKey;
import com.estatetrader.annotation.inject.DefineDatum;
import com.estatetrader.annotation.inject.ImportDatumKey;
import com.estatetrader.annotation.inject.InjectDatumKey;
import com.estatetrader.generic.*;
import com.estatetrader.functions.Holder;
import com.estatetrader.functions.IntegerHolder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatumProviderSpec {
    private final DatumDefinition definition;
    private final Param[] params;
    private final Param pluralParam;

    private DatumProviderSpec(DatumDefinition definition, Param[] params) {
        /*
         * 构造provider-spec时需要进行如下检查：
         * 1. datum-key有以下三种声明位置：
         *    a. 参数本身（包括单数和复数形式）
         *    b. 实体类型的参数的实体字段（包括单数和复数形式）
         *    c. 实体集合类型的参数的实体字段（该字段为单数形式，但参数为复数形式）
         * 2. 其中前两种声明方式可以相互组合，但第三种形式只得单独存在
         * 3. 前两种声明方式最多只得定义一个复数datum-key，第三种形式不得声明任何复数datum-key
         * 4. 任何需要导入的datum key不得重复定义
         * 5. 如果参数中声明了任何一个复数形式的datum-key，或者参数为实体集合类型，则返回值必须为复数形式的datum
         */
        Param pluralParam = null;
        Set<String> datumKeysInParams = new HashSet<>();
        for (Param param : params) {
            if (param.isPlural()) {
                if (pluralParam != null) {
                    throw new IllegalApiDefinitionException("最多仅允许定义一个包含复数datum-key的参数或者实体集合类型的参数");
                } else {
                    pluralParam = param;
                }
            }
            param.visitImportedKeyInfos(keyInfo -> {
                if (!datumKeysInParams.add(keyInfo.getName())) {
                    throw new IllegalApiDefinitionException("需要导入的datum-key " + keyInfo.getName() + "不得重复定义");
                }
            });
        }
        if (pluralParam != null) {
            if (!definition.isPlural()) {
                throw new IllegalApiDefinitionException("在datum注入中，如果参数已经声明为复数，则返回值应声明为Datum的集合类型");
            }
        }
        if (pluralParam == null && params.length > 0 && definition.isPlural()) {
            throw new IllegalApiDefinitionException("在datum注入中，如果参数声明为单数类型，则返回值不能声明为Datum的集合类型");
        }
        if (pluralParam != null
            && pluralParam.node instanceof EntitiesNode
            && params.length > 1) {
            throw new IllegalApiDefinitionException("在datum注入中，如果定义了实体集合类型的参数，则不能定义任何其他的参数");
        }

        this.definition = definition;
        this.pluralParam = pluralParam;
        this.params = params;
    }

    public String getDatumType() {
        return definition.getDatumType();
    }

    public StaticType getDatumImplType() {
        return definition.getImplType();
    }

    public DatumKeyTypeMatched matchKeyType(String keyName, GenericType keyType) {
        DatumKeyInfo keyInfo = definition.keyInfoOf(keyName);
        if (keyInfo != null) {
            DatumKeyTypeMatched matched = keyInfo.tryMatchType(keyType);
            if (matched == null) {
                throw new IllegalApiDefinitionException("@ExposeDatumKey标注的字段类型" + keyType + "必须和其在datum定义"
                    + definition.getDatumType() + "中的类型" + keyInfo.getType() + "一致或者其复数类型一致");
            }
            return matched;
        }
        DatumImportedKeyInfo importedKeyInfo = importedKeyInfoOfKey(keyName);
        if (importedKeyInfo != null) {
            // 该key仅在参数中定义
            DatumKeyTypeMatched matched = importedKeyInfo.tryMatchType(keyType);
            if (matched == null) {
                throw new IllegalApiDefinitionException("@ExposeDatumKey标注的字段类型" + keyType + "必须和其在datum定义"
                    + definition.getDatumType() + "中的类型" + importedKeyInfo.getType() + "一致或者其复数类型一致");
            }
            return matched;
        } else {
            throw new IllegalApiDefinitionException("指定的key " + keyName + "尚未在" + getDatumType()
                + " datum中定义或者在其导入的参数中定义");
        }
    }

    public DatumExportedKeyAdaptor createExportedKeyAdaptor(DatumExportedKeySchema exportedKeySchema) {
        /*
         * 对datum的provider的入参和consumer的参数导出进行配对，并返回能够将后者转换为前者的转换器
         * provider中所有必填的可导入的参数都必须在consumer中导出
         *
         * 支持如下配对模式：
         * provider                      | consumer
         * 仅定义一个实体集合类型的参数       | 无限制
         * 参数中包含一个复数形式的datum-key ｜ 1. 多组，但仅有一个单数或复数的在provider参数中定义的key
         *                               | 2. 单组，无复数key
         *                               | 3. 单组，仅有有一个复数key（与provider的复数key同名）
         * 无复数参数                      | 单组，无复数key
         *
         * 各datum-key的总结如下：
         * 基于datum的返回值注入设计一共包含了以下三种key：
         * 1. datum-key，指的是定义在实现了Datum接口的类中的字段，使用 @DatumKey声明
         * 2. datum-imported-key，指的是定义在injection-provider侧的函数入参上，使用 @DatumImportedKey 声明
         * 3. datum-exported-key，指的是定义在injection-consumer侧的返回值类型字段（或嵌套字段），使用 @DatumExportedKey声明
         * 以上三种key完成了参数提取-构建datum对象-筛选并注入datum对象到受体中的过程
         * 这三种key的每一种均可以定义多个key，在一次合法的注入实践中，这三个key之间的集合关系如下：
         * 2. datum-exported-key ∈ datum-key ∪ datum-imported-key
         * 3. { key | key ∈ datum-imported-key 且key必填 } ∈ datum-exported-key
         */

        // datum-imported-key 和 datum-exported-key 的交集的key数量
        IntegerHolder interestKeyCountHolder = new IntegerHolder();
        visitImportedKeyInfos(keyInfo -> {
            if (exportedKeySchema.keyInfoOf(keyInfo.getName()) != null) {
                interestKeyCountHolder.value++;
            } else if (keyInfo.isRequired()) {
                // 校验必填的key是否均已导出
                throw new IllegalApiDefinitionException("导出的key中缺少datum provider " + getDatumType()
                    + "定义的必填key " + keyInfo.getName());
            }
        });
        int interestKeyCount = interestKeyCountHolder.value;

        // 执行配对逻辑，并构造适配器
        DatumExportedKeyInfo exportedPluralKeyInfo = exportedKeySchema.getPluralKey();
        if (interestKeyCount == 0) {
            return new EmptyKeys2ParamAdaptor(params);
        } else if (pluralParam == null) {
            if (exportedKeySchema.isMultiple()) {
                throw new IllegalApiDefinitionException("datum provider " + getDatumType()
                    + "尚未支持任何可注入的复数参数，因此无法支持多组注入");
            } else if (exportedPluralKeyInfo != null) {
                throw new IllegalApiDefinitionException("datum provider " + getDatumType()
                    + "尚未支持任何可注入的复数参数，因此无法支持" + exportedPluralKeyInfo.getName() + "的复数形式");
            } else {
                return new SingleGroupSingularKey2NonEntitiesParamAdaptor(params);
            }
        } else if (pluralParam.node instanceof EntitiesNode) {
            String paramName = pluralParam.name;
            EntitiesNode entitiesNode = (EntitiesNode) pluralParam.node;
            if (exportedPluralKeyInfo == null) { // 单数key的consumer（不区分是否多组）
                return new SingularKey2EntitiesParamAdaptor(paramName, entitiesNode);
            } else { // 复数key的consumer（不区分是否多组）
                return new PluralKey2EntitiesParamAdaptor(paramName, entitiesNode);
            }
        } else if (exportedKeySchema.isMultiple()) {
            // 校验是否仅有一个被provider导入的key
            if (interestKeyCount > 1) {
                throw new IllegalApiDefinitionException("datum provider " + getDatumType()
                    + "仅支持简单的复数形式，consumer在启用了多组注入之后不能使用多个注入key");
            }
            String paramName = pluralParam.name;
            SimpleNode entityNode = (SimpleNode) pluralParam.node;
            if (exportedPluralKeyInfo == null) {
                return new MultigroupSingularKey2PluralParamAdaptor(paramName, entityNode);
            } else if (!pluralParam.pluralKeyInfo().getName().equals(exportedPluralKeyInfo.getName())) {
                throw new IllegalApiDefinitionException("datum provider " + getDatumType() + "尚未支持"
                    + exportedPluralKeyInfo.getName() + "的复数形式");
            } else {
                return new MultigroupPluralKey2PluralParamAdaptor(paramName, entityNode);
            }
        } else {
            if (exportedPluralKeyInfo == null) {
                return new SingleGroupSingularKey2NonEntitiesParamAdaptor(params);
            } else if (!pluralParam.pluralKeyInfo().getName().equals(exportedPluralKeyInfo.getName())) {
                throw new IllegalApiDefinitionException("datum provider " + getDatumType() + "尚未支持"
                    + exportedPluralKeyInfo.getName() + "的复数形式");
            } else {
                return new SingleGroupPluralKey2PluralParamAdaptor(params);
            }
        }
    }

    private void visitImportedKeyInfos(ImportedKeyInfoVisitor visitor) {
        for (Param param : params) {
            param.visitImportedKeyInfos(visitor);
        }
    }

    private DatumImportedKeyInfo importedKeyInfoOfKey(String keyName) {
        Holder<DatumImportedKeyInfo> holder = new Holder<>();
        visitImportedKeyInfos(keyInfo -> {
            if (keyInfo.getName().equals(keyName)) {
                holder.item = keyInfo;
            }
        });
        return holder.item;
    }

    public static DatumProviderSpec parse(Method method, String injectProviderName, StructTypeResolver typeResolver) {
        GenericType resolvedReturnType = typeResolver.concreteReturnType(method);
        DatumDefinition definition = parseDefinitionForReturnType(resolvedReturnType, injectProviderName, typeResolver);
        if (definition == null) {
            return null;
        }
        Param[] params = parseDatumParams(method, definition, typeResolver);
        return new DatumProviderSpec(definition, params);
    }

    private static Param[] parseDatumParams(Method method, DatumDefinition definition, StructTypeResolver typeResolver) {
        List<Param> params = new ArrayList<>();
        for (Parameter p : method.getParameters()) {
            String keyName;
            boolean required;
            ImportDatumKey importDatumKey = p.getAnnotation(ImportDatumKey.class);
            if (importDatumKey != null) {
                keyName = importDatumKey.keyName();
                required = importDatumKey.required();
            } else {
                //noinspection deprecation
                InjectDatumKey injectDatumKey = p.getAnnotation(InjectDatumKey.class);
                if (injectDatumKey != null) {
                    keyName = injectDatumKey.keyName();
                    required = injectDatumKey.required();
                } else {
                    keyName = null;
                    required = false;
                }
            }
            StaticType paramType = parseParameterType(p);
            Node node;
            if (keyName != null) {
                node = createLeafNode(keyName, paramType, required, definition);
            } else {
                node = parseParamNode(paramType, definition, typeResolver);
            }
            if (node != null) {
                params.add(new Param(p.getName(), node));
            }
        }
        // 兼容逻辑
        if (params.isEmpty()) {
            for (Parameter p : method.getParameters()) {
                InjectID injectID = p.getAnnotation(InjectID.class);
                InjectIDs injectIDs = p.getAnnotation(InjectIDs.class);
                //noinspection deprecation
                InjectIDList injectIDList = p.getAnnotation(InjectIDList.class);
                if (injectID != null || injectIDs != null || injectIDList != null) {
                    StaticType paramType = parseParameterType(p);
                    Node node = createLeafNode("id", paramType, true, definition);
                    params.add(new Param(p.getName(), node));
                }
            }
        }
        // 第二兼容逻辑
        if (params.isEmpty()) {
            Param param = null;
            for (Parameter p : method.getParameters()) {
                ApiParameter apiParameter = p.getAnnotation(ApiParameter.class);
                if (apiParameter == null) continue;
                if (apiParameter.serviceInject() != ServiceInjectable.class) continue;
                if (p.getAnnotation(ImportParam.class) != null) continue;
                //noinspection deprecation
                if (p.getAnnotation(InjectParam.class) != null) continue;
                if (param != null) throw new IllegalApiDefinitionException("在声明datum导入参数时，datum key id不得重复定义");
                StaticType paramType = parseParameterType(p);
                Node node = createLeafNode("id", paramType, true, definition);
                param = new Param(p.getName(), node);
            }
            if (param != null) {
                params.add(param);
            }
        }
        return params.toArray(new Param[0]);
    }

    private static StaticType parseParameterType(Parameter parameter) {
        GenericType paramType = GenericTypes.of(parameter.getAnnotatedType());
        if (paramType instanceof StaticType) {
            return (StaticType) paramType;
        } else {
            throw new IllegalApiDefinitionException("the param type " + paramType + " for parameter " + parameter
                + " should be a static type");
        }
    }

    public DatumProvidedValue provideValue(Object result) {
        return new DatumProvidedValue(result, definition.getDatumReader());
    }

    static DatumDefinition parseDefinitionForReturnType(GenericType returnType, String injectProviderName, StructTypeResolver typeResolver) {
        GenericType datumClass = GenericTypes.of(Datum.class);
        // 1. 确定是否为复数形式，并提取元素类型，我们支持Datum的集合
        StaticType datumImplType;
        boolean plural;
        DatumReader datumReader;
        if (isDefineDatumPresent(returnType) || datumClass.isAssignableFrom(returnType)) {
            if (returnType instanceof StaticType) {
                datumImplType = (StaticType) returnType;
                plural = false;
                datumReader = response -> response == null ? Stream.empty() : Stream.of(response);
            } else {
                throw new IllegalApiDefinitionException("unsupported return type " + returnType
                    + ", only static type is supported for datum definition");
            }
        } else if (returnType instanceof CollectionLikeType) {
            CollectionLikeType colType = (CollectionLikeType) returnType;
            GenericType elementType = colType.getElementType();
            plural = true;
            ElementReader elementReader = ElementReader.forType(colType);

            if (isDefineDatumPresent(elementType) || datumClass.isAssignableFrom(elementType)) {
                if (elementType instanceof StaticType) {
                    datumImplType = (StaticType) elementType;
                    datumReader = response -> response == null ? Stream.empty()
                        : elementReader.stream(response).filter(Objects::nonNull);
                } else {
                    throw new IllegalApiDefinitionException("unsupported element type " + elementType
                        + ", only static type is supported for datum definition");
                }
            } else {
                return null;
            }
        } else {
            return null;
        }

        // 3. 提取Datum的元信息
        DefineDatum defineDatum = datumImplType.getRawType().getAnnotation(DefineDatum.class);

        // 4. 判断datum-type
        String datumType;
        if (defineDatum != null) {
            datumType = defineDatum.value();
        } else {
            // 兼容逻辑，使用定义在API上的inject-provider作为datum-type
            datumType = injectProviderName;
        }
        if (datumType.isEmpty()) {
            throw new IllegalApiDefinitionException("datum type不得为空");
        }

        // 5. 提取Datum中定义的所有key
        List<DatumKeyInfo> keyInfos = parseDatumDefinedKeys(datumImplType, typeResolver);
        DatumUnionKeyInfo unionKeyInfo = new DatumUnionKeyInfo(keyInfos);

        return new DatumDefinition(datumType, plural, datumImplType, unionKeyInfo, datumReader);
    }

    private static boolean isDefineDatumPresent(GenericType type) {
        return type.isAnnotationPresent(DefineDatum.class)
            || type instanceof StaticType
            && ((StaticType) type).getRawType().isAnnotationPresent(DefineDatum.class);
    }

    static List<DatumKeyInfo> parseDatumDefinedKeys(StaticType datumImplType, StructTypeResolver typeResolver) {
        List<DatumKeyInfo> keyInfos = new ArrayList<>();
        for (GenericField field : typeResolver.recordFields(datumImplType)) {
            DatumKey datumKey = field.getAnnotation(DatumKey.class);
            if (datumKey == null) {
                continue;
            }
            DatumKeyReader accessor = new DatumKeyReader.ByField(field);
            String datumKeyName;
            if (datumKey.value().isEmpty()) {
                datumKeyName = field.getName();
            } else {
                datumKeyName = datumKey.value();
            }
            DatumKeyInfo keyInfo = new DatumKeyInfo(datumKeyName, field.getResolvedType(), accessor);
            keyInfos.add(keyInfo);
        }
        return keyInfos;
    }

    static Node parseParamNode(StaticType type, DatumDefinition definition, StructTypeResolver typeResolver) {
        if (type instanceof CollectionLikeType) {
            GenericType elementType = ((CollectionLikeType) type).getElementType();
            if (elementType instanceof StaticType) {
                StaticType staticElementType = (StaticType) elementType;
                if (typeResolver.isRecordType(staticElementType)) {
                    return createEntitiesNode(staticElementType, definition, typeResolver);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else if (typeResolver.isRecordType(type)) {
            return createEntityNode(type, definition, typeResolver);
        } else {
            return null;
        }
    }

    static EntitiesNode createEntitiesNode(StaticType elementType, DatumDefinition definition, StructTypeResolver typeResolver) {
        EntityNode element = createEntityNode(elementType, definition, typeResolver);
        if (element.isPlural()) {
            throw new IllegalApiDefinitionException("如果声明实体集合作为参数类型，则该实体中不能再声明复数key");
        }
        return new EntitiesNode(element);
    }

    static EntityNode createEntityNode(StaticType type, DatumDefinition definition, StructTypeResolver typeResolver) {
        List<EntityField> entityFields = new ArrayList<>();
        for (GenericField field : typeResolver.recordFields(type)) {
            GenericType fieldType = field.getResolvedType();
            if (!(fieldType instanceof StaticType)) {
                throw new IllegalApiDefinitionException("the type " + fieldType + " for field " + field
                    + " should be a static type");
            }
            String keyName;
            boolean required;
            ImportDatumKey importDatumKey = field.getAnnotation(ImportDatumKey.class);
            if (importDatumKey != null) {
                keyName = importDatumKey.keyName();
                required = importDatumKey.required();
            } else {
                //noinspection deprecation
                InjectDatumKey injectDatumKey = field.getAnnotation(InjectDatumKey.class);
                if (injectDatumKey == null) {
                    throw new IllegalApiDefinitionException("the field " + field + " of entity " + type + " should have @ImportDatumKey annotated");
                }
                keyName = injectDatumKey.keyName();
                required = injectDatumKey.required();
            }

            LeafNode child = createLeafNode(keyName, (StaticType) fieldType, required, definition);
            entityFields.add(new EntityField(field, child));
        }
        if (entityFields.isEmpty()) {
            throw new IllegalApiDefinitionException("no field found in entity " + type);
        }
        return new EntityNode(entityFields);
    }

    static LeafNode createLeafNode(String keyName, StaticType type, boolean required, DatumDefinition definition) {
        if (keyName == null || keyName.isEmpty()) {
            throw new IllegalArgumentException("key name should not be empty");
        }
        DatumKeyInfo keyInfo = definition.keyInfoOf(keyName);
        if (keyInfo != null) {
            DatumKeyTypeMatched matched = keyInfo.tryMatchType(type);
            if (matched != null) {
                DatumImportedKeyInfo importedKeyInfo = new DatumImportedKeyInfo(keyName, type, matched.plural, required);
                return new LeafNode(importedKeyInfo);
            } else {
                throw new IllegalApiDefinitionException("datum key " + keyName + "须和datum " + definition.getDatumType()
                    + "中的key类型一致，或者为它的复数类型");
            }
        } else {
            DatumImportedKeyInfo importedKeyInfo = new DatumImportedKeyInfo(keyName, type,
                type instanceof CollectionLikeType, required);
            return new LeafNode(importedKeyInfo);
        }
    }

    private static class Param {
        private final String name;
        private final Node node;

        private Param(String name, Node node) {
            this.name = name;
            this.node = node;
        }

        boolean isPlural() {
            return node.isPlural();
        }

        void visitImportedKeyInfos(ImportedKeyInfoVisitor visitor) {
            node.visitImportedKeyInfos(visitor);
        }

        DatumImportedKeyInfo pluralKeyInfo() {
            return node.pluralKeyInfo();
        }
    }

    interface Node {
        boolean isPlural();
        DatumImportedKeyInfo pluralKeyInfo();
        Object defaultValue();
        void visitImportedKeyInfos(ImportedKeyInfoVisitor visitor);
    }

    static class EntitiesNode implements Node {
        private final EntityNode entityNode;

        public EntitiesNode(EntityNode entityNode) {
            this.entityNode = entityNode;
        }

        @Override
        public boolean isPlural() {
            return true;
        }

        @Override
        public Object defaultValue() {
            return Collections.emptyList();
        }

        @Override
        public DatumImportedKeyInfo pluralKeyInfo() {
            throw new IllegalStateException("bug");
        }

        @Override
        public void visitImportedKeyInfos(ImportedKeyInfoVisitor visitor) {
            entityNode.visitImportedKeyInfos(visitor);
        }

        List<Map<String, Object>> createValueFromPluralKey(List<DatumExportedUnionKey> unionKeys){
            List<Map<String, Object>> result = null;
            for (DatumExportedUnionKey unionKey : unionKeys) {
                List<Map<String, Object>> ls = entityNode.createValuesFromPluralKey(unionKey);
                if (ls == null || ls.isEmpty()) continue;
                if (result == null) result = new ArrayList<>(unionKeys.size());
                for (Map<String, Object> map : ls) {
                    if (!map.isEmpty() && !result.contains(map)) {
                        result.add(map);
                    }
                }
            }
            return result;
        }

        List<Map<String, Object>> createValueFromSingularKey(List<DatumExportedUnionKey> unionKeys) {
            List<Map<String, Object>> result = null;
            for (DatumExportedUnionKey unionKey : unionKeys) {
                Map<String, Object> value = entityNode.createValueFromSingularKey(unionKey);
                if (value == null || value.isEmpty()) continue;
                if (result == null) result = new ArrayList<>(unionKeys.size());
                result.add(value);
            }
            return result;
        }
    }

    interface SimpleNode extends Node {
        Object createValueFromPluralKey(List<DatumExportedUnionKey> unionKeys);

        Object createValueFromSingularKey(List<DatumExportedUnionKey> unionKeys);

        Object createValueFromSingularKey(DatumExportedUnionKey unionKeys);

        Object createValueAsIs(DatumExportedUnionKey unionKey);
    }

    static class EntityNode implements SimpleNode {
        private final List<EntityField> fields;
        private final EntityField pluralField;

        public EntityNode(List<EntityField> fields) {
            this.fields = fields;
            EntityField pluralField = null;
            for (EntityField field : fields) {
                if (field.node.isPlural()) {
                    if (pluralField != null) {
                        throw new IllegalApiDefinitionException("最多仅能定义一个复数字段：" + pluralField.field + "和"
                            + field.field + "均为复数字段");
                    } else {
                        pluralField = field;
                    }
                }
            }
            this.pluralField = pluralField;
        }

        @Override
        public boolean isPlural() {
            return pluralField != null;
        }

        @Override
        public Object defaultValue() {
            return null;
        }

        @Override
        public DatumImportedKeyInfo pluralKeyInfo() {
            return pluralField != null ? pluralField.node.keyInfo : null;
        }

        @Override
        public void visitImportedKeyInfos(ImportedKeyInfoVisitor visitor) {
            for (EntityField field : fields) {
                field.node.visitImportedKeyInfos(visitor);
            }
        }

        @Override
        public Map<String, Object> createValueFromPluralKey(List<DatumExportedUnionKey> unionKeys) {
            return doCreateValue(node -> node.createValueFromPluralKey(unionKeys));
        }

        @Override
        public Map<String, Object> createValueFromSingularKey(List<DatumExportedUnionKey> unionKeys) {
            return doCreateValue(node -> node.createValueFromSingularKey(unionKeys));
        }

        @Override
        public Map<String, Object> createValueFromSingularKey(DatumExportedUnionKey unionKey) {
            return doCreateValue(node -> node.createValueFromSingularKey(unionKey));
        }

        @Override
        public Map<String, Object> createValueAsIs(DatumExportedUnionKey unionKey) {
            return doCreateValue(node -> node.createValueAsIs(unionKey));
        }

        public List<Map<String, Object>> createValuesFromPluralKey(DatumExportedUnionKey unionKey){
            Map<String, Object> example = new HashMap<>();
            DatumExportedKey pluralKey = null;
            for (EntityField field : fields) {
                DatumExportedKey exportedKey = unionKey.keyOfName(field.getKeyName());
                if (exportedKey == null) continue;
                if (field == pluralField) {
                    pluralKey = exportedKey;
                } else {
                    Object value = exportedKey.getValue();
                    if (value != null) {
                        example.put(field.field.getName(), value);
                    }
                }
            }
            if (pluralKey == null) {
                return Collections.emptyList();
            }
            return pluralKey
                .getAsPlural()
                .map(x -> mergeMap(example, pluralField.getFieldName(), x))
                .collect(Collectors.toList());
        }

        private Map<String, Object> doCreateValue(Function<LeafNode, Object> fieldVisitor) {
            Map<String, Object> map = null;
            for (EntityField field : fields) {
                Object value = fieldVisitor.apply(field.node);
                if (value == null) continue;
                if (map == null) map = new HashMap<>(fields.size());
                map.put(field.field.getName(), value);
            }
            return map;
        }
    }

    static class EntityField {
        final GenericField field;
        final LeafNode node;

        public EntityField(GenericField field, LeafNode node) {
            this.field = field;
            this.node = node;
        }

        public String getFieldName() {
            return field.getName();
        }

        public String getKeyName() {
            return node.keyInfo.getName();
        }
    }

    static class LeafNode implements SimpleNode {

        private final DatumImportedKeyInfo keyInfo;

        private LeafNode(DatumImportedKeyInfo keyInfo) {
            this.keyInfo = keyInfo;
        }

        @Override
        public DatumImportedKeyInfo pluralKeyInfo() {
            return keyInfo.isPlural() ? keyInfo : null;
        }

        @Override
        public Object defaultValue() {
            return keyInfo.isPlural() ? Collections.emptyList() : null;
        }

        @Override
        public boolean isPlural() {
            return keyInfo.isPlural();
        }

        @Override
        public void visitImportedKeyInfos(ImportedKeyInfoVisitor visitor) {
            visitor.visit(keyInfo);
        }

        @Override
        public Object createValueFromPluralKey(List<DatumExportedUnionKey> unionKeys) {
            List<Object> result = unionKeys
                .stream()
                .flatMap(unionKey -> {
                    DatumExportedKey exportedKey = unionKey.keyOfName(keyInfo.getName());
                    return exportedKey != null ? exportedKey.getAsPlural() : Stream.empty();
                })
                .distinct()
                .collect(Collectors.toList());
            return result.isEmpty() ? null : result;
        }

        @Override
        public Object createValueFromSingularKey(List<DatumExportedUnionKey> unionKeys) {
            List<Object> result = null;
            for (DatumExportedUnionKey unionKey : unionKeys) {
                DatumExportedKey exportedKey = unionKey.keyOfName(keyInfo.getName());
                if (exportedKey == null) continue;
                if (result == null) result = new ArrayList<>(unionKeys.size());
                Object value = exportedKey.getValue();
                if (value != null && !result.contains(value)) {
                    result.add(value);
                }
            }
            return result;
        }

        @Override
        public Object createValueFromSingularKey(DatumExportedUnionKey unionKey) {
            DatumExportedKey exportedKey = unionKey.keyOfName(keyInfo.getName());
            if (exportedKey == null) {
                return null;
            } else if (keyInfo.isPlural()) {
                Object value = exportedKey.getValue();
                return value != null ? Collections.singletonList(value) : Collections.emptyList();
            } else {
                return exportedKey.getValue();
            }
        }

        @Override
        public Object createValueAsIs(DatumExportedUnionKey unionKey) {
            DatumExportedKey exportedKey = unionKey.keyOfName(keyInfo.getName());
            return exportedKey != null ? exportedKey.getValue() : null;
        }
    }

    private static class EmptyKeys2ParamAdaptor implements DatumExportedKeyAdaptor {
        private final Map<String, Object> result;

        public EmptyKeys2ParamAdaptor(Param[] params) {
            Map<String, Object> result;
            if (params.length == 0) {
                result = Collections.emptyMap();
            } else {
                result = null;
                for (Param param : params) {
                    Object value = param.node.defaultValue();
                    if (value == null) continue;
                    result = new HashMap<>();
                    result.put(param.name, value);
                }
                if (result == null) {
                    result = Collections.emptyMap();
                }
            }
            this.result = result;
        }

        @Override
        public Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys) {
            return result;
        }
    }

    /**
     * 单数key的consumer -> 实体集合类型的参数的provider
     */
    private static class SingularKey2EntitiesParamAdaptor implements DatumExportedKeyAdaptor {
        private final String paramName;
        private final EntitiesNode node;

        public SingularKey2EntitiesParamAdaptor(String paramName, EntitiesNode node) {
            this.paramName = paramName;
            this.node = node;
        }

        @Override
        public Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys) {
            Object value = node.createValueFromSingularKey(unionKeys);
            return Collections.singletonMap(paramName, value != null ? value : node.defaultValue());
        }
    }

    /**
     * 复数key的consumer -> 实体集合类型的参数的provider
     */
    private static class PluralKey2EntitiesParamAdaptor implements DatumExportedKeyAdaptor {
        private final String paramName;
        private final EntitiesNode node;

        public PluralKey2EntitiesParamAdaptor(String paramName, EntitiesNode node) {
            this.paramName = paramName;
            this.node = node;
        }

        @Override
        public Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys) {
            List<Map<String, Object>> value = node.createValueFromPluralKey(unionKeys);
            return Collections.singletonMap(paramName, value != null ? value : node.defaultValue());
        }
    }

    /**
     * 单组单数key的consumer -> 非实体集合类型的参数的provider（包括复数参数和单数参数）
     */
    private static class SingleGroupSingularKey2NonEntitiesParamAdaptor implements DatumExportedKeyAdaptor {
        private final Param[] params;

        public SingleGroupSingularKey2NonEntitiesParamAdaptor(Param[] params) {
            this.params = params;
        }

        @Override
        public Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys) {
            DatumExportedUnionKey unionKey = getAtMostOneUnionKey(unionKeys);
            Map<String, Object> map = new HashMap<>(params.length);
            for (Param param : params) {
                Object value = null;
                if (unionKey != null) {
                    value = ((SimpleNode) param.node).createValueFromSingularKey(unionKey);
                }
                if (value == null) {
                    value = param.node.defaultValue();
                }
                if (value == null) continue;
                map.put(param.name, value);
            }
            return map;
        }
    }

    /**
     * 单组复数key的consumer -> 复数参数的provider（consumer的复数key必须和provider的复数param对应的key一致）
     */
    private static class SingleGroupPluralKey2PluralParamAdaptor implements DatumExportedKeyAdaptor {
        private final Param[] params;

        public SingleGroupPluralKey2PluralParamAdaptor(Param[] params) {
            this.params = params;
        }

        @Override
        public Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys) {
            DatumExportedUnionKey unionKey = getAtMostOneUnionKey(unionKeys);
            Map<String, Object> map = new HashMap<>(params.length);
            for (Param param : params) {
                Object value;
                if (unionKey == null) {
                    value = param.node.defaultValue();
                } else {
                    value = ((SimpleNode) param.node).createValueAsIs(unionKey);
                    if (value == null) {
                        value = param.node.defaultValue();
                    }
                }
                if (value == null) continue;
                map.put(param.name, value);
            }
            return map;
        }
    }

    /**
     * 多组单数key的consumer -> 复数参数的provider（consumer仅能定义一个key）
     */
    private static class MultigroupSingularKey2PluralParamAdaptor implements DatumExportedKeyAdaptor {
        private final String paramName;
        private final SimpleNode node;

        public MultigroupSingularKey2PluralParamAdaptor(String paramName, SimpleNode node) {
            this.paramName = paramName;
            this.node = node;
        }

        @Override
        public Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys) {
            Object value = node.createValueFromSingularKey(unionKeys);
            return Collections.singletonMap(paramName, value != null ? value : node.defaultValue());
        }
    }

    /**
     * 多组复数key的consumer -> 复数参数的provider（consumer仅能定义一个key）
     */
    private static class MultigroupPluralKey2PluralParamAdaptor implements DatumExportedKeyAdaptor {
        private final String paramName;
        private final SimpleNode node;

        public MultigroupPluralKey2PluralParamAdaptor(String paramName, SimpleNode node) {
            this.paramName = paramName;
            this.node = node;
        }

        @Override
        public Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys) {
            Object value = node.createValueFromPluralKey(unionKeys);
            return Collections.singletonMap(paramName, value != null ? value : node.defaultValue());
        }
    }

    private static DatumExportedUnionKey getAtMostOneUnionKey(List<DatumExportedUnionKey> unionKeys) {
        if (unionKeys.isEmpty()) {
            return null;
        } else if (unionKeys.size() > 1) {
            throw new IllegalStateException("bug, more than one union keys found");
        } else {
            return unionKeys.get(0);
        }
    }

    private static <K, V> Map<K, V> mergeMap(Map<K, V> map, K key, V value) {
        if (map == null || map.isEmpty()) {
            return Collections.singletonMap(key, value);
        } else {
            Map<K, V> newMap = new HashMap<>(map);
            newMap.put(key, value);
            return newMap;
        }
    }

    @FunctionalInterface
    private interface ImportedKeyInfoVisitor {
        void visit(DatumImportedKeyInfo keyInfo);
    }
}
