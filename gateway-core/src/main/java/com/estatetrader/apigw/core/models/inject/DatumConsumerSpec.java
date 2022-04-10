package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.annotation.DatumType;
import com.estatetrader.annotation.inject.ExportDatumKey;
import com.estatetrader.annotation.inject.ExposeDatumKey;
import com.estatetrader.annotation.inject.ImportDatum;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.generic.GenericField;
import com.estatetrader.objtree.*;
import com.estatetrader.apigw.core.features.ResponseFilterFeature;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.annotation.inject.InjectDatum;
import com.estatetrader.generic.CollectionLikeType;
import com.estatetrader.generic.GenericType;
import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.generic.StaticType;
import com.estatetrader.typetree.TypePath;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class DatumConsumerSpec {
    private final ObjectTree<InjectionHandler> treeVisitor;
    private final Map<String, DatumExportedKeyAdaptor> keyAdaptorMap;

    private DatumConsumerSpec(ObjectTree<InjectionHandler> treeVisitor,
                              Map<String, DatumExportedKeyAdaptor> keyAdaptorMap) {
        this.treeVisitor = treeVisitor;
        this.keyAdaptorMap = keyAdaptorMap;
    }

    public Map<String, Object> export(String datumType, Object response) {
        List<DatumExportedUnionKey> exportedUnionKeys = new ArrayList<>();
        ExportDatumKeyHandler handler = new ExportDatumKeyHandler(datumType, exportedUnionKeys);
        treeVisitor.visit(response, handler);
        DatumExportedKeyAdaptor keyAdaptor = keyAdaptorMap.get(datumType);
        if (keyAdaptor == null) {
            if (exportedUnionKeys.isEmpty()) {
                return Collections.emptyMap();
            } else {
                throw new IllegalArgumentException("invalid datum-type " + datumType);
            }
        }
        return keyAdaptor.adapt(exportedUnionKeys);
    }

    public void inject(String datumType, Object response, DatumProvidedValue providedValue) {
        treeVisitor.visit(response, new InjectDatumHandler(datumType, providedValue));
    }

    public String datumTypeOfField(GenericField field) {
        return treeVisitor.inspect(new FindBundleInspector<>(bundle -> {
            for (DatumAcceptorInfo acceptorInfo : bundle.acceptorInfos) {
                if (acceptorInfo.isForField(field)) {
                    return bundle.datumType;
                }
            }
            return null;
        }));
    }

    public static DatumConsumerSpec parseConsumerSpec(Method method,
                                                      Map<String, DatumProviderSpec> providerSpecMap,
                                                      StructTypeResolver typeResolver) {
        GenericType actualReturnType = typeResolver.concreteReturnType(method);
        ObjectTree<InjectionHandler> treeVisitor = new ObjectTreeBuilder
                <InjectionHandler>(actualReturnType)
            .structPathPioneer()
            .visitOnlyOnce()
            .handlerFactory(new StructHandlerProvider(providerSpecMap))
            .build();

        if (treeVisitor == null) {
            return null;
        }

        Map<String, DatumExportedKeyAdaptor> keyAdaptorMap = new HashMap<>(providerSpecMap.size());
        for (DatumProviderSpec providerSpec : providerSpecMap.values()) {
            String datumType = providerSpec.getDatumType();
            DatumExportedKeySchema keySchema = treeVisitor.inspect(new ExportedKeySchemaInspector(datumType));
            if (keySchema == null) continue;
            DatumExportedKeyAdaptor keyAdaptor = providerSpec.createExportedKeyAdaptor(keySchema);
            keyAdaptorMap.put(datumType, keyAdaptor);
        }
        return new DatumConsumerSpec(treeVisitor, keyAdaptorMap);
    }

    public static ApiMethodInfo determineDatumProvider(ApiSchema apiSchema, ApiMethodInfo info, GenericField field) {
        // todo 后期做强制检查，不允许出现定义了Datum却不定义@ResponseInjectFromApi的情况
        if (info.datumConsumerSpec == null) {
            return null;
        }
        String target;
        //noinspection deprecation
        DatumType annotation;
        if (field == null) {
            //noinspection deprecation
            annotation = info.proxyMethodInfo.getAnnotation(DatumType.class);
            target = info.proxyMethodInfo.getDeclaringClass().getName() + "." + info.proxyMethodInfo.getName();
        } else {
            //noinspection deprecation
            annotation = field.getAnnotation(DatumType.class);
            target = field.getDeclaringClass().getName() + "." + field.getName();
        }

        String datumType = info.datumConsumerSpec.datumTypeOfField(field);
        String providerName;
        List<ResponseFilterFeature.InjectFromApi> filters = ResponseFilterFeature.InjectFromApi.getInjectFromApiFilters(info);
        if (datumType != null) {
            providerName = filters.stream()
                .map(f -> apiSchema.responseInjectProviderMap.get(f.getFromApiName()))
                .filter(m -> m.datumProviderSpec.getDatumType().equals(datumType))
                .map(m -> m.responseInjectProviderName)
                .findAny()
                .orElse(null);
        } else if (annotation == null && filters.size() == 1) {
            providerName = filters.get(0).getFromApiName();
        } else if (filters.isEmpty()) {
            throw new IllegalApiDefinitionException("无法找到" + target + "的实际定义提供者，" +
                "请在你的API " + info.methodName + "的定义上添加注解@ResponseInjectFromApi");
        } else if (annotation == null) {
            throw new IllegalApiDefinitionException("由于API " + info.methodName + "声明了多个返回值注入，网关无法确定" +
                target + "的实际定义提供者，请使用@DatumType声明你需要使用的具体Datum类型");
        } else if (filters.stream().anyMatch(f -> annotation.value().equals(f.getFromApiName()))) {
            providerName = annotation.value();
        } else {
            throw new IllegalApiDefinitionException("无法找到" + target + "的实际定义提供者" + annotation.value() + "，" +
                "请在你的API定义上添加注解@ResponseInjectFromApi");
        }

        return apiSchema.responseInjectProviderMap.get(providerName);
    }

    private static class InjectionBundle {
        private final String datumType;
        private final List<ExportedKeyDescriptor> keyDescriptors;
        private final List<DatumAcceptorInfo> acceptorInfos;

        public InjectionBundle(String datumType, List<ExportedKeyDescriptor> keyDescriptors,
                               List<DatumAcceptorInfo> acceptorInfos) {
            this.datumType = datumType;
            this.keyDescriptors = keyDescriptors;
            this.acceptorInfos = acceptorInfos;
        }

        DatumExportedKeySchema exportedKeySchema() {
            DatumExportedKeyInfo[] keyInfos = keyDescriptors
                .stream()
                .map(d -> new DatumExportedKeyInfo(d.name, d.plural))
                .toArray(DatumExportedKeyInfo[]::new);
            if (keyInfos.length == 0) {
                return null;
            } else {
                return new DatumExportedKeySchema(false, keyInfos);
            }
        }

        void export(Object container, List<DatumExportedUnionKey> exportedUnionKeys) {
            List<DatumExportedKey> keys = new ArrayList<>();
            for (ExportedKeyDescriptor keyInfo : keyDescriptors) {
                DatumExportedKey key = keyInfo.export(datumType, container);
                if (key != null) {
                    keys.add(key);
                }
            }
            if (!keys.isEmpty()) {
                exportedUnionKeys.add(new DatumExportedUnionKey(keys.toArray(new DatumExportedKey[0])));
            }
        }

        void inject(Object container, DatumProvidedValue providedValue) {
            for (DatumAcceptorInfo acceptorInfo : acceptorInfos) {
                Stream<Object> stream = providedValue.stream().filter(datum -> datumMatched(container, datum));
                acceptorInfo.write(container, stream, datumType);
            }
        }

        boolean datumMatched(Object container, Object datum) {
            for (ExportedKeyDescriptor keyInfo : keyDescriptors) {
                if (!keyInfo.matchesDatum(container, datum, datumType)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class ExportedKeyDescriptor {
        private final String name;
        private final boolean plural;
        private final DatumExportedKeyReader accessor;
        private final DatumKeyMatcher datumKeyMatcher;
        private final ElementReader elementReader;

        private ExportedKeyDescriptor(String name,
                                      GenericType type,
                                      boolean plural,
                                      DatumExportedKeyReader accessor,
                                      DatumKeyMatcher datumKeyMatcher) {
            this.name = name;
            this.plural = plural;
            this.accessor = accessor;
            this.datumKeyMatcher = datumKeyMatcher;
            if (plural) {
                elementReader = ElementReader.forType((CollectionLikeType) type);
            } else {
                elementReader = object -> object != null ? Stream.of(object) : Stream.empty();
            }
        }

        public boolean matchesDatum(Object container, Object datum, String datumType) {
            if (datumKeyMatcher == null) {
                return true;
            }
            Object exportedKey = accessor.read(container, datumType);
            return datumKeyMatcher.matches(datum, exportedKey);
        }

        public DatumExportedKey export(String datumType, Object container) {
            Object key = accessor.read(container, datumType);
            if (key == null) {
                return null;
            } else {
                return new DatumExportedKey(name, key, elementReader);
            }
        }

        public static ExportedKeyDescriptor create(String keyName,
                                                   GenericType keyType,
                                                   DatumExportedKeyReader accessor,
                                                   DatumProviderSpec providerSpec) {
            DatumKeyTypeMatched matched = providerSpec.matchKeyType(keyName, keyType);
            boolean plural = matched.plural;
            return new ExportedKeyDescriptor(keyName, keyType, plural, accessor, matched.datumKeyMatcher);
        }
    }

    private static class StructHandlerProvider implements NodeHandlerFactory<StructHandlerKey, Object, InjectionHandler> {
        final Map<String, DatumProviderSpec> providerSpecMap;

        StructHandlerProvider(Map<String, DatumProviderSpec> providerSpecMap) {
            this.providerSpecMap = providerSpecMap;
        }

        @Override
        public StructHandlerKey handlerKey(TypePath path, RecordTypeResolver resolver) {
            GenericType type = path.endType();
            if (!(type instanceof StaticType) || !resolver.isRecordType((StaticType) type)) {
                return null;
            }

            StaticType recordType = (StaticType) type;
            return new StructHandlerKey(recordType);
        }

        @Override
        public NodeHandler<Object, InjectionHandler> createHandler(StructHandlerKey handlerKey, RecordTypeResolver resolver) {
            BundleMapProducer producer = new BundleMapProducer(handlerKey.recordType, resolver, providerSpecMap);
            InjectionBundleMap bundleMap = producer.produce();
            if (bundleMap.isEmpty()) {
                return null;
            } else {
                return new StructHandler(bundleMap);
            }
        }
    }

    private static class StructHandlerKey {
        final StaticType recordType;

        public StructHandlerKey(StaticType recordType) {
            this.recordType = recordType;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof StructHandlerKey)) return false;
            StructHandlerKey that = (StructHandlerKey) object;
            return recordType.equals(that.recordType);
        }

        @Override
        public int hashCode() {
            return recordType.hashCode();
        }
    }

    private static class BundleMapProducer {
        final StaticType recordType;
        final RecordTypeResolver resolver;
        final Map<String, DatumProviderSpec> providerSpecMap;
        final Map<String, List<ExportedKeyDescriptor>> keyInfosMap;
        final Map<String, List<DatumAcceptorInfo>> acceptorInfosMap;

        BundleMapProducer(StaticType recordType,
                                 RecordTypeResolver resolver,
                                 Map<String, DatumProviderSpec> providerSpecMap) {
            this.recordType = recordType;
            this.resolver = resolver;
            this.providerSpecMap = providerSpecMap;
            this.keyInfosMap = new LinkedHashMap<>();
            this.acceptorInfosMap = new LinkedHashMap<>();
        }

        InjectionBundleMap produce() {
            // 分析实体中的各个字段
            for (GenericField field : resolver.recordFields(recordType)) {
                parseRecordField(field);
            }
            // 汇总
            Map<String, InjectionBundle> map = collectBundles();
            return new InjectionBundleMap(map);
        }

        private void parseRecordField(GenericField field) {
            GenericType fieldType = field.getResolvedType();
            // 检查字段是否提供Datum key
            for (ExportDatumKey exportDatumKey : field.getAnnotationsByType(ExportDatumKey.class)) {
                String datumType = exportDatumKey.datumType();
                ExportedKeyDescriptor keyDesc = createKeyDescriptor(field, datumType, exportDatumKey.keyName());
                keyInfosMap.computeIfAbsent(datumType, k -> new ArrayList<>()).add(keyDesc);
            }
            //noinspection deprecation
            ExposeDatumKey exposeDatumKey = field.getAnnotation(ExposeDatumKey.class);
            if (exposeDatumKey != null) {
                String datumType = exposeDatumKey.datumType();
                ExportedKeyDescriptor keyDesc = createKeyDescriptor(field, datumType, exposeDatumKey.keyName());
                keyInfosMap.computeIfAbsent(datumType, k -> new ArrayList<>()).add(keyDesc);
            }

            // 检查字段是否需要注入Datum
            for (InjectDatum injectDatum : field.getAnnotationsByType(InjectDatum.class)) {
                String datumType = injectDatum.datumType();
                DatumAcceptorInfo acceptorInfo = new DatumAcceptorInfo(fieldType, new DatumWriter.ByField(field));
                acceptorInfosMap.computeIfAbsent(datumType, k -> new ArrayList<>()).add(acceptorInfo);
            }
            //noinspection deprecation
            ImportDatum importDatum = field.getAnnotation(ImportDatum.class);
            if (importDatum != null) {
                String datumType = importDatum.datumType();
                DatumAcceptorInfo acceptorInfo = new DatumAcceptorInfo(fieldType, new DatumWriter.ByField(field));
                acceptorInfosMap.computeIfAbsent(datumType, k -> new ArrayList<>()).add(acceptorInfo);
            }
        }

        private ExportedKeyDescriptor createKeyDescriptor(GenericField field, String datumType, String keyName) {
            DatumProviderSpec providerSpec = providerSpecMap.get(datumType);
            if (providerSpec == null) {
                throw new IllegalApiDefinitionException("无法找到" + datumType + "的datum定义");
            }
            DatumExportedKeyReader accessor = new DatumExportedKeyReader.ByField(field);
            return ExportedKeyDescriptor.create(keyName, field.getResolvedType(), accessor, providerSpec);
        }

        private Map<String, InjectionBundle> collectBundles() {
            // 找出所有的bundle
            Map<String, InjectionBundle> bundles = new HashMap<>();
            // 针对一个特定的datum type，允许在一个container中仅定义acceptor而不定义exported key
            for (Map.Entry<String, List<DatumAcceptorInfo>> entry : acceptorInfosMap.entrySet()) {
                String datumType = entry.getKey();
                List<ExportedKeyDescriptor> keyInfos = keyInfosMap.getOrDefault(datumType, Collections.emptyList());
                InjectionBundle bundle = new InjectionBundle(datumType, keyInfos, entry.getValue());
                bundles.put(datumType, bundle);
            }
            // 但是我们不允许只定义exported key，而不定义acceptor
            for (String datumType : keyInfosMap.keySet()) {
                if (!acceptorInfosMap.containsKey(datumType)) {
                    throw new IllegalApiDefinitionException("由于类" + recordType
                        + "中包含了@ExposedDatumKey注释的字段，所以须同时包含@ImportDatum字段");
                }
            }
            return bundles;
        }
    }

    private static class FindBundleInspector<R> implements ObjectTreeInspector<R> {

        private final Function<InjectionBundle, R> finder;

        public FindBundleInspector(Function<InjectionBundle, R> finder) {
            this.finder = finder;
        }

        @Override
        public R inspect(ObjectTreeNode node, ObjectTreeInspectorContext<R> context) {
            // 优先返回更早查到的信息
            if (context.siblingReport() != null) {
                return context.siblingReport();
            }

            NodeHandler<?, ?> handler = node.getHandler();
            if (handler instanceof StructHandler) {
                StructHandler sh = ((StructHandler) handler);
                for (InjectionBundle bundle : sh.bundleMap) {
                    R r = finder.apply(bundle);
                    if (r != null) {
                        return r;
                    }
                }
            }

            return context.descend(this);
        }
    }

    private static class ExportedKeySchemaInspector implements ObjectTreeInspector<DatumExportedKeySchema> {
        private final String datumType;

        public ExportedKeySchemaInspector(String datumType) {
            this.datumType = datumType;
        }

        @Override
        public DatumExportedKeySchema inspect(ObjectTreeNode node, ObjectTreeInspectorContext<DatumExportedKeySchema> context) {
            // 计算本结点的自己的schema
            DatumExportedKeySchema selfSchema;
            NodeHandler<?, ?> handler = node.getHandler();
            if (handler instanceof StructHandler) {
                StructHandler sh = ((StructHandler) handler);
                InjectionBundle bundle = sh.bundleMap.find(datumType);
                if (bundle != null) {
                    selfSchema = bundle.exportedKeySchema();
                } else {
                    selfSchema = null;
                }
            } else {
                selfSchema = null;
            }

            // 计算子结点的schema
            DatumExportedKeySchema childrenSchema = context.descend(this);
            if (childrenSchema != null
                && (node instanceof ArrayNode || node instanceof CollectionNode)) {
                // 数组和集合会导致schema变为复数
                childrenSchema = childrenSchema.asMultiple();
            }

            // 计算本结点的汇总schema
            DatumExportedKeySchema thisSchema = DatumExportedKeySchema.merge(selfSchema, childrenSchema, true);

            // 计算最终的schema
            if (node instanceof UnionTypeNode) {
                // 动态类型中的各个具体类型在运行时只出现一次
                return DatumExportedKeySchema.merge(context.siblingReport(), thisSchema, false);
            } else {
                return DatumExportedKeySchema.merge(context.siblingReport(), thisSchema, true);
            }
        }
    }

    private static class StructHandler implements NodeHandler<Object, InjectionHandler> {

        final InjectionBundleMap bundleMap;

        public StructHandler(InjectionBundleMap bundles) {
            this.bundleMap = bundles;
        }

        @Override
        public void handle(Object value, InjectionHandler param, NodeHandlerContext context) {
            param.handle(value, bundleMap);
            context.descend();
        }

    }

    private static class InjectionBundleMap implements Iterable<InjectionBundle> {
        private final Map<String, InjectionBundle> map;

        public InjectionBundleMap(Map<String, InjectionBundle> map) {
            this.map = map;
        }

        /**
         * Returns an iterator over elements of type {@code T}.
         *
         * @return an Iterator.
         */
        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<InjectionBundle> iterator() {
            return map.values().iterator();
        }

        InjectionBundle find(String datumType) {
            return map.get(datumType);
        }

        boolean isEmpty() {
            return map.isEmpty();
        }
    }

    private interface InjectionHandler {
        void handle(Object container, InjectionBundleMap bundleMap);
    }

    private static class ExportDatumKeyHandler implements InjectionHandler {
        private final String datumType;
        private final List<DatumExportedUnionKey> exportedUnionKeys;

        public ExportDatumKeyHandler(String datumType, List<DatumExportedUnionKey> exportedUnionKeys) {
            this.datumType = datumType;
            this.exportedUnionKeys = exportedUnionKeys;
        }

        @Override
        public void handle(Object container, InjectionBundleMap bundleMap) {
            InjectionBundle bundle = bundleMap.find(datumType);
            if (bundle != null) {
                bundle.export(container, exportedUnionKeys);
            }
        }
    }

    private static class InjectDatumHandler implements InjectionHandler {
        private final String datumType;
        private final DatumProvidedValue providedValue;

        public InjectDatumHandler(String datumType, DatumProvidedValue providedValue) {
            this.datumType = datumType;
            this.providedValue = providedValue;
        }

        @Override
        public void handle(Object container, InjectionBundleMap bundleMap) {
            InjectionBundle bundle = bundleMap.find(datumType);
            if (bundle != null) {
                bundle.inject(container, providedValue);
            }
        }
    }
}
