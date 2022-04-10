package com.estatetrader.apigw.core.models;

import com.estatetrader.apigw.core.contracts.ApiCallExecutor;
import com.estatetrader.define.ApiCallCallback;

import java.lang.reflect.*;
import java.util.Map;

public class ServiceProxy {

    private final Map<Method, ApiMethodInfo> methodMapping;
    private final Constructor<?> proxyCreator;

    public ServiceProxy(Class<?> serviceType, Map<Method, ApiMethodInfo> methodMapping) {
        this.methodMapping = methodMapping;
        Class<?> proxyType = Proxy.getProxyClass(serviceType.getClassLoader(), serviceType);
        try {
            this.proxyCreator = proxyType.getDeclaredConstructor(InvocationHandler.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object getProxy(ApiCallExecutor executor, ApiCallCallback<Object> callback) {
        try {
            return proxyCreator.newInstance(new Handler(executor, callback));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private class Handler implements InvocationHandler {

        ApiCallExecutor executor;
        ApiCallCallback<Object> callback;

        Handler(ApiCallExecutor executor, ApiCallCallback<Object> callback) {
            this.executor = executor;
            this.callback = callback;
        }

        /**
         * Processes a method invocation on a proxy instance and returns
         * the result.  This method will be invoked on an invocation handler
         * when a method is invoked on a proxy instance that it is
         * associated with.
         *
         * @param proxy  the proxy instance that the method was invoked on
         * @param method the {@code Method} instance corresponding to
         *               the interface method invoked on the proxy instance.  The declaring
         *               class of the {@code Method} object will be the interface that
         *               the method was declared in, which may be a superinterface of the
         *               proxy interface that the proxy class inherits the method through.
         * @param args   an array of objects containing the values of the
         *               arguments passed in the method invocation on the proxy instance,
         *               or {@code null} if interface method takes no arguments.
         *               Arguments of primitive types are wrapped in instances of the
         *               appropriate primitive wrapper class, such as
         *               {@code java.lang.Integer} or {@code java.lang.Boolean}.
         * @return the value to return from the method invocation on the
         * proxy instance.  If the declared return type of the interface
         * method is a primitive type, then the value returned by
         * this method must be an instance of the corresponding primitive
         * wrapper class; otherwise, it must be a type assignable to the
         * declared return type.  If the value returned by this method is
         * {@code null} and the interface method's return type is
         * primitive, then a {@code NullPointerException} will be
         * thrown by the method invocation on the proxy instance.  If the
         * value returned by this method is otherwise not compatible with
         * the interface method's declared return type as described above,
         * a {@code ClassCastException} will be thrown by the method
         * invocation on the proxy instance.
         * @throws Throwable the exception to throw from the method
         *                   invocation on the proxy instance.  The exception's type must be
         *                   assignable either to any of the exception types declared in the
         *                   {@code throws} clause of the interface method or to the
         *                   unchecked exception types {@code java.lang.RuntimeException}
         *                   or {@code java.lang.Error}.  If a checked exception is
         *                   thrown by this method that is not assignable to any of the
         *                   exception types declared in the {@code throws} clause of
         *                   the interface method, then an
         *                   {@link UndeclaredThrowableException} containing the
         *                   exception that was thrown by this method will be thrown by the
         *                   method invocation on the proxy instance.
         * @see UndeclaredThrowableException
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            ApiMethodInfo methodInfo = methodMapping.get(method);
            if (methodInfo == null) {
                throw new UnsupportedOperationException("method " + method + " is not allowed to invoke");
            }

            if (executor == null) {
                throw new IllegalStateException("api call executor is not set");
            }

            ApiMethodCall call = new ApiMethodCall(methodInfo);
            executor.start(call, args, callback);
            return null;
        }
    }
}
