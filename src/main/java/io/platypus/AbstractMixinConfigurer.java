package io.platypus;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import io.platypus.internal.InterfacesInstanceProvider;
import io.platypus.internal.InterfacesInstanceProviders;
import io.platypus.internal.ProxyInvocationHandler;

import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Extend this class to configure new proxy instances initialization.
 *
 * <pre>
 * MixinClass<FooBar> fooBarClass = Mixins.createClass(FooBar.class);
 * FooBar fooBar = fooBarClass.newInstance(new AbstractInstanceConfigurer<FooBar>() {
 *   &#64;Override
 *   protected void configure() {
 *     implement(Foo.class).with(new FooImpl());
 *     implement(Bar.class).with(new BarImpl());
 *   }
 * });
 * </pre>
 *
 * @author rui.figueira
 *
 * @param <T>
 */
public abstract class AbstractMixinConfigurer<T> implements MixinConfigurer<T> {

    public static <T> MixinConfigurer<T> nullInstanceConfigurer() {
        return new AbstractMixinConfigurer<T>() {
            @Override
            protected void configure() {
            }
        };
    }

    protected static class ImplementedInterfacesFunction implements Function<InterfacesInstanceProvider<?>, Iterable<Class<?>>> {
        @Override
        public Iterable<Class<?>> apply(InterfacesInstanceProvider<?> iip) {
            return iip.getImplementedInterfaces();
        }
    }

    public class Implementation {

        private final Set<Class<?>> intfs;

        public Implementation(Class<?> ... intfs) {
            this(Arrays.asList(intfs));
        }

        public Implementation(Collection<Class<?>> intfs) {
            this.intfs = ImmutableSet.copyOf(intfs);
        }

        public MixinConfigurer<T> with(InstanceProvider<?> provider) {
            Preconditions.checkState(proxyInvocationHandler != null, "implement(...).with(...) can only be called inside configure()!");
            proxyInvocationHandler.add(InterfacesInstanceProviders.of(provider, intfs));
            return AbstractMixinConfigurer.this;
        }

        public MixinConfigurer<T> with(Object instance) {
            return with(InstanceProviders.ofInstance(instance));
        }

        public MixinConfigurer<T> with(InvocationHandler handler) {
            return with(InstanceProviders.adapt(handler, intfs));
        }
    }

    private ProxyInvocationHandler<T> proxyInvocationHandler;

    protected Implementation implement(Class<?> ... intfs) {
        return new Implementation(intfs);
    }

    protected Implementation implement(Collection<Class<?>> intfs) {
        return new Implementation(intfs);
    }

    protected abstract void configure();

    /* (non-Javadoc)
     * @see io.platypus.MixinConfigurer#configure(io.platypus.internal.ProxyInvocationHandler)
     */
    @Override
    public final void configure(ProxyInvocationHandler<T> proxyInvocationHandler) {
        checkState(this.proxyInvocationHandler == null, "Re-entry is not allowed.");

        this.proxyInvocationHandler = checkNotNull(proxyInvocationHandler, "proxyInvocationHandler");
        try {
          configure();
        }
        finally {
            this.proxyInvocationHandler = null;
        }
    }

}
