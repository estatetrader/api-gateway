package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.apigw.core.models.ApiAwareTypePathPioneer;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.define.Datum;
import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.apigw.core.models.TypeSpanApiMetadata;
import com.estatetrader.generic.CollectionLikeType;
import com.estatetrader.generic.GenericField;
import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.StaticType;
import com.estatetrader.typetree.FieldSpan;
import com.estatetrader.typetree.TypePath;

import java.util.Objects;

/**
 * 被网关管理的出现在返回值、入参或它们的字段以及其嵌套字段位置的实体类型，
 * 包含注入信息
 */
public class ApiInjectAwareTypePathPioneer extends ApiAwareTypePathPioneer {

    private final ApiSchema schema;

    public ApiInjectAwareTypePathPioneer(StructTypeResolver typeResolver, ApiMethodInfo rootApi, ApiSchema schema) {
        super(typeResolver, Objects.requireNonNull(rootApi));
        this.schema = schema;
    }

    @Override
    protected FieldSpan recordField(GenericField field, TypePath path) {
        StaticType ownerType = (StaticType) path.endType();
        GenericType fieldType = field.getResolvedType();
        if (onlyComposedByDatum(fieldType)) {
            ApiMethodInfo currentApi = getApiMetadata(path);
            ApiMethodInfo provider = DatumConsumerSpec.determineDatumProvider(schema, currentApi, field);
            if (provider == null) {
                return null;
            }
            GenericType datumImplType = provider.datumProviderSpec.getDatumImplType();
            GenericType boundFieldType = replaceDatum(fieldType, datumImplType);
            GenericField boundField = field.replaceResolvedType(boundFieldType);
            return new FieldSpan(ownerType, boundField, new TypeSpanApiMetadataImpl(provider));
        } else {
            return new FieldSpan(ownerType, field, null);
        }
    }

    private ApiMethodInfo getApiMetadata(TypePath path) {
        TypeSpanApiMetadata metadata = path.metadataOf(TypeSpanApiMetadata.class);
        if (metadata == null) {
            throw new IllegalStateException("could not find the api metadata in path " + path);
        }
        return metadata.getApiMethod();
    }

    private boolean onlyComposedByDatum(GenericType type) {
        return type.equals(Datum.class)
            || type instanceof CollectionLikeType
            && onlyComposedByDatum(((CollectionLikeType) type).getElementType());
    }

    private GenericType replaceDatum(GenericType originType, GenericType datumImplType) {
        return originType.replace(type -> type.equals(Datum.class) ? datumImplType : type);
    }
}
