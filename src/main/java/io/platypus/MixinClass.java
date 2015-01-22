package io.platypus;

import java.lang.reflect.Type;
import java.util.Set;

public interface MixinClass<T> extends Type {

    public Set<Class<?>> getDeclaredInterfaces();

    public T newInstance(MixinInitializer provider);

}
