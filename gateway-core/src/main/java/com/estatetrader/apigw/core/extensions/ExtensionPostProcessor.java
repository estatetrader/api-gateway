package com.estatetrader.apigw.core.extensions;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ExtensionPostProcessor implements BeanFactoryPostProcessor {
    /**
     * Modify the application context's internal bean factory after its standard
     * initialization. All bean definitions will have been loaded, but no beans
     * will have been instantiated yet. This allows for overriding or adding
     * properties even to eager-initializing beans.
     *
     * @param beanFactory the bean factory used by the application context
     * @throws BeansException in case of errors
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<Class<?>> toRemove = new ArrayList<>();
        List<String> defaults = new ArrayList<>();

        String[] names = beanFactory.getBeanNamesForAnnotation(Extension.class);
        for (String name : names) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            if (bd instanceof AbstractBeanDefinition) {
                AbstractBeanDefinition abd = ((AbstractBeanDefinition) bd);
                if (!abd.hasBeanClass()) {
                    continue;
                }

                Class<?> beanClass = abd.getBeanClass();
                Extension an = beanClass.getAnnotation(Extension.class);
                if (an != null) {
                    if (an.replace().length > 0) {
                        toRemove.addAll(Arrays.asList(an.replace()));
                    }

                    if (an.defaults()) {
                        defaults.add(name);
                    }
                }
            }
        }

        for (Class<?> c : toRemove) {
            String[] namesToRemove = beanFactory.getBeanNamesForType(c);
            for (String name : namesToRemove) {
                BeanDefinition bd = beanFactory.getBeanDefinition(name);
                bd.setLazyInit(true);
                bd.setAutowireCandidate(false);
            }
        }

        for (String name : defaults) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            if (bd instanceof AbstractBeanDefinition) {
                AbstractBeanDefinition abd = ((AbstractBeanDefinition) bd);
                if (!abd.hasBeanClass()) {
                    continue;
                }

                Class<?> beanClass = abd.getBeanClass();
                Class<?>[] interfaces = beanClass.getInterfaces();
                if (interfaces.length != 1) {
                    throw new IllegalArgumentException("all default Extension must implement " +
                        "one and only one interface");
                }

                String[] allNames = beanFactory.getBeanNamesForType(interfaces[0]);
                if (allNames.length > 1) {
                    bd.setLazyInit(true);
                    bd.setAutowireCandidate(false);
                }
            }
        }
    }
}
