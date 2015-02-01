package io.platypus.internal;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import io.platypus.IncompleteImplementationException;
import io.platypus.InstanceProvider;
import io.platypus.InstanceProviders;
import io.platypus.Mixin;
import io.platypus.MixinImplementor;
import io.platypus.MixinInitializer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ProxyInvocationHandler<T> implements InvocationHandler, MixinImplementor {

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

    static final Function<InterfacesInstanceProvider, Iterable<Class<?>>> PROVIDER_IMPLEMENTED_INTFS_FN = new Function<InterfacesInstanceProvider, Iterable<Class<?>>>() {
        @Override
        public Iterable<Class<?>> apply(InterfacesInstanceProvider provider) {
            return provider.getImplementedInterfaces();
        }
    };

    private static class InterfacesInstanceProvider {

        private final InstanceProvider<?> provider;
        private final Set<Class<?>> intfs;
        private final boolean overrides;

        public InterfacesInstanceProvider(InstanceProvider<?> provider, Collection<Class<?>> intfs) {
            this(provider, intfs, false);
        }

        public InterfacesInstanceProvider(InstanceProvider<?> provider, Collection<Class<?>> intfs, boolean overrides) {
            this.provider = provider;
            this.intfs = ImmutableSet.copyOf(intfs);
            this.overrides = overrides;
        }

        public Object provide() {
            return provider.provide();
        }

        public Set<Class<?>> getImplementedInterfaces() {
            return intfs;
        }

        public boolean overrides() {
            return overrides;
        }
    }

    private class ImplementationImpl<I> implements Implementation<I> {

        protected final Collection<Class<?>> intfs;

        public ImplementationImpl(Class<I> intf) {
            this.intfs = Casts.unsafeCast(Collections.singleton(intf));
        }

        public ImplementationImpl(Collection<Class<?>> intfs) {
            this.intfs = intfs;
        }

        @Override
        public MixinImplementor with(I obj) {
            return with(InstanceProviders.ofInstance(obj));
        }

        @Override
        public MixinImplementor with(InvocationHandler handler) {
            return with(InstanceProviders.<I>adapt(handler, intfs));
        }

        @Override
        public MixinImplementor with(InstanceProvider<? extends I> provider) {
            providers.add(new InterfacesInstanceProvider(provider, intfs));
            return ProxyInvocationHandler.this;
        }
    }

    private class OverrideImplementationImpl<I> extends ImplementationImpl<I> {

        public OverrideImplementationImpl(Class<I> intf) {
            super(intf);
        }

        public OverrideImplementationImpl(Collection<Class<?>> intfs) {
            super(intfs);
        }

        @Override
        public MixinImplementor with(InstanceProvider<? extends I> provider) {
            providers.add(new InterfacesInstanceProvider(provider, intfs, true));
            return ProxyInvocationHandler.this;
        }
    }

    private final MixinClassImpl<T> mixinClass;
    private final T proxy;
    private final List<InterfacesInstanceProvider> providers = Lists.newArrayList();
    private final LinkedHashMap<Class<?>, Object> impls = Maps.newLinkedHashMap();

    public ProxyInvocationHandler(MixinClassImpl<T> mixinClass, MixinInitializer initializer) {
        this.mixinClass = mixinClass;
        this.proxy = newProxyInstance();

        // this will add all the necessary providers
        initializer.initialize(this);

        Set<Class<?>> allMixinIntfs = getAllInterfaces(mixinClass);
        checkCompleteImplementation(allMixinIntfs);

        instanciateProviders(allMixinIntfs);
        initImplementationsProxy();
    }

    private void initImplementationsProxy() {
        Set<Object> identityImpls = Sets.newIdentityHashSet();
        identityImpls.addAll(impls.values());

        for (Mixin.Impl impl : from(identityImpls).filter(Mixin.Impl.class)) {
            impl.setProxy(proxy);
        }
    }

    @Override
    public <I> Implementation<I> implement(Class<I> clazz) {
        return new ImplementationImpl<I>(clazz);
    }

    @Override
    public Implementation<Object> implement(Class<?>... clazz) {
        return implement(Arrays.asList(clazz));
    }

    @Override
    public Implementation<Object> implement(Collection<Class<?>> clazzes) {
        return new ImplementationImpl<Object>(clazzes);
    }

    @Override
    public Implementation<Object> implementRemainers() {
        Set<Class<?>> remainers = getRemainers(getAllInterfaces(mixinClass));
        return new ImplementationImpl<Object>(remainers);
    }

    @Override
    public <I> Implementation<I> override(Class<I> clazz) {
        return new OverrideImplementationImpl<I>(clazz);
    }

    @Override
    public Implementation<Object> override(Class<?>... clazz) {
        return override(Arrays.asList(clazz));
    }

    @Override
    public Implementation<Object> override(Collection<Class<?>> clazzes) {
        return new OverrideImplementationImpl<Object>(clazzes);
    }

    public T getProxy() {
        return proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        Object impl = impls.get(declaringClass);
        if (impl == null) throw new IncompleteImplementationException(format("No implementation could be found for %s", method));

        try {
            if (Proxy.isProxyClass(impl.getClass())) {
                // if it is a proxy, we get the handler and call it directly but with our proxy object
                InvocationHandler wrappedHandler = Proxy.getInvocationHandler(impl);
                return wrappedHandler.invoke(proxy, method, args);
            } else {
                return method.invoke(impl, args);
            }
        } catch (InvocationTargetException e) {
            throw Preconditions.checkNotNull(e.getTargetException());
        }
    }

    protected T newProxyInstance() {
        try {
            return mixinClass.proxyConst.newInstance(this);
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    protected void checkCompleteImplementation(Set<Class<?>> allMixinIntfs) {
        Set<Class<?>> differences = getRemainers(allMixinIntfs);
        if (!differences.isEmpty()) {
            throw new IncompleteImplementationException(format("The following interfaces are  missing: %s", Joiner.on(", ").join(differences)));
        }
    }

    protected ImmutableSet<Class<?>> getAllInterfaces(MixinClassImpl<T> mixinClass) {
        return from(mixinClass.intfs).transformAndConcat(ALL_INTFS_FN).toSet();
    }

    protected Set<Class<?>> getRemainers(Set<Class<?>> allMixinIntfs) {
        Set<Class<?>> allProvidersIntfs = from(providers).transformAndConcat(PROVIDER_IMPLEMENTED_INTFS_FN).transformAndConcat(ALL_INTFS_FN).toSet();
        Set<Class<?>> differences = from(Sets.difference(allMixinIntfs, allProvidersIntfs)).filter(WITH_DECLARED_METHODS_FN).toSet();
        return differences;
    }

    protected void instanciateProviders(Set<Class<?>> allMixinIntfs) {
        LOGGER.trace("Instanciating providers for [{}]", allMixinIntfs);
        for (InterfacesInstanceProvider provider : providers) {
            Set<Class<?>> allProviderIntfs = from(provider.getImplementedInterfaces()).transformAndConcat(ALL_INTFS_FN).toSet();
            Object impl = provider.provide();
            for (Class<?> intf : allProviderIntfs ) {
                if (!(allMixinIntfs.contains(intf) || intf == Object.class)) {
                    LOGGER.trace("This Mixin class does not implement [{}], skipping its instance provider", intf);
                    continue;
                }
                if (impls.containsKey(intf) && !provider.overrides()) {
                    LOGGER.trace("[{}] was already implemented by prior instance provider, skipping this ones", intf);
                } else {
                    boolean overrides = impls.containsKey(intf) && provider.overrides();
                    impls.put(intf, impl);
                    LOGGER.trace("[{}] is {} by instance of [{}]", overrides ? "overriden" : "implemented", intf, impl.getClass());
                }
            }
        }
    }

}