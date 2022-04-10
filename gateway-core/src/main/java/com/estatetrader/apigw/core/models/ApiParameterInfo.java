package com.estatetrader.apigw.core.models;

import com.estatetrader.core.GatewayException;
import com.estatetrader.core.ParameterConverter;
import com.estatetrader.define.ApiParameterEncryptionMethod;
import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.entity.FileUploadInfo;
import com.estatetrader.generic.GenericType;

import java.util.regex.Pattern;

public class ApiParameterInfo {

    public ApiParameterInfo() {
    }

    /**
     * 参数是否是纯后端参数（客户端不可见）
     */
    public boolean serverOnly;

    /**
     * 参数名
     */
    public String name;

    /**
     * 参数类型
     */
    public GenericType type;

    /**
     * 用于定义String类型参数取值的枚举类型
     */
    @SuppressWarnings("rawtypes")
    public Class<? extends Enum> verifyEnumType;

    /**
     * 默认值
     */
    public Object defaultValue;

    /**
     * 默认值字符串形式
     */
    public String defaultValueInText;

    /**
     * 验证字符串表达式
     */
    public Pattern verifyRegex;

    /**
     * 验证错误提示
     */
    public String verifyMsg;

    /**
     * 是否必须
     */
    public boolean isRequired;

    /**
     * 为了防止在不安全网络环境下通信可能导致的敏感信息泄漏，某些敏感的API参数应以密文的方式传输
     * 按照参数的保密级别，我们提供以下加密方式，具体使用哪种加密方式完全由API定义决定
     *
     * 网关在收到客户端请求的参数后，按照API定义按照声明的加密方法对其进行解密，并使用解密后的明文调用对应的dubbo函数
     */
    public ApiParameterEncryptionMethod encryptionMethod;

    /**
     * 参数的本地名称，对应到java方法定义中的参数名称
     */
    public String nativeName;

    /**
     * 名字列表
     */
    public String[] names;

    /**
     * 自动注入参数需要用到的参数，具体含义由 {@link #name} 决定
     */
    public String injectArg;

    /**
     * 参数描述
     */
    public String description;

    /**
     * 是否是自动注入参数
     */
    public boolean isAutowired;

    /**
     * 由于安全原因需要在日志系统中忽略的参数
     */
    public boolean ignoreForSecurity;

    /**
     * 该参数在接口中的次序, 与类型相关. 当前可能的取值有 int0, int1...int9 str0, str1...str9
     * 目前被用在etl处理接口调用日志时按照该顺序放置各个参数
     */
    public String sequence;

    /**
     * 本参数可以使用其他接口隐式返回的数据
     */
    public ServiceInjectable injectable;

    /**
     * 指示需要网关协助上传文件，包含了上传文件的具体细节（目标云存储供应商、上传目录、可接受的content-type，文件限制）
     * TODO:
     * 如果以下三种情况同时满足：
     *  1. isAutowired == true
     *  2. fileUploadInfo != null
     *  3. 当前的request的content-type为<tt>multipart/form-data</tt>
     *  4. 网关的getParts()函数返回的part中包含与fileUploadInfo匹配的文件
     * 则网关会主动上传这个文件到相应的云存储提供商的指定目录，并将获得的文件url作为这个参数的实际值
     */
    public FileUploadInfo fileUploadInfo;

    /**
     * API参数的样例值
     */
    public String exampleValue;

    /**
     * 是否为mix的输入参数
     */
    public boolean isMixInput;

    /**
     * 将字符串类型转换为API参数的声明类型
     */
    public ParameterConverter converter;

    /**
     * 将文本类型转换为本参数所需要的类型
     * @param text 参数输入文本
     * @return 转换后的值
     */
    public Object convert(String text) throws GatewayException {
        if (text != null && verifyRegex != null && !verifyRegex.matcher(text).matches()) {
            throw new GatewayException(ApiReturnCode.PARAMETER_ERROR, verifyMsg, null);
        }

        if (text == null) {
            if (isRequired) {
                throw new GatewayException(ApiReturnCode.PARAMETER_ERROR);
            }
            return defaultValue;
        }

        try {
            return converter.convert(text);
        } catch (Exception e) {
            String message = String.format("parameter %s of type %s converts from text %s failed", name, type, text);
            throw new GatewayException(ApiReturnCode.PARAMETER_ERROR, message, e);
        }
    }
}
