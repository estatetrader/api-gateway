package com.estatetrader.algorithm;

import java.util.Arrays;

/**
 * a simpler implementation of list for long integer
 */
public class LongList {

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private long[] data;
    private int len;

    public LongList() {
        this(0);
    }

    public LongList(int capacity) {
        this.data = new long[capacity];
    }

    public void add(long l) {
        ensureCapacity(1);
        data[len++] = l;
    }

    public void addAll(long[] ls) {
        ensureCapacity(ls.length);
        System.arraycopy(ls, 0, data, len, ls.length);
        len += ls.length;
    }

    public long[] toArray() {
        if (len == data.length) {
            return data;
        } else {
            return Arrays.copyOf(data, len);
        }
    }

    public void ensureCapacity(int delta) {
        if (len + delta > data.length) {
            grow(len + delta);
        }
    }

    private void grow(int minCapacity) {
        int oldCapacity = data.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        if (newCapacity > MAX_ARRAY_SIZE) {
            newCapacity = MAX_ARRAY_SIZE;
        }
        data = Arrays.copyOf(data, newCapacity);
    }
}
