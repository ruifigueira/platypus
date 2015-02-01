package io.platypus;

import java.lang.reflect.InvocationHandler;
import java.util.Collection;

public interface MixinImplementor {

    public interface Implementation<T> {
        public MixinImplementor with(T obj);
        public MixinImplementor with(InstanceProvider<? extends T> provider);
        public MixinImplementor with(InvocationHandler handler);
    }

    public <T> Implementation<T> implement(Class<T> clazz);
    public Implementation<Object> implement(Class<?> ... clazz);
    public Implementation<Object> implement(Collection<Class<?>> clazzes);
    public Implementation<Object> implementRemainers();

    public <T> Implementation<T> override(Class<T> clazz);
    public Implementation<Object> override(Class<?> ... clazz);
    public Implementation<Object> override(Collection<Class<?>> clazzes);
}
