package platypus.internal;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.FluentIterable.from;
import static platypus.internal.Casts.unsafeCast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import platypus.MixinClass;
import platypus.MixinInitializer;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class MixinClassImpl<T> implements MixinClass<T> {

    static final Logger LOGGER = LoggerFactory.getLogger(MixinClassImpl.class);

    private static final Predicate<Class<?>> OBJECT_OR_INTERFACE = new Predicate<Class<?>>() {
        @Override
        public boolean apply(Class<?> intf) {
            return intf == Object.class || intf.isInterface();
        }
    };

    final Set<Class<?>> intfs;
    final Constructor<T> proxyConst;

    public MixinClassImpl(Class<?> intf, Class<?> ... others) {
        this(intf, Arrays.asList(others));
    }

    public MixinClassImpl(Class<?> intf, Collection<Class<?>> others) {
        Set<Class<?>> intfs = Sets.newLinkedHashSet(Iterables.concat(Collections.singleton(intf), others));
        Set<Class<?>> notIntfs = from(intfs).filter(not(OBJECT_OR_INTERFACE)).toSet();

        Preconditions.checkArgument(notIntfs.isEmpty(), "The following classes are not interfaces or Object.class: %s", Joiner.on(", ").join(notIntfs));

        this.intfs = ImmutableSet.copyOf(intfs);

        try {
            Class<Class<?>> classClazz = Casts.unsafeCast(Class.class);
            Class<?>[] intfsArray = Iterables.toArray(intfs, classClazz);
            proxyConst = unsafeCast(Proxy.getProxyClass(intf.getClassLoader(), intfsArray).getConstructor(InvocationHandler.class));
        } catch (SecurityException e) {
            throw propagate(e);
        } catch (IllegalArgumentException e) {
            throw propagate(e);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    @Override
    public Set<Class<?>> getDeclaredInterfaces() {
        return intfs;
    }

    @Override
    public T newInstance(MixinInitializer initializer) {
        try {
            ProxyInvocationHandler<T> proxyInvocationHandler = new ProxyInvocationHandler<T>(this, initializer);
            return proxyInvocationHandler.getProxy();
        } catch (Exception e) {
            throw propagate(e);
        }
    }

}
