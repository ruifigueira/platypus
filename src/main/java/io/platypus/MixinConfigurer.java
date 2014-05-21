package io.platypus;

import io.platypus.internal.ProxyInvocationHandler;

public interface MixinConfigurer<T> {

    public abstract void configure(ProxyInvocationHandler<T> proxyInvocationHandler);

}