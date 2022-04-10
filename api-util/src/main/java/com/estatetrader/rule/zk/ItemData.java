package com.estatetrader.rule.zk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.Feature;
import com.estatetrader.functions.JsonSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * An item of the black list
 */
public class ItemData<T> implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemData.class);

    // note: these fields must be marked as public for the FASTJSON
    /**
     * optional data
     */
    public String data;

    /**
     * deserialize from data
     */
    public transient T value;

    /**
     * the time (in milliseconds from 1970) when the last time this item is put into
     */
    @SuppressWarnings("WeakerAccess")
    public long timestamp;

    /**
     * after how long in milliseconds this node will be removed (set to 0 to avoid expire)
     */
    @SuppressWarnings("WeakerAccess")
    public long timeToLive;

    public void refreshData() {
        if (value != null) {
            if (value instanceof JsonSerializable) {
                this.data = ((JsonSerializable) value).toJsonString();
            } else {
                this.data = JSON.toJSONString(value);
            }
        }
    }

    public byte[] serialize() {
        if (timeToLive == 0 && value == null) {
            return null; // 此时节点数据中没有任何有用信息，为了节省存储空间，返回null
        }

        return JSON.toJSONString(this).getBytes(StandardCharsets.UTF_8);
    }

    static <T> ItemData<T> deserialize(byte[] bytes, Class<T> dataType) {
        if (bytes == null || bytes.length == 0) {
            return new ItemData<>(); // 节点数据中没有数据意味着我们有一个空的ItemData
        }

        @SuppressWarnings("unchecked")
        ItemData<T> item = JSON.parseObject(new String(bytes, StandardCharsets.UTF_8), ItemData.class);
        if (item.data != null) {
            if (dataType == null) {
                throw new IllegalArgumentException("dataType is not set for " + item.data);
            }
            try {
                item.value = JSON.parseObject(item.data, dataType, Feature.SupportAutoType);
            } catch (JSONException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("failed to parse resource.item.value [" + item.data + "] to " + dataType, e);
                }
            }
        }

        return item;
    }
}
