package io.platypus.internal;

import io.platypus.InstanceProvider;

import java.util.Set;

public interface InterfacesInstanceProvider<T> extends InstanceProvider<T> {
    public Set<Class<?>> getImplementedInterfaces();
}
