package com.estatetrader.rule.definition;

import com.estatetrader.entity.FileUploadInfo;

public interface FileUploadInfoListener {
    FileUploadInfo getFileUploadInfo(String folder);
}
