package io.platypus.internal;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import io.platypus.AbstractInstanceConfigurer;
import io.platypus.IncompleteImplementationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ProxyInvocationHandler<T> implements InvocationHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(ProxyInvocationHandler.class);

    static final Predicate<Class<?>> WITH_DECLARED_METHODS_FN = new Predicate<Class<?>>() {

        @Override
        public boolean apply(Class<?> clazz) {
            return clazz.getDeclaredMethods().length > 0;
        }
    };

    static final Function<Class<?>, Iterable<Class<?>>> ALL_INTFS_FN = new Function<Class<?>, Iterable<Class<?>>>() {
        @Override
        public Iterable<Class<?>> apply(Class<?> clazz) {
            return Classes.getInterfacesClosure(clazz);
        }
    };

    static final Function<InterfacesInstanceProvider<?>, Iterable<Class<?>>> PROVIDER_IMPLEMENTED_INTFS_FN = new Function<InterfacesInstanceProvider<?>, Iterable<Class<?>>>() {
        @Override
        public Iterable<Class<?>> apply(InterfacesInstanceProvider<?> provider) {
            return provider.getImplementedInterfaces();
        }
    };

    private final MixinClassImpl<T> mixinClass;
    private final T proxy;
    private final List<InterfacesInstanceProvider<?>> providers = Lists.newArrayList();
    private final LinkedHashMap<Class<?>, Object> impls = Maps.newLinkedHashMap();

    public ProxyInvocationHandler(MixinClassImpl<T> mixinClass, Collection<AbstractInstanceConfigurer<?>> configurers) {
        this.mixinClass = mixinClass;
        this.proxy = newProxyInstance();

        // this will add all the necessary providers
        for (AbstractInstanceConfigurer<?> configurer : configurers) {
            unsafeDoConfigure(configurer);
        }

        Set<Class<?>> allMixinIntfs = from(mixinClass.intfs).transformAndConcat(ALL_INTFS_FN).toSet();
        checkCompleteImplementation(allMixinIntfs);

        instanciateProviders(allMixinIntfs);
    }

    public T getProxy() {
        return proxy;
    }

    public void add(InterfacesInstanceProvider<?> provider) {
        this.providers .add(provider);
    }

    protected void checkCompleteImplementation(Set<Class<?>> allMixinIntfs) {
        Set<Class<?>> allProvidersIntfs = from(providers).transformAndConcat(PROVIDER_IMPLEMENTED_INTFS_FN).transformAndConcat(ALL_INTFS_FN).toSet();

        Set<Class<?>> differences = from(Sets.difference(allMixinIntfs, allProvidersIntfs)).filter(WITH_DECLARED_METHODS_FN).toSet();
        if (!differences.isEmpty()) {
            throw new IncompleteImplementationException(format("The following interfaces are  missing: %s", Joiner.on(", ").join(differences)));
        }
    }

    protected T newProxyInstance() {
        try {
            return mixinClass.proxyConst.newInstance(this);
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        Object impl = impls.get(declaringClass);
        if (impl == null) throw new IncompleteImplementationException(format("No implementation could be found for %s", method));

        if (Proxy.isProxyClass(impl.getClass())) {
            // if it is a proxy, we get the handler and call it directly but with our proxy object
            InvocationHandler wrappedHandler = Proxy.getInvocationHandler(impl);
            return wrappedHandler.invoke(proxy, method, args);
        } else {
            return method.invoke(impl, args);
        }
    }

    @SuppressWarnings("unchecked")
    protected void unsafeDoConfigure(@SuppressWarnings("rawtypes") AbstractInstanceConfigurer configurer) {
        configurer.doConfigure(this);
    }

    protected void instanciateProviders(Set<Class<?>> allMixinIntfs) {
        for (InterfacesInstanceProvider<?> provider : providers) {
            Set<Class<?>> allProviderIntfs = from(provider.getImplementedInterfaces()).transformAndConcat(ALL_INTFS_FN).toSet();
            Object impl = provider.provide(proxy);
            for (Class<?> intf : allProviderIntfs ) {
                if (!(allMixinIntfs.contains(intf) || intf == Object.class)) {
                    LOGGER.trace("This Mixin class does not implement {}, skipping its instance provider", intf);
                    continue;
                }
                if (impls.containsKey(intf)) {
                    LOGGER.trace("Interface {} was already implemented by prior instance provider, skipping this ones", intf);
                } else {
                    impls.put(intf, impl);
                    LOGGER.trace("Interface {} is implemented by {}", intf, impl);
                }
            }
        }
    }

}