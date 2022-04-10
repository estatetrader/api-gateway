package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.serializer.AfterFilter;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.objtree.*;
import com.estatetrader.objtree.*;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.apigw.core.models.DynamicTypeObjectMap;
import com.estatetrader.apigw.core.models.inject.ApiInjectAwareTypePathPioneer;
import com.estatetrader.apigw.core.phases.executing.serialize.SerializingConfigurer;
import com.estatetrader.apigw.core.phases.parsing.SchemaProcessor;
import com.estatetrader.generic.GenericType;
import com.estatetrader.typetree.PossibleSpan;
import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.typetree.TypePath;
import com.estatetrader.typetree.TypePathPioneer;

import java.util.Collections;
import java.util.List;

public interface DynamicTypeFeature {

    @Extension(after = ApiDocumentFeature.class)
    class SchemaProcessorImpl implements SchemaProcessor {
        @Override
        public void process(ApiSchema schema) {
            for (ApiMethodInfo api : schema.getApiInfoList()) {
                GenericType responseType = api.responseWrapper.responseType();
                StructTypeResolver typeResolver = new StructTypeResolver();
                TypePathPioneer pathPioneer = new ApiInjectAwareTypePathPioneer(typeResolver, api, schema);
                api.dynamicTypeObjectTree = new ObjectTreeBuilder
                    <DynamicTypeObjectMap>(responseType)
                    .pathPioneer(pathPioneer)
                    .visitOnlyOnce()
                    .handlerFactory(new NodeHandlerFactoryImpl())
                    .build();
            }
        }
    }

    class NodeHandlerFactoryImpl implements NodeHandlerFactory<String, Object, DynamicTypeObjectMap> {
        @Override
        public String handlerKey(TypePath path, RecordTypeResolver resolver) {
            if (path.current() instanceof PossibleSpan) {
                return ApiDocumentFeature.getPossibleTypeKey((PossibleSpan)path.current());
            } else {
                return null;
            }
        }

        @Override
        public NodeHandler<Object, DynamicTypeObjectMap> createHandler(String handlerKey, RecordTypeResolver resolver) {
            return new DynamicTypeObjectRecorder(handlerKey);
        }
    }

    class DynamicTypeObjectRecorder implements NodeHandler<Object, DynamicTypeObjectMap> {

        private final String typeName;

        public DynamicTypeObjectRecorder(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public void handle(Object value, DynamicTypeObjectMap param, NodeHandlerContext context) {
            param.record(value, typeName);
        }
    }

    class TypeNameWriter extends AfterFilter {

        private static final String CLASS_KEY_FIELD_NAME = "@classKey";
        private final DynamicTypeObjectMap dynamicTypeObjectMap;

        public TypeNameWriter(DynamicTypeObjectMap dynamicTypeObjectMap) {
            this.dynamicTypeObjectMap = dynamicTypeObjectMap;
        }

        @Override
        public void writeAfter(Object object) {
            if (object == null) {
                return;
            }
            String typeName = dynamicTypeObjectMap.typeNameOf(object);
            if (typeName != null) {
                writeKeyValue(CLASS_KEY_FIELD_NAME, typeName);
            }
        }
    }

    @Extension
    class SerializingConfigurerImpl implements SerializingConfigurer {
        /**
         * serialize filters should be used when serializing api result to client
         *
         * @param object the object to be serialized
         * @param methodCall the api to serialize
         * @param context context of this very request
         * @return serialize filters (FastJson)
         */
        @Override
        public List<SerializeFilter> filters(Object object, ApiMethodCall methodCall, ApiContext context) {
            if (methodCall == null || object == null) {
                return Collections.emptyList();
            }
            ObjectTree<DynamicTypeObjectMap> objectTree = methodCall.method.dynamicTypeObjectTree;
            if (objectTree == null) {
                return Collections.emptyList();
            }
            DynamicTypeObjectMap dynamicTypeObjectMap = new DynamicTypeObjectMap();
            objectTree.visit(object, dynamicTypeObjectMap);
            TypeNameWriter filter = new TypeNameWriter(dynamicTypeObjectMap);
            return Collections.singletonList(filter);
        }
    }
}
