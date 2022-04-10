package com.estatetrader.define;

public enum FileUploadCloud {
    /**
     * 阿里云OSS
     */
    OSS,
    /**
     * 腾讯点播系统, 使用ApiFileUploadInfo中的options指定上传视频时需要使用的任务流
     */
    VOD,
    /**
     * 七牛云
     */
    QINIU,
}
