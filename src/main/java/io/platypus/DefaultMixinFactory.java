package io.platypus;

import io.platypus.utils.Preconditions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.reflect.TypeToken;

public class DefaultMixinFactory implements MixinFactory {

    private class MixinInvocationHandler implements InvocationHandler {

        private final Map<Method, Object> methodsToHandlers;
        private final Object proxy;

        public MixinInvocationHandler() {
            this.proxy = Proxy.newProxyInstance(classLoader, intfs, this);
            this.methodsToHandlers = getMethodsToHandlers();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Preconditions.checkArgument(proxy == this.proxy);

            Object handler = methodsToHandlers.get(method);
            Preconditions.checkNotNull(handler, "Provider for %s didn't provide an handler object", method);

            if (handler instanceof InvocationHandler) {
                return ((InvocationHandler) handler).invoke(proxy, method, args);
            }
            else {
                return method.invoke(handler, args);
            }
        }

        @SuppressWarnings("unchecked")
        protected <T> T getProxy(Class<T> clazz) {
            Preconditions.checkNotNull(clazz);
            Preconditions.checkArgument(clazz.isInstance(proxy), "Proxy does not implements %s", clazz);
            return (T) proxy;
        }

        @SuppressWarnings("unchecked")
        protected <T> T getProxy(TypeToken<T> typeToken) {
            Preconditions.checkNotNull(typeToken);
            Preconditions.checkArgument(typeToken.getRawType().isInstance(proxy), "Proxy does not implements %s", typeToken);
            return (T) proxy;
        }

        private Map<Method, Object> getMethodsToHandlers() {
            final Map<InstanceProvider<?>, Object> providerToHandlers = new HashMap<InstanceProvider<?>, Object>();
            final Map<Method, Object> methodToHandlers = new HashMap<Method, Object>();

            for (InstanceProvider<?> provider : methodsToProviders.values()) {
                if (providerToHandlers.containsKey(provider)) continue;
                providerToHandlers.put(provider, provider.provide(proxy));
            }

            for (Entry<Method, InstanceProvider<?>> entry : methodsToProviders.entrySet()) {
                methodToHandlers.put(entry.getKey(), providerToHandlers.get(entry.getValue()));
            }

            return methodToHandlers;
        }
    }

    private final Class<?>[] intfs;
    private final Map<Method, InstanceProvider<?>> methodsToProviders;
    private ClassLoader classLoader;

    public DefaultMixinFactory(ClassLoader classLoader, Set<Class<?>> intfs, Map<Method, InstanceProvider<?>> methodsToHandlers) {
        Preconditions.checkNotNull(classLoader);
        Preconditions.checkNotNull(intfs);
        Preconditions.checkNotNull(methodsToHandlers);
        this.classLoader = classLoader;
        this.intfs = intfs.toArray(new Class<?>[intfs.size()]);
        this.methodsToProviders = new HashMap<Method, InstanceProvider<?>>(methodsToHandlers);
    }

    @Override
    public Object newInstance() {
        return newInstance(Object.class);
    }

    @Override
    public <T> T newInstance(Class<T> clazz) {
        return new MixinInvocationHandler().getProxy(clazz);
    }

    @Override
    public <T> T newInstance(TypeToken<T> typeToken) {
        return new MixinInvocationHandler().getProxy(typeToken);
    }
}
