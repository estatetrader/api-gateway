package com.estatetrader.apigw.core.phases.parsing;

import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.annotation.ApiGroup;
import com.estatetrader.apigw.core.models.ApiMethodInfo;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarFile;

public interface ParsingJar {

    interface PrepareJarHandler {
        void prepareJarForParse(JarFile jf, String jarPath);
    }

    @Extension
    class DefaultPrepareJarHandler implements PrepareJarHandler {
        @Override
        public void prepareJarForParse(JarFile jf, String jarPath) {
            // nothing to do
        }
    }

    interface ParseJarHandler {
        void parseApiFromJar(JarFile jf,
                             String jarPath,
                             ClassLoader classLoader,
                             Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                             List<ApiMethodInfo> infoList);
    }

    @Extension(first = true)
    class ParseJarHandlerImpl implements ParseJarHandler {

        private final Extensions<ParsingClass.ParseClassHandler> parseClassHandlers;

        public ParseJarHandlerImpl(Extensions<ParsingClass.ParseClassHandler> parseClassHandlers) {
            this.parseClassHandlers = parseClassHandlers;
        }

        @Override
        public void parseApiFromJar(JarFile jf,
                                    String jarPath,
                                    ClassLoader classLoader,
                                    Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                                    List<ApiMethodInfo> infoList) {
            try {
                String type = jf.getManifest().getMainAttributes().getValue("Api-Dependency-Type");
                if (!"dubbo".equals(type)) return;

                String ns = jf.getManifest().getMainAttributes().getValue("Api-Export");
                if (ns == null) return;

                String[] names = ns.split(" +");
                for (String name : names) {
                    if (name.isEmpty()) continue;
                    try {
                        Class<?> clazz = classLoader.loadClass(name);
                        if (clazz.getAnnotation(ApiGroup.class) == null) continue;
                        List<ApiMethodInfo> infoListInClass = new LinkedList<>();
                        ServiceInstance serviceInstance = serviceInstanceGetter.apply(clazz);
                        parseClassHandlers.forEach(h -> h.parseApi(clazz, serviceInstance, jarPath, infoListInClass));
                        infoList.addAll(infoListInClass);
                    } catch (ClassNotFoundException | NoClassDefFoundError | IllegalApiDefinitionException e) {
                        throw new IllegalApiDefinitionException("parse api interface " + name + " failed: " + e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
