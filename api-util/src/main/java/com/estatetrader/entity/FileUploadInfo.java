package com.estatetrader.entity;

import com.estatetrader.define.Bucket;
import com.estatetrader.define.FileUploadCloud;
import com.estatetrader.util.Lambda;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class FileUploadInfo implements Serializable {

    /**
     * 是否私有读文件
     */
    public Bucket bucket;

    /**
     * 选择云存储的提供商，默认为阿里云OSS
     */
    public FileUploadCloud cloud;

    /**
     * 该文件所在文件夹名,
     *  例如:
     *      PRODUCT
     *      PRODUCT/IMG
     */
    public String folderName;

    /**
     * 针对云存储的提供商提供额外参数
     */
    public String options;

    /**
     * 支持的文件类型列表
     */
    public FileUploadType[] allowedTypes;

    public FileUploadType getUploadTypeByContentType(String contentType) {
        return Lambda.find(allowedTypes, t -> t.contentTypeMatch(contentType));
    }

    public FileUploadType getUploadTypeByExtension(String extension) {
        List<FileUploadType> fts = Lambda.filter(allowedTypes, x -> Objects.equals(x.extension, extension));
        if (fts.size() == 0) return null;
        if (fts.size() == 1 && !fts.get(0).contentTypeContainsStar()) return fts.get(0);
        return Lambda.find(fts, x -> x.mainType);
    }
}
