package io.platypus;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Extend this class to configure mixin instances initialization.
 *
 * <pre>
 * MixinClass<FooBar> fooBarClass = Mixins.createClass(FooBar.class);
 * FooBar fooBar = fooBarClass.newInstance(new AbstractMixinConfigurer<FooBar>() {
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
public abstract class AbstractMixinInitializer implements MixinInitializer {

    public class Implementation {

        private final Set<Class<?>> intfs;

        public Implementation(Class<?> ... intfs) {
            this(Arrays.asList(intfs));
        }

        public Implementation(Collection<Class<?>> intfs) {
            this.intfs = ImmutableSet.copyOf(intfs);
        }

        public MixinInitializer with(InstanceProvider<?> provider) {
            Preconditions.checkState(mixinImplementor != null, "implement(...).with(...) can only be called inside configure()!");
            mixinImplementor.implement(intfs).with(provider);
            return AbstractMixinInitializer.this;
        }

        public MixinInitializer with(Object instance) {
            return with(InstanceProviders.ofInstance(instance));
        }

        public MixinInitializer with(InvocationHandler handler) {
            return with(InstanceProviders.adapt(handler, intfs));
        }
    }

    class RemainersImplementation extends Implementation {

        @Override
        public MixinInitializer with(InstanceProvider<?> provider) {
            mixinImplementor.implementRemainers().with(provider);
            return AbstractMixinInitializer.this;
        }
    }


    private MixinImplementor mixinImplementor;

    protected Implementation implement(Class<?> ... intfs) {
        return new Implementation(intfs);
    }

    protected Implementation implement(Collection<Class<?>> intfs) {
        return new Implementation(intfs);
    }

    protected Implementation implementRemainers() {
        return new RemainersImplementation();
    }

    protected abstract void initialize();

    /* (non-Javadoc)
     * @see io.platypus.MixinConfigurer#configure(io.platypus.internal.ProxyInvocationHandler)
     */
    @Override
    public final void initialize(MixinImplementor mixinImplementor) {
        checkState(this.mixinImplementor == null, "Re-entry is not allowed.");

        this.mixinImplementor = checkNotNull(mixinImplementor, "mixinImplementor");
        try {
          initialize();
        }
        finally {
            this.mixinImplementor = null;
        }
    }

}
