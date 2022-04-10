package com.estatetrader.algorithm;

import java.util.HashSet;
import java.util.Set;

/**
 * 一个能够自动根据时间淘汰元素的的集合
 * 每个元素在加入集合后的规定时间后会自动从集合中移除
 * 可用于实现会话功能
 *
 * 每个集合只能设置一个固定的淘汰时间，为了方便性能优化，淘汰时间由时间间隔和间隔数量决定
 */
public class TimedSet<T> {
    private final int interval;
    private final int period;
    private final long startTime;
    private final Set<T> set;
    private final Item<T>[] wheel;
    private long purgedTicks;

    public TimedSet(int interval, int period) {
        this.interval = interval;
        this.period = period;
        long timestamp = System.currentTimeMillis();
        this.startTime = timestamp;
        this.set = new HashSet<>();
        //noinspection unchecked
        this.wheel = new Item[period];
        this.purgedTicks = timestamp2ticks(timestamp) - period;
    }

    public synchronized boolean add(T value) {
        purge();

        if (!set.add(value)) {
            return false;
        }

        int index = (int) (currentTicks() % period);
        wheel[index] = new Item<>(value, wheel[index]);
        return true;
    }

    private void purge() {
        long expiredTicks = currentTicks() - period;
        for (long t = Math.max(purgedTicks, expiredTicks - period) + 1; 0 < t && t <= expiredTicks; t++) {
            int index = (int) (t % period);
            for (Item<T> p = wheel[index]; p != null; p = p.next) {
                set.remove(p.value);
            }
            wheel[index] = null;
        }
        purgedTicks = expiredTicks;
    }

    private long currentTicks() {
        return timestamp2ticks(System.currentTimeMillis());
    }

    private long timestamp2ticks(long timestamp) {
        return (timestamp - startTime) / interval;
    }

    private static class Item<T> {
        final T value;
        final Item<T> next;

        public Item(T value, Item<T> next) {
            this.value = value;
            this.next = next;
        }
    }
}
