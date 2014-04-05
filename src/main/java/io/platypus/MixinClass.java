package io.platypus;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

public interface MixinClass<T> extends Type {

    public Set<Class<?>> getDeclaredInterfaces();

    public T newInstance();

    public T newInstance(AbstractInstanceConfigurer<?> ... providers);

    public T newInstance(Collection<AbstractInstanceConfigurer<?>> providers);
}
