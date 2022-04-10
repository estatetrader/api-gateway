package com.estatetrader.dubboext;

import com.alibaba.fastjson.JSON;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.ConstField;
import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.gateway.backendmsg.BackendMessageBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关向各服务暴露的dubbo旁路通信控制工具
 * 请使用此类的各静态函数实现读取当前dubbo请求的attachment，或者设置返回给上游服务/网关的notification
 */
public class DubboExtProperty {

    private static final Logger logger = LoggerFactory.getLogger(DubboExtProperty.class);
    public static final  String LOG_SPLITTER = new String(new char[] { ' ', 2 });

    /**
     * 非dubbo provider调用用来暂存notification,例如:单元测试等
     */
    private final static ThreadLocal<Map<String, String>> notifications
        = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * dubbo filter 中调用用来暂存调用信息
     */
    private final static ThreadLocal<Map<String, String>> callInfos
        = ThreadLocal.withInitial(ConcurrentHashMap::new);

    @SuppressWarnings("unused")
    public static void setCallId(String callId) {
        if (callId != null && callId.length() > 0) {
            callInfos.get().put(CommonParameter.callId, callId);
        }
    }

    @SuppressWarnings("unused")
    public static void putCallInfo(String key, String value) {
        callInfos.get().put(key, value);
    }

    @SuppressWarnings("unused")
    public static void putCallInfo(Map<String, String> info) {
        callInfos.get().putAll(info);
    }

    @SuppressWarnings("unused")
    public static String getCallId() {
        return callInfos.get().get(CommonParameter.callId);
    }

    /**
     * 获取当前的call info
     * @return 当前的call info
     */
    public static Map<String, String> getCallInfo() {
        return new LinkedHashMap<>(callInfos.get());
    }

    /**
     * 获取当前执行链路的最初发起方的用户，即客户端访问网关时使用的utk中的uid，null表示未登录
     * @return 返回当前登录用户的userId
     */
    public static Long getCurrentUserId() {
        String s = callInfos.get().get(CommonParameter.userId);
        return s != null ? Long.parseLong(s) : null;
    }

    /**
     * 设置call info为指定的map，并同时设置MDC，用于透传cid
     * @param info 要设置的新的call info
     */
    public static void setCallInfoAndMDC(Map<String, String> info) {
        callInfos.set(new HashMap<>(info));
        String cid = info.get(CommonParameter.callId);
        if (cid != null) {
            MDC.put(CommonParameter.callId, cid);
        }
        String deviceId = info.get(CommonParameter.deviceId);
        if (deviceId != null) {
            MDC.put(CommonParameter.deviceId, deviceId);
        }
        String userId = info.get(CommonParameter.userId);
        if (userId != null) {
            MDC.put(CommonParameter.userId, userId);
        }
    }

    /**
     * 删除call info和mdc，用于透传cid
     */
    public static void clearCallInfoAndMDC() {
        callInfos.remove();
        MDC.remove(CommonParameter.callId);
        MDC.remove(CommonParameter.deviceId);
        MDC.remove(CommonParameter.userId);
    }

    public static void clearCallInfos() {
        callInfos.remove();
    }

    static void addNotifications(String key, String value) {
        if (logger.isDebugEnabled()) {
            logger.debug("add notification {} = {}", key, value);
        }

        notifications.get().put(key, value);
    }

    public static void mergeNotifications(Map<String, String> notifications, Map<String, String> more) {
        if (more == null || more.isEmpty() || notifications == more) {
            return;
        }

        for (Entry<String, String> entry : more.entrySet()) {
            if (ConstField.SERVICE_LOG.equals(entry.getKey())) {
                notifications.merge(entry.getKey(), entry.getValue(), (a, b) -> a + LOG_SPLITTER + b);
            } else {
                //其他类型进行覆盖
                notifications.put(entry.getKey(), entry.getValue());
            }
        }
    }

    //do value copy
    @SuppressWarnings("unused")
    public static void addNotifications(Map<String, String> rpcMap) {
        mergeNotifications(notifications.get(), rpcMap);
    }

    public static Map<String, String> getCurrentNotifications() {
        return notifications.get();
    }

    static String getValue(String key) {
        return notifications.get().get(key);
    }

    /**
     * 覆盖写入token信息 以及 stoken信息 和 stoken的过期时间
     */
    @SuppressWarnings("unused")
    public static void setCookieToken(String token, String stoken, int stkDuration) {
        if (token != null) {
            addNotifications(ConstField.SET_COOKIE_TOKEN, token);
        }
        if (stoken != null) {
            addNotifications(ConstField.SET_COOKIE_STOKEN, stoken + "|" + stkDuration);
        }
    }

    /**
     * 覆盖写入token信息 以及 stoken信息 和 stoken的过期时间
     */
    @SuppressWarnings("unused")
    public static void setCookieToken(String token, String stoken, int stkDuration, int appId) {
        if (token != null) {
            addNotifications(ConstField.SET_COOKIE_TOKEN, token + "|" + appId);
        }
        if (stoken != null) {
            addNotifications(ConstField.SET_COOKIE_STOKEN, stoken + "|" + stkDuration + "|" + appId);
        }
    }

    /**
     * 覆盖写入extension token 信息
     * @param etkDuration 单位: 秒
     */
    @SuppressWarnings("unused")
    public static void setExtensionToken(String extensionToken, int etkDuration) {
        if (extensionToken != null) {
            addNotifications(ConstField.SET_COOKIE_ETOKEN, extensionToken + "|" + etkDuration);
        }
    }

    /**
     * 覆盖写入extension token 信息
     * @param etkDuration 单位: 秒
     */
    @SuppressWarnings("unused")
    public static void setExtensionToken(String extensionToken, int etkDuration, int appId) {
        if (extensionToken != null) {
            addNotifications(ConstField.SET_COOKIE_ETOKEN, extensionToken + "|" + etkDuration + "|"+ appId);
        }
    }

    /**
     * 覆盖写入temp token
     * @param ttkDuration 单位: 秒
     */
    @SuppressWarnings("unused")
    public static void setTempToken(String tempToken, int ttkDuration) {
        if (tempToken != null) {
            addNotifications(ConstField.SET_COOKIE_TTOKEN, tempToken + "|" + ttkDuration);
        }
    }

    @SuppressWarnings("unused")
    public static void setOperatorCookie(String operator) {
        if (operator != null) {
            addNotifications(ConstField.SET_COOKIE_OPERATOR, operator);
        }
    }

    /**
     * 覆盖写入token信息 以及 stoken信息
     */
    @SuppressWarnings("unused")
    public static void setCookieToken(String token, String stoken) {
        if (token != null) {
            addNotifications(ConstField.SET_COOKIE_TOKEN, token);
        }
        if (stoken != null) {
            addNotifications(ConstField.SET_COOKIE_STOKEN, stoken);
        }
    }

    /**
     * 覆盖写入重定向URL
     */
    @SuppressWarnings("unused")
    public static void setRedirectUrl(String url) {
        if (url != null) {
            addNotifications(ConstField.REDIRECT_TO, url);
        }
    }

    @SuppressWarnings("unused")
    public static String getRedirectUrl() {
        return getValue(ConstField.REDIRECT_TO);
    }

    /**
     * 同步向客户端发送后台消息，此消息会被当前请求直接直接读取。
     * 若需要异步发送消息，请使用BackendMessageService
     */
    public static void sendBackendMessage(BackendMessageBody messageBody) {
        if (messageBody == null) {
            throw new NullPointerException("messageBody");
        }

        String tmp = getValue(ConstField.BACKEND_MESSAGE);
        if (tmp == null) {
            addNotifications(ConstField.BACKEND_MESSAGE, JSON.toJSONString(messageBody));
        } else if (tmp.length() > 1 << 20) {
            throw new IllegalArgumentException("message is already too large: " + tmp.length());
        } else {
            addNotifications(ConstField.BACKEND_MESSAGE, tmp + "," +
                    JSON.toJSONString(messageBody));
        }
    }

    public static void clearNotifications() {
        notifications.remove();
    }

    /**
     * 上报一些核心日志信息至api,日志长度不超过500字符
     */
    @SuppressWarnings("unused")
    public static void appendServiceLog(String log) {
        if (log != null) {
            String tmp = getValue(ConstField.SERVICE_LOG);
            if (tmp == null) {
                if (log.length() > 5000) {
                    logger.warn("append log failed ,length of log is large than 5000, actually length:{}", log.length());
                } else {
                    addNotifications(ConstField.SERVICE_LOG, log);
                }
            } else if (tmp.length() < 10000) {
                StringBuilder sb = new StringBuilder(tmp);
                sb.append(LOG_SPLITTER).append(log);
                if (log.length() > 5000) {
                    logger.warn("append log failed ,length of log is large than 5000, actually length:{}", log.length());
                } else {
                    addNotifications(ConstField.SERVICE_LOG, sb.toString());
                }
            } else {
                logger.warn("append log failed ,length of total service log is large than 10000, " +
                    "actually length:{}", tmp.length());
            }
        }
    }

    /**
     * 设置http头的ETag值
     */
    @SuppressWarnings("unused")
    public static void setEtag(String etag) {
        if (etag != null) {
            addNotifications(ConstField.NOTIFICATION_ETAG, etag);
        }
    }

    /**
     * 强制将网关的request的错误码设置为给定值
     * @param code 需要返回给客户端的request级别的错误码
     */
    @SuppressWarnings("unused")
    public static void setRequestErrorCode(int code) {
        addNotifications(ConstField.REQUEST_ERROR_CODE_EXT, String.valueOf(code));
    }

    /**
     * 对外暴露额外的可注入参数
     */
    public static void exportServiceData(ServiceInjectable.InjectionData data) {
        if (data != null) {
            String name = ConstField.SERVICE_PARAM_EXPORT_PREFIX + data.getName();
            String originDataText = getValue(name);
            if (originDataText != null) {
                ServiceInjectable.InjectionData origin = JSON.parseObject(originDataText, data.getClass());
                origin.batchMerge(data);
                data = origin;
            }
            addNotifications(name, JSON.toJSONString(data));
        }
    }
}
