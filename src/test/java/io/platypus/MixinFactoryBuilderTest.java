package io.platypus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;


public class MixinFactoryBuilderTest {

    public interface Foo {}
    public interface Bar {
        public Foo getFoo();
    }

    public static class FooImpl implements Foo {
    }
    public static class BarImpl implements Bar {
        Foo proxy;

        @InjectProxy
        public BarImpl(Foo proxy) {
            this.proxy = proxy;
        }

        @Override
        public Foo getFoo() {
            return proxy;
        }
    }

    @Test
    public void testMixinFactory() {
        MixinFactory factory = new MixinFactoryBuilder()
            .implement(Foo.class).with(InstanceProviders.ofInstance(new FooImpl()))
            .implement(Bar.class).with(BarImpl.class)
            .build();
        Foo instance = factory.newInstance(Foo.class);
        // implements interfaces
        assertThat(instance, instanceOf(Foo.class));
        assertThat(instance, instanceOf(Bar.class));
        // does not extend implementation classes
        assertThat(instance, not(instanceOf(FooImpl.class)));
        assertThat(instance, not(instanceOf(BarImpl.class)));

        // ensure proxy was set in bar
        assertThat(((Bar) instance).getFoo(), sameInstance(instance));
        assertThat(((Bar) instance).getFoo(), equalTo(instance));
    }
}
