# 6 请求处理流程

在前面的章节中，我们介绍了网关的各个功能点的流程，但是由于按照功能分割，不能了解网关整个处理过程的全貌，有种管中窥豹的感觉。
在本章中，我们详细介绍网关在收到客户端请求后的处理过程，将之前提到的功能点和处理细节呈现出来，方面大家从全局的角度理解请求的处理过程。

## 6.1 请求处理概述

网关在收到请求后，会对请求进行解析、校验，然后根据请求中的要求，调用相应的API，并将API的执行结果返回给客户端，或者在解析、校验失败后确定需要返回给客户端的错误码。下图为请求处理的总体流程图：

```plantuml
:parse-request;
if (parse failed?) then (failed)
    :determine-code;
else (success)
    :process-request-context;
    if (process failed?) then (failed)
        :determine-code;
    else (success)
        :verify-request;
        if (request invalid?) then (invalid)
            :determine-code;
        else (valid)
            fork
                :execute-api(1);
            fork again
                :execute-api(2);
            fork again
                :...;
            end fork
            :serialize-result;
        endif
    endif
endif
:write-response;
```

在后续章节中，我们会逐一阐述图中的每个节点的内部细节。

## 6.2 解析请求

解析请求是请求处理的第一个阶段，用于解析出请求中的各个参数，并构建一个初步的请求上下文（`request-context`）。

请求上下文用于存储本次请求所要用到的所有信息，包括如下信息：

* 已解析的请求参数（包括通用参数和业务参数）
* 需要调用的API列表
* 请求校验结果
* 一些能够影响通用返回结果的标志位
  * utk是否已过期
  * 客户端是否需要清理etk
  * 自动续签得到的新的utk
* 请求处理的各个阶段之间需要共享的数据

请求解析阶段应仅包含对请求的直接提取过程，其后期加工过程应放置到下一节的请求上下文阶段中。

下面是请求解析的流程：

```plantuml
:construct-request-context;
note right
    构造一个空的请求上下文
    后续解析须将解析结果存入到这个上下文中
end note
partition parse-request-parameters {
    :parse-base-parameters;
    note right
        * call id [Header X-Unique-ID]
        * app id [_aid]
        * user agent
        * referer
        * client-ip
        * client version [_vc]
    end note
    :parse-token;
    note right
        来源
        * request parameter _tk
        * request header _tk
    end note
    :parse-device-id;
    note right
        来源
        * request parameter _did
    end note
    :parse-extension-token;
    note right
        来源
        * request parameter _etk
        * request header _etk
    end note
}
partition parse-request-cookies {
    :collect-cookies;
    note right
        来源 (合并)
        * request cookies
        * request parameter _cookie
    end note
    :parse-token-from-cookie;
    note right
        如果参数和header中没有定义tk
        则判断在cookie中是否有定义
        名称为appId + '__tk'
    end note
    :parse-device-id-from-cookie;
    note right
        如果参数中未定义did
        则判断在cookie中是否有定义
        名称为appId + '__did'
    end note
}
:parse-method;
note right
    解析请求参数_mt，mt语法如下：
    * 多个由','拼接的api名
    * api名由3个部分组成：函数名@实例名:依赖函数名1@实例名/依赖函数名2@实例名
    * 除了函数名以外的信息都可以缺省.
end note
```

## 6.3 处理请求上下文

解析请求的结果是一个初步的请求上下文，而解析结束之后我们仍然需要有其他更进一步的解析逻辑。这些解析逻辑对请求上下文中的信息进行读取、格式转换、提取、再加工，并将这些信息存入到请求上下文中。例如，在请求解析阶段，我们确定了token的文本形式，而在请求上下文处理阶段，我们对token进行解析，得到格式化之后的token对象。

请求上下文的处理过程按照不同的功能，可以划分为多个处理器，每个处理器负责自己相关的处理需求，处理器之间可以有依赖关系，将上一个处理器的处理结果作为自己的输入。

下图描述了请求上下文的处理流程：

```plantuml
start
partition mock {
    if (mock-enabled) then (need-mock)
        :call API mock.getEnabledMockedApis;
        note right
            确定请求中哪些API需要走mock流程
        end note
    endif
}

partition security {
    :parse-token;
    note right
        使用预设的AES密码对token进行解密
        并反序列化成CallerInfo对象
    end note
    :set-device-id-from-token;
    note right
        用tokne中的did取代请求上下文中的did
    end note
    :determine-required-security-level;
    note right
        合并请求中所调用的所有API要求的权限级别
        作为请求最终要求的权限级别
    end note
    :process-token;
    note right
        对token进行更进入的处理
        参考下图
    end note
}
stop
```

其中处理token的`process-token`过程如下图所示：

```plantuml
start
:parse-extension-token;
note right
    使用base64解码器解码etk
    并反序列化成ExtensionCallerInfo对象
end note
if (utk expired but in renew window?) then (expired but not too late)
    :renew-user-token;
    note right
        调用续签utk的API进行续签
        并将续签后的新的utk设置会请求上下文
    end note
endif
:check-user-expire-token-tree;
note right
    检查utk过期树
end note
if (utk should be expired) then (should be expired)
    if (renew before expire) then (need renew)
        :renew-user-token;
        note left
            调用续签utk的API进行续签
            并将续签后的新的utk设置会请求上下文
        end note
    else
        :force-utk-expire;
        note right
            强制将utk的过期时间设置为已过期
            并将utk过期树中给定的过期原因设置到请求上下文中
            这种设置会在后续步骤中被检查出来
        end note
    endif
endif
if (user token expired?) then (expired)
    :degrade-utk-to-dtk;
    note right
        设置请求上下文中的caller(tk)字段：
        * uid=0
        * role=null
        并设置请求上下文中的userTokenExpired为true
    end note
endif
stop
```

## 6.4 校验请求

在请求解析和请求上下文处理完成之后，整个请求的解析工作全部完成。接下来，在真正执行API之前，我们需要对我们的解析结果进行完整的合法性校验，包括签名是否合法、用户是否有权限访问API等。

请求的校验过程如下：

```plantuml
start
:check-if-token-expired;
note right
    校验是否需要向客户端utk过期错误码
    仅在tk已过期并且本次请求需要utk的情况下拒绝请求
end note
:check-request-verify-code;
note right
    判断此次请求是否需要验证码
end note
:check-base-security;
note right
    基础权限检查
end note
:check-request-signature;
note right
    校验请求签名
end note
:check-authorization;
note right
    校验当前用户是否有权限访问请求中包含的API
end note
:check-if-blocked;
note right
    校验当前用户/设备/IP/手机号是否处于风控黑名单中
end note
:check-extension-token;
note right
    校验etk是否合法
end note
stop
```

> 请参考其他章节了解每个校验过程的具体细节。
