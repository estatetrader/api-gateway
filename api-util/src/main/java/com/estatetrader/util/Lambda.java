package com.estatetrader.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class Lambda {
    public static <T> boolean contains(T[] a, T x) {
        for (T t : a) {
            if (Objects.equals(t, x)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T find(T[] a, Predicate<T> p) {
        for (T t : a) {
            if (p.test(t)) {
                return t;
            }
        }
        return null;
    }

    public static <T> T find(Collection<T> a, Predicate<T> p) {
        for (T t : a) {
            if (p.test(t)) {
                return t;
            }
        }
        return null;
    }

    public static <T> List<T> filter(T[] a, Predicate<T> p) {
        List<T> result = new ArrayList<>(a.length);
        for (T t : a) {
            if (p.test(t)) {
                result.add(t);
            }
        }
        return result;
    }

    public static <T> List<T> filter(Collection<T> a, Predicate<T> p) {
        List<T> result = new ArrayList<>(a.size());
        for (T t : a) {
            if (p.test(t)) {
                result.add(t);
            }
        }
        return result;
    }

    public static <T> String toString(Collection<T> c) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (T t : c) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(t);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String join(String dem, Collection<?> c) {
        List<String> list = new ArrayList<>(c.size());
        for (Object x : c) {
            list.add(String.valueOf(x));
        }
        return String.join(dem, list);
    }

    public static <T> String join(String dem, T[] c) {
        List<String> list = new ArrayList<>(c.length);
        for (Object x : c) {
            list.add(String.valueOf(x));
        }
        return String.join(dem, list);
    }

    public static <T> boolean any(Collection<T> c, Predicate<T> f) {
        for (T t : c) {
            if (f.test(t)) return true;
        }
        return false;
    }

    public static <T> boolean all(Collection<T> c, Predicate<T> f) {
        for (T t : c) {
            if (!f.test(t)) return false;
        }
        return true;
    }

    /**
     * compare two methods with their names and signatures
     * @param m1 method 1 to compare
     * @param m2 method 2 to compare
     * @return return true if equals
     */
    public static boolean equals(Method m1, Method m2) {
        if (!Objects.equals(m1.getName(), (m2.getName())))
            return false;

        if (!Objects.equals(m1.getReturnType(), m2.getReturnType()))
            return false;

        return Arrays.equals(m1.getParameterTypes(), m2.getParameterTypes());
    }

    public static <T, R> List<R> map(Collection<T> c, Function<T, R> f) {
        List<R> list = new ArrayList<>(c.size());
        for (T x : c) {
            list.add(f.apply(x));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> R[] map(T[] c, Function<T, R> f) {
        Object[] a = new Object[c.length];
        for (int i = 0; i < c.length; i++) {
            a[i] = f.apply(c[i]);
        }
        return (R[]) a;
    }

    public static <K, V, R> List<R> map(Map<K, V> m, BiFunction<K, V, R> f) {
        List<R> list = new ArrayList<>(m.size());
        for (K k : m.keySet()) {
            list.add(f.apply(k, m.get(k)));
        }
        return list;
    }

    public static <K, V, R> Map<K, R> mapValues(Map<K, V> m, Function<V, R> f) {
        Map<K, R> map = new HashMap<>(m.size());
        for (Map.Entry<K, V> entry : m.entrySet()) {
            map.put(entry.getKey(), f.apply(entry.getValue()));
        }
        return map;
    }

    public static <T> void foreach(Iterator<T> iterator, Consumer<T> consumer) {
        while (iterator.hasNext()) {
            consumer.accept(iterator.next());
        }
    }

    public static <T, R extends Comparable<R>> List<T> orderBy(Collection<T> iterator, Function<T, R> keyExtractor) {
        List<T> list = new ArrayList<>(iterator);
        list.sort(Comparator.comparing(keyExtractor));
        return list;
    }

    public static <T> int count(Collection<T> a, Predicate<T> p) {
        int c = 0;
        for (T t : a) {
            if (p.test(t)) {
                c++;
            }
        }
        return c;
    }

    public static <T> int count(T[] a, Predicate<T> p) {
        int c = 0;
        for (T t : a) {
            if (p.test(t)) {
                c++;
            }
        }
        return c;
    }

    public static <K, V> Map<K, V> toMap(K k, V v) {
        Map<K, V> map = new HashMap<>(1);
        map.put(k, v);
        return map;
    }

    public static <K, V> Map<K, V> toMap(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>(1);
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static <K, V> Map<K, V> toMap(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>(1);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static <K, V> Map<K, V> toMap(Consumer<Map<K, V>> producer) {
        Map<K, V> map = new HashMap<>();
        producer.accept(map);
        return map;
    }

    public static <T> Set<T> toSet(Consumer<Set<T>> producer) {
        Set<T> set = new HashSet<>();
        producer.accept(set);
        return set;
    }

    public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getFieldValue(Field field, Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T first(Iterable<T> iterable) {
        Iterator<T> it = iterable.iterator();
        if (it.hasNext()) {
            return it.next();
        }
        throw new NoSuchElementException();
    }

    public static <T> List<T> concat(Collection<T> c1, Collection<T> c2) {
        List<T> list = new ArrayList<>(c1.size() + c2.size());
        list.addAll(c1);
        list.addAll(c2);
        return list;
    }

    public static <T> List<T> prepend(T x, Collection<T> c1) {
        List<T> list = new ArrayList<>(c1.size() + 1);
        list.add(x);
        list.addAll(c1);
        return list;
    }

    public static <T> List<T> append(Collection<T> c1, T x) {
        List<T> list = new ArrayList<>(c1.size() + 1);
        list.addAll(c1);
        list.add(x);
        return list;
    }

    public static Object prependArray(Object x, Object a) {
        int al = Array.getLength(a);
        Object c = Array.newInstance(a.getClass().getComponentType(),  al+ 1);
        Array.set(a, 0, x);
        System.arraycopy(a, 0, c, 1, al);
        return c;
    }

    public static Object appendArray(Object a, Object x) {
        int al = Array.getLength(a);
        Object c = Array.newInstance(a.getClass().getComponentType(),  al+ 1);
        System.arraycopy(a, 0, c, 0, al);
        Array.set(c, al, x);
        return c;
    }

    public static Object concatArray(Object a, Object b) {
        Class<?> clazz = a.getClass().getComponentType();
        if (clazz != b.getClass().getComponentType()) {
            throw new IllegalArgumentException("array component type does not match: " + clazz + " <> " + b.getClass().getComponentType());
        }
        int al = Array.getLength(a), bl = Array.getLength(b);
        Object c = Array.newInstance(a.getClass().getComponentType(), al + bl);
        System.arraycopy(a, 0, c, 0, al);
        System.arraycopy(b, 0, c, al, bl);

        return c;
    }

    public static byte[] concatBytes(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static List<?> array2list(Object a) {
        if (a == null) {
            return null;
        }
        int len = Array.getLength(a);
        List<Object> list = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            list.add(Array.get(a, i));
        }
        return list;
    }

    public static <T> T cascade(T t1, T t2) {
        return t1 == null ? t2 : t1;
    }

    public static String cascade(String ... a) {
        for (String s : a) {
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    public static <T, R> List<R> collectMany(List<T> c, Function<T, Collection<R>> f) {
        List<R> list = new ArrayList<>(c.size());
        for (T t : c) {
            list.addAll(f.apply(t));
        }
        return list;
    }

    public static Field getDeclaredField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(fieldName, e);
        }
    }

    public static <T> Iterator<T> toIterator(Enumeration<T> source) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return source.hasMoreElements();
            }

            @Override
            public T next() {
                return source.nextElement();
            }
        };
    }

    public static <T> Iterable<T> toIterable(Enumeration<T> source) {
        return () -> toIterator(source);
    }

    public static <T> void with(T target, Consumer<T> visitor) {
        visitor.accept(target);
    }

    /**
     * 获取一个由a中的元素组成的集合，但是这些元素均不在b中出现
     * @param a 集合
     * @param b 集合
     * @param <T> 类型
     * @return 差集
     */
    public static <T> List<T> except(Collection<T> a, Collection<T> b) {
        List<T> result = new ArrayList<>(a.size());
        if (a.size() > 100 && b.size() > 100) {
            Set<T> s = new HashSet<>(b);
            for (T t : a) {
                if (!s.contains(t)) {
                    result.add(t);
                }
            }
        } else {
            for (T t : a) {
                if (!b.contains(t)) {
                    result.add(t);
                }
            }
        }

        return result;
    }
}
