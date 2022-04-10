package com.estatetrader.apigw.core.extensions;

import com.estatetrader.algorithm.Graph;
import com.estatetrader.util.Lambda;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 所有使用插件模式的代码都应使用此类注入特定插件接口的实现类实例列表
 * @param <T> 要注入的插件实例的接口类型
 */
public interface Extensions<T> extends Collection<T> {

    default void apply(Consumer<T> consumer) {
        for (T t : this) consumer.accept(t);
    }

    default <R> List<R> collect(Function<T, R> fun) {
        List<R> result = new ArrayList<>(this.size());
        for (T t : this) result.add(fun.apply(t));
        return result;
    }

    default T singleton() {
        Iterator<T> iter = iterator();
        if (iter.hasNext()) {
            T t = iter.next();
            if (iter.hasNext()) {
                throw new IllegalArgumentException("more then one extension implementation are found " +
                    "for the specified extension");
            }
            return t;
        } else {
            throw new IllegalArgumentException("could not find the specified extension");
        }
    }

    default <R, E extends Throwable> Next<R, E> chain(ChainMethod<T, R, E> method) {
        return new NextImpl<>(this, method::apply);
    }

    default <E extends Throwable> Next.NoResult<E> chain(ChainMethod.NoResult<T, E> method) {
        return new NextNoResultImpl<>(this, method::apply);
    }

    default <P1, R, E extends Throwable> Next<R, E> chain(
        ChainMethod1<T, P1, R, E> method,
        P1 param1) {

        return new NextImpl<>(this, (t, n) -> method.apply(t, param1, n));
    }

    default <P1, E extends Throwable> Next.NoResult<E> chain(
        ChainMethod1.NoResult<T, P1, E> method,
        P1 param1) {

        return new NextNoResultImpl<>(this, (t, n) -> method.apply(t, param1, n));
    }

    default <P1, P2, R, E extends Throwable> Next<R, E> chain(
        ChainMethod2<T, P1, P2, R, E> method,
        P1 param1, P2 param2) {

        return new NextImpl<>(this, (t, n) -> method.apply(t, param1, param2, n));
    }

    default <P1, P2, E extends Throwable> Next.NoResult<E> chain(
        ChainMethod2.NoResult<T, P1, P2, E> method, P1 param1, P2 param2) {
        return new NextNoResultImpl<>(this, (t, n) -> method.apply(t, param1, param2, n));
    }

    default <P1, P2, P3, R, E extends Throwable> Next<R, E> chain(
        ChainMethod3<T, P1, P2, P3, R, E> method,
        P1 param1, P2 param2, P3 param3) {

        return new NextImpl<>(this, (t, n) -> method.apply(t, param1, param2, param3, n));
    }

    default <P1, P2, P3, E extends Throwable> Next.NoResult<E> chain(
        ChainMethod3.NoResult<T, P1, P2, P3, E> method,
        P1 param1, P2 param2, P3 param3) {

        return new NextNoResultImpl<>(this, (t, n) -> method.apply(t, param1, param2, param3, n));
    }

    default <P1, P2, P3, P4, R, E extends Throwable> Next<R, E> chain(
        ChainMethod4<T, P1, P2, P3, P4, R, E> method,
        P1 param1, P2 param2, P3 param3, P4 param4) {

        return new NextImpl<>(this, (t, n) -> method.apply(t, param1, param2, param3, param4, n));
    }

    default <P1, P2, P3, P4, E extends Throwable> Next.NoResult<E> chain(
        ChainMethod4.NoResult<T, P1, P2, P3, P4, E> method,
        P1 param1, P2 param2, P3 param3, P4 param4) {

        return new NextNoResultImpl<>(this, (t, n) -> method.apply(t, param1, param2, param3, param4, n));
    }

    default <P1, P2, P3, P4, P5, R, E extends Throwable> Next<R, E> chain(
        ChainMethod5<T, P1, P2, P3, P4, P5, R, E> method,
        P1 param1, P2 param2, P3 param3, P4 param4, P5 param5) {

        return new NextImpl<>(this, (t, n) -> method.apply(t, param1, param2, param3, param4, param5, n));
    }

    default <P1, P2, P3, P4, P5, E extends Throwable> Next.NoResult<E> chain(
        ChainMethod5.NoResult<T, P1, P2, P3, P4, P5, E> method,
        P1 param1, P2 param2, P3 param3, P4 param4, P5 param5) {

        return new NextNoResultImpl<>(this, (t, n) -> method.apply(t, param1, param2, param3, param4, param5, n));
    }

    /**
     * 默认实现类
     * @param <T>
     */
    class ExtensionsImpl<T> extends AbstractCollection<T> implements Extensions<T> {
        public final List<T> list;

        public ExtensionsImpl(Collection<T> list) {
            this.list = sort(list);
        }

        /**
         * Returns an iterator over the elements contained in this collection.
         *
         * @return an iterator over the elements contained in this collection
         */
        @Override
        public Iterator<T> iterator() {
            return list.iterator();
        }

        @Override
        public int size() {
            return list.size();
        }

        static <T> List<T> sort(Collection<T> list) {
            return createExtensionGraph(list).topology();
        }

        private static <T> Graph<T, T> createExtensionGraph(Collection<T> list) {
            Graph<T, T> graph = new Graph<>();
            for (T t : list) {
                graph.node(t, t);
            }
            processDependencies(list, graph);
            return graph;
        }

        private static <T> void processDependencies(Collection<T> list, Graph<T, T> graph) {
            for (T a : list) {
                for (T b : list) {
                    if (hasDependency(a, b)) {
                        graph.arc(b, a);
                    }
                }
            }

            processFirst(list, graph);
            processLast(list, graph);
        }

        private static <T> void processFirst(Collection<T> list, Graph<T, T> graph) {
            T first = null;

            for (T a : list) {
                Extension e = a.getClass().getAnnotation(Extension.class);
                // add @First annotated class to the head of the list and ensure there is no more @First annotated classes.
                if (e != null && e.first()) {
                    if (first != null) {
                        throw new InvalidExtensionException("Duplicate First found on class " + a.getClass());
                    }
                    first = a;
                }
            }

            // make dependencies from every nodes to the first node
            if (first != null) {
                for (T a : list) {
                    if (a != first) {
                        graph.arc(first, a);
                    }
                }
            }
        }

        private static <T> void processLast(Collection<T> cs, Graph<T, T> graph) {
            T last = null;

            for (T a : cs) {
                Extension e = a.getClass().getAnnotation(Extension.class);
                if (e != null && e.last()) {
                    if (last != null) throw new InvalidExtensionException("Duplicate @Last found on class " + a.getClass());
                    last = a;
                }
            }

            // make dependencies from every nodes to the last node
            if (last != null) {
                for (T t : cs) {
                    if (t != last) {
                        graph.arc(t, last);
                    }
                }
            }
        }

        private static <T> boolean hasDependency(T a, T b) {
            Extension ea = a.getClass().getAnnotation(Extension.class);
            if (ea != null && containedInEnclosingClasses(ea.after(), b.getClass())) {
                return true;
            }

            Extension eb = b.getClass().getAnnotation(Extension.class);
            if (eb != null && containedInEnclosingClasses(eb.before(), a.getClass())) {
                return true;
            }

            return false;
        }

        private static boolean containedInEnclosingClasses(Class<?>[] classes, Class<?> clazz) {
            for (Class<?> c = clazz; c != null; c = c.getEnclosingClass()) {
                if (Lambda.contains(classes, c)) {
                    return true;
                }
            }
            return false;
        }
    }

    class NextImpl<T, R, E extends Throwable> implements Next<R, E> {

        private final Iterator<T> extensions;
        private final Chain<T, R, E> chain;

        public NextImpl(Iterable<T> extensions, Chain<T, R, E> chain) {
            this(extensions.iterator(), chain);
        }

        public NextImpl(Iterator<T> extensions, Chain<T, R, E> chain) {
            this.extensions = extensions;
            this.chain = chain;
        }

        private R previousResult;

        @Override
        public R go() throws E {
            if (extensions.hasNext()) {
                return chain.apply(extensions.next(), this);
            }
            return previousResult;
        }

        @Override
        public R go(R result) throws E {
            this.previousResult = result;
            return go();
        }

        @Override
        public R previousResult() {
            return previousResult;
        }
    }

    class NextNoResultImpl<T, E extends Throwable> implements Next.NoResult<E> {

        private final Iterator<T> extensions;
        private final Chain.NoResult<T, E> chain;

        public NextNoResultImpl(Iterable<T> extensions, Chain.NoResult<T, E> chain) {
            this.extensions = extensions.iterator();
            this.chain = chain;
        }

        @Override
        public void go() throws E {
            if (extensions.hasNext()) {
                chain.apply(extensions.next(), this);
            }
        }
    }
}
