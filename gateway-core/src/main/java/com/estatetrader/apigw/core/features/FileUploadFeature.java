package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.ApiFileUploadType;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.phases.parsing.ApiRegister;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.define.ApiOpenState;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.entity.FileUploadInfo;
import com.estatetrader.entity.FileUploadType;
import com.estatetrader.util.Lambda;
import com.estatetrader.annotation.ApiFileUploadInfo;
import com.estatetrader.annotation.ApiFileUploadTypes;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import com.estatetrader.apigw.core.models.ApiSchema;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 文件上传相关功能
 */
public interface FileUploadFeature {

    @Extension
    class ParseParameterHandlerImpl implements ParsingClass.ParseParameterHandler {

        @Override
        public void parseParameter(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo) {
            Annotation[] annotations = parameter.getAnnotations();
            ApiFileUploadInfo fileUploadInfo = (ApiFileUploadInfo) Lambda.find(annotations, x -> x.annotationType() == ApiFileUploadInfo.class);
            if (fileUploadInfo == null) return;

            String folderName = fileUploadInfo.folderName();
            if (folderName.length() == 0) {
                throw new IllegalApiDefinitionException("invalid folder name: " + apiInfo.methodName + "  " + clazz.getName());
            } else if (folderName.startsWith("/") || folderName.endsWith("/")) {
                throw new IllegalApiDefinitionException("folder name should not start or ends with '/' " + apiInfo.methodName + "  " + clazz.getName());
            }

            FileUploadInfo info = new FileUploadInfo();
            info.folderName = folderName.toUpperCase();
            info.bucket = fileUploadInfo.bucket();
            info.cloud = fileUploadInfo.cloud();
            info.options = fileUploadInfo.options();

            ApiFileUploadType[] types = null;
            ApiFileUploadTypes uploadTypes = (ApiFileUploadTypes) Lambda.find(annotations, a -> a.annotationType() == ApiFileUploadTypes.class);
            if (uploadTypes != null) {
                types = uploadTypes.value();
            } else {
                ApiFileUploadType uploadType = (ApiFileUploadType) Lambda.find(annotations, a -> a.annotationType() == ApiFileUploadType.class);
                if (uploadType != null) {
                    types = new ApiFileUploadType[]{uploadType};
                }
            }

            List<FileUploadType> allowedTypes = new LinkedList<>();
            if (types != null) {
                Map<String, FileUploadType> mainTypeMap = new HashMap<>();
                for (ApiFileUploadType type : types) {
                    FileUploadType t = new FileUploadType();
                    t.contentType = type.contentType().isEmpty() ? null : type.contentType();
                    t.extension = type.extension();
                    t.maxSize = type.maxSize();
                    t.mainType = type.mainType();
                    t.includeImageDimension = type.includeImageDimension();

                    if (t.contentType == null || t.contentType.isEmpty()) {
                        throw new IllegalApiDefinitionException("extension of ApiFileUploadType must be set in " + method.getName());
                    }
                    if (t.maxSize <= 0) {
                        throw new IllegalApiDefinitionException("max size of ApiFileUploadType must be set in " + method.getName());
                    }
                    if (t.mainType) {
                        if (mainTypeMap.containsKey(t.extension)) {
                            throw new IllegalApiDefinitionException("duplicate ApiFileUploadType found with the same extension " + t.extension + " marked with mainType");
                        }
                        if (t.contentTypeContainsStar()) {
                            throw new IllegalApiDefinitionException("content-type cannot contain '*' if its main-type is set");
                        }
                        mainTypeMap.put(t.extension, t);
                    }

                    allowedTypes.add(t);
                }
            }

            info.allowedTypes = allowedTypes.toArray(new FileUploadType[0]);
            pInfo.fileUploadInfo = info;
        }
    }

    @Extension
    class ApiInfoRegisterImpl implements ApiRegister {

        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            if (info.state != ApiOpenState.OPEN && info.state != ApiOpenState.DEPRECATED) return;

            for (ApiParameterInfo parameterInfo : info.parameterInfos) {
                if (parameterInfo.fileUploadInfo == null) continue;

                String folderName = parameterInfo.fileUploadInfo.folderName;
                if (schema.fileUploadInfoMap.containsKey(folderName)) {
                    throw new IllegalApiDefinitionException("duplicate definition found for file upload info: " + folderName);
                }
                schema.fileUploadInfoMap.put(folderName, parameterInfo.fileUploadInfo);
            }
        }
    }
}
