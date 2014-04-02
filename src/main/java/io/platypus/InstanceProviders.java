package io.platypus;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.toArray;
import static java.lang.String.format;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collection;

import com.google.common.base.Preconditions;

public class InstanceProviders {

    private static class ClassInstanceProvider<T> implements InstanceProvider<T> {

        private final Constructor<T> constructor;
        private final Class<? extends T> clazz;

        @SuppressWarnings("unchecked")
        public ClassInstanceProvider(Class<? extends T> clazz) {
            this.clazz = Preconditions.checkNotNull(clazz);

            Constructor<?>[] constructors = clazz.getDeclaredConstructors();

            Constructor<?> defaultConstructor = null;
            Constructor<?> annotatedConstructor = null;

            for (Constructor<?> constructor : constructors) {
                switch (constructor.getParameterTypes().length) {
                case 0:
                    defaultConstructor = constructor;
                    break;
                case 1:
                    // check annotation
                    if (constructor.isAnnotationPresent(InjectProxy.class)) {
                        annotatedConstructor = constructor;
                    }
                    break;
                }
            }

            Preconditions.checkState(defaultConstructor != null || annotatedConstructor != null,
                    "There must be one default constructor or a single argument construct annotated with @InjectProxy for %s",
                    clazz.getName());

            // annotated constructor has priority over default constructor
            constructor = (Constructor<T>) (annotatedConstructor != null ? annotatedConstructor : defaultConstructor);
        }

        @Override
        public T provide(Object proxy) {
            Preconditions.checkNotNull(proxy);
            Object[] args = constructor.getParameterTypes().length == 0 ? new Object[0] : new Object[] { proxy };
            try {
                return constructor.newInstance(args);
            } catch (InstantiationException e) {
                throw propagate(e);
            } catch (IllegalAccessException e) {
                throw propagate(e);
            } catch (InvocationTargetException e) {
                throw propagate(e);
            }
        }

        @Override
        public int hashCode() {
            return clazz.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ClassInstanceProvider) {
                return clazz == ((ClassInstanceProvider<?>) other).clazz;
            }
            return false;
        }

        @Override
        public String toString() {
            return format("ClassInstanceProvider{clazz=%s}", clazz);
        }
    }

    private static class IdentityInstanceProvider<T> implements InstanceProvider<T> {

        private final T obj;

        public IdentityInstanceProvider(T obj) {
            this.obj = Preconditions.checkNotNull(obj);
        }

        @Override
        public T provide(Object proxy) {
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

        private Object proxy;
        private final InstanceProvider<T> delegate;

        private boolean initialized = false;
        private T value;

        public MemoizingInstanceProvider(InstanceProvider<T> delegate) {
            this.delegate = Preconditions.checkNotNull(delegate);
        }

        @Override
        public T provide(Object proxy) {
            Preconditions.checkArgument(this.proxy == null || this.proxy == proxy, "This MemoizingInstanceProvider can only accept requests for proxy object %s, and received request for proxy object %s", this.proxy, proxy);

            if (!initialized) {
                value = delegate.provide(proxy);
                this.proxy = proxy;
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
        public T provide(Object proxy) {
            return (T) Proxy.newProxyInstance(proxy.getClass().getClassLoader(), intfs, handler);
        }
    }

    public static <T> InstanceProvider<T> ofInstance(T instance) {
        return new IdentityInstanceProvider<T>(instance);
    }

    public static <T> InstanceProvider<T> ofClass(Class<T> clazz) {
        return new ClassInstanceProvider<T>(clazz);
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

    @SuppressWarnings("unchecked")
    protected static <T> T unsafeCast(Object obj) {
        return (T) obj;
    }
}
