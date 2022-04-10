package com.estatetrader.apigw.core.models;

import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.typetree.AbstractTypePathPioneer;
import com.estatetrader.generic.GenericType;
import com.estatetrader.typetree.RootSpan;

import java.util.Objects;

public class ApiAwareTypePathPioneer extends AbstractTypePathPioneer {
    /**
     * 结构所属的API，null表示全局结构
     */
    private final ApiMethodInfo rootMethod;

    public ApiAwareTypePathPioneer(StructTypeResolver typeResolver, ApiMethodInfo rootMethod) {
        super(typeResolver);
        this.rootMethod = rootMethod;
    }

    @Override
    protected RootSpan rootType(GenericType type) {
        if (rootMethod == null) {
            return super.rootType(type);
        } else {
            return new RootSpan(type, new TypeSpanApiMetadataImpl(rootMethod));
        }
    }

    protected static class TypeSpanApiMetadataImpl implements TypeSpanApiMetadata {
        private final ApiMethodInfo apiMethod;

        public TypeSpanApiMetadataImpl(ApiMethodInfo apiMethod) {
            this.apiMethod = apiMethod;
        }

        @Override
        public ApiMethodInfo getApiMethod() {
            return apiMethod;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TypeSpanApiMetadataImpl)) return false;

            TypeSpanApiMetadataImpl that = (TypeSpanApiMetadataImpl) object;

            return Objects.equals(apiMethod, that.apiMethod);
        }

        @Override
        public int hashCode() {
            return apiMethod != null ? apiMethod.hashCode() : 0;
        }
    }
}
