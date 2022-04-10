package com.estatetrader.document;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("参数信息")
public class ParameterInfo implements Serializable {
    @Description("参数名")
    public String name;

    @Description("在sdk中的参数名称")
    public String nameInSdk;

    @Description("参数类型")
    public GenericTypeInfo type;

    @Description("默认值(非必选参数)")
    public String defaultValue;

    @Description("验证规则(正则表达式)")
    public String verifyRegex;

    @Description("验证失败提示")
    public String verifyMsg;

    @Description("是否必选参数")
    public boolean required;

    @Description("只能通过服务端注入来传入该参数")
    public boolean injectOnly;

    @Description("参数描述")
    public String description;

    @Description("该参数可以使用的隐式参数名")
    public String serviceInjection;

    @Description("参数的加密方法，如果不为null，则客户端应在传输前对参数进行加密")
    public String encryptionMethod;

    @Description("参数在接口中的次序, 当前可能的取值有 int0, int1...int9 str0, str1...str9")
    public String sequence;

    @Description("参数需要的文件上传信息")
    public String fileUploadInfo;

    @Description("参数的样例值")
    public String exampleValue;
}
