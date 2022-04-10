package com.estatetrader.rule.zk;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public interface ResourceKeySupport {

    /**
     * encode the given key, since sometimes the provided key is not valid for a zookeeper node name.
     * @param key origin key
     * @return encoded key
     */
     default String encodeKey(String key) {
        try {
            return URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * encode the given key, since sometimes the provided key is not valid for a zookeeper node name.
     * @param key encoded key
     * @return origin key
     */
    default String decodeKey(String key) {
        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
