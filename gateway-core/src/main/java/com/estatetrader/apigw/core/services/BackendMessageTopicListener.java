package com.estatetrader.apigw.core.services;

import com.estatetrader.gateway.backendmsg.BackendMessageConditionMatcher;
import com.estatetrader.common.utils.ServiceInfo;
import com.estatetrader.common.utils.redis.RedisPool;
import com.estatetrader.common.utils.redis.RedisQueueService;
import com.estatetrader.common.utils.redis.RedisSerializableValueTypes;
import com.estatetrader.common.utils.redis.RedisTopicListener;
import com.estatetrader.common.utils.redis.RedisValueTypes;
import com.estatetrader.common.utils.serializer.HessionSerializer;
import com.estatetrader.gateway.backendmsg.PolledBackendMessage;
import com.estatetrader.gateway.backendmsg.QueuedBackendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BackendMessageTopicListener implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendMessageTopicListener.class);

    private static final int MAX_POLL_LIMIT = 100;

    private final RedisTopicListener<String, QueuedBackendMessage> listenerForUserId;
    private final RedisTopicListener<String, QueuedBackendMessage> listenerForDeviceId;
    private final Executor executor;

    public BackendMessageTopicListener(RedisPool redisPool) {
        RedisValueTypes defaultValueTypes = new RedisSerializableValueTypes(new HessionSerializer());

        RedisQueueService topicService = ServiceInfo.withServiceName(
            "api-gateway",
            () -> new RedisQueueService(redisPool, defaultValueTypes)
        );

        this.listenerForUserId = topicService.topicListener(
            "backend-messages-for-user-id", QueuedBackendMessage.class
        );
        this.listenerForDeviceId = topicService.topicListener(
            "backend-messages-for-device-id", QueuedBackendMessage.class
        );
        this.executor = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("backend-message-poller");
            return t;
        });
    }

    public void start() {
        listenerForUserId.start();
        listenerForDeviceId.start();
    }

    /**
     * 获取指定的userId或deviceId的消息列表
     * @param userId 不为空表示需要拉取指定userId的消息
     * @param deviceId 不为空表示同时也需要拉取指定deviceId的消息
     * @param matcher 如果要读取的消息指定了condition，则使用此谓词来判断是否可以读取此消息，返回true表示可以读取此消息
     * @return 发给指定用户/设备的消息列表
     */
    public CompletableFuture<List<PolledBackendMessage>> poll(Long userId, Long deviceId, BackendMessageConditionMatcher matcher) {
        CompletableFuture<List<PolledBackendMessage>> forUid = doAsyncPoll(listenerForUserId, userId, matcher);
        CompletableFuture<List<PolledBackendMessage>> forDid = doAsyncPoll(listenerForDeviceId, deviceId, matcher);
        if (forUid != null) {
            if (forDid != null) {
                return CompletableFuture.allOf().thenApply(v -> {
                    List<PolledBackendMessage> result = new ArrayList<>();
                    try {
                        result.addAll(forUid.get());
                    } catch (Exception e) {
                        LOGGER.error("failed to poll the backend messages for user {}", userId, e);
                    }
                    try {
                        result.addAll(forDid.get());
                    } catch (Exception e) {
                        LOGGER.error("failed to poll the backend messages for device {}", deviceId, e);
                    }
                    return result;
                });
            } else {
                return forUid;
            }
        } else if (forDid != null) {
            return forDid;
        } else {
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        listenerForUserId.close();
        listenerForDeviceId.close();
    }

    private CompletableFuture<List<PolledBackendMessage>> doAsyncPoll(RedisTopicListener<String, QueuedBackendMessage> receiver,
                                                                      Long subjectId,
                                                                      BackendMessageConditionMatcher matcher) {
        if (subjectId == null || receiver.isTopicEmpty(subjectId)) {
            return null;
        } else {
            return CompletableFuture.supplyAsync(() -> doPoll(receiver, subjectId, matcher), executor);
        }
    }

    private List<PolledBackendMessage> doPoll(RedisTopicListener<String, QueuedBackendMessage> receiver,
                                              long subjectId,
                                              BackendMessageConditionMatcher matcher) {
        long now = System.currentTimeMillis();
        return receiver.poll(subjectId, filterMessage(matcher), MAX_POLL_LIMIT)
            .values()
            .stream()
            .filter(Objects::nonNull)
            .filter(m -> {
                if (m.expiredAt < now) {
                    LOGGER.warn("the backend message {} has expired before delivery", m.messageKey);
                    return false;
                } else {
                    LOGGER.debug("backend message {} polled to deliver: {}", m.messageKey, m);
                    return true;
                }
            })
            .map(msg -> new PolledBackendMessage(msg.messageKey, msg.body))
            .collect(Collectors.toList());
    }

    private Predicate<Map.Entry<String, QueuedBackendMessage>> filterMessage(BackendMessageConditionMatcher matcher) {
        return entry -> {
            QueuedBackendMessage message = entry.getValue();
            return message == null || matcher.match(message.condition);
        };
    }
}
