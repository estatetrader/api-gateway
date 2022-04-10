package com.estatetrader.apigw.core.models;

import java.lang.reflect.Method;
import java.util.*;

import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.models.inject.DatumProviderSpec;
import com.estatetrader.define.*;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.apigw.core.models.inject.DatumConsumerSpec;
import com.estatetrader.generic.GenericType;
import com.estatetrader.objtree.ObjectTree;

/**
 * 接口信息
 */
public class ApiMethodInfo {
    /**
     * 方法名称
     */
    public String methodName;

    /**
     * which group it belongs to
     */
    public ApiGroupInfo groupInfo;

    /**
     * 定义该接口信息的jar（包含路径）
     * 为null如果不来自jar文件
     */
    public String jarFile;

    /**
     * 表示定义此接口信息的jar文件名（不包含路径）
     * 为null表示不来自jar文件
     * <p>
     * 注意：如果你修改了jarFile，则应同步修改jarFileSimpleName
     */
    public String jarFileSimpleName;

    /**
     * 接口类型
     */
    public ApiMethodType apiMethodType;

    /**
     * 返回值类型的泛型版本
     */
    public GenericType returnType;

    /**
     * 接口是否被mock
     */
    public boolean mocked;

    /**
     * 静态声明的返回值 mock 值
     */
    public Object staticMockValue;

    /**
     * 对返回的基本类型及其数组进行封装的封装器
     */
    public ResponseWrapper responseWrapper;

    /**
     * 方法调用说明
     */
    public String description;

    /**
     * 方法详细说明
     */
    public String detail;

    /**
     * 方法需要的安全级别
     */
    public SecurityType securityLevel = SecurityType.Anonym;

    /**
     * 方法所属的子系统
     */
    public String subSystem;

    /**
     * 资源所属组名
     */
    public String groupName;

    /**
     * 方法状态
     */
    public ApiOpenState state = ApiOpenState.CLOSED;

    /**
     * 参数类型
     */
    public ApiParameterInfo[] parameterInfos;

    /**
     * 该方法会返回的隐式参数列表
     */
    public Map<String, Class<? extends ServiceInjectable.InjectionData>> exportParams;

    /**
     * 该方法声明的内部业务异常映射，用于计算innerReturnCodeMap
     */
    public Map<Integer, Integer> innerCodeMap;

    /**
     * 该方法声明的内部业务异常映射，为innerCodeMap的return code形式，方便快速转换
     */
    public Map<Integer, AbstractReturnCode> innerReturnCodeMap;

    /**
     * 该方法可能抛出的业务异常的error code int集合, 用于二分查找
     */
    public int[] errors;

    /**
     * 所代理的方法的信息
     */
    public Method proxyMethodInfo;

    /**
     * 被代理的方法所属的接口,dubbo interface
     */
    public Class<?> dubboInterface;

    /**
     * 提供被代理方法的实例
     */
    public ServiceInstance serviceInstance;

    /**
     * 资源负责人
     */
    public String owner;

    /**
     * 资源组负责人
     */
    public String groupOwner;

    /**
     * 本接口是否只接受加密传输
     */
    public boolean encryptionOnly;

    /**
     * 接口是否需要网关进行签名验证
     */
    public boolean needVerifySignature;

    /**
     * 接口是否需要网关进行验证码验证
     */
    public boolean needVerifyCode;

    /**
     * 接口是否需要网关进行权限验证
     */
    public boolean needVerifyAuthorization;

    /**
     * 指示当前接口是否提供authentication服务
     */
    public boolean authenticationMethod;

    /**
     * 指示当前接口是否提供renew user token服务
     */
    public boolean renewUserTokenService;

    /**
     * 非空表示此API被声明为datum提供者
     */
    public String responseInjectProviderName;

    /**
     * 本API定义的datum provider，每个API最多可以定义一个provider，与datumProviderName对应
     */
    public DatumProviderSpec datumProviderSpec;

    /**
     * 本API定义的datum消费者格式，用于实现对返回值进行API注入
     */
    public DatumConsumerSpec datumConsumerSpec;

    /**
     * a list of filters used to modify the response of this API
     * will be executed against the response after the API returned.
     */
    public List<ResponseFilter> responseFilters;

    /**
     * optional list for mix
     */
    public String[] optionalList;

    /**
     * all services allowed to call
     */
    public Map<Class<?>, ServiceProxy> servicesAllowedToCall;

    /**
     * etk签发器信息，如果不为null，则表示此API能够签发etk
     */
    public ExtensionTokenIssuerInfo etkIssuerInfo;

    /**
     * 指示此API在执行时是否需要etk
     */
    public boolean requiredExtensionToken;

    /**
     * 指示是否记录此API的返回值，包含以下两种情况：
     * 1. 在网关设计中扮演重要角色的API（例如签发etk的API和续签的API）
     * 2. 在@HttpApi中指定recordResponse=true的API（返回值长度过大会被忽略）
     */
    public boolean recordResult;

    /**
     * API样例返回值
     */
    public String exampleValue;

    /**
     * 返回值结构的对象树，用于实现动态数据类型
     */
    public ObjectTree<DynamicTypeObjectMap> dynamicTypeObjectTree;
}