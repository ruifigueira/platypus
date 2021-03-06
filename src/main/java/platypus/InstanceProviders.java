package platypus;

import static com.google.common.collect.Iterables.toArray;
import static java.lang.String.format;
import static platypus.internal.Casts.unsafeCast;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collection;

import com.google.common.base.Preconditions;

public class InstanceProviders {

    private static class IdentityInstanceProvider<T> implements InstanceProvider<T> {

        private final T obj;

        public IdentityInstanceProvider(T obj) {
            this.obj = Preconditions.checkNotNull(obj);
        }

        @Override
        public T provide() {
            return obj;
        }

        @Override
        public int hashCode() {
            return obj.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof IdentityInstanceProvider) {
                return obj.equals(((IdentityInstanceProvider<?>) other).obj);
            }
            return false;
        }

        @Override
        public String toString() {
            return format("IdentityInstanceProvider{obj=%s}", obj);
        }
    }

    private static class MemoizingInstanceProvider<T> implements InstanceProvider<T> {

        private final InstanceProvider<T> delegate;

        private boolean initialized = false;
        private T value;

        public MemoizingInstanceProvider(InstanceProvider<T> delegate) {
            this.delegate = Preconditions.checkNotNull(delegate);
        }

        @Override
        public T provide() {
            if (!initialized) {
                value = delegate.provide();
                initialized = true;
            }

            return value;
        }

    }

    private static final class InvocationHandlerInstanceProviderAdapter<T> implements InstanceProvider<T> {

        private final InvocationHandler handler;
        private final Class<?>[] intfs;

        private InvocationHandlerInstanceProviderAdapter(InvocationHandler handler, Class<?> ... intfs) {
            this.handler = handler;
            this.intfs = intfs;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T provide() {
            return (T) Proxy.newProxyInstance(handler.getClass().getClassLoader(), intfs, handler);
        }
    }

    public static <T> InstanceProvider<T> ofInstance(T instance) {
        return new IdentityInstanceProvider<T>(instance);
    }

    public static <T> InstanceProvider<T> adapt(InvocationHandler handler, Collection<Class<?>> intfs) {
        Class<Class<?>> classClazz = unsafeCast(Class.class);
        return new InvocationHandlerInstanceProviderAdapter<T>(handler, toArray(intfs, classClazz));
    }

    public static <T> InstanceProvider<T> adapt(InvocationHandler handler, Class<?> ... intfs) {
        return new InvocationHandlerInstanceProviderAdapter<T>(handler, intfs);
    }

    public static <T> InstanceProvider<T> memoize(InstanceProvider<T> provider) {
        return new MemoizingInstanceProvider<T>(provider);
    }
}
