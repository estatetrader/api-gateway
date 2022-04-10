# 7 网关工作原理

网关的实现需要满足以下要求：

* 性能方面  
  由于网关是后端服务的总入口，其性能的重要性不言而喻。好的网关实现应尽可能提高其工作效率，减少延迟、增加吞吐量
* 稳定性  
  网关的稳定性决定了系统的整体稳定性，好的网关实现应尽可能使其稳定工作，不会因为个别请求导致网关整体功能失效
* 灵活性  
  虽然由于脱离具体业务逻辑从而使得网关的代码改动并没有后端服务那么多，但是仍然有持续不断的需求需要实现
* 隔离性  
  因为对稳定性的要求，好的网关实现应将核心流程和边缘功能隔离开，减少因为小功能的需求导致核心流程的修改，从而提高网关的稳定性
* 可读性  
  由于本身在开发过程中的重要地位，好的网关实现应有很好的可读性，包括完善的文档，必要的代码注释和合理的代码组织结构

目前的网关实现主要采用了以下两种设计模式：

* 异步非阻塞模式  
  处理请求中遇到的所有耗时的任务均采用异步回调模式，尽可能避免线程阻塞。这种设计模式可以满足网关在性能方面的要求
* 插件模式  
  将实现代码分为核心代码和插件代码，核心代码仅包含最核心的功能，其他所有扩展功能均通过插件方式实现。这种设计模式可以满足网关在稳定性、灵活性、隔离性和可读性方面的要求

在本章中，我们会对这两种设计模式作详细的介绍。

## 7.1 API工作流图

为了满足网关对性能的要求，我们采用了异步非阻塞的设计模式，并通过所谓的`工作流图`实现这种设计模式。

工作流图的本质是将请求处理过程中的所有操作拆解成多个工作阶段，称之为工作流图中的节点，两个有前后顺序关系的工作阶段意味着这两个阶段之间有依赖关系，称之为工作流图中的弧。每个阶段仅包含不阻塞的代码，能快速执行，不会阻塞当前线程。如果一段代码会导致当前线程阻塞一次，则应使用该阻塞点将这段代码分割为两个阶段（节点），并根据原始代码语义决定是否增加一个从第一段代码指向第二段代码的弧。

在执行工作流图时，当前线程从没有依赖项的节点开始执行。节点可以选择同步模式或异步模式。

在同步模式下，当前线程在执行完这个节点后判断它的后续节点是否可以执行（其他依赖项均已执行），如果可以执行，则执行它的后续节点。

在异步模式下，当前线程在执行完这个节点后，将回调函数注册到节点指定的钩子上，并在回调函数中判断是否可以执行后续节点。

通过这种方式，网关可以避免等待异步任务导致的线程浪费和上下文切换开销。并且由于可以同时启动多个异步任务，借助并发提高了执行效率。

目前网关采用了异步工作流模型执行API。

TOMCAT在收到客户端的HTTP请求后，会启动一个工作线程并执行网关的代码处理该请求：

1. 分析`HTTP REQUEST`请求中的`_mt`参数，并根据`mt`语法组织出API工作流图。每个API都是工作流中的一个节点，拥有执行依赖的两个API
   在工作流图中有个从被依赖API到依赖API的有向弧。
2. 从`HTTP REQUEST`中提取其他通用参数和token，并进行权限验证、签名验证、token有效期验证（续签token）、滑块验证码校验、黑名单等。
3. 提取业务参数，并放到对应的API参数列表中
4. 将`REQUEST`设置为异步模式
5. 启动工作流中的所有无前序节点的节点（无依赖项的API），退出当前工作线程
6. DUBBO通信线程将请求发送到响应DUBBO服务端
7. 远程机器执行API处理
8. DUBBO通信线程收到API处理结果，并启动消费端工作线程
9. 消费端工作线程继续API的后续处理：处理返回值拦截（注入）、返回值处理器、异常处理等，并启动后续工作流节点，继续工作流的执行
10. 在工作流所有节点执行完毕后执行请求的收尾工作：错误码处理、序列化API执行结果到响应流等
11. 通知TOMCAT结束本次请求

为了防止DUBBO长时间不返回导致的工作流无法结束问题，网关引入了超时巡逻机制，确保所有工作流能在规定时间内结束（或超时结束）。

## 7.2 Phase-Feature设计模式

为了满足网关对灵活性的要求，为了提高代码的可读性，网关采用了流行的插件设计模式。网关的核心业务逻辑会按照大的阶段划分到各个阶段，称之为`phase`，每个`phase`均包含一系列扩展点。这些`phase`本身就构成了网关的基本功能：

* 解析API JAR（基本定义）
* 处理请求
  * 解析请求参数
  * 执行请求中指定的API
  * 整理执行结果，写入到响应中
* 文档生成

这些功能本身就可以支撑一个简单的网关功能，可以执行请求中的API，但不能进行身份认证和请求签名等其他功能。

网关将其他所有功能封装到了各个`feature`之中，这些`feature`使用`phase`中的扩展点，将自己寄生在基本流程之中。例如基本流程中有请求校验扩展点，而请求签名`feature`可以在这个扩展点挂载有关请求签名的逻辑。每个`feature`除了可以挂载主流程的扩展点，也可以声明自己的扩展点，让其他更细节的`feature`挂载到自己的扩展点上，以实现所谓的插件的插件。

网关主要包含以下`feature`：

* `SecurityFeature`，定义了基本的身份认证功能
  * `RequestSignatureFeature`，请求签名功能
  * `AuthorizationFeature`，授权校验功能
  * `BlacklistFeature`，黑名单功能
  * `ExtensionTokenFeature`，扩展凭据功能
  * `ForceUserTokenExpireFeature`，用户凭据强制过期功能
  * `RenewUserTokenFeature`，用户凭据自动续签功能
  * `UserTokenExpireFeature`，用户凭据过期和降级功能
  * `VerifyCodeFeature`，验证码功能
* `ErrorCodeFeature`，错误码功能
* `FileUploadFeature`，文件上传功能
* `MixerFeature`，mixer功能
* `MockServiceFeature`，mock功能
* `ResponseFilterFeature`，返回值过滤器功能
  * `ServerInjectionFeature`，服务端注入功能