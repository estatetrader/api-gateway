package com.estatetrader.apigw.core.phases.parsing;

import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarFile;

@Service
public class ApiParser {

    private static final Logger logger = LoggerFactory.getLogger(ApiParser.class);

    private final Extensions<CommonInfoParser> commonInfoParsers;
    private final Extensions<ParsingJar.PrepareJarHandler> prepareJarHandlers;
    private final Extensions<ParsingJar.ParseJarHandler> parseJarHandlers;
    private final Extensions<ParsingClass.ParseClassHandler> parseClassHandlers;
    private final Extensions<ApiRegister> registers;
    private final Extensions<SchemaProcessor> processors;
    private final Extensions<ApiVerifier> verifiers;

    public ApiParser(Extensions<CommonInfoParser> commonInfoParsers,
                     Extensions<ParsingJar.PrepareJarHandler> prepareJarHandlers,
                     Extensions<ParsingJar.ParseJarHandler> parseJarHandlers,
                     Extensions<ParsingClass.ParseClassHandler> parseClassHandlers,
                     Extensions<ApiRegister> registers,
                     Extensions<SchemaProcessor> processors,
                     Extensions<ApiVerifier> verifiers) {
        this.commonInfoParsers = commonInfoParsers;
        this.prepareJarHandlers = prepareJarHandlers;
        this.parseJarHandlers = parseJarHandlers;
        this.parseClassHandlers = parseClassHandlers;
        this.registers = registers;
        this.processors = processors;
        this.verifiers = verifiers;
    }

    @SuppressWarnings("unused")
    public void parseJars(ApiSchema schema,
                          Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                          List<String> jarFilePaths,
                          ClassLoader classLoader) {
        parseCommonInfo(schema);
        register(parseJars(serviceInstanceGetter, jarFilePaths, classLoader), schema);
        process(schema);
        verify(schema);
    }

    public void parseClasses(ApiSchema schema,
                             String jarFile,
                             Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                             Class<?> ... classes) {
        parseClasses(schema, jarFile, serviceInstanceGetter, Arrays.asList(classes));
    }

    public void parseClasses(ApiSchema schema,
                             String jarFile,
                             Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                             List<Class<?>> classes) {
        parseCommonInfo(schema);
        register(parseClasses(jarFile, serviceInstanceGetter, classes), schema);
        process(schema);
        verify(schema);
    }

    public void parseCommonInfo(ApiSchema schema) {
        for (CommonInfoParser p : commonInfoParsers) {
            p.parse(schema);
        }
    }

    public List<ApiMethodInfo> parseClasses(String jarFile,
                                            Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                                            Class<?> ... classes) {
        return parseClasses(jarFile, serviceInstanceGetter, Arrays.asList(classes));
    }

    public List<ApiMethodInfo> parseJars(Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                                         List<String> jarFilePaths,
                                         ClassLoader classLoader) {

        List<ApiMethodInfo> list = new ArrayList<>();
        List<String> errors = new LinkedList<>();

        for (String jarFilePath : jarFilePaths) {
            try {
                try(JarFile jf = new JarFile(jarFilePath)) {
                    prepareJarHandlers.forEach(h -> h.prepareJarForParse(jf, jarFilePath));
                    List<ApiMethodInfo> infoListInJar = new LinkedList<>();

                    parseJarHandlers.forEach(h ->
                        h.parseApiFromJar(jf, jarFilePath, classLoader, serviceInstanceGetter, infoListInJar));
                    list.addAll(infoListInJar);
                }
            } catch (Exception e) {
                String error = "Failed to load jar " + new File(jarFilePath).getName() + ": " + e.getMessage();
                logger.error(error, e);
                errors.add(error);
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalApiDefinitionException(String.join("\n", errors));
        }

        return list;
    }

    public List<ApiMethodInfo> parseClasses(String jarFile,
                                            Function<Class<?>, ServiceInstance> serviceInstanceGetter,
                                            List<Class<?>> classes) {
        List<ApiMethodInfo> list = new ArrayList<>();

        for (Class<?> clazz : classes) {
            ServiceInstance serviceInstance = serviceInstanceGetter.apply(clazz);
            parseClassHandlers.forEach(h -> h.parseApi(clazz, serviceInstance, jarFile, list));
        }

        return list;
    }

    public void register(List<ApiMethodInfo> infoList, ApiSchema schema) {
        registers.forEach(r -> r.register(infoList, schema));
    }

    public void process(ApiSchema schema) {
        processors.forEach(p -> p.process(schema));
    }

    public void verify(ApiSchema schema) {
        verifiers.forEach(v -> v.verify(schema));
    }
}
