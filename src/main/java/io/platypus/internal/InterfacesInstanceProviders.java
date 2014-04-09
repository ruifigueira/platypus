package io.platypus.internal;

import io.platypus.InstanceProvider;
import io.platypus.InstanceProviders;

import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class InterfacesInstanceProviders {

    static class InterfacesInstanceProviderImpl<T> implements InterfacesInstanceProvider<T> {

        private final InstanceProvider<T> provider;
        private final Set<Class<?>> intfs;

        public InterfacesInstanceProviderImpl(InstanceProvider<T> provider, Collection<Class<?>> intfs) {
            this.provider = provider;
            this.intfs = ImmutableSet.copyOf(intfs);
        }

        @Override
        public T provide() {
            return provider.provide();
        }

        @Override
        public Set<Class<?>> getImplementedInterfaces() {
            return intfs;
        }
    }

    public static <T> InterfacesInstanceProviderImpl<T> of(InstanceProvider<T> provider, Class<?> ... intfs) {
        return of(provider, Arrays.asList(intfs));
    }

    public static <T> InterfacesInstanceProviderImpl<T> of(InstanceProvider<T> provider, Collection<Class<?>> intfs) {
        return new InterfacesInstanceProviderImpl<T>(provider, intfs);
    }

    public static <T> InterfacesInstanceProviderImpl<T> of(InvocationHandler handler, Class<?> ... intfs) {
        InstanceProvider<T> provider = InstanceProviders.adapt(handler, intfs);
        return of(provider, Arrays.asList(intfs));
    }

    public static <T> InterfacesInstanceProviderImpl<T> of(InvocationHandler handler, Collection<Class<?>> intfs) {
        InstanceProvider<T> provider = InstanceProviders.adapt(handler, intfs);
        return new InterfacesInstanceProviderImpl<T>(provider, intfs);
    }
}
