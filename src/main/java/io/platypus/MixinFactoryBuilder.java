package io.platypus;

import static java.lang.String.format;
import io.platypus.utils.Preconditions;
import io.platypus.utils.Strings;

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
            return format("Proxy{%s}", Strings.join(intfs, ", "));
        }
    }

    public class ImplementBuilder<T> {

        private final Class<T> intf;

        public ImplementBuilder(Class<T> intf) {
            this.intf = intf;
        }

        public MixinFactoryBuilder with(Class<? extends T> implClass) {
            return with(InstanceProviders.ofClass(implClass));
        }

        public MixinFactoryBuilder with(InvocationHandler handler) {
            addProvider(InstanceProviders.ofInstance(handler));
            return MixinFactoryBuilder.this;
        }

        public MixinFactoryBuilder with(InstanceProvider<? extends T> implProvider) {
            addProvider(implProvider);
            return MixinFactoryBuilder.this;
        }

        protected void addProvider(InstanceProvider<?> implProvider) {
            for (Method method : intf.getMethods()) {
                InstanceProvider<?> instanceProvider = methodsToProviders.get(method);
                if (instanceProvider == null) {
                    methodsToProviders.put(method, implProvider);
                }
            }
            if (intf != Object.class) intfs.add(intf);
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

    public <T> ImplementBuilder<T> implement(Class<T> intf) {
        Preconditions.checkArgument(intf == Object.class || intf.isInterface());
        return new ImplementBuilder<T>(intf);
    }

    public MixinFactory build() {
        return build(Thread.currentThread().getContextClassLoader());
    }

    public MixinFactory build(ClassLoader classloader) {
        return new DefaultMixinFactory(classloader, intfs, methodsToProviders);
    }
}
