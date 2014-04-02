package io.platypus;

import static java.lang.String.format;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;


public class MixinFactoryBuilder {

    private static class DefaultObjectImpl {

        private Object that;

        @InjectProxy
        public DefaultObjectImpl(Object proxy) {
            this.that = proxy;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(that);
        }

        @Override
        public boolean equals(Object obj) {
            return that == obj;
        }

        @Override
        public String toString() {
            List<Class<?>> intfs = Arrays.asList(that.getClass().getInterfaces());
            Collections.sort(intfs, new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return format("Proxy{%s}", Joiner.on(", ").join(intfs));
        }
    }

    public static class ImplementBuilder<T> {

        private final Class<T> intf;
        private MixinFactoryBuilder builder;

        public ImplementBuilder(MixinFactoryBuilder builder, Class<T> intf) {
            this.intf = intf;
            this.builder = builder;
        }

        public MixinFactoryBuilder with(T object) {
            return with(InstanceProviders.ofInstance(object));
        }

        public MixinFactoryBuilder with(Class<? extends T> implClass) {
            return with(InstanceProviders.ofClass(implClass));
        }

        public MixinFactoryBuilder with(InvocationHandler handler) {
            addProvider(InstanceProviders.ofInstance(handler));
            return builder;
        }

        public MixinFactoryBuilder with(InstanceProvider<? extends T> implProvider) {
            addProvider(implProvider);
            return builder;
        }

        protected void addProvider(InstanceProvider<?> implProvider) {
            for (Method method : intf.getMethods()) {
                InstanceProvider<?> instanceProvider = builder.methodsToProviders.get(method);
                if (instanceProvider == null) {
                    builder.methodsToProviders.put(method, implProvider);
                }
            }
            if (intf != Object.class) builder.intfs.add(intf);
        }
    }

    private Set<Class<?>> intfs = new HashSet<Class<?>>();
    private Map<Method, InstanceProvider<?>> methodsToProviders = new HashMap<Method, InstanceProvider<?>>();

    public MixinFactoryBuilder() {
        this(InstanceProviders.ofClass(DefaultObjectImpl.class));
    }

    public MixinFactoryBuilder(InstanceProvider<? extends Object> objectProvider) {
        implement(Object.class).with(objectProvider);
    }

    protected MixinFactoryBuilder(MixinFactoryBuilder other) {
        this.intfs.addAll(other.intfs);
        this.methodsToProviders.putAll(other.methodsToProviders);
    }

    public <T> ImplementBuilder<T> implement(Class<T> intf) {
        Preconditions.checkArgument(intf == Object.class || intf.isInterface());
        return new ImplementBuilder<T>(clone(), intf);
    }

    @SuppressWarnings("unchecked")
    public <T> ImplementBuilder<T> implement(TypeToken<T> intf) {
        Class<? super T> rawType = intf.getRawType();
        Preconditions.checkArgument(rawType == Object.class || rawType.isInterface());
        return new ImplementBuilder<T>(clone(), (Class<T>) intf.getRawType());
    }

    public MixinFactory build() {
        return build(Thread.currentThread().getContextClassLoader());
    }

    public MixinFactory build(ClassLoader classloader) {
        return new DefaultMixinFactory(classloader, intfs, methodsToProviders);
    }

    @Override
    protected MixinFactoryBuilder clone() {
        return new MixinFactoryBuilder(this);
    }
}
