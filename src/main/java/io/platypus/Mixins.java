package io.platypus;

import io.platypus.internal.MixinClassImpl;

import java.util.Arrays;
import java.util.Collection;

public class Mixins {

    public static <T> MixinClass<T> createClass(Class<T> intf, Class<?> ... others) {
        return createClass(intf, Arrays.asList(others));
    }

    public static <T> MixinClass<T> createClass(Class<T> intf, Collection<Class<?>> others) {
        return new MixinClassImpl<T>(intf, others);
    }
}
