# 4 加载API

网关在启动之前需要由发版系统配合，将其所需要的所有的API JAR放置于它的一个特定文件夹，称之为API JAR收集。网关在启动之后会对这些JAR进行
分析，从中解析出API以及其他信息。本章节我们会对这些过程做详细讲解。

## 4.1 收集API JAR

网关的部署有两种模式：

* 标准发版模式
  
  包括编译网关代码，收集API JAR，启动网关
  
* 重新加载API JAR模式

  包括收集API JAR，启动网关，不包括编译网关代码
  
这两种模式的主要区别是是否编译网关代码，其中第一种模式包含了第二种模式的所有功能。第二种模式的提出主要是为了应对大多数的情况：网关代码没有变动，但是需要加载
集群中最新的API JAR。本节我们着重介绍第二种模式中的API JAR的过程。

API JAR的收集流程设计到了DUBBO服务发版过程和网关启动过程，需要发版系统的深度配合。整个流程分为两个阶段：暴露阶段和收集阶段。

### 4.1.1 API JAR暴露阶段

在发版系统的配合下，每个DUBBO服务的每个运行实例在成功启动之后会将它所使用的API JAR提交给发版系统，API JAR的的文件名需要满足模式：

```text
(service-name)-api-*.jar
```

其中`(service-name)`指的是当前服务的名称，例如`user`，`order`。发布系统会在找不到这种jar时报告错误，如果你的项目的确不存在这种
jar，可以作为特例向发布系统维护人员申请。

除了DUBBO服务之外，其他的一些项目也有暴露API JAR的需求，例如`API Mixer`项目。这类项目可以分为两种：

1. 有运行时进程，与DUBBO服务类似，会在进程成功启动后向发版系统提交API JAR
2. 无运行时进程，简单的java类库项目，会在编译成功后向发版系统提交API JAR

发版系统在接收到新的API JAR时会删除这个JAR对应的老版本的JAR，并更新API JAR清单。

DUBBO项目默认具备暴露API JAR的能力，其他项目如果需要类似的功能，请联系运维人员登记。

### 4.1.2 API JAR收集阶段

API JAR收集阶段时网关发版或者重新加载API JAR的一个环节。在这个过程中发版系统会按顺序从如下两个位置收集API JAR：

1. 网关上一次加载API时使用的所有API JAR
2. 从发版系统中导出截止到目前为止DUBBO服务或者其他项目提交的API JAR

其中来源#2中的API JAR会根据命名规则覆盖掉来源#1中的某些JAR，以去除属于同一项目的重复JAR文件。来源#1存在的意义是为了防止发版系统自身的
文件管理问题导致JAR丢失，以此提高API JAR收集流程的稳定性。

### 4.1.3 API JAR过期

在项目演进的过程中，一些项目可能会被合并或者拆分，或者迁移到其他项目，这个时候会遇到API JAR过期的问题，即这些API JAR应该从网关收集API JAR
的流程中除去掉。发版系统维护了`已过期API JAR名单`，用于实现这个需求。如果你的项目需要从网关中抹除，请联系运维人员登记。

由于网关在收集API JAR之后会对这些JAR进行语法校验，如果你在合并项目之后碰巧没有登记过期API JAR，这个语法检查逻辑能够检测到这些JAR之间的
重复类，并阻止进一步发版。

## 4.2 API加载

* 循环每个api-jar
* 读取jar中定义的Api-Export MainAttribute并根据空格拆成多个类名，加载并循环这些类
* 检查类是否定义了@ApiGroup
* 检查@ApiGroup中定义的ErrorCode
* 循环类中定义的所有方法
* 如果方法有@HttpApi，则认为该方法公开到网关，否则继续检查后续方法
* 检查方法的额外注解：

  * @DesignedErrorCode：接口只能抛其中定义的错误码
  * @ErrorCodeMapping：网关自动将接口抛出的错误码转换成其他错误码
  
* 检查方法每个参数的注解：
  
  * @ApiParameter表示此参数由客户端提供
  * @ApiAutowired表示此参数由网关自动注入（来自于token，cookie，http header等）
  * @ApiCookieAutowired表示参数来自于cookie

* 分析接口依赖、文件上传、参数校验、授权服务、子系统等信息
* 分析并检查参数类型和返回值类型（递归）信息
