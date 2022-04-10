package com.estatetrader.document;

import java.io.Serializable;
import java.util.List;

import com.estatetrader.annotation.Description;

@Description("接口信息")
public class MethodInfo implements Serializable {
    @Description("接口名")
    public String methodName;

    @Description("接口分组名")
    public String groupName;

    @Description("返回值类型")
    public GenericTypeInfo returnType;

    @Description("API定义来源")
    public String origin;

    @Description("接口简介")
    public String description;

    @Description("接口详细信息")
    public String detail;

    @Description("调用接口所需安全级别")
    public String securityLevel;

    @Description("接口可用状态")
    public String state;

    @Description("接口参数列表信息")
    public List<ParameterInfo> parameters;

    @Description("接口返回业务异常列表")
    public List<CodeInfo> errorCodes;

    @Description("该方法会返回的隐式参数列表")
    public List<String> exportParams;

    @Description("接口组负责人")
    public String groupOwner;

    @Description("接口负责人")
    public String methodOwner;

    @Description("是否只允许通过加密通道访问")
    public boolean encryptionOnly;

    @Description("Integrated级别接口是否需要网关对请求进行签名验证")
    public boolean needVerify;

    @Description("所属的子系统")
    public String subsystem;

    @Description("生成request类的类名")
    public String requestClassName;

    @Description("所属JAR")
    public String jarFile;

    @Description("API的样例返回值")
    public String exampleValue;

    public ParameterInfo findParameter(String parameterName) {
        if (parameters == null) {
            return null;
        }
        for (ParameterInfo pInfo : parameters) {
            if (parameterName.equals(pInfo.name)) {
                return pInfo;
            }
        }
        return null;
    }
}
