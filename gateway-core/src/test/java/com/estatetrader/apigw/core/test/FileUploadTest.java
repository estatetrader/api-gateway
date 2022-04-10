package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.*;
import com.estatetrader.define.Bucket;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.FileUploadInfo;
import com.estatetrader.apigw.core.models.ApiSchema;
import org.junit.Assert;
import org.junit.Test;


@ApiGroup(name = "test9", minCode = 0, maxCode = 100, codeDefine = FileUploadTest.RC.class, owner = "nick")
public class FileUploadTest extends BaseHttpTest {

    public static class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @HttpApi(name = "test9.t1", desc = "test9", security = SecurityType.Anonym, owner = "nick")
    public String t1(
            @ApiParameter(name = "url", required = true, desc = "url")
            @ApiFileUploadInfo(folderName = "NICK/FOLDER/PATH", bucket = Bucket.PUBLIC)
            @ApiFileUploadType(contentType = "image/jpeg", extension = ".jpg", maxSize = 1 << 20)
            @ApiFileUploadType(contentType = "audio/m*3", extension = ".mp3", maxSize = 2 << 20) // all audio types
            @ApiFileUploadType(contentType = "video/*p*", maxSize = 3 << 20) // all video types with sub types starting with mp
            @ApiFileUploadType(contentType = "*/*", maxSize = 4 << 20) // all content types
            String url
    ) {
        return url;
    }

    @Test
    public void testDefinition() {
        ApiSchema apiSchema = parseApi(FileUploadTest.class);
        FileUploadInfo uploadInfo = apiSchema.fileUploadInfoMap.get("NICK/FOLDER/PATH");
        Assert.assertNotNull(uploadInfo);
        Assert.assertEquals(1 << 20, uploadInfo.getUploadTypeByContentType("image/jpeg").maxSize);
        Assert.assertEquals(2 << 20, uploadInfo.getUploadTypeByContentType("audio/mp3").maxSize);
        Assert.assertEquals(3 << 20, uploadInfo.getUploadTypeByContentType("video/mp4").maxSize);
        Assert.assertEquals(4 << 20, uploadInfo.getUploadTypeByContentType("application/json").maxSize);
        Assert.assertEquals(4 << 20, uploadInfo.getUploadTypeByContentType("audio/mp4").maxSize);

        Assert.assertEquals("image/jpeg", uploadInfo.getUploadTypeByExtension(".jpg").contentType);
        Assert.assertNull(uploadInfo.getUploadTypeByExtension(".mp3"));
    }
}
