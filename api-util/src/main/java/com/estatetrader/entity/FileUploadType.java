package com.estatetrader.entity;

import java.io.Serializable;

/**
 * 承载来自@ApiFileUploadType的信息
 */
public class FileUploadType implements Serializable {

    /**
     * 客户端请求携带的content-type的值
     */
    public String contentType;

    /**
     * 受支持的文件扩展名
     */
    public String extension;

    /**
     * 文件能够允许的最大值
     */
    public int maxSize;

    /**
     * 指示此类型是否为该extension的主类型
     * 由于同一个extension可能会有多种content-type，所以在根据extension寻找对应的content-type的时候会出现多种后选项
     * 这时mainType为true的那个type会胜出
     * 每个文件夹的每种extension最多可以定义一个mainType为true的FileUploadType
     */
    public boolean mainType;

    /**
     * 指示网关在上传此类型文件时，是否在最终生成的文件名中包含图片的纬度（长✖宽）信息
     * 只能在文件为图片类型时才可以设置此标志
     */
    public boolean includeImageDimension;

    public boolean contentTypeContainsStar() {
        return contentType.contains("*");
    }

    public boolean contentTypeMatch(String targetContentType) {
        if (contentType.equals(targetContentType)) return true;
        if (!contentType.contains("*") || targetContentType == null) return false;

        String[] parts1 = contentType.split("/");
        String[] parts2 = targetContentType.split("/");
        if (parts1.length != parts2.length) return false;

        for (int i = 0; i < parts1.length; i++) {
            String part1 = parts1[i], part2 = parts2[i];
            if (part1.equals(part2)) continue;

            String[] segs = part1.split("\\*");
            int k = 0;
            for (int j = 0; j < segs.length; j++) {
                String seg = segs[j];
                int index = part2.indexOf(seg, k);
                if (j == 0 && index != 0 || index < 0) return false;
                k = index + seg.length();
            }
            if (!part1.endsWith("*") && k != part2.length()) return false;
        }

        return true;
    }
}
