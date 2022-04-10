package com.estatetrader;

import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.net.URL;
import java.util.Set;

/**
 * 提供与反射有关的搜索支持
 */
public interface Reflections {
    /**
     * 获取当前classpath下com.estatetrader 包下的反射工具
     */
    Reflections current = new ReflectionsImpl("com.estatetrader");

    /**
     * gets all sub types in hierarchy of a given type
     * <p/>depends on SubTypesScanner configured, otherwise an empty set is returned
     */
    <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type);

    class ReflectionsImpl implements Reflections {
        private final org.reflections.Reflections reflections;

        private ReflectionsImpl(String packageName) {
            Set<URL> urls = ClasspathHelper.forPackage(packageName,
                ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader());
            ConfigurationBuilder builder = new ConfigurationBuilder().setUrls(urls);
            this.reflections = new org.reflections.Reflections(builder);
        }

        /**
         * gets all sub types in hierarchy of a given type
         * <p/>depends on SubTypesScanner configured, otherwise an empty set is returned
         */
        @Override
        public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
            return reflections.getSubTypesOf(type);
        }
    }
}
