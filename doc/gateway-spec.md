# 2 面向网关编程

网关作为介于前后端的中间件，同时为前端和后端提供功能，这涉及到前端和后端开发人员协作开发，
即后端开发人员使用网关提供的语法声明它所要提供的所有API，前端通过网关使用这些API，
这种工作模式称为面向网关编程。

针对每个微服务，如果它需要向网关暴露API（即向前端页面提供功能），则它需要在它的代码中按照网关规定的语法进行声明。

## 2.1 基本语法

本章节介绍后端如何声明一个简单的API以及前端如何使用这个API（以安卓为例）

### 2.1.1 声明API

使用`@ApiGroup`声明一个API组，被`@ApiGroup`注解的类中所定义的所有API都隶属于这个组。
使用`@HttpApi`声明一个API

```java
// ProductInfo.java 声明API的返回值类型
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("商品编号")
    public int id;
    @Description("商品名称")
    public String name;
}

// ProductService.java 声明API所在的服务
@ApiGroup(name = "product", minCode = 1000, maxCode = 2000, codeDefine = ProductService.RC.class, owner = "nick")
public interface ProductService {

    // 声明API返回的错误代码类
    class ReturnCode extends AbstractReturnCode {
        protected ReturnCode(String desc, int code) {
            super(desc, code);
        }

        // 声明实际可能抛出的业务异常代码
        public static final int _C_PRODUCT_NOT_FOUND = 1001;
        public static final ReturnCode PRODUCT_NOT_FOUND = new ReturnCode("商品不存在", _C_PRODUCT_NOT_FOUND);
    }

    // 声明API
    @HttpApi(name = "product.getProduct", desc = "获取商品信息", security = SecurityType.Anonym, owner = "nick")
    @DesignedErrorCode({ReturnCode._C_PRODUCT_NOT_FOUND}) // 声明可能抛出的业务错误代码
    ProductInfo geProduct(
            @ApiParameter(name = "id", required = true, desc = "商品编号") int id
    );
}

// ProductServiceImpl.java 实现API的具体逻辑
public class ProductServiceImpl implements ProductService {
    @Override
    public ProductInfo geProduct(int id) {
        if (id == 1) {
            ProductInfo info = new ProductInfo();
            info.id = id;
            info.name = "product#" + id;
            return info;
        } else {
            throw new ServiceRuntimeException(ReturnCode.PRODUCT_NOT_FOUND);
        }
    }
}
```

网关会自动分析上述java代码，并从中提取出它声明的API：

* API名称：`product.getProduct`以及Owner `nick`
* API所属的接口分组：`product`以及Owner `nick`
* API能够抛出的错误代码范围：1000 - 2000（网关会对实际抛出的错误代码进行校验，以确保每个服务只抛出它所声明的错误代码
* API的权限级别为匿名
* API的返回值类型：`ProductInfo`
* API可能会抛出`ReturnCode._C_PRODUCT_NOT_FOUND`业务错误代码
* API在检测到id不等于1时抛出`ServiceRuntimeException(ReturnCode.PRODUCT_NOT_FOUND)`业务异常

### 2.1.2 API参数

本质上，一个API就是一个被@HttpApi标注了的普通Java函数，而这个函数的所有参数便是对应API的参数。网关提供了功能丰富的参数类型的支持，
包括直接由客户端传入的参数和从上下文自动提取的参数，后文有详细介绍。

为了方便统计各API的调用情况和具体细节，网关会记录每次API的请求信息以及调用API时使用的具体参数。

### 2.1.3 API返回值

返回值类型是API契约的一部分，表征这个API能够返回给前端的稳定的数据格式。使用这种声明式的API可以为前端提供静态类型信息
以便在前端代码（app）编译阶段发现数据格式不一致的问题。

默认情况下，基于性能考虑，网关在记录API调用信息的时候不会记录其具体返回值。但是如果你的API的返回值非常重要，为了方便后期排查问题，
可以设置`@HttpApi`的`recordResult`字段为true，以要求网关记录每次API调用之后的返回值。但要注意返回值不能太大。

### 2.1.4 API SDK生成

上述API生成的SDK为（以Android为例）：

```java
// Product_GetProduct.java 请求API product.getProduct 的代理Request定义
/**
 * 获取商品信息
 *
 * @author nick
 */
public class Product_GetProduct extends BaseRequest<Api_Product_ProductInfo> {

    /**
     * 当前请求的构造函数，以下参数为该请求的必填参数
     *
     * @param id 商品编号
     */
    public Product_GetProduct(int id) {
        super("product.getProduct", SecurityType.Anonym);

        try {
            params.put("id", String.valueOf(id));
        } catch(Exception e) {
            throw new LocalException("SERIALIZE_ERROR", LocalException.SERIALIZE_ERROR, e);
        }
    }

    /**
     * 私有的默认构造函数，请勿使用
     */
    private Product_GetProduct() {
        super("product.getProduct", SecurityType.Anonym);
    }

    /**
     * 当前请求有可能的异常返回值
     */
    public int handleError() {
        // ...
    }

    /**
     * 不要直接调用这个方法，API使用者应该访问基类的getResponse()获取接口的返回值
     */
    @Override
    protected Api_Product_ProductInfo getResult(JsonObject json) {
        // ...
    }
}

// Api_Product_ProductInfo.java 返回值类型的定义
public class Api_Product_ProductInfo implements JsonSerializable {

    /**
     * 商品编号
     */
    public int id;

    /**
     * 商品名称
     */
    public String name;

    /**
     * 反序列化函数，用于从json字符串反序列化本类型实例
     */
    public static Api_Product_ProductInfo deserialize(String json) {
        // ...
    }

    /**
     * 反序列化函数，用于从json节点对象反序列化本类型实例
     */
    public static Api_Product_ProductInfo deserialize(JsonObject json) {
        // ...
    }

    /**
     * 序列化函数，用于从对象生成数据字典
     */
    public JsonObject serialize() {
        // ...
    }
}
```

由上可见，网关会为每个API生成一个Request代理类，并为API的返回值`ProductInfo`生成了`Api_Product_ProductInfo`类型。

### 2.1.5 API SDK实际用法

客户端代码可以使用如上生成的两个类以及其他SDK中的辅助类来调用我们的API `product.getProduct`：

```java
public class Application {
    public static void main(String[] args) {
        Product_GetProduct getProduct = new Product_GetProduct(1);
        ApiAccessor apiAccessor = createApiAccessor();
        ServerResponse serverResponse = apiAccessor.fillApiResponse(getProduct);
        int requestCode = serverResponse.getReturnCode(); // 网关在处理请求时返回的错误代码
        int accessCode = getProduct.getReturnCode(); // 后端服务器在处理请求时返回的错误代码
        Api_Product_ProductInfo productInfo = getProduct.getResponse(); // 请求的实际返回结果
    }

    public static ApiAccessor createApiAccessor() {
        String appId = 2; // your app id
        int versionCode = 251; // your app version code
        String versionName = "2.5.1"; // your version name
        int connTimeout = 3000;
        int soTimeout = 3000;
        String userAgent = "android-client";
        String apiGatewayUrl = "your/api/gateway/server/url";
        ApiContext apiContext = ApiContext.getDefaultContext(appId, versionCode, versionName);
        return new ApiAccessor(apiContext, connTimeout, soTimeout, userAgent, apiGatewayUrl);
    }
}
```

## 2.2 网关对外通信协议

上文提到网关的核心功能是将与前端之间的`HTTP`协议转换为与后端之间的`DUBBO`协议，这里面的来自前端的`HTTP`即时我们本章节所讨论的
网关对外通信协议。前端（即`API SDK`）与网关之间的通信协议基于标准的`HTTP/HTTPS`，但是对`HTTP`请求（request）的`HEADER`和`BODY`
以及请求的响应（`response`）格式做了具体规定。

### 2.2.1 请求规范

请求由前端（API SDK）向网关发起，请求需要包含以下参数：

* 通用参数
  通用参数以下划线(_)开头，与所调用的具体API无关，一般每次请求都应提供
  * _mt
  
    所要调用的API的名称，多个API之间用半角逗号隔开，例如 product.getProduct,price.getPrice

  * _aid
  
    所属APP的编号，一个预定义的正整数值

  * _tk
  
    当前客户端所持有的能够代表当前用户身份的token，在登录时由后端用户系统签发。匿名API无需提供

  * _sm
  
    对请求签名时所选用的算法。网关要求前端对每一次请求进行签名，前端有权在网关提供的签名算法列表中选择一种算法，
    并通过_sm告知网关具体所选算法。此参数不区分大小写。

  * _sig
  
    对请求文本执行_sm指定的签名算法后得到的签名，Base64格式。稍后会对签名做详细解释。
  
  其他通用参数可以参考实时文档(/apigw/info.api)
  
  > 需要注意的是，前端可以传递任何网关没有在文档声明的通用参数，网关不会拒绝它不能识别的通用参数
  
* 非通用参数

  非通用参数为某一个API提供具体参数，参数名需要和API定义的参数名一致：
  例如，假设有一个API，api1，参数名id，参数按照下述方式给出：
  
  ```text
  _mt = "api1"
  id = 1
  ```
  
  但在多API请求的情况下使用API序号作为前缀来区分它属于哪个API。
  例如，假设有两个API，api1和api2，它们各自有一个参数叫id，参数按照下述方式给出：
  
  ```text
  _mt = "api1,api2"
  0_id = 1
  1_id = 2
  ```
  
请求参数可以拼接到请求到URL中作为query string，也可以作为请求的body传递给网关。请求的谓词可以是`GET/POST`，并且语义相同。
请求需发送到网关的`/apigw/m.api`端点。

> 注意：如果要发给网关的参数值是复杂类型（例如数组、对象等），则需要对该值进行JSON序列化，并将序列化之后的字符串作为参数值。

实际用例：

* URL Query String：

  ```text
  URL:    http://api.gatewa.domain/apigw/m.api?_mt=product.getProductInfo&id=1&_aid=1&_sm=md5&_sig=signature
  METHOD: GET
  ```
  
* REQUEST BODY:

  ```text
  URL:    http://api.gateway.domain/apigw/m.api
  BODY:   _mt=product.getProductInfo&id=1&_aid=1&_sm=md5&_sig=signature
  METHOD: POST
  ```

### 2.2.2 多API请求

前端在一些情况下，例如渲染页面，可能会请求多个API。为了减少多次HTTP请求带来的额外开销，网关支持在一次HTTP请求中包含多个API。
默认情况下一次请求中的所有API在网关会同时执行（和在_mt中声明的API的顺序无关），以此来缩减整体处理时间。
在某些情况下，前端可能需要一个API在另外一个API执行之后执行，即API执行依赖。网关原生支持API的执行依赖。
网关也支持在一次请求中包含两个同名API，但使用不同的参数。

使用如下语法来声明一个复杂的_mt：

```text
_mt="api1,api2@1,api1@2,api3:api2@1,api4:api1/api2@2/api3,..."
```

上述声明的含义是：

* 此次请求一共包含api1, api2@1, api2@2, api3, api4五个请求
* 其中api2被包含了两次：api2@1和api2@2
* api3依赖了一个API：api2的api2@1
* api4依赖了三个API：api1, api2@2, api3

如果一个API依赖了另外一个API，则被依赖的API可以暴露一些参数给依赖它的API，从而达到更复杂的API依赖应用场景，即API注入。
API注入是API的固有特性，由后端开发人员控制，前端无需关注也不能修改这种注入行文，后面后详细探讨具体的注入用法细节。

### 2.2.3 请求签名

前端对请求进行签名，并将签名的结果连同请求参数一起传输给网关，网关使用相同的过程进行同样的签名，然后和前端的签名结果进行对比。
如果签名不一致意味着此次请求可能被中间人篡改，网关会直接拒绝执行此次请求，并返回前端签名错误代码（-180, -181, -182）。
前端可以从网关所支持的签名算法列表中选择一款签名算法。

网关支持两种算法模式，静态盐签名和动态盐签名。所谓盐，指的是在对参数进行哈希求值时混入一串字符，以防止第三方模拟签名过程，使之失效。
为了保证签名的不可模拟性，我们需要使用动态盐，即只有网关和客户端知道的一串随机字符，每个客户端（设备）都不相同。这串随机字符称之为
`device secret`，客户端在注册设备时由服务端签发，注册设备时除了签发`device secret`之外，还会签发一个设备凭据`device token`，
简称`dtk`，是一个由`AES`加密的字符串，其中包含了刚刚签发的`device secret`。`device secret`属于敏感信息，不适宜在网络上传输，
所以在客户端第一次收到它之后需要保存到本地缓存中，后续的每次请求都不应该携带这个信息。相反，由于`dtk`或者`utk`是对称加密之后的密文信息，
可以在网络上传输。

客户端在签名前，将动态盐`device secret`拼接到待签名的请求参数上，并对拼装结果求哈希值，请求时将客户端存在的`utk`
（如果有，或者`dtk`，如果`utk`不存在）作为通用参数`_tk`传递给网关。网关收到请求之后，会对收到的`utk/dtk`进行解密，并从中提取其中保存的
`device secret`，并以此作为签名的动态盐进行签名校验。

现在出现了一个问题，很明显，每次请求都应该使用动态盐进行签名保护，但是如果这样的话，获取`device token`的请求（注册设备）将无法实现，
因为此时客户端还有没有可用的`device secret`和`dtk/utk`。所以为了解决这个问题，我们引入了静态盐，即使用一个网关和客户端提前约定好的
固定的随机字符串，用于请求某些安全性不是很高的API（例如所有的匿名API）。

静态盐的引入解决了`device secret`的获取问题，但是也为整个签名保护体系引入了安全隐患，因为客户端代码是公开的，而静态盐又直接硬编码到
客户端代码中，所以等价于静态盐公开出去，因此静态盐的签名是可伪造的，属于不可信赖的签名。参考`HTTPS`的实现方式或许解决这种问题，做到
100%安全。但是这引入了巨大的复杂度，并且现实意义不大，因为`HTTPS`已经提供了很完善的解决方案，转用`HTTPS`通信协议可以替代并超越
我们实现的签名保护体系。

尽管`HTTPS`提供了通信相关的保护，签名保护体系仍然有其存在的价值。从文档第五章我们可以得知，使用动态盐签名，我们可以做到可靠的设备行为跟踪。
假设设备凭据（`dtk`）落入攻击者手中，因为签名保护，他也必须要与之匹配的动态盐`device secret`才能使用该凭据`dtk`，否则会因为签名不匹配
而遭到拒绝。

```plantuml
participant "客户端业务层" as cb
participant "客户端传输层" as ct
participant "API-Gateway" as gt

cb -> ct: 调用API
activate ct
ct -> ct: 将请求参数和值按规定顺序拼接成请文本request
alt device secret和dtk或utk同时存在
ct -> ct: 选择动态盐salt
else
ct -> ct: 选择静态盐salt
end
ct -> ct: 将request和salt拼接到一起得到text
ct -> ct: 选择一种签名算法sm（md5/sha1等）
ct -> ct: 使用sm对text求哈希值sig
ct -> ct: 将_sig=sig, _sm=sm加入到请求体中
ct -> gt: 发送包含签名信息的请求
deactivate ct
activate gt
alt 客户端提供的_tk有效
gt -> gt: 使用动态盐salt
else tk无效并且请求为全匿名API
gt -> gt: 使用静态盐salt
else 请求中存在非匿名API
gt -> gt: 签名不匹配
end
gt -> gt: 根据通用参数_mt决定签名算法\n按照和客户端相同的方式计算签名
alt 签名不匹配
    gt -> gt: 分析推导签名出错原因（-180/-181/-182）
    gt -> ct: 返回相应错误码
    activate ct
    alt "-180"
        ct -> ct: 清除utk/ntk
    else "-181"
        ct -> ct: 清除did/device-secret/dtk
        ct -> ct: 重新注册设备
    end
else 签名匹配
    gt -> gt: 继续处理请求
    gt -> ct: 返回请求结果
    deactivate gt
    ct -> cb: 返回请求结果
    deactivate ct
end
deactivate gt
```

#### 2.2.3.1 选择静态盐还是动态盐

为了保证签名保护的效果，我们应该尽可能使用动态盐，但是要确保客户端的选择和服务端的选择一致。

在讨论之前，我们需要引入两个概念：*匿名请求*和*非匿名请求*。匿名请求指的是_mt参数指定的所有API均为匿名权限级别的API，
如果有任何一个API不是匿名的，则此次请求为非匿名请求。

客户端在选择盐的时候需要参考以下规则：

1. 如果此次请求为匿名请求（所有API均为匿名权限级别的API）。总体规则为，如果传递`_tk`则使用动态盐，否则静态盐，这又分以下几种情况：
   1. 本地有可用的`device secret`和`dtk/utk`，则选择使用动态盐，即`device secret`，并在请求网关时将`dtk/utk`传递给网关
   2. 本地有可用的`device secret`，但是没有`dtk`或`utk`，则选择使用静态盐，因为`dtk/utk`的缺失，你无法将动态盐传递给网关，导致签名
      被迫降级为静态盐签名
   3. 本地没有可用的`device secret`，无论存在不存在`dtk/utk`，都应该选择静态盐，此时请求网关不应该将`dtk/utk`传递给网关
2. 如果此次请求为非匿名请求（请求中存在非匿名权限的API），则：
   1. 如果本地没有可用的`device secret`，则须延迟本次请求，并清理设备信息(`did/dtk/utk`），并重新注册设备
   2. 如果本地有可用的`device secret`，但是`dtk`和`utk`均不存在，则须延迟本次请求，并清理设备信息(`did/device secret/dtk/utk`)，
      并重新注册设备
   3. 如果本地有可用的`device secret`，并且`dtk`或者`utk`存在，则使用`device secret`作为动态盐，并将存在的`dtk/utk`传递给网关

> 注意：由于之前的签名策略是，当切仅当请求是匿名请求时使用静态盐，而现在改为匿名请求通过传递`_tk`来使用动态盐。
理想的做法是，网关在判断有合法`token`时仅做动态盐签名校验。考虑到遗留客户端问题，我们会在动态盐校验失败后对匿名请求进行静态盐签名校验。
这是一种临时过度方案！！！客户端由于各种原因而选择了静态盐签名校验，则*不应该*传递`dtk/utk`给网关。

#### 2.2.3.2 签名哈希值算法列表

网关支持多种求哈希值算法，具体的选择由客户端通过`_sm`通用参数指定。

受支持的静态盐的签名算法列表：

* MD5
* SHA1 （默认）

受支持的动态盐的签名算法列表：

* RSA
* MD5
* SHA1
* ECC （默认）

#### 2.2.3.3 签名计算过程

网关按照如下的算法计算签名，客户端必须按照如下的语义进行计算。

签名计算过程如下：

1. 设此次请求的所有参数（除正在计算的`_sig`之外）为`parameters`：

   ```text
   parameters = [{name: name1, value: value1}, {name: name2, value: value2}, ...]
   ```

   因为签名的计算过程需要在客户端和网关分别计算，这里面涉及到不同语言平台以及通信相关的问题，要确保客户端的计算语义和网关一致。
   为了达到这一点，在参数方面有两个地方需要注意：
   1. 参数值的`toString`过程  
      因为不同平台对非字符串类型的参数值的toString实现算法不同，例如`null/undefined/bool`，浮点类型的精度也可能会是一个问题
   2. 参数传递给网关的过程  
      非字符串类型的参数不一定都能正确传递给网关，例如`undefined`类型的参数会在发起请求的时候被过滤掉，导致网关接收不到这类参数

   因此，为了和网关逻辑保持一致，客户端应该在计算签名之前，对请求参数（包含业务参数和通用参数）进行如下处理：
   1. 去除所有参数值为`undefined/null`的参数
   2. 将所有非字符串类型的参数转换为字符串类型

   > 注意：客户端必须使用如上步骤处理之后的参数请求网关，这样可以保证客户端签名使用的参数列表和网关收到的参数一致

2. 按照参数名的自然（Unicode）顺序排列对`parameters`进行生序排序：

   ```text
   sortedParameters = parameters.sortBy(p -> p.name)
   ```

3. 将每个参数的参数名和参数值用等于号连接起来，然后将这些字符串直接拼接起来

   ```text
   requestParametersText = sortedParameters.map(p -> p.name + '=' + p.value).join()
   ```

   > 注意：parameter.key和parameter.value均为字符串类型，如果此parameter对应对API参数是复杂类型（数组或者对象），
则它必须是JSON.stringify()之后的字符串。

4. 确定签名用到的盐。由于用到的算法和加密过程都是公开的，第三方完全可以模拟签名过程，使整个签名验签系统失效，
   所以需要一个特殊的字符串对签名内容进行混淆，这个字符串就是所谓的"盐"（`salt`），参考上一节决定最终选择静态盐和动态盐

5. 将第三步得到的`requestParametersText`和第四步得到的`salt`拼接到一起，得到最终的签名输入文本：

   ```text
   signatureInputText = requestParametersText + salt
   ```

6. 选择一种签名算法（参照通用参数_sm），并对上一步得到对signatureInputText进行签名

   ```text
   _sig = md5(signatureInputText) // if _ms == md5
   ```

网关在收到客户端的请求之后，会使用以上步骤进行签名计算，并将计算结果和客户端传递的`_sig`进行比较。两个值不相同意味着签名校验失败。

如果签名校验失败，则终止本次请求的处理，并返回`-180/-181/-182`给客户端，以提示客户端如何应对这种错误。这些错误的具体含义如下：
  
   1. -180 指示客户端签名时使用的`deviceSecret`（即动态盐）和它传递给网关的`utk/ntk`（使用`_tk`通用参数）中的`key`不一致。
      客户端在收到这种错误码时应该清理掉其本地的`utk`并提示用户重新登录（以获取新的`utk`）
   2. -181 指示客户端签名时使用的`deviceSecret`（即动态盐）和它传递给网关的`dtk`（使用`_tk`通用参数）中的`key`不一致。
      客户端在收到这种错误码时应该清理掉其本地的`did/deviceSecret/dtk/utk`，然后重新生成新的`did`并注册设备（以获取新的`dtk`）
   3. -182 未知的签名错误。除`-180/-181`之外的所有签名错误都是`-182`。这个错误码意味着客户端出现了签名相关的bug，清理信息和重新登录
      或者注册都不能缓解这个问题。`-182`是一个集合错误码，它包含了其他几种更具体的错误码（每种错误码对应到一种可能的情形上），
      这些具体的错误码不会暴露给前端，但是会在网关日志上表现出来，有如下几种：
      1. -183 静态盐签名错误，表示客户端没有提供有效`utk/dtk`，并且仅请求了匿名API时出现的签名不匹配。
         这种情况可能时客户端使用了错误的静态盐，或者对请求参数的处理逻辑和网关不一致，参考以上步骤。
      2. -184 动态盐缺失签名错误，表示客户端使用了静态盐签名了非匿名请求。非匿名请求仅能使用动态盐，但是客户端却使用了静态盐。
         网关可以通过模拟使用静态盐对此情景进行核对。这种情况可能意味着本次请求中的某个API从匿名转换到了非匿名，而客户端skd没有及时更新。
         也可能是因为客户端对动态盐的应用不准确。
      3. -186 (open-api) 三方平台的信息不存在。客户端请求了一个不存在的三方平台。因为open-api的验签用的是rsa算法，并且每个三方平台的
         公私钥对是独立的，在验证签名时找不到这个信息会便会报告`-186`
      4. -187 (open-api) 参考`-186`，配置的三方平台信息未配置当前请求的API
      5. -188 (open-api) 参考`-186`，rsa签名验证不匹配

### 2.2.4 响应规范

网关在收到前端请求之后，根据请求中的`_mt`参数进行API编排，根据`_mt`中给出的API依赖关系规划出一个执行工作流，
借助与其他通用参数和API参数提供的数据，执行这个工作流，并将执行结果写入到一个预定义的响应流中。
根据不同的使用场景，响应格式可分为两种原始格式和标准格式两种。

#### 2.2.4.1 原始格式

在某些情景下，客户端需要拿到API返回值的原始内容，不进行任何包装，例如与三方系统集成，外部系统不能识别我们的响应格式。
这个时候需要API直接兼容第三方所需要的返回格式，网关不做任何修改。当然，在这种用法只允许请求中只有一个API。

#### 2.2.4.2 标准格式

响应中包含这次请求本身的错误代码、每个API返回的错误代码、每个API的返回值以及其他旁路信息，支持JSON或XML序列化方式。
包含以下字段：

| 字段名称 | 含义 |
|:---|:---|
| stat | 执行状态，包含请求的错误代码和每个API的执行状态，Response类型 |
| content | 每个API的具体返回值，数组类型，按照_mt中API的顺序，每个元素对应一个API的返回值或其包装 |

stat字段的Response类型所包含在字段列表：

| 字段名 | 含义 | 客户端处理流程 |
|:---|:---|:---|
| systime | 当前服务端时间 | 用于同步本地时间和服务器时间 |
| code | 请求错误码，非0表示请求处理失败 | 在非0时，针对特定的错误码，应执行相应的逻辑 |
| cid | 调用标识符，用于跟踪请求处理链路 | 客户端在回报与一次请求有关的客户端日志时，应将此字段的值包含进去，以方便关联客户端日志与请求在后端的完整链路 |
| stateList | API调用状态，code的信息请参考ApiCode定义文件 | |
| notificationList | 服务端返回的通知事件集合，旁路返回信息 | 与所调用API的作者确定每个通知的含义与期待的行为 |
| newUserToken | 续签后的新user token，此字段不为空意味着本次请求所使用的token已过期但续签成功 | 如果不为空，请使用此字段来替换你之前保存的utk |
| newUserTokenExpire | 续签后的新user token的过期时间 | 记录此值，用于跟踪新的utk的过期时间，进而能提前预知utk的有效期 |
| needRenewUserToken | 表示此次请求中使用的utk已经过期，但无法续签，并在网关发生了utk降级 | 如果为true，客户端应主动清理utk，并在恰当的时间引导用户重新登录 |
| needRenewExtensionToken | 表示此次请求中使用的etk已经过期（或其他不合法的情况） | 如果为true，客户端应主动清理etk，并重新申请 |

其中`stateList`是`CallState`对象的数组。`CallState`表示一次API的调用结果，和`_mt`的API一一对应，
即_mt的第n个API的返回状态是stateList中的第n项。`CallState`的格式如下：

| 字段名 | 含义 |
|:---|:---|
| code | 返回值 |
| length | 数据长度 |
| msg | 返回信息 |

#### 2.2.4.3 返回值封装类型

考虑到前端代码的升级周期，网关对API返回值的兼容性修改提供充分的支持。对于API返回的非自定义对象类型，
网关都提供了一个对应的包装类，将原始的返回值封装到一个包装类的字段中，从而对前端返回一个自定义对象。

网关提供以下包装类：

| API返回值类型 | 包装类 | 包装类中保存原始返回值的字段 | 字段类型 |
|:---|:---|:---|:---|
| boolean | BoolResp | value | boolean |
| boolean[] | BoolArrayResp | value | boolean[] |
| byte | NumberResp | value | int |
| byte[] | NumberArrayResp | value | int[] |
| short | NumberResp | value | int |
| short[] | NumberArrayResp | value | int[] |
| char | NumberResp | value | int |
| char[] | NumberArrayResp | value | int[] |
| int | NumberResp | value | int |
| int[] | NumberArrayResp | value | int[] |
| long | LongResp | value | long |
| long[] | LongArrayResp | value | long[] |
| float | DoubleResp | value | double |
| float[] | DoubleArrayResp | value | double[] |
| double | DoubleResp | value | double |
| double[] | DoubleArrayResp | value | double[] |
| String | StringResp | value | String |
| String[] | StringArrayResp | value | Collection\<String> |
| Date | StringResp | value | String |
| Date[] | DateArrayResp | value | Collection\<String> |
| BigDecimal | DoubleResp | value | double |
| BigDecimal[] | DoubleArrayResp | value | double[] |
| Collection\<String> | StringArrayResp | value | Collection\<String> |
| Collection\<BigDecimal> | DoubleArrayResp | value | double[] |
| Collection\<Date> | DateArrayResp | value | Collection\<String> |

> 除上表列举之外的API返回值类型都按原格式返回

例如，假设前端只请求了一个API api1，API成功执行并返回了int 1，则客户端收到的返回值为：

```javascript
let result = {
    "stat": {
        "cid": "a:8a8aa8|t:270|s:1544065925879",
        "code": 0,
        "newUserToken": "",
        "newUserTokenExpire": 0,
        "notificationList": [],
        "stateList":[
            {
                "code": 0,
                "length": 1,
                "msg": "成功"
            }
        ],
        "systime": 1544065925892
    },
    "content": [
        // api1返回值的包装对象，API的实际返回值保存在字段value中
        {
            "value": 1
        }
    ]
}
```

### 2.2.5 通信实践

本章节我们以javascript为例，举例解释客户端如何与网关通信。

假设网关存在我们在$2.1.4定义的API `product.getProduct`，可以使用如下方式调用这个API：

```javascript
let apiGatewayUrl = "http://api.gateway.domain/apigw/m.api";
let response = await fetch(apiGatewayUrl, {
    "body":"_mt=time.getServerTime&id=1",
    "method":"POST"
});
let result = await response.json();
console.debug(result.stat.code); // this is the request processing return code, which will also be 0
console.debug(result.stat.stateList[0].code); // code will be 0
let productInfo = result.content[0]; // the result of API product.getProduct
console.debug(productInfo.id); // id will be 1
console.debug(productInfo.name); // name will be "product#1" deal to previous implementation of product.getProduct
```

## 2.3 自定义实体类型

为了丰富返回值类型和参数类型，网关支持自定义实体类型。

### 2.3.1 自定义实体类型的特征

自定实体类型满足以下特征：

* 类型需实现`Serializable`接口
* 将字段声明为公有类型`public`。不支持使用private/get/set方式声明字段（考虑到数据结构需要多语言支持）
* 类型和字段上都需要有`@Description`注解，用于解释这个类或者字段的实际含义（可以使用中文）。
  此信息会被展示到自动生成的API文档上并且嵌入到客户端SDK中。为了方便前端开发以及后续维护，
  需要给每个字段和数据类型一个合理的@Description
* 暂不支持继承，所有父类中的字段都会在文档和SDK中丢失
* 暂不支持范型类型，范型类型会导致API分析失败。但是可以使用List&lt;T&gt;
* 返回值的字段可以使用其他自定义类型，要求满足上述特征，即数据结构可以嵌套

参考示例代码：

```java
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("商品编号")
    public long id;
    @Description("商品名称")
    public String name;
    @Description("价格")
    public PriceInfo price;
}

@Description("price-info")
public class PriceInfo implements Serializable {
    @Description("商品编号")
    public long id;
    @Description("商品价格")
    public int price;
}
```

### 2.3.2 枚举类型的字段

一般情况下，枚举类型和其他类型在用法上没有什么区别，由于其具有强类型，表意清楚，所以具有一定的实用价值。但是在枚举定义发生变动的时候，
由于DUBBO及其相关工具的技术细节，会出现一些意料之外的问题。

典型的情况是，如果远程进程内的枚举定义和本地的不一致（一般发生在部署过程中），而本地收到了远程发来的新的枚举值，则在反序列化中会出现枚举值
未定义的异常。枚举的固定取值范围既是它的优点，也是它的缺点。

因此，从兼容性角度考虑，网关不推荐使用枚举类型的字段，而是直接使用字符串类型，为了弥补字符串类型的弱类型问题，网关提供了枚举注解的机制，
以方便在使用字符串字段的情况下，依然可以提供强类型的文档和SDK。

即，将字段类型声明为字符串类型，同时在字段上加上`@EnumDef`注解，将该字段与一个枚举定义关联起来。字段类型可以是字符串类型或者字符串的列表类型。

例如：

```java
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("商品状态")
    @EnumDef(ProductStatus.class)
    public String status;
}

public enum ProductStatus {
    @Description("on-sale")
    onSale,
    @Description("off-sale")
    offSale
}
```

被@EnumDef引用的类型必须是一个枚举类型，并且枚举类型中的每个枚举值都需要有`@Description`注解。

### 2.3.3 自定义实体命名空间

网关在生成强类型的SDK（例如java/oc）时，会将API所引用的所有实体，包括直接或者间接引用的自定义返回值类型、自定义参数类型，全部生成一遍。为了
防止生成的类型出现命名冲突，目前网关的做法是将这个API所属的组名（`@ApiGroup`）作为生成类的类名前缀。

例如，假设我们有如下实体：

```java
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("商品编号")
    public long id;
    @Description("商品名称")
    public String name;
}
```

这个实体在被product组下的`product.getProductInfo`的返回值引用时，网关生成的类名为：`API_PRODUCT_ProductInfo`。其中 API_ 为固定前缀，
PRODUCT 由API的组名决定。

这种工作模式的一个问题是，如果同一个实体被多个不同命名空间下的API返回值引用，则会生成多个同字段列表但是类名前缀不同的类，生成的SDK产生产生
了很大的类冗余，为前端开发增加类复杂度。

为此，网关引入了实体命名空间的概念。即，你可以直接将一个自定义实体声明到某个实体组下，被声明的实体生成的SDK类的前缀实体所在的组名决定，
而不是之前的API组。这种声明方式可以有效减少SDK的类冗余问题。

从设计上说，网关应该强制所有实体类声明实体命名空间，或者给出默认实体命名空间的规则，而不是使用被使用的API的组名，这属于设计遗留问题。

为了纠正这种设计问题，同时又不影响已有代码，网关采用了保守的做法：

* 建议所有实体类声明实体命名空间，但非强制要求
* 没有声明实体命名空间的实体类默认仍然采用被引用类的API组名作为其命名空间

如果按照上述规则，我们可能会遇到一种特殊情况：当一个实体被多个命名空间下的API引用，并且这个实体引用了另外一个实体作为字段类型时，如果这个
实体声明了实体命名空间，而其字段的实体类型没有声明，在这种情况下，字段类型会生成多份不同前缀的类，而这个实体只生成一份，出现矛盾。

为了避免这种问题，网关要求所有声明了实体命名空间的实体所引用的其他实体类型必须也要声明实体空间。网关会对这个规则进行强校验，并在校验失败
时阻止网关启动。

使用`@EntityGroup`声明实体类的命名空间。例如：

```java
@EntityGroup("product")
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("商品编号")
    public long id;
    @Description("商品名称")
    public String name;
}
```

### 2.3.4 自定义实体类的传输

自定义实体类或者其数组、集合类型在传输的过程时需要进行序列化：

1. 对于null，则返回null
2. 对于字符串类型（String），则直接返回原始值
3. 对于其他类型，使用JSON进行序列化

## 2.4 错误代码介绍

网关采用整数表示API的执行情况，0为成功，非0为执行出现错误。客户端在收到网关返回值后应首先判断错误代码
（字段stat.code和stat.stateList*.code），只有在错误代码为0时，返回值才有意义。

### 2.4.1 请求错误代码和API错误代码

读到这里，读者可能会有疑惑，为何除了每个API的错误代码之外，还有一个总的stat.code错误代码?

这是因为网关处理请求分为两个阶段：

1. 对请求参数进行分析，确定需要执行哪些API，以及执行顺序，即编排API执行工作流。
   在这个阶段，网关会进行所有的身份认证（验证token，以确定用户身份）和权限验证（用户是否可以访问特定的API）
   如果其中任何一个步骤遇到错误，或者校验不通过，都会影响stat.code这个返回值字段，并终止进一步的执行。
2. 如果上一步骤没有遇到问题，则网关会按照编排的顺序执行API。每个API的执行结果都只影响这个API对应的stat.stateList.code。

我们把第一阶段产生的错误代码称为请求错误代码，把第二阶段产生的错误代码称为API错误代码。

### 2.4.2 错误代码隐藏

在一些情况下，网关或者API提供方不愿意向客户端暴露过于细节的错误代码，但是又想记录错误代码记录，以方便后期排查问题。
针对这种场景，网关给出了错误代码隐藏功能：每个错误代码都可以定义一个专用于返回给前端的错误代码，网关在遇到这种错误代码时，
会将原始错误代码打印到日志中，但将其指定的展示错误代码返回给前端，通过这种方式可以向前端隐藏具体的错误细节。

错误代码(`ErrorCode`)包含以下三个字段：

* int code 具体代码，正整数或者负整数
* String desc 错误描述，可为null
* ErrorCode display 用于展示给前端的错误代码，默认为其本身

如果一个错误代码e满足`e.display == e`，则我们称e为向前端暴露的错误代码。另外，对于任意错误代码`e`，`e.display`都向前端暴露。

### 2.4.3 业务错误代码和通用错误代码

我们把具有实际业务意义的错误代码叫作业务错误代码，或业务异常。例如登录时遇到的用户名密码错误等。业务错误代码均为正整数。
同样，如果一个错误和具体的业务无关，而是网关本身或框架层提供的错误代码，则称为通用错误代码。通用错误代码均为负整数。
有些API在执行的时候可能报告了一个业务错误，但是由于安全性或其他方面考虑，不适合直接展示给前端，网关会把这个业务错误代码
转换为一个预定义的通用错误代码，提示前端这次API执行不成功，原因未知。

### 2.4.4 通用错误代码

网关预定义的通用错误代码如下（摘录）：

| 错误代码 | 含义 | 出现原因 | 客户端处理逻辑 |
|:---|:---|:---|:---|
| -100 | 服务端返回未知错误 | API执行出现了未知错误、反序列化出错、网络通信错误、API调用超时等 | |
| -120 | mt参数服务端无法识别 | _mt中发现了网关不识别的API | |
| -140 | 参数错误 | 提供给API的参数不满足API要求的格式，例如不能为空 | |
| -160 | 访问被拒绝 | 普通用户访问了只允许内部用户访问的API，或者从一个不受信任的IP访问或缺少utk |  |
| -166 | 访问被风控拒绝 | 当前请求的账号、设备、手机号或者IP地址被风控拉黑 | |
| -180 | 签名错误 | 客户端提供的签名和网关计算的签名不一致，原因可能是utk/ntk中的key和deviceSecret不一致 | 清理utk和ntk并重新登录 |
| -181 | 签名错误 | 客户端提供的签名和网关计算的签名不一致，原因可能是dtk中的key和deviceSecret不一致 | 清理_did/dtk/utk/ntk/deviceSecret，重新生成did，注册设备并重新登录 |
| -182 | 签名错误 | 客户端提供的签名和网关计算的签名不一致，原因未知，可能是客户端bug（例如权限级别和服务端不一致、undefined/null值的处理问题和java不一致、静态盐不匹配等 |  |
| -190 | 非法的请求组合 | 客户端在请求**RAW String**的API时又请求了其他API | |
| -200 | 请求解析错误 | _mt为空，客户端不请求任何API | |
| -220 | 客户端版本过久 | 客户端版本过久 | 提示升级客户端版本 |
| -360 | 用户token错误 | 客户端未传token或网关无法解析token、或者token过期，但请求的API需要用户凭据 | 重新登录 |
| -361 | 设备token错误 | 客户端未传token或网关无法解析token，但请求的API需要设备凭据 | 清理did/dtk并重新注册设备 |
| -362 | 扩展token错误 | 客户端未传etk或所传的etk与utk/dtk不一致，或etk已过期 | 清理etk并重新申请etk |
| -363 | 未认证类用户token错误 | 客户端未传ntk/utk或网关无法解析token、或者token过期，但请求的API需要未认证类或以上用户凭据 | 提醒用户授权并调用API获取ntk |
| -400 | 权限不足 | 当前用户试图访问一个没有权限的API（权限验证，和360/361的身份认证区分开） | |
| -444 | 需要验证码 | 当前用户的设备编号、用户名、所属在的IP或者手机号段处于需要验证码的名单列表中，客户端必须要求用户输入验证码，并通过风控系统解除验证码 | 显示滑块验证码 |

> 上表罗列的所有错误代码均向前端展示，这意味着前端必须面对这些通用错误。

### 2.4.5 业务错误代码

业务错误代码只由API返回，不会出现在网关返回值的`stat.code`字段中。业务错误代码由API提供方定义，具有一定的业务语义。
下一章节我们会详细介绍如何定义业务错误代码。

## 2.5 定义业务错误代码

业务错误代码，或业务异常，由API的提供方定义，并由API显式声明它会返回的所有业务错误代码。如果API返回了一个它未显式声明的错误代码，
网关会将其转换为`-100`（服务端返回未知错误）。

### 2.5.1 定义业务错误代码

业务错误代码通过一个java类的静态字段来定义，并在注解@ApiGroup中引用。注解@ApiGroup中的三个字段与错误代码定义有关：

* minCode
* maxCode
* codeDefine

> @ApiGroup 定义在包含API（方法）的类或者接口上，表示这个类中所有定义的API都隶属于一个组，并具备@ApiGroup定义的共同属性。

其中`minCode`和`maxCode`字段界定了这组API可能使用的错误代码的最大范围。网关会对所有的错误代码返回进行重叠检查，以确保不会有两组不同的
API碰巧使用了相同的错误代码。因此，可以使用minCode和maxCode预定一个范围内的所有错误代码。

`codeDefine`字段用于引用一个定义了错误代码的java类，组内所有API都只能使用codeDefine引用的类中定义的错误代码。
网关会扫描这个类中所有满足下列条件的字段，并将其解释为错误代码定义：

* 由当前类定义，不包含继承自父类的字段
* 静态字段
* 只读（由const修饰）
* 字段类型是`AbstractReturnCode`或其子类

网关会检查codeDefine中定义的错误代码是否落入minCode（包含）和maxCode（不包含）范围内，并且在超过范围时报告非法API定义错误。

援引本文开始时的例子：

```java
// 声明API返回的错误代码类
class ReturnCode extends AbstractReturnCode {
    protected ReturnCode(String desc, int code) {
        super(desc, code);
    }

    // 声明实际可能抛出的业务异常代码
    public static final int _C_PRODUCT_NOT_FOUND = 1001;
    public static final ReturnCode PRODUCT_NOT_FOUND = new ReturnCode("商品不存在", _C_PRODUCT_NOT_FOUND);
}
```

### 2.5.2 显式声明业务错误代码 - Designed Error Code

API需要显式声明它所能声明的业务错误代码，如果API在实际执行的时候返回了一个没有显式声明的错误代码，
网关会将其替换为`-100`（服务端返回未知错误），但是会通过日志记录它的原始代码。

强制显式声明错误代码的这种设计，主要目的是为了能够在API文档生成的时候清楚界定这个API能够返回哪些错误代码，
而不用深入阅读实现代码，以求提高前后端协作开发的效率。这种设计类似于java语言中的检查异常（checked exception），
由被调用显式声明它可能会抛出的异常类型列表，主调用方能够明确确定是否所有异常都已被处理。而网关中的通用异常则类似于java语言中
的非检查异常，随时可能会抛出，和具体函数无关。

如果API的一种异常行为需要让前端感知到，则这种异常行为应该显式声明业务错误代码。如果业务错误代码仅仅为了监控报警，而前端没有这种错误代码的
处理逻辑，则不应该显式声明。

我们总结一下一般API实践中关于错误代码的推荐做法：

* 关于是否显式声明业务错误代码，即 `Designed Error Code`
  
  从前端的角度考虑，如果前端对于这种异常需要有自己的处理方法，例如显式特定的信息给用户，或执行某个动作等，则需要声明为`Designed Error Code`。

* 关于是否使用独立的业务错误代码

  API的执行代码在遇到导致其无法达到预期的状况时（异常），应该向客户端报告一个特定的错误代码。如果前端需要处理这种特定的异常，则应声明为
  `Designed Error Code`，因此需要一个特定的code。
  
  如果前端不需要区分这种异常情况，但是需要进一步的跟踪和监控时，则同样需要将其声明为独立的业务错误代码，但不显式声明为
  `Designed Error Code`。
  
  如果你无法界定这种错误，或者这种错误不需要特殊的监控报警机制，则可直接抛出为`-100`。
  
  不推荐微服务级别的类通用错误代码，例如`通用商品服务错误`或`通用订单错误`这种含糊冗余的业务错误代码，他们所包含的使用场景都可以细化到具体
  的错误代码上，或者泛化到`-100`这种全局通用错误上。

通过将`@DesignedErrorCode`注解到一个API方法上声明这个API能够返回的业务错误代码列表。`@DesignedErrorCode`只有一个字段`value`，
类型为整型数组，在其中包含所有这个API需要暴露给前端的业务异常代码（整数格式）。同样，我们援引本文开始时的例子：

```java
@ApiGroup(name = "product", minCode = 1000, maxCode = 2000, codeDefine = ProductService.RC.class, owner = "nick")
interface ProductService {
// 声明API
    @HttpApi(name = "product.getProduct", desc = "获取商品信息", security = SecurityType.Anonym, owner = "nick")
    @DesignedErrorCode({ReturnCode._C_PRODUCT_NOT_FOUND}) // 声明可能抛出的业务错误代码
    ProductInfo geProduct(
            @ApiParameter(name = "id", required = true, desc = "商品编号") int id
    );
}
```

### 2.5.3 业务错误代码映射

在微服务架构中，一个API的实现可能需要另外一个项目的API的支持，由于不同组的API不能声明重复错误代码，所以如果被调用的API返回了业务异常，
则主调用的API必须捕获这个业务异常，并把它转换成自己的业务异常，否则会被网关认定返回非法错误代码（并被转换为-100）。
考虑到这种场景普遍存在，并且实现代码也很固定，所以网关提供了原生的业务错误代码映射功能，即通过声明的方式让网关自动进行错误代码转换。

使用`@ErrorCodeMapping`注解声明错误代码映射，注解在需要代码映射的API方法上。`@ErrorCodeMapping`包含`mapping`, `mapping1`, `mapping2` ... `mapping19`共20个字段，字段类型均为int[]，各字段的意义相同。数组的长度必须为偶数个，数组元素按顺序连续两个元素为一组，
每组第一个元素为主调API的错误代码，第二个元素为被调API的错误代码。所以每个字段指向的数组长度必须为偶数个，其中第基数个元素属于当前API的
错误代码，必须由`@DesignedErrorCode`声明。

例如：

```java
@ApiGroup(name = "product", minCode = 1000, maxCode = 2000, codeDefine = ProductService.RC.class, owner = "nick")
interface ProductService {
// 声明API
    @HttpApi(name = "product.getProduct", desc = "获取商品信息", security = SecurityType.Anonym, owner = "nick")
    @DesignedErrorCode({ReturnCode._C_PRODUCT_NOT_FOUND}) // 声明可能抛出的业务错误代码
    @ErrorCodeMapping(mapping = {ReturnCode._C_PRODUCT_NOT_FOUND, 3001}) // 3001为其他系统定义的业务错误代码
    ProductInfo geProduct(
            @ApiParameter(name = "id", required = true, desc = "商品编号") int id
    );
}
```

## 2.6 API参数

绝大多数API在执行的时候，像普通java函数一样，需要一些输入参数。例如，用于登录的API需要用户名和密码。一般情况下，API的参数是由API
定义，并由前端直接提供，但是在某些情况下，客户端提供不了或由于安全原因不能提供这些参数，或者参数与具体API无关，这些参数称为自动注入参数，
由网关提供。例如当前用户的用户编号，或者客户端所在机器的IP地址。这种类型的参数称为自动注入参数。

### 2.6.1 声明API参数

将`@ApiParameter`, `@ApiAutowired`, `@ApiCookieAutowired`或`@ExtensionAutowired`四个注解中的一个放置到由@HttpApi标注的API函数
的参数上使这个参数成为API参数。API的函数的所有参数均应被注解，即API函数的所有参数都必须使API参数。除`@ApiParameter`外，其他三种注解声明
的参数均为自动注入的通用参数。

### 2.6.2 @ApiParameter方式声明

声明一个非通用参数，参数服务于正在声明的API，参数可以是以下类型：

* 基元类型（`boolean`, `byte`, `char`, `short`, `int`, `long`, `float`, `double`）以及数组类型
* 字符串（`String`）以及数组类型
* 日期类型（`Date`）以及数组类型
* 十进制类型（`BigDecimal`）以及其数组类型
* 枚举类型及其数组类型
* 自定义实体类型
* 以上所有类型的集合类型（`Collection`或`List`）

> ！！！不支持基元类型的包装类型，例如 Integer, Long, Double等

`@ApiParameter`提供了丰富的字段，允许你定义API的参数的很多特性：

* name 参数名称，不可缺省
  
  这里面设定的参数名称可以和它所要修饰的Java参数名称不一致。网关使用该名称生成文档和API SDK，并且前端和网关通信时使用的也是这个名称。
  参数名必须为合法的C语言标识符（以字母或下划线开头，只能包含下划线、字母或数字），并且不能使用网关的保留字：
  
  `params        e`
  
* required 指示该参数是否为必要参数

  网关在执行每个API时会对API的参数做合法性校验，包括是否为空，是否符合给定的正则表达式等，并且在校验失败后把-140（参数错误）
  作为API的错误代码，不影响其他API的执行。
  
* desc 参数注释

  参数注释会显示到生成的文档中，并且会作为代码注释出现在API SDK代码中，作为被修饰参数的代码注释。良好的注释有利于前端更好理解参数的含义，
  利于提高开发效率，减少沟通成本，所以需要后端清楚合理地设置这个字段的值。
  
* rsaEncrypted 是否为rsa加密参数

  网关支持RSA加密参数，如果启用加密，网关会使用预定义的RSA密钥对该参数进行解密，并将解密结果作为API的实际参数。
  请注意，如果开启这个选项，前端需要知道和网关配对的RSA公钥，并且使用该公钥对参数的原始值进行加密，并将加密结果作为参数的最终值
  提供给网关。
  
* verifyRegex 如果参数的实际值是一个满足特定规则的字符串类型，可以将规则的正则表达式形式设置为此字段，网关在执行API调用的时候会
  进行正则匹配，和required一样，如果匹配失败，会返回-140（参数错误），并将verifyMsg字段设置的值返回给前端
  
* verifyMsg 正则匹配失败后返回给前端的信息

* ignoreForSecurity 是否在网关打印访问日志时忽略该参数的实际值

  默认情况下网关在打印访问日志时，会将所有参数的实际值打印出来，但考虑到安全性，某些敏感信息不适宜出现在日志系统中。
  如果你有不适宜打印的参数，请开启该选项。
  
* enumDef 参数的可选值
  
  一些字符串类型的参数用于明确的取值范围，例如性别等。网关可以做这种合法取值校验，如果发现该参数没有出现在enumbDef的可选值范围内，
  则返回给前端-140（参数错误）。`enumDef`需要指向一个定义了所有可能取值的枚举类，网关会将该枚举的所有成员设置为参数的可选值空间。
  为什么不直接使用枚举类型作为参数类型，而是使用字符串类型，然后再设置枚举呢？从表面上看，两者都能达到相同的效果，而且前者更加方便。
  主要问题是，在枚举类的成员发生变化的时候，直接使用枚举对象可能会遇到反序列化时的兼容性问题。具体表现是，如果一个新增的枚举值被序列化
  并传输到网关，网关在反序列化时由于无法找到该枚举值而反序列化失败。
  
* serviceInject 参数的自动注入器

  在API依赖调用的时候，可能会被依赖的API会将某个值注入给主依赖API的某个特定参数，即API参数依赖。后续章节会详细介绍用法和原理。
  
我们继续援引本文开头的例子，作为`@ApiParameter`的一个用法实践：

```java
// ...
interface ProductService {
// 声明API
    // ...
    ProductInfo geProduct(
            @ApiParameter(name = "id", required = true, desc = "商品编号") int id
    );
}
```

上述例子定义了一个名为"id"的必填API参数。

### 2.6.3 @ApiAutowired方式声明

使用`@ApiAutowired`声明一个API参数，使之成为能够自动注入网关通用参数的参数。所有通用参数都是可注入的参数，参考`$2.2.1 请求规范`。
某些可注入不属于通用参数，前端不能直接提供这个参数。以下罗列了部分网关支持的注入参数列表：

| 参数名 | 含义 | 来源 |
|:---|:---|:---|
| _tk | user token 代表访问者身份,完成用户登入流程后获取 | 来自前端传入 |
| _dtk | device token 代表访问设备的身份,完成设备注册流程后获取 | 来自前端传入 |
| _etk | extension token 代表子系统给该用户的授权身份 | 来自前端传入 |
| _aid | application id 应用编号 | 来自前端传入 |
| _bid | business id 业务流水号, 用于做幂等判断 | 来自前端传入 |
| _did | device id 设备标示符 | 从token中解析，如果token不存在，则接受前端传入 |
| _uid | user id 用户标示符 | 从token中解析 |
| _cip | client ip 用户ip | HTTP Request HEADER |
| _vn | version 客户端版本名 | 来自前端传入 |
| _host | 当前站点host | HTTP Request HEADER |
| _ts | request 时间戳 | 网关产生 |

`@ApiAutowired`包含两个字段：

* value 要注入的通用参数名称

  可以选择上表中的一个参数填入（引用`CommonParameter`使用这些通用参数的名称），也可以使用一个网关未定义的通用参数。
  如果网关没有找到该通用参数，或客户端没有提供这个参数，则该参数的实际值为null。
  
* serviceInject 参数的自动注入器

  在API依赖调用的时候，可能会被依赖的API会将某个值注入给主依赖API的某个特定自动注入的参数，即API参数依赖。后续章节会详细介绍用法和原理。

### 2.6.4 @ApiCookieAutowired方式声明

使用`@ApiCookieAutowired`声明一个API参数，使之能够获得多个来自客户端的cookie值，参数必须为`Map<String, String>`类型。
`@ApiCookieAutowired`中只有一个字段（value），表示所有你感兴趣的cookie。网关会把已经存在的value字段中列举的所有cookie名和其对应的值
填充到一个map中，并将map作为最终参数值传递给API。value是一个字符串数组类型，不能为空。

考虑到网关所支持的某些客户端平台不支持cookie，我们采用请求参数替代方案，即每个cookie都可以通过请求参数传递。如果cookie和请求参数同时存在，
则请求参数覆盖cookie。针对`@ApiCookieAutowired.value`中列举的每个感兴趣的cookie名称n，网关使用以下方式确定cookie的值：

1. 如果n以下划线开头，则查找请求中是否存在名为n的参数，否则查找请求中是否存在名为下划线加n（"_" + n）的参数
2. 如果步骤1没有找到该参数，则查找请求中的cookie中是否存在名称为n的cookie
3. 如果步骤1, 2都找不到该参数，则忽略该cookie项。否则最终的map中会出现该cookie的名称和对应值。

### 2.6.5 @ExtensionAutowired方式声明

网关使用token表示用户登录凭据。用户在登录时会获取这个凭据，并在每次请求非匿名API时携带该凭据（通过cookie或者请求参数的方式），
网关会对携带的凭据（token）做校验，并且在必要的时候拒绝本次请求。从某个角度看，可以把token理解为一个保存在客户端的大规模分布式存储，
token中保存的信息代表了当前用户的一些信息。在某些时候，类似于网站设计的会话（session），其他的一些附加信息仍然可以使用类似的存储方案。
考虑到用户凭据的纯粹性，token中只保存了用户身份的相关信息，例如用户名、所属角色等。因此这种附加信息不适宜存放到token中。

考虑到上述情况，网关引入了扩展凭据概念，即`extension token`，用于在客户端保存非登录相关的附加信息。扩展凭据由业务系统签发，由前端保存，
并在每次请求时携带。扩展凭据中包含了业务系统签发时的放入的一组键值对，`@ExtensionAutowired`的`value`字段可以指定需要提取其中的值。

### 2.6.6 API参数的兼容性修改

由于DUBBO的远程调用的机制问题，对API参数的修改都会出现不兼容问题。在本章节中我们讨论这种不兼容产生的原因以及应对方案。

#### 2.6.6.1 DUBBO远程方法调用机制

DUBBO是一个分布式RPC调用框架，它以远程方法为单元，称为`服务`，集群中的每个节点都包含若干个这种单元（横向扩展），称为`提供服务`，不同节点
也可以包含相同的单元（冗余节点）。

DUBBO使用中心注册中心作为其管理集群的主要手段，每个节点都将其提供的的服务写入到注册中心。DUBBO的客户端会根据注册中心的数据去决定它需要调用
的服务由哪个节点提供，并直接向那个节点发起调用请求。

DUBBO的每个节点以方法为单位，将它所提供的所有方法写入到注册中心。多个重载的同名方法由于方法名相同，在注册中心中只会出现一次。

DUBBO客户端会缓存它需要调用的方法名及其签名（可能包含多个重载版本），称之为`客户端本地方法签名缓存`。DUBBO在发起调用时根据方法名确定这个
方法的远程提供方，然后将方法名和实参列表发送给这个提供方。

远程服务在收到方法执行的请求时，会根据方法名和实参类型确定该方法的具体重载版本，如果满足这种实参类型的重载版本找不到，则报告
`MethodNotFound`。

由此可见，方法重载（签名）不参与调度，只在服务端内部起作用。

#### 2.6.6.2 DUBBO方法签名的兼容性问题

由于网关对DUBBO集群的调用依赖于DUBBO框架，所以DUBBO方法签名的兼容性问题同样对网关有影响。下面我们就探讨一下DUBBO的方法签名兼容性问题。

远程方法的签名发生变动之后，由于DUBBO客户端本地方法签名的缓存没有更新，导致客户端在调用这个方法时所提供的实参类型和实际方法不兼容，进而
导致调用失败。DUBBO客户端随后进行部署，并更新本缓存，提供新的实际参数，并结束不兼容过程。我们称从远程签名变动到所有客户端缓存更新完毕这个时间段
称为`方法签名更新的不兼容窗口期`。发版时间越久，窗口期就越大，影响就越深远。

#### 2.6.6.3 DUBBO方法签名的兼容修改方案

对于DUBBO方法签名的修改会导致不兼容问题，因此为了避免不兼容问题，我们采用重载来进行方法签名修改：

如果需要对一个方法的参数列表进行修改，例如增减参数、修改参数类型、调整参数顺序等，应该为这个方法新增一个重载版本，并在这个重载版本上进行你的
修改。保留你原来的重载版本直至下次发版。

设存在一个DUBBO远程方法`m(s1)`，其中方法名为`m`，方法签名（参数类型、数量、顺序）为`s1`。我们看如下流程：

1. 服务端提供`m(s1)`
2. 客户端的方法签名缓存为`m(s1)`
3. 服务端增加`m(s2)`，并同时提供`m(s1)`和`m(s2)`
4. 客户端使用`m(s1)`对`m`发起调用
5. 服务端确认此次调用客户端使用的签名为`s1`，因此执行`m(s1)`
6. 客户端在升级之后拿到了`m(s2)`
7. 客户端使用`m(s2)`对`m`发起调用
8. 服务端确认此次调用客户端使用的签名为`s2`，因此执行`m(s2)`

由此可见，同时使用新增方法重载版本这种方式，可以在客户端升级前后保证兼容性，代价是增加了代码冗余，这种冗余可以在后续发版中删除。

#### 2.6.6.4 API参数的兼容修改方案

从网关与DUBBO集群的角度看，API对应于DUBBO远程方法，而API参数对应于DUBBO远程方法的参数，对于API参数的调整意味着需要对DUBBO远程方法的签名
进行调整。DUBBO客户端本地方法签名缓存即网关本地方法签名缓存。升级DUBBO客户端即网关重新加载API JAR。

我们采用同样的方案来解决API参数修改的兼容性问题。

由于网关与前端采用了HTTP协议，所以对于前端而言，API参数的变动不全是不兼容修改，例如：

* 参数类型从一种基本类型转变为另外一种基本类型，比如整型到浮点型，字符串到整型等
* 删除参数
* 新增一个可选参数
* 参数顺序变动（因为客户端不关心参数顺序）

上述修改对客户端而言是兼容修改，但是对DUBBO来说并不是，但是我们可以使用上节中使用的新增方法重载的办法来达到兼容修改，从而做到前后端兼容
修改。由于API解析的问题，我们需要将网关所关心的注解从原来的方法版本上移到新的版本上。

设存在一个API `a@m(s1)`，其中`a`为API的名字，`m`为`a`所对应的方法，`s1`为m的方法签名（参数类型、数量、顺序）。我们看如下流程：

1. 服务端提供`m(s1)`，并将API注解放置于`m(s1)`上，即`a@m(s1)`
2. 网关中的API映射关系为`a@m(s1)`
3. 服务端增加`a@m(s2)`，API相关注解从`m(s1)`迁移到`m(s2)`，同时提供`m(s1)`和`a@m(s2)`
4. 由于网关中的API映射关系为`a@m(s1)`，网关在执行`a`时，使用`m(s1)`对`m`发起调用
5. 服务端确认此次调用客户端使用的签名为`s1`，因此执行`m(s1)`
6. 网关在重新加载API后获得了API新的映射关系`a@m(s2)`
7. 网关在执行`a`时使用`m(s2)`对`m`发起调用
8. 服务端确认此次调用客户端使用的签名为`s2`，因此执行`m(s2)`

> 注意，服务端在提供新的重载版本时，应将网关相关注解从老版本中移除，以防止对网关解析产生干扰

#### 2.6.6.5 API参数兼容性修改举例

我们举一个例子来作进一步阐释如何做到API参数的兼容性修改：

设我们有个提供价格的API：

```java
interface PriceService {

    @HttpApi(name = "price.getPrice", desc = "获取价格信息", security = SecurityType.Anonym, owner = "nick")
    PriceInfo getPrice(
        @ApiParameter(name = "productId", desc = "product Id", required = true) int productId
    );
}
```

它只有一个参数`productId`。客户端使用如下参数方式调用：

```text
_mt: "price.getPrice"
productId: 1
```

后续有新的需求，需要在获取价格的时候判断当前折扣，而折扣属于通用参数，可以自动注入，因此不是一个必填参数。根据上文所讨论的方案，从理论上
说我们有机会在原有API基础上满足这个需求，而不用新增一个API。按照兼容升级方案，我们增加一个新的`getPrice`版本，并将注解迁移到新的版本上：

```java
interface PriceService {

    PriceInfo getPrice(
        int productId
    );

    @HttpApi(name = "price.getPrice", desc = "获取价格信息", security = SecurityType.Anonym, owner = "nick")
    PriceInfo getPrice(
        @ApiParameter(name = "productId", desc = "product Id", required = true) int productId,
        @ApiAutoWired("_dlv") double dlv
    );
}
```

> 保持老版本的方法签名不变，并将其所有的API相关的注解移到新版本的方法重载上

上述改动可以在网关加载API前后对DUBBO集群保持兼容，并始终对前端保持兼容。前端如果目前没有提供`_dlv`通用参数，可以后续升级代码提供。
在上述代码改动后，如果前端没有提供`_dlv`，则网关会在执行`price_getPrice`时为`dlv`参数传递`0`。

## 2.7 API依赖与返回值整合

微服务框架的主要目的是将系统中的不同领域的业务隔离开来，将核心业务和非核心业务隔离开来。而API作为微服务的对外接口，随着业务的分隔粒度进行
分隔。每个API有自己特定的义务含义，需要有很高的内聚，功能应该足够单一。而作为API的使用方，前端则需要API大而全，以满足他们的渲染需求，
微服务的API设计与前端的需求产生了天然矛盾。这意味着单一的API往往不能满足前端的需求，因此前端会经常将多个API的返回结果进行组合再加工，
以使用他们的渲染需求。

客户端组合多API调用有如下问题：

* 由于客户端跨越多个平台，这种API整合代码需要在多个平台上实现，增加了开发成本。
* 对于有依赖关系的API，即一个API的返回值决定了另外一个API的参数，客户端多API调用不仅复杂，而且涉及到多次http调用，有性能上的问题。
* API与其他API的依赖关系是API的固有特征，这种特征应该是API的开发者去提供和维护，而不是客户端。

当然，从技术上说，API的开发者可以将他的API打造成大而全的API，全面提供客户端所需的所有数据。这种设计方式有如下问题：

* 大而全的API这种设计方式违反了API的功能单一原则。
* 如果一个API需要返回另外一个API的返回结果，则需要将那个API的返回值类型的定义拷贝到自己的API项目中，并且在对方更新定义时同步更新自己的
  定义。这种方式增加了维护成本，也提高了由于未及时更新导致的风险。
  
  如果需要访问被依赖的API返回值字段，这种拷贝定义的方式也可以理解。但是如果只是存粹将别人的返回值嵌入到自己的返回值中，那么这种高成本的
  工作就值得商榷了。
  
* 微服务之间的依赖关系应该是清楚明确的，并且应该在合理的条件下，尽可能减少这种依赖关系。简洁清楚的服务依赖关系有利于微服务集群的治理和维护。
  由于返回值整合的原因让微服务之间的关系变得复杂，进而导致治理和维护难度的提升，是得不偿失的。
  
* 从服务降级的角度考虑，核心服务原则上不应依赖非核心服务。但是核心服务的API可能会返回非核心服务的数据结构，这也是大而全的API的一个问题。

鉴于以上问题，网关先后提出了如下四种解决方案：

1. API组合调用

   客户端可以在一次http请求中指定多个API，每个API独立执行，不相互依赖。上文对此已有表述。
2. API组合调用 + API依赖

   客户端在一次http请求中指定多个API，并且指定这些API之间的依赖关系。网关会梳理出`API执行工作流`，并按照依赖关系进行执行。
   被依赖的API可以向主依赖的API提供它所需要的参数，即`参数注入契约`。

   从设计上说，API之间的依赖关系是API的固有特征。一个API可以被谁依赖，或者依赖谁，在运行阶段是稳定的，并且这种依赖关系反映在文档和SDK中。
   前端在调用多个有依赖关系的API时，可以选择使用或者忽略这些依赖。
3. 返回值注入

   一个API的返回值可以混合其他一个或者多个API的返回值，这种返回值混合关系由后端声明，依赖细节对前端透明。网关提供完整的语法机制，使得API
   可以在不修改API实现代码的情况下，将其他某个API的返回值混合到自己的返回值中，这种组合方式称为`返回值注入`。

   同样，返回值注入也是API的固有特征。在前端看来，声明返回值注入的API像是真的是自己提供了这些额外注入的数据。注入的依赖关系也反映在文档和
   SDK中。
4. API Mixer

   相比于以上各种API组合方案，API Mixer是最彻底的做法。它是通过声明一个特殊的API，主动将多种API的返回值混合到一起。对于前端而言，它是一个
   为页面量身定做的API，能根据前端的口味和风格进行定制，既能减少多API带来的性能开销，又减少了多平台导致的开发成本。对于后端而言，它是多个
   功能单一的API的粘合剂，解决了API纯粹性的特征与前端页面渲染的天然矛盾。

   API Mixer可以混合多个API的返回值，也可以根据业务需求，在特定条件下选择性地整合其他可选API的返回值，以做到彻底的API整合。

   API Mixer的引入使得网关所提供的API分成了两层，第一层是原始的API，功能单一，根据微服务业务隔离开。第二层是Mixer层，将多个原始API进行
   加工、整合、转换，并最终向前端提供一站式的数据结构，使前端从繁琐的数据整合工作中解放出来。

由简到难、由基本到彻底，网关提供了多个层次的解决方案供你选择。后续章节将会对上面四种解决方案的概念、用法及注意事项作详细描述。具体选择哪款
方案，你需要结合具体业务场景和实现成本作综合考虑。

## 2.8 参数注入契约

在一些业务场景下，某个API执行时所需要的参数可能与另外一个API的执行结果有关，而这个API的执行可能又决定了第三个API的参数，
由此组成了API执行链路。客户端可以自主组合实现这种链式API调用，但是由于客户端平台较多，实现起来所要达到的目标比较明确。而且，
在一般情况下，这些API的链式调用组合具有很明显的业务意义。所以网关将这种API组合集成进来，由网关提供组合的实现逻辑，并对前端返回最后的
执行结果。

针对这种需求，网关所采用的设计方案是：

* 链式调用的被依赖方API在声明参数时，指定哪些参数可以接受其他API的注入（`import parameters`）
* 链式调用的依赖方API在声明API时，指定它可以提供哪些用于注入的参数值 （`export parameters`）
* import和export独立声明，默认不生效，除非客户端指定了两个API的执行依赖关系
* 网关在执行具有依赖关系的API时，会在执行被依赖的API时检查它所声明的`import parameters`，并从其依赖项中提取`export parameters`
  填充这些可注入参数
  
`import/export parameters`合称API依赖的参数注入契约。参数注入契约包含两个角色：

* 参数的提供方，即export parameters，用于提供一些参数的值用于注入
* 参数的接收方，即import parameters，用于接收一些参数的注入

参数注入契约使用独立的命名系统。每个参数在声明为`import parameter`的时候需要指定它的参数注入契约名，可以与该参数的名称不一致。API在声明
`export parameters`时，需要指定它的参数注入契约名称。网关使用参数的契约名称进行`export/import`配对。

### 2.8.1 声明参数注入契约

具体来说，声明参数注入契约（其中契约名n）需要参数注入契约的提供方和注入方双方配合完成：

* 参数注入契约提供方：

  1. 使用`@ExportParam(name = n)`放置于API声明所属的方法上方
  2. 使用 `ServiceInjectable.export(n, real-parameter-value)` 设置参数注入契约的实际值为 real-parameter-value
  
* 参数注入契约注入方：

  1. 使用`@ImportParam(n)`放置于接收参数注入的API的参数声明上方

> 注意：由于技术限制，我们不会校验你在声明`@ExportParam`的实现中是否真正通过 `ServiceInjectable.export` 设置了声明的参数注入契约的
>
实际值。如果你没有那么做，或者在某个情景下 `ServiceInjectable.export` 没有真正执行，或者 `ServiceInjectable.export` 中设置的
参数注入契约名和其声明的`@ExportParam`不一致，网关不会有任何额外的尝试，被注入的参数实际值保持不变。
>
> 注意：参数注入契约的提供/注入双方不需要同时上线，也没有先后上线顺序。另外，对于多API声明提供同一参数注入契约和多API声明注入同一参数
注入契约没有额外限制。网关对此的处理方式是：有则用。

参考如下示例代码（为表述清晰，已忽略其他声明代码）：

```java
// ProductService.java for export parameters
interface ProductService {
    @ExportParam(name = "product.id")
    @HttpApi(name = "product.getProduct", desc = "获取商品信息", security = SecurityType.Anonym, owner = "nick")
    ProductInfo geProduct(
        @ApiParameter(name = "id", required = true, desc = "商品编号")
        int productId
    );
}
// ProductServiceImpl.java 实现API的具体逻辑
class ProductServiceImpl implements ProductService {
    @Override
    public ProductInfo geProduct(int productId) {
        ProductInfo info = new ProductInfo();
        info.id = productId;
        info.name = "product#" + productId;
        // 将`product.id`的实际值暴露出来
        ServiceInjectable.export("product.id", productId);
        return info;
    }
}
// PriceService.java for import parameters
interface PriceService {

    @HttpApi(name = "price.getPrice", desc = "获取价格信息", security = SecurityType.Anonym, owner = "nick")
    PriceInfo gePrice(
        @ImportParam("product.id")
        @ApiParameter(name = "productId", desc = "product Id", required = true)
        int productId
    );
}
```

其中，API `product.getProduct` 声明它可以提供一个名为`product.id`的参数注入契约，API `price.getPrice`声明它可以接收一个
名为`product.id`的参数注入契约。参数注入契约的实际值为`productId`，作为API`price.getPrice`参数`productId`的实际值。

### 2.8.2 参数注入契约兼容升级

参数注入契约主要解决了API链式调用时的参数传递问题。当一个API的参数值来源于另外一个API的执行时，就可以考虑采用参数注入契约。在整个项目
开发周期中，大致有两个时间点可以引入参数注入契约：

1. API开发阶段。这个时候如果清楚该API与其他API的依赖关系，可以在此时使用参数注入契约，作为提供方或者注入方。这个时候引入参数注入契约没有
   升级风险。
2. API上线后。在其他API开发的过程中或者有新需求到来需要复用已有API时，对于API之间的依赖关系会有更清楚的理解，这个时候可能会考虑引入
   参数注入契约。由于网关会自动配对参数注入契约的提供方和注入方，可能会出现多于意料之外的注入行为。  
   所以在新增一个参数注入契约提供方时，需要考虑所有消费这个契约的接收方。在新增一个参数注入契约接收方时，需要考虑所有提供这个契约的提供方。
   确保语义正确。

### 2.8.3 参数注入的潜在问题

`ServiceInjectable.export`能够设置一个参数注入契约的值，并由网关转设给该参数注入契约的接收方。由于该设置方法出现在API的实现代码中，
后者可能会被抽象、封装、提取等面向对象编程处理，或者其他的例如面向切面编程的影响，导致`ServiceInjectable.export`语句前后多次对
同一参数注入契约设置不同的值。**后续`ServiceInjectable.export`语句会覆盖参数注入契约**，这一点需要注意，既不能让其他
`ServiceInjectable.export`覆盖你的值（多次赋值），也不能只在部分执行路径赋值（缺少赋值）。

**网关的要求是：实现API的代码必须确保正确设置了参数注入契约的值，不覆盖，也不能漏。**

### 2.8.4 参数注入契约与自动注入的关系

参数注入契约中的注入参数，指的是网关在处理API执行依赖时，能够将被依赖方所暴露的参数自动注入到接收注入的参数中。而自动注入和执行依赖无关，
其最终参数值只与本次请求的请求信息和客户端传递的参数有关。

在一些场景下，API的参数可以同时接收自动注入和参数注入契约注入，同时使用`@ImportParam和@ApiAutowired`。
在两者都可用时，以参数注入契约注入的结果为最终结果。

### 2.8.5 参数注入契约的命名规范

由于参数注入契约名是全局名称，涉及到目前API和以后的API发展，契约名需要有非常明确的业务意义。推荐按照以下方式对参数注入契约命名：

API所属系统的名称和参数的实际意义名，中间用逗号隔开。例如`product.productId`。

## 2.9 返回值过滤器

随着业务系统的发展，后端集群中的各微服务之间的依赖关系变得越来越复杂。例如在电商系统中，价格服务可能要为包括商品服务、订单服务在内的
很多微服务提供价格信息。鉴于DUBBO的强类型信息，如果价格信息增加一个字段，所有使用价格信息的服务都需要同时升级这个字段，但是由于大多数
业务系统只是将价格信息嵌入它自己的返回值中，并没有对其进行额外的操作。因此网关引入了一个通用的返回值过滤概念来解决这种返回值嵌入的问题。

从概念上说，返回值烂机器指的是，API可以向网关声明，在网关执行完它之后调用一个它预设的拦截器（在网关中执行），以允许它对返回值做进一步的修改，
比如调用其他API，并将它的返回值嵌入到自己的返回值中。

返回值过滤和在API实现中嵌入其他API的返回值的区别是，如果被嵌入的API返回值的类型发生变化，则后者需要同步更新依赖的版本号，否则按照序列化的
规则，拿到的返回值不包含新增的字段，而前者可以跟随最新API的改动而同步变动，省去了同步更新依赖版本的开销。

### 2.9.1 声明返回值过滤

返回值过滤是一个实现了接口`ResponseFilter`的类，将`@FilterResponse`注解到一个API上，并将其value字段指向一个拦截器类，从而将API
和拦截器关联到一起。一个API可以关联多个拦截器，多个拦截器之间的执行顺序和声明它的`@FilterResponse`顺序一致。

例如（省略了非关键代码）：

```java
// ProductInfo.java 声明API的返回值类型
@Description("product-info")
class ProductInfo implements Serializable {
    @Description("商品编号")
    public int id;
    @Description("商品名称")
    public String name;
}

/**
* 定义商品信息的返回值过滤
*/
class ProductInfoFilter implements ResponseFilter {
    /**
     * 将在被拦截的API返回后执行，在该函数内部提供
     * @param response      被拦截的接口的返回值
     */
    @Override
    public void filter(Object response) {
        ProductInfo productInfo = (ProductInfo) response;
        if (productInfo.name == null) {
            productInfo.name = "#none";
        }
    }
}

interface ProductService {
    // 声明API
    @HttpApi(name = "product.getProduct", desc = "获取商品信息", security = SecurityType.Anonym, owner = "nick")
    @FilterResponse(ProductInfoFilter.class) // 使用返回值过滤
    ProductInfo geProduct(
            @ApiParameter(name = "id", required = true, desc = "商品编号") int id
    );
}
```

> 以上代码声明了一个商品信息的返回值过滤，并在将其关联到API `product.getProduct`上。拦截器判断API执行后的商品名是否为null，并且在
商品名为null是设置一个默认商品名（仅作为展示用途，无业务意义）。
  
## 2.10 返回值注入

返回值过滤提出的主要目的是为了解决在一个API返回值中嵌入另外一个API返回值的问题，考虑到嵌入的复杂度，简单的返回值过滤不能很好实现这一点。
所以网关在返回值过滤的基础上进一步提出了返回值注入的概念。

返回值注入指的是，API向网关声明在它执行结束后，网关需要执行一个由它指定的API，并将执行结果注入到原API的返回值中。返回值注入涉及到如下几个
问题：

* 要注入哪个API的返回值
* 执行要注入的API时需要的参数如何获得
* 要注入的API执行结束后得到的返回值如何注入回原API的返回值中

针对第一个问题，类似于参数注入契约，我们引入了返回值注入契约概念。返回值注入契约也包含两个角色：

* 提供返回值注入的API，即返回值注入提供方
* 使用返回值注入的API，即返回值注入使用方

返回值注入契约的提供方和使用方之间通过契约名进行匹配。一个API如果需要注入另外一个API的返回值，那么第一个API就是注入的使用方，它通过契约名
声明它需要使用哪个注入提供方的注入。

针对第二个问题，网关复用参数注入契约的框架，返回值注入的提供方声明它可以接收哪些可以注入的参数，返回值注入的使用方声明它会提供哪些可注入
参数的值。

针对第三个问题，网关使用了返回值过滤的框架，因此返回值注入功能隶属于返回值过滤。

返回值注入和参数注入的主要区别是，返回值注入是纯后端行为，前端无感值，无需声明执行依赖，注入提供方的返回值会并入原API的返回值中。
而参数注入必须由前端主动声明依赖关系才可以生效，相关的两个API的返回值都会返回给前端，不发生合并。而且参数注入过程也发生在返回值注入过程中。

返回值注入的一大特点是，它具有很强的主体性，即有一个主要的API，由它定义返回给前端的返回值类型，客户端只知它的存在；以及多个辅助的API，由它
提供额外的返回值。这种方式类似于Java编程模式中的装饰者模式。

### 2.10.1 声明返回值注入

返回值注入契约有自己专用的注解和类型。

使用`@ResponseInjectProvider`声明一个返回值注入的提供方，使用`@ResponseInjectFromApi`声明需要使用哪个返回值注入提供方。通过这
两个注解来指示网关需要注入哪个API的返回值。

例如：

```java
public interface ProductService {
    @HttpApi(name = "product.getProductInfo", desc = "getProductInfo", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectFromApi("priceInfo")
    ProductInfo getProductInfo(@ApiParameter(name = "pid", required = true, desc = "pid") int pid);
}

public interface PriceService {
    @HttpApi(name = "price.getPriceInfo", desc = "getPriceInfo", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectProvider("priceInfo")
    PriceInfo getPriceInfo(@ApiParameter(name = "pid", required = true, desc = "pid") int pid);
}

```

> 上例声明了一个返回值注入契约，契约名为`priceInfo`，由`price.getPriceInfo`提供，并被`product.getProductInfo`引用。  
> 注意：返回值注入契约的契约名需要是一个名词，以表示这个契约提供的返回值的具体含义。

### 2.10.2 类型占位符 - Datum

在微服务编程模型下，API的定义根据业务分散到不同的项目中，API相关的返回值类型也随之分散开来。两个微服务的API项目不能相互引用，所以一个项目
中的实体定义不能引用另外一个项目的实体，但是实际开发中，API的返回值经常出现嵌套情况，例如API1的返回值R1中有一个字段F1需要是API2的返回值
R2的类型，但是由于API1不能直接引用R2，所以一般的做法是将R2的实体定义拷贝到API1项目中。这种做法的主要问题是，如果R2有变化，那么API1的作者
不得不去同步R2，但是却并不关心R2的字段信息。

针对这种情况，网关引入了类型占位符，即`Datum`。API2的R2提供Datum的定义，API1的R1引用Datum，网关根据注入契约等方式推导出R1引用的
Datum的实际定义。

Datum是一个`Java Interface`：

```java
/**
 * 表示一条实体数据（通过id唯一表示）
 */
interface Datum extends Serializable {
    /**
     * 获取唯一标示
     */
    long getId();
}
```

我们将所有实现Datum的类（实体）称为`Datum 定义`，例如：

```java
@Description("price info")
public static class PriceInfo implements Datum {
    @Description("product-id")
    public long productId;
    @Description("min-price")
    public int minPrice;
    @Description("max-price")
    public int maxPrice;

    /**
     * 获取唯一标示
     */
    @Override
    public long getId() {
        return productId;
    }
}
```

实体可以将字段声明成Datum类型，例如：

```java
@Description("product info")
public static class ProductInfo implements Serializable {
    @Description("product-id")
    public long productId;
    @Description("price")
    public Datum price;
}
```

网关会根据一些预定义的规则（见后文），自动推导出Datum类型的字段的实际类型，并且在无法推断时忽略该字段，推导结果体现在生成的文档和SDK中。

由此可见，占位符Datum将类型的声明和引用关联起来，避免了跨项目引用API Jar的问题。

### 2.10.3 面向Datum编程

为了方便阐述返回值注入模型，我们引入了`以Datum为中心的编程模型`，即`面向Datum编程`：

* API1的开发人员提供Datum的定义（返回值类型实现Datum接口）
* API2的开发人员使用Datum声明其字段类型，并使用特定的语法机制，将其与特定的Datum定义关联起来（即Datum类型推导）
* API2通过实现`Datum.Aware`（参见后文）声明它所感兴趣的Datum的编号
* API1根据Datum的编号，提供实际的Datum对象
* API2通过实现`Datum.Aware`（参见后文）将API1所提供的Datum对象嵌入到自己的字段中

其中涉及到四个角色，API1和API1的返回值，API2和API2的返回值，前两者提供Datum，后两者消费Datum。Datum的引入让API实体具备编号能力，
使得网关对返回值层面的管理成为可能。下一节会详细介绍Datum编程模型中的另外一个重要概念`Datum.Aware`。

> 注意：目前只支持长整型的`Datum.id`，后续考虑支持字符串类型。

### 2.10.4 将Datum对象注入到返回值中 - Datum.Aware / Datum.ListAware

网关在成功调用返回值注入的提供方API后，会将其返回值（Datum对象）注给需要返回值注入的API返回值本身。考虑到灵活性，网关采用了
`Aware / ListAware`解决如何如何注入返回值这个问题。即，需要Datum对象的返回值类型可以实现`Aware/ListAware`接口，网关会感知到这个接口，
并调用接口的`setDatum/setData`启动注入过程。

`Datum.Aware`的定义：

```java
/**
 * 实现该接口，如果你需要将单个datum注入到你到实体中
 */
interface Aware {
    /**
     * 你所感兴趣的Datum编号，只有id与getId()函数返回的id一样的Datum才会注入进来
     * @return Datum.id
     */
    long getId();

    /**
     * 将datum注册到实体中，支持多注入提供方
     * @param datumType  返回值注入的提供方
     * @param datum 要注入的数据
     */
    default void setDatum(String datumType, Datum datum) {
        setDatum(datum);
    }

    /**
     * 将datum注册到实体中
     * @param datum 要注入的数据
     */
    default void setDatum(Datum datum) {
        throw new UnsupportedOperationException();
    }
}
```

从概念上说，如果你实现了`Aware`，则意味着你需要注入一个Datum，这个Datum的编号由`Aware.getId()`指定，网关在得到这个Datum后
会调用`Aware.setDatum()`将这个Datum提供给你。

我们看如下例子：

```java
@Description("product info")
public static class ProductInfo implements Serializable, Datum.Aware {
    @Description("product-id")
    public long productId;
    @Description("price")
    public Datum price;

    /**
     * 获取唯一标示
     */
    @Override
    public long getId() {
        return productId;
    }

    /**
     * 将datum注册到实体中
     *
     * @param datum 要注入的数据
     */
    @Override
    public void setDatum(Datum datum) {
        this.price = datum;
    }
}
```

> 这里面的Datum定义是上节中的PriceInfo，网关会执行API `price.getPriceInfo`，并将得到的返回值（`PriceInfo`类型）作为参数传递给
上例中的`setDatum`方法中，setDatum方法由使用返回值注入的返回值类型提供具体实现，在其中将datum设置到正确的字段上。
  
`ListAware`则作为Aware的集合版本出现，即如果你需要将Datum集合注入到你的返回值中，则需要实现该接口。

`Datum.ListAware`的定义：

```java
/**
 * 实现该接口，如果你需要将多个datum注入到你到实体中
 */
interface ListAware {
    /**
     * 你所感兴趣的一组Datum编号，只有你指定的那些编号的Datum才会注入进来
     * @return Datum.id
     */
    long[] getIds();

    /**
     * 将datum列表注册到实体中，支持多注入提供方
     * @param datumType  返回值注入的提供方
     * @param data          准备注入的datum list
     */
    default void setData(String datumType, Collection<? extends Datum> data) {
        setData(data);
    }

    /**
     * 将datum列表注册到实体中
     * @param data 准备注入的datum list
     */
    default void setData(Collection<? extends Datum> data) {
        throw new UnsupportedOperationException();
    }
}
```

从概念上说，如果你实现了`ListAware`，则意味着你需要注入一组Datum，这组Datum的编号由`ListAware.getIds()`提供，网关在得到这组Datum后
会调用`ListAware.setData()`将这组Datum提供给你。

我们看如下例子：

```java
@Description("product info")
public class ProductInfo implements Serializable, Datum.ListAware {
    @Description("product-id")
    public int productId;
    @Description("sku-list")
    public List<SkuInfo> skuInfoList;

    @Override
    public long[] getIds() {
        long[] ids = new long[skuInfoList.size()];
        for (int i = 0; i < skuInfoList.size(); i++) {
            ids[i] = skuInfoList.get(i).skuId;
        }
        return ids;
    }

    /**
     * 将priceInfo列表注册到实体中
     *
     * @param data 准备注入的price info list
     */
    @Override
    public void setData(Collection<? extends Datum> data) {
        Map<Long, Datum> map = new HashMap<>(data.size());
        for (Datum d : data) {
            map.put(d.getId(), d);
        }
        for (SkuInfo sku : skuInfoList) {
            sku.price = map.get(sku.skuId);
        }
    }
}

@Description("sku-info")
public class SkuInfo implements Serializable {
    @Description("sku-id")
    public long skuId;
    @Description("desc")
    public String desc;
    @Description("price")
    public Datum price;
}
```

> 这里面的Datum定义是上节中的`PriceInfo`，网关会执行API `price.getPriceInfoList`，并将得到的返回值（`List<PriceInfo>`类型）
作为参数调用`setData。setData`由业务方实现，在其中完成对其或其字段中的Datum字段的赋值。由于`Datum.getId()`表示的是Datum的编号，  
所以可以根据`Datum.getId()`对多个Datum配对，如上例所示。

### 2.10.5 常用的使用场景

为了降低Datum编程的复杂度以及提高API的开发效率，这里我们列举了三种常用的使用场景：

#### 2.10.5.1 一对一
  
即API1返回值R1实现了Datum接口，API2返回值R2实现了`Datum.Aware`接口。R2指定所需的datum编号，API1根据编号生成datum对象并返回，
R2将datum赋值到自己的字段中。如下例：

```java
@Description("price info")
public static class PriceInfo implements Datum {
    @Description("product-id")
    public long productId;
    @Description("min-price")
    public int minPrice;
    @Description("max-price")
    public int maxPrice;

    /**
     * 获取唯一标示
     */
    @Override
    public long getId() {
        return productId;
    }
}

public interface PriceService {
    @HttpApi(name = "price.getPriceInfo", desc = "getPriceInfo", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectProvider("priceInfo")
    PriceInfo getPriceInfo(@InjectID @ApiParameter(name = "pid", required = true, desc = "pid") int pid);
}

@Description("product info")
public static class ProductInfo implements Serializable, Datum.Aware {
    @Description("product-id")
    public long productId;
    @Description("price")
    public Datum price;

    /**
     * 获取唯一标示
     */
    @Override
    public long getId() {
        return productId;
    }

    /**
     * 将datum注册到实体中
     *
     * @param datum 要注入的数据
     */
    @Override
    public void setDatum(Datum datum) {
        this.price = datum;
    }
}

public interface ProductService {
    @HttpApi(name = "product.getProductInfo", desc = "getProductInfo", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectFromApi("priceInfo")
    ProductInfo getProductInfo(@ApiParameter(name = "pid", required = true, desc = "pid") int pid);
}
```

> 其中 @InjectID 表示API参数可接收datum编号

根据上述示例，网关在处理`product.getProductInfo` API时会进行如下步骤：

1. 执行API `product.getProductInfo`，得到`ProductInfo`对象
2. 调用`ProductInfo`对象的`getId()`函数，得到datum编号
3. 使用datum编号作为pid参数的实际参数，调用API `price.getPriceInfo`，得到`PriceInfo`对象
4. 使用`PriceInfo`对象作为参数，调用`ProductInfo`对象的`setDatum()`函数
5. 将`ProductInfo`对象返回给前端

#### 2.10.5.2 多对多

即API1返回值是Datum类型的集合，API2返回值是Datum.Aware的集合。R2中的每个Datum.Aware指定一个datum编号，API1根据这组编号生成一组
对应的datum对象并返回，网关根据datum编号对R2中的Aware进行配对，分别调用其对应的setDatum函数将datum赋值到自己的字段中。如下例：

```java
@Description("price info")
public static class PriceInfo implements Datum {
    @Description("product-id")
    public long productId;
    @Description("min-price")
    public int minPrice;
    @Description("max-price")
    public int maxPrice;

    /**
     * 获取唯一标示
     */
    @Override
    public long getId() {
        return productId;
    }
}

public interface PriceService {
    @HttpApi(name = "price.getPriceInfoList", desc = "getPriceInfoList", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectProvider("priceInfoList")
    List<PriceInfo> getPriceInfoList(@InjectIDs @ApiParameter(name = "pids", required = true, desc = "pids") int[] pids);
}

@Description("product info")
public static class ProductInfo implements Serializable, Datum.Aware {
    @Description("product-id")
    public long productId;
    @Description("price")
    public Datum price;

    /**
     * 获取唯一标示
     */
    @Override
    public long getId() {
        return productId;
    }

    /**
     * 将datum注册到实体中
     *
     * @param datum 要注入的数据
     */
    @Override
    public void setDatum(Datum datum) {
        this.price = datum;
    }
}

public interface ProductService {
    @HttpApi(name = "product.getProductInfoList", desc = "getProductInfoList", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectFromApi("priceInfoList")
    List<ProductInfo> getProductInfoList(@ApiParameter(name = "pids", required = true, desc = "pids") int[] pids);
}
```

根据上述示例，网关在处理`product.getProductInfoList` API时会进行如下步骤：

1. 执行API `product.getProductInfoList`，得到一组ProductInfo对象
2. 调用每个`ProductInfo`对象的`getId()`函数，得到一组datum编号
3. 使用这组datum编号作为pids参数的实际参数，调用API `price.getPriceInfoList`，得到一组`PriceInfo`对象
4. 使用`ProductInfo.getId()`和`PriceInfo.getId()`将这组`ProductInfo和PriceInfo`对象进行配对 (`INNER JOIN`)
5. 针对配对成功的每一对`ProductInfo & PriceInfo`，使用`PriceInfo`作为参数调用`ProductInfo.setDatum`
6. 将这组`ProductInfo`对象返回给前端

#### 2.10.5.3 一对多

API1的返回值是Datum集合类型，API2的返回值类型是Datum.ListAware类型。R2提供一组datum编号，API1根据这组编号生成一组对应的datum对象
并返回，网关将这组datum提供给R2的setData，分发和赋值过程交由API2的开发人员负责。如下示例：

```java
@Description("price info")
public static class PriceInfo implements Datum {
    @Description("sku-id")
    public long productId;
    @Description("min-price")
    public int minPrice;
    @Description("max-price")
    public int maxPrice;

    /**
     * 获取唯一标示
     */
    @Override
    public long getId() {
        return productId;
    }
}

public interface PriceService {
    @HttpApi(name = "price.getPriceInfoList", desc = "getPriceInfoList", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectProvider("priceInfoList")
    List<PriceInfo> getPriceInfoList(@InjectIDs @ApiParameter(name = "skuIds", required = true, desc = "skuIds") int[] skuIds);
}

@Description("product info")
public class ProductInfo implements Serializable, Datum.ListAware {
    @Description("product-id")
    public int productId;
    @Description("sku-list")
    public List<SkuInfo> skuInfoList;

    @Override
    public long[] getIds() {
        long[] ids = new long[skuInfoList.size()];
        for (int i = 0; i < skuInfoList.size(); i++) {
            ids[i] = skuInfoList.get(i).skuId;
        }
        return ids;
    }

    /**
     * 将priceInfo列表注册到实体中
     *
     * @param data 准备注入的price info list
     */
    @Override
    public void setData(Collection<? extends Datum> data) {
        Map<Long, Datum> map = new HashMap<>(data.size());
        for (Datum d : data) {
            map.put(d.getId(), d);
        }
        for (SkuInfo sku : skuInfoList) {
            sku.price = map.get(sku.skuId);
        }
    }
}

@Description("sku-info")
public class SkuInfo implements Serializable {
    @Description("sku-id")
    public long skuId;
    @Description("desc")
    public String desc;
    @Description("price")
    public Datum price;
}

public interface ProductService {
    @HttpApi(name = "product.getProductInfo", desc = "getProductInfo", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectFromApi("priceInfoList")
    ProductInfo getProductInfo(@ApiParameter(name = "pid", required = true, desc = "pid") int pid);
}
```

根据上述示例，网关在处理`product.getProductInfo` API时会进行如下步骤：

1. 执行API `product.getProductInfo`，得到一组`ProductInfo`对象
2. 调用`ProductInfo`对象的`getIds()`函数，得到一组datum编号
3. 使用这组datum编号作为pids参数的实际参数，调用API `price.getPriceInfoList`，得到一组`PriceInfo`对象
4. 使用这组`PriceInfo`对象作为参数，调用`ProductInfo`对象的`setData`
5. 将`ProductInfo`对象返回给前端

### 2.10.6 返回值注入提供方接收Datum编号 - @InjectID / @InjectIDs

一个提供返回值注入能力的API需要声明一个能够接收Datum编号的参数，以便它根据编号查找主调方所期待的datum。

使用`@InjectID`将一个API参数声明为可接收一个Datum编号。使用`@InjectIDs`将一个API参数声明为可接收一组Datum编号。

默认情况下网关使用`Datum.Aware.getId()`来确定`@InjectID`的参数值，但是返回值注入的使用方可以在其服务端的实现代码中调用
`ServiceInjectable.exportID(id)`来覆盖`Datum.Aware.getId()`的结果。以达到灵活控制的目的。

同样，默认情况下网关使用`Datum.ListAware.getIds()`来确定`@InjectIDs`的参数值，但是返回值注入的使用方可以在其服务端的实现代码中调用
`ServiceInjectable.exportIDs(id)`来覆盖`Datum.ListAware.getIds()`的结果。以达到灵活控制的目的。

例如：

```java
public class ProductServiceImpl implements ProductService {
    @Override
    public ProductInfo getProductInfo(int pid) {
        // ...
        ServiceInjectable.exportID(pid);
    }
}
```

### 2.10.7 多返回值注入

上文只讨论了单返回值注入的用法，但是在一些情况下，单返回值注入可能还不能满足需求，因此网关也在单返回值注入的基础上，提供了多返回值注入的功能。
多返回值注入指的是一个API可以声明它需要多个API的返回值，网关会依次调用这些API，并将返回结果依次注入到返回值中。

需要返回值注入的API可以多次使用`@ResponseInjectFromApi`来声明它所需要的所有的返回值注入契约。

网关会调用多次Aware.setDatum或者ListAware.setData，并提供该函数的重载版本，指示此次调用是谁的返回值。我们回到Aware的定义：

```java
/**
 * 实现该接口，如果你需要将单个datum注入到你到实体中
 */
interface Aware {
    /**
     * 你所感兴趣的Datum编号，只有id与getId()函数返回的id一样的Datum才会注入进来
     * @return Datum.id
     */
    long getId();

    /**
     * 将datum注册到实体中，支持多注入提供方
     * @param datumType  返回值注入的提供方
     * @param datum 要注入的数据
     */
    default void setDatum(String datumType, Datum datum) {
        setDatum(datum);
    }

    /**
     * 将datum注册到实体中
     * @param datum 要注入的数据
     */
    default void setDatum(Datum datum) {
        throw new UnsupportedOperationException();
    }
}
```

实质上，网关调用的是`Aware.setDatum(datumType, datum)`这个重载版本，其中datumType指的是要注入的datum来自哪个返回值注入契约，即契约名称。

同理，`ListAware.setData(datumType, data)`也是同一个用途。

实现`Aware.setDatum(datumType, datum)/ListAware.setData(datumType, data)`可以让你在进行返回值注入时区分不同的返回值契约。
即使在单返回值注入的情形下，也推荐使用这个重载版本，以方便后续转变为多返回值注入。

### 2.10.8 Datum类型推导

Datum是一个类型占位符，是为了解决类型强依赖而引入的一个动态类型。但是动态类型本身不能为文档API SDK生成提供帮助，datum类型的字段的实际类型
是一个运行时概念。但是为了方便客户端开发，网关尝试通过多个办法试图推断出datum类型的字段的真实类型，这里所做的工作称为Datum类型推断。

类型推断使用以下步骤：

1. 如果声明Datum类型的字段声明了`@DatumType`注解，否则转向步骤6
2. 将`@DatumType`中填写的名字解读为返回值注入契约的名字，并根据契约名查找提供这个契约的API
3. 如果这样的API不存在，则报告非法契约名错误
4. 检查这个API的返回值是否为Datum或Datum集合类型，否则报告非法API错误
5. 并将该API的返回值类型中的实现Datum的类作为字段的最终类型，并返回
6. 确认引用本实体的API所声明需要使用的返回值契约数量。如果数量为0则跳转到8，为1则进行下一步，否则报告缺少@DatumType注解错误
7. 将其引用的API声明的唯一的返回值契约作为缺省的`@DatumType`，并执行步骤4, 5
8. 由于无法确定该字段，并且引用该实体的API没有声明返回值注入契约，则可以判定该字段不会使用，所以忽略该字段。被忽略的字段不会出现在文档中。

> 注意：强烈建议在任何声明Datum类型的字段上使用@DatumType，以静态确定其字段类型

### 2.10.9 级联返回值注入

网关同样支持级联返回值注入，即一个API需要另外一个API的返回值，而这个API又需要第三个API的返回值，等等。原理和上文描述一致，不再复述。

## 2.11 API Mixer

网关所提供的返回值注入功能能够允许一个API的开发人员通过声明的方式将另外一个或多个API的返回值嵌入到自己的返回值中。这种做法本质上是将多个
API的返回值结果进行混合，并将混合结果返回给前端。返回值注入的主要特点是客户端不可知，并且参与混合的API中有一个主体API，其他API把结果混入
到这个API中。

在某些场景下，一些业务需求返回值注入可能不能满足需求，或者语义上不适合使用返回值注入。例如客户端需要一个新的数据，这个数据中的所有部分都
能从已有的API上获取，但是这些API之间对最终的数据结果没有明显的主次之分，或者整合这些API的返回值不只是简单的赋值。对于这种情况我们依然
可以通过增加一个新的API来实现。但是如果大面积采用这种方案，就会有很多做返回值整合的API，他们没有非常具体的业务逻辑，只是进行返回值整合。

为此，网关提供了一个新的编程模型，`API混合器`，即`API Mixer`，简称`mixer`，作为API返回值整合的总体解决方案。

顾名思义，mixer是将多个API的返回值整合成一个返回值返回给前端。能够被整合的API通常为查询类API，没有业务上的副作用。mixer主要
应用于页面渲染，可为某个页面提供一站式的数据结构。从这个层面上说，mixer是面向前端的特殊API。

在前端需要渲染数据时，推荐优先考虑增加`mixer`的可能性。这种做法的目的是为了保证API的纯粹性，即一个API只负责一个微服务领域的数据，
低内聚高耦合，最终返回给前端的数据结构由mixer控制，由mixer的开发人员负责和前端对接。

### 2.11.1 声明API Mixer

使用`@HttpDataMixer`注解声明一个mixer，与普通API不同的是，`@HttpDataMixer`声明在类上，而不是在方法上。这意味着每个类只能有一个mixer，
这个mixer对应的函数是这个类的名为mix的静态函数。同样，与普通API不同，mixer只运行于网关内部，没有独立的进程，也不需要DUBBO调用，
mixer的实际代码就写到这个mix的静态函数中。参考以下示例：

```java
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("product.id")
    public int id;
    @Description("name")
    public String name;
}

@HttpDataMixer(name = "getProductFullInfo", desc = "getProductFullInfo", owner = "nick", pagePath = "index.html")
public class ProductMixer {

    @Description("product-full-info")
    public static class ProductFullInfo implements Serializable {
        @Description("product-id")
        public int productId;
        @Description("product-name")
        public String name;
        @Description("price")
        public int price;
    }

    public static ProductFullInfo mix(ProductInfo productInfo, int price/* .. 在此声明你需要混合哪些实体 ..*/) {
        // ...
        ProductFullInfo info = new ProductFullInfo();
        info.productId = productInfo.id;
        info.name = productInfo.name;
        info.price = price;
        // ...
        return info;
    }
}
```

`@HttpDataMixer`中可以指定你的mixer的名字、描述、owner以及所服务的前端页面。

这里mixer的名字和API的名字不是同一个概念。虽然mixer也是一种API，但是却和普通API有明显区别，所以为了区分，网关固定在mixer名称上加上
"mixer."前缀作为API的最终名称，前端需要使用带前缀的名字来执行这个mixer。由于前缀的存在，在API文档中所有的mixer都被分到了mixer组内。

静态的mix函数可以声明0到多个参数，其中每个参数都是它想要混合的返回值类型。mixer能够混合多个API组合，只要这些API的返回值类型兼容mix函数的
参数列表。mixer具体需要混合哪些API，最终是由前端决定的，即请求的_mt参数。mt参数中指定mixer依赖哪些API，则该mixer混合哪些API。网关会
先执行这些API，并在API都执行成功后，将这些API返回值作为参数，调用mix函数。

> _mt中指定的mixer的依赖项必须和mixer的参数列表匹配，否则网关会报告参数错误。
例如：_mt=product.getProductInfo,price.getPrice,mixer.getProductFullInfo:product.getProductInfo/price.getPrice

### 2.11.2 API Mixer项目

由于API Mixer主要用于混合其他API的返回值，直接引用它需要混合的API的返回值类型能够大规模减轻mixer开发者的负担，因此API Mixer项目允许
依赖其他API项目。

允许依赖其他API项目的一个显著问题是，在API Mixer开发阶段所引用的一个版本的API Jar，在运行阶段可能是另外一个版本，可能会出现严重的二进制
兼容问题。假设API Mixer引用了一个类，而这个类在运行阶段的API Jar中已被删除，则会出现ClassNotFound问题。为此，在网关解析API Jar阶段，
会进行一次较为严格的语法检查，确保所有API Jar引用的的类、字段、方法等都合法，以此来保证在早期发现运行时的二进制不兼容问题。

### 2.11.3 可选API

mixer混合的API可以分为两种，一种是写入到`mix函数`参数和`_mt`参数中的，称为`必选API`，必选API由前端指定，并由网关执行，执行时间早于mixer。
另外一种API称为`可选API`，由mixer的实现方根据情况选择性地主动调用这些API，执行时间晚于mixer。

对于mixer开发者来说，必选API和可选API从性能和编程复杂度上考虑没有明显差异，对前端来说，可选API是透明或者近似透明的，而必选API必须由客户端
指定。在混合一个API的时候，具体使用必选API方式还是可选API方式，需要根据具体业务逻辑来确定，即是不是必须要混合的。

mixer需要在`@HttpDataMixer`中通过`optional`字段指定它可能会混合的所有可选API集合，网关在协助mixer混合可选API时会进行校验，
所有没有声明的可选API都会被阻止。

前端同样也可以干预可选API的混合。通过指定`_opt={可选API列表}`向mixer建议它真正感兴趣的可选API，但是mixer可以选择忽略这些建议。

### 2.11.4 执行可选API

在mixer的实现代码中执行一个API可能比想象的要复杂很多，要处理包括提供API需要的API参数和自动注入参数、API返回值拦截（注入）以及异步等问题。
网关为此做了很多工作，使之易于编写和阅读。

如果要在mixer中执行一个API，需要将该API加入到`@HttpDataMixer`的optional列表中，并且在mix参数中增加一个类型为`ApiCallProxy`的
参数（位置不限），该参数不作为mixer的必选API的混合列表。ApiCallProxy是我们执行API的主要工具类。

接下来我们对ApiCallProxy用法进行深入探讨。

#### 2.11.4.1 获取客户端建议的可选API集合

ApiCallProxy的成员函数optional：

```java
/**
 * optional list
 * @return optional list
 */
Set<String> optional();
```

用于获取前端建议的可选API集合。客户端如果没有传递_opt，则此函数返回空集合，
否则返回由客户端指定API集合。网关不会关注这个集合，mixer可以执行不在这个集合中的API。

#### 2.11.4.2 执行API

网关使用动态类型来达到方便调用API的目的，可以使用ApiCallProxy的成员函数：

```java
/**
 * get an instance of a certain type
 * @param serviceType service type
 * @param <T> service type
 * @return instance of the service
 */
<T> ApiCallService<T> service(Class<T> serviceType);
```

来获取一个服务类的动态代理对象，并借助这个代理对象调用实际的API。网关借助Lambda表达式、范型和API的接口定义提升实际编码体验。service函数
返回`ApiCallService<T>`类型，其中`T`为要调用的API所属接口的类型。

`ApiCallService<T>`包含以下两个重载函数：

* `<R> ApiCallService<T> api(Function<T, R> calling, ApiCallCallback<R> callback)`

  执行一个API，并且在API执行成功或失败后都调用callback

* `<R> ApiCallService<T> api(Function<T, R> calling, ApiCallCallback.Simple<R> callback)`

  执行一个API，并在只在API成功执行后调用callback，如果API执行失败，则整体调用失败

所有API的调用均为异步调用，连续两次执行api函数会启动两次异步API调用，而且api函数执行结束后`callback`不一定会执行，换句话说，整个mixer函数
执行结束后callback也不一定会执行。callback会在后续的某个时间点，在它所关联的API执行结束后执行。

参数`calling`是一个`Lambda表达式`，用于选择你要执行的API，以及为这个API提供实际参数。由于技术限制，网关无法从语法层面上限制你在calling中只
选择API，你可能不会选择API，或者选择多个API，或者做其他额外的事情，未来网关会做进一步限制。

参考以下示例：

```java
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("product.id")
    public int id;
    @Description("name")
    public String name;
    @Description("activity")
    public String activity;
}

@ApiGroup(name = "activity", minCode = 0, maxCode = 100, codeDefine = RC.class, owner = "nick")
public interface ActivityService {
    @HttpApi(name = "activity.getActivity", desc = "getActivity", security = SecurityType.Anonym, owner = "nick")
    String getActivity(
        @ApiParameter(name = "productId", required = true, desc = "id") int productId
    );

    class Impl implements ActivityService {
        @Override
        public String getActivity(int productId) {
            return productId == 1 ? "" : "on-sale";
        }
    }
}

@HttpDataMixer(name = "getProductFullInfo", desc = "getProductFullInfo", owner = "nick", pagePath = "index.html", optional = {"activity.getActivity"})
public class ProductMixer {

    @Description("product-full-info")
    public static class ProductFullInfo implements Serializable {
        @Description("product-id")
        public int productId;
        @Description("product-name")
        public String name;
        @Description("price")
        public int price;
        @Description("activity")
        public String activity;
    }

    public static ProductFullInfo mix(ProductInfo productInfo, int price, ApiCallProxy proxy) {
        ProductFullInfo info = new ProductFullInfo();
        info.productId = productInfo.id;
        info.name = productInfo.name;
        info.price = price;

        if (proxy.optional().contains("activity.getActivity")) {
            proxy.service(ActivityService.class).api(
                s -> s.getActivity(productInfo.id),
                r -> info.activity = r
            );
        }

        return info;
    }
}
```

我们近距离放大看`ApiCallProxy`的用法：

```java
proxy.service(ActivityService.class).api(
    s -> s.getActivity(productInfo.id),
    r -> info.activity = r
);
```

这里我们先使用`service方法`选择要调用哪个服务，然后再使用`api`调用那个服务的API，并且在第二个回调函数中将API的调用结果赋值给
`info.activity`。

> 注意：回调函数可能会在mix函数执行之后才会执行

我们看一下`ApiCallCallback<T>`的定义：

```java
/**
 * callback which will be called when an api call has completed
 */
@FunctionalInterface
public interface ApiCallCallback<T> {
    /**
     * called when the api call completed
     * @param result the result of the api call
     * @param code the return code of the api call
     * @throws Throwable
     */
    void onCompleted(T result, int code) throws Throwable;

    @FunctionalInterface
    interface Simple<T> extends ApiCallCallback<T> {
        /**
         * called when the api call completed
         * @param result the result of the api call
         * @param code the return code of the api call
         * @throws Throwable
         */
        default void onCompleted(T result, int code) throws Throwable {
            if (code != 0) {
                throw new GatewayException(ApiReturnCode.DEPENDENT_API_FAILURE);
            }
            onCompleted(result);
        }

        /**
         * called when the api call completed
         * @param result the result of the api call
         * @throws Throwable
         */
        void onCompleted(T result) throws Throwable;
    }
}
```

再回到`callback`的重载版本上来。它的参数有两种格式：

* `ApiCallCallback<T>`

  如果你希望无论API执行成功还是失败都要执行你的回调函数，则使用这个版本。这个版本的回调有两个参数，第一个参数是API的返回值
  （如果执行成功，否则为null），第二个参数是API执行的错误码（执行成功时为0）。
  
  使用这个版本的回调函数时需要显式判断错误码是否为0，只有在错误码为0的条件下返回值才有意义。例如：
  
  ```java
  (r, c) -> {
      if (c == 0) {
          info.activity = r;
      } else {
          info.activity = "none";
      }
  }
  ```

* `ApiCallCallback.Simple<T>`
  
  如果你只希望在API执行成功后才执行你的回调函数，则使用这个版本。这个版本的回调只有唯一一个参数，表示API执行的返回值。
  
  例如：
  
  ```java
  r -> {
      info.activity = r;
  }
  ```

  或：
  
  ```java
  r -> info.activity = r
  ```

#### 2.11.4.3 执行多个API

同时调用两个API，不分先后：

```java
proxy.service(ActivityService.class).api(
    s -> s.getActivity(productInfo.id),
    r -> info.activity = r
);
// ...
proxy.service(ActivityService.class).api(
    s -> s.getActivity2(productInfo.id),
    r -> info.activity2 = r
);
```

或者：

```java
proxy.service(ActivityService.class).api(
    s -> s.getActivity(productInfo.id),
    r -> info.activity = r
).api(
    s -> s.getActivity2(productInfo.id),
    r -> info.activity2 = r
);
```

以上两种方式意义等价，均并发执行，没有先后顺序。

如果需要在一个API调用结束之后再调用另外一个API，可以将调用逻辑写到前一个API的回调函数中，例如：

```java
proxy.service(ActivityService.class).api(
    s -> s.getActivity(productInfo.id),
    r -> {
        if (r == null) {
            proxy.service(ActivityService.class).api(
                s -> s.getActivity2(productInfo.id),
                r -> info.activity = r
            );
        } else {
            info.activity = r;
        }
    }
);
```

#### 2.11.4.4 代码块序列

但是这种用法只能做到在一个API执行结束后执行一段代码。如果你想在两个或多个API执行结束之后再执行一段整合代码，就不能用这种办法了。

为此ApiCallProxy提供了另外一个成员函数：

```java
/**
 * series of blocks which will be executed in order
 * @param blocks blocks to execute
 */
void sequence(SequenceBlock... blocks);
```

其中`SequenceBlock`是一个函数型接口：

```java
/**
 * a block used to build a sequence
 */
interface SequenceBlock {
    /**
     * the execution of the block
     * @param proxy a proxy used to build nested blocks
     * @throws Throwable throwable may be thrown
     */
    void run(ApiCallProxy proxy) throws Throwable;
}
```

`sequence`中可以写入多个`代码块`，这些代码块及其代码块会按照顺序依次执行。每个代码块中可以执行API，也可以调用`sequence`嵌套代码块。
只有上一个代码块中的所有执行的API以及嵌套的代码块都执行完后才会执行下一个代码块。需要注意的是，每个代码块都有一个独立的ApiCallProxy对象，
只有使用这个ApiCallProxy对象才可以将API的执行绑定到这个代码块上，使用其他ApiCallProxy对象无法达到顺序执行的目的。

参考以下用例：

```java
proxy.sequence(p1 -> { // 步骤1

    p1.service(ActivityService.class)
        .api(s -> s.getActivity1(info.productId), r -> info.activity1 = r);

}, p2 -> { // 步骤2

    if (info.activity1 == null || info.activity1.isEmpty()) {
        p2.service(ActivityService.class)
            .api(s -> s.getActivity2(info.productId), r -> info.activity2 = r);
    }

    p2.service(ActivityService.class)
        .api(s -> s.getActivity3(info.productId), r -> info.activity3 = r);

}, p3 -> { // 步骤3
    // ...
});
```

> 注意：`sequence`的每个代码块都有一个ApiCallProxy类型的参数，示例代码中的p1/p2，代码块中必须通过这个参数执行API，
否则不能达到顺序执行的目的。

### 2.11.5 API Mixer接收额外参数

一般情况下，API Mixer只做简单的必选API或者可选API的多API混合工作，但是在某些业务场景下，mixer本身也需要接收来自客户端的参数，或者注入
一些通用参数，例如当前的用户id等。为了达到这个目的，网关允许mixer向普通API一样，可以使用 `@ApiParameter / @ApiAutowired /
@ApiCookieAutowired / @ExtensionAutowired` 将参数声明为API参数或者可注入参数。

从设计上说，mixer的静态mix函数可以有如下四类参数：

| 参数类型 | 参数注解 | 含义 | 是否出现在API文档和SDK中 |
|:---|:---|:---|:---|
| ApiCallProxy | 无注解 | 用于执行可选API | 否 |
| 合法的API参数类型 | @ApiParameter | API参数 |
| 合法的API参数类型 | @ApiAutowired @ApiCookieAutowired @ExtensionAutowired | 可注入参数 | 否 |
| 合法的API返回值类型 | 无注解 | 用于混合的API返回值 |

> 注意：*用于混合的API返回值*这类参数的顺序和数量需要和_mt中提供的mixer的依赖项保持对应。但是这类参数在mix函数的所有参数中的绝对位置
没有关系。换句话说就是，其他种类的参数的位置可以随意调换，不影响原本的mixer依赖语义。

客户端向mixer提供API参数的逻辑和其他普通API类似。

从技术层面上说，mixer函数也可以参与返回值注入和参数注入，但是具体是否要用，需要仔细斟酌。

### 2.11.6 组合API Mixer与返回值注入

根据上文描述，API Mixer可以选择混合必选API，也可以选择混合可选API，或者两者结合。混合可选API时可以控制每个API的执行顺序和依赖关系，
但是混合必选API就没有这么方便了。如果要混合的多个必选API之间有依赖或者参数注入的需求，需要要求这些API的提供者去声明这种关系，mixer本身
无法干预。网关会在执行mixer之前很好的处理这些必选API之间的依赖关系，并将最终执行结果传递给mixer。

### 2.11.7 完整的API Mixer示例

本节我们使用一个`product/price/activity`示例来展示一个具体的API Mixer的用法。我们会先展示mixer所要混合的商品、价格以及活动的API，
最后再展示mixer。

#### 2.11.7.1 商品服务

```java
@Description("product-info")
public class ProductInfo implements Serializable {
    @Description("product.id")
    public int id;
    @Description("name")
    public String name;
}

@ApiGroup(name = "product", minCode = 0, maxCode = 100, codeDefine = ProductService.RC.class, owner = "nick")
public interface ProductService {
    class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @ExportParam(name = "product.id")
    @HttpApi(name = "product.getProduct", desc = "getProduct", security = SecurityType.Anonym, owner = "nick")
    ProductInfo geProduct(@ApiParameter(name = "id", required = true, desc = "id") int id);
}

public class ProductServiceImpl implements ProductService {
    @Override
    public ProductInfo geProduct(int id) {
        ProductInfo info = new ProductInfo();
        info.id = id;
        info.name = "product#" + id;
        ServiceInjectable.export("product.id", id);
        return info;
    }
}
```

#### 2.11.7.2 价格服务

```java
@ApiGroup(name = "product", minCode = 0, maxCode = 100, codeDefine = PriceService.RC.class, owner = "nick")
public interface PriceService {
    class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @HttpApi(name = "price.getPrice", desc = "getPrice", security = SecurityType.Anonym, owner = "nick")
    int getPrice(
        @ImportParam("product.id")
        @ApiParameter(name = "productId", required = true, desc = "id")
        int productId
    );
}

public class PriceServiceImpl implements PriceService, TestDubboSupport {
   @Override
   public int getPrice(int productId) {
       return productId == 1 ? 10 : 20;
   }
}
```

#### 2.11.7.3 活动服务

```java
@ApiGroup(name = "activity", minCode = 0, maxCode = 100, codeDefine = ActivityService.RC.class, owner = "nick")
public interface ActivityService {
    class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @HttpApi(name = "activity.getActivity1", desc = "getActivity1", security = SecurityType.Anonym, owner = "nick")
    String getActivity1(
            @ApiParameter(name = "productId", required = true, desc = "id")
                    int productId
    );
    @HttpApi(name = "activity.getActivity2", desc = "getActivity2", security = SecurityType.Anonym, owner = "nick")
    String getActivity2(
        @ApiParameter(name = "productId", required = true, desc = "id")
            int productId
    );
    @HttpApi(name = "activity.getActivity3", desc = "getActivity3", security = SecurityType.Anonym, owner = "nick")
    String getActivity3(
        @ApiParameter(name = "productId", required = true, desc = "id")
            int productId
    );
}

public class ActivityServiceImpl implements ActivityService {
   @Override
   public String getActivity1(int productId) {
       return productId == 1 ? "" : "on-sale", 50;
   }

   @Override
   public String getActivity2(int productId) {
       return productId == 1 ? "on-sale2" : "";
   }

   @Override
   public String getActivity3(int productId) {
       return productId == 1 ? "on-sale3" : "";
   }
}
```

#### 2.11.7.4 API Mixer

```java
@HttpDataMixer(name = "getProductFullInfo", desc = "getProductFullInfo", owner = "nick", pagePath = "index.html",
    optional = {"activity.getActivity1", "activity.getActivity2", "activity.getActivity3"})
public class ProductMixer {
    @Description("product-full-info")
    public static class ProductFullInfo implements Serializable {
        @Description("product-id")
        public int productId;
        @Description("product-name")
        public String name;
        @Description("price")
        public int price;
        @Description("activity1")
        public String activity1;
        @Description("activity2")
        public String activity2;
        @Description("activity3")
        public String activity3;
    }

    public static ProductFullInfo mix(ProductInfo productInfo, int price, @ApiParameter(name = "dlv", required = true, desc = "dlv") double dlv, ApiCallProxy proxy) {
        ProductFullInfo info = new ProductFullInfo();
        info.productId = productInfo.id;
        info.name = productInfo.name;
        info.price = (int) (price * dlv);

        proxy.sequence(p1 -> { // 步骤1

            p1.service(ActivityService.class)
                .api(s -> s.getActivity1(info.productId), r -> info.activity1 = r);

        }, p2 -> { // 步骤2

            if (info.activity1 == null || info.activity1.isEmpty()) {
                p2.service(ActivityService.class)
                    .api(s -> s.getActivity2(info.productId), r -> info.activity2 = r);
            }

            p2.service(ActivityService.class)
                .api(s -> s.getActivity3(info.productId), r -> info.activity3 = r);

        }); // ... 步骤3/4/5...

        return info;
    }
}
```

客户端执行该mixer时可以按照以下方式：

```javascript
let mt = "product.getProduct,price.getPrice:product.getProduct,mixer.getProductFullInfo:product.getProduct/price.getPrice";
let response = await fetch("http://api.gateway.url/apigw/m.api", {
    "body": "_mt=" + mt + "&0_id=1&2_dlv=0.8",
    "method": "POST"
});
```

> 注意`_mt`的参数格式

## 2.12 API MOCK

一个功能的演进和开发需要前后端共同配合，从API的角度考虑是，提供API和使用API。为了更大可能的提高前后端开发效率，前后端往往会先确定好API的
名称、参数以及返回值格式，然后前后端独立开发。但是为了方便前端调试，在API最终实现之前，可以短路掉API的实际值，即`API MOCK`。

`API MOCK`有两种模式：

* API静态返回值
  
  使用`@ApiShortCircuit(Class)`注解API方法，将API的返回值设置为`@ApiShortCircuit`指定的类的实例对象。网关会调用这个类的无参构造函数进行
  对象构造，并将其作为这个API的静态返回值。
  
* API所在接口的伪造实现类

  如果你希望为你的API所在的接口声明指定一个伪造的实现类，并希望网关在处理这个类中的API时全部调用你指定的那个伪造类，而不是发起dubbo调用，
  那么可以使用`@ApiMockInterfaceImpl(class)`将你的伪造类与API所属接口关联起来。网关会使用无参构造函数构造函数创建你指定的伪造类的对象，
  并在执行某个API时，直接在网关进程中执行那个伪造对象的实现方法，从而实现屏蔽实际API实现的目的。
  
  伪造类需要同时实现MockApiImplementation接口和它需要拦截的API所属的接口。`MockApiImplementation`中有个`$setProxy(T proxy)`函数，
  会在初始化时被调用，proxy是实际的dubbo调用代理，可以用于继续调用实际的dubbo接口。换句话说，你可以通过`MockApiImplementation`在
  网关和你的DUBBO服务之间增加一个代理层，按需调用实际的dubbo接口。
  
## 2.13 API切面编程

一般情况下，面向网关编程的主要工作是开发API和使用API。开发的过程就是修改已有API的功能或者提供新的API。但是有时候可能存在另外一种场景，
修改已存在的API行为。视角从提供或者修改某个API的实现，转变为调整一批API的行为，例如为一些满足条件的API增加新的返回值字段。这种编程方式
即为API切面编程。

普通的面向切面编程主要是为了调整一些满足条件的函数的逻辑，比如在函数执行前后执行某些动作。API切面编程与之类似，网关提供给你在API执行前后
执行你的代码的能力，以实现对某些API的执行进行拦截。

目前网关支持的API切面编程的节点有：

* API 返回值处理器

### 2.13.1 API 返回值处理器

如果你希望网关在执行一些特定的API后能够调用你的一个回调函数，也就是所谓的API 返回值处理器，你需要在你的API JAR中实现网关的一个接口：

```java
/**
 * 如果你需要在API执行结束之后对API的返回值进行进一步处理，请实现本接口
 */
public interface ApiResultProcessor extends Extension {

    /**
     * 返回你感兴趣的API列表，只有这些API的返回值才会被processApiResult处理
     * @return api set
     */
    Set<String> interestedApiSet();

    /**
     * 处理返回值
     * @param name API名称
     * @param parameters API执行时所使用的参数
     * @param result API返回值
     */
    void processApiResult(String name, ApiParameterAccessor parameters, Object result);
}
```

该接口包含`interestedApiSet`和`processApiResult`两个函数，第一个函数用于声明你对哪些API感兴趣，只会在网关启动的时候执行。
网关会将这个对象静态绑定到感兴趣的API上，以便后续该API调用的时候执行回调。第二个函数是一个回调函数，会在你感兴趣的API执行结束后调用，
你可以在其中修改返回值，也可以使用它的参数parameters获取这个API执行时所使用的参数。

回调函数执行失败会导致本次API执行失败，但不会影响同次请求中的其他API的执行。

回调函数的执行时间晚于返回值过滤和返回值注入，并且仅在API执行成功后才会执行。

网关对实现接口的类有自动发现功能，API JAR中所有实现`ApiResultProcessor`的类都会绑定接到合适的API上，一个API可以绑定多个回调，网关会依次
同步执行这些回调，并且在第一次遇到错误后终止执行，执行顺序不确定。

## 2.14 API JAR MANIFEST 文件

API JAR指的是包含API定义的Java JAR文件。部署框架在部署网关前，应该向网关提供所有包含API的JAR，并放置于某个固定文件夹。网关在部署时会去
加载解析这些JAR文件。由于JAR中存在非API的类，所以网关采用了显式声明的策略，即所有需要向网关暴露的API都需要在其JAR的`MANIFEST 文件`中
显式填写它所要暴露的API所属的类名。

对于普通API（非mixer）项目，`MANIFEST文件`需要包含以下属性：

* Api-Dependency-Type

  取固定值：`dubbo`
  
* Api-Export
  
  需要暴露的API所属的类名，必须是包含包名的完整名称，多个类名之间用空格分隔，类名不能重复

例如：

```text
Api-Export: com.estatetrader.dubbo.product.ProductService com.estatetrader.dubbo.product.PriceService
Api-Dependency-Type: dubbo
```

对于API MIXER项目，MANIFEST文件需要包含以下属性：

* Api-Dependency-Type

  取固定值：`mixer`
  
* Api-Mixer-Namespace
  
  需要暴露的mixer所属的类名，必须是包含包名的完整名称，多个类名之间用空格分隔，类名不能重复
  
例如：

```text
Api-Mixer-Namespace: com.estatetrader.api.mixer.ProductMixer
Api-Dependency-Type: mixer
```

> 注意：目前网关不支持JAR中同时包含普通API和mixer API

MANIFEST文件必须要添加到JAR包中才会起作用，如果你发现你的API没有被网关识别，请检查你的MANIFEST文件，并确保其已经正确添加到了JAR中。

可以按照如下方式在maven的pom.xml声明向JAR中添加MANIFEST文件：

```xml
<build>
  <plugins>
      <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-jar-plugin</artifactId>
          <version>${maven-jar-plugin.version}</version>
          <configuration>
              <archive>
                  <manifestFile>src/main/resources/META-INF/MANIFEST.MF</manifestFile>
              </archive>
          </configuration>
      </plugin>
  </plugins>
</build>
```

> 其中`src/main/resources/META-INF/MANIFEST.MF`就是你需要添加的MANIFEST文件
