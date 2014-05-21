package io.platypus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;

import org.junit.Test;

import com.google.common.base.Defaults;
import com.google.common.reflect.AbstractInvocationHandler;

public class MixinsTest {

    public interface Foo extends Mixin {
        String foo();
    }

    public interface Bar {
        String bar();
    }

    public interface FooBar extends Foo, Bar {
    }

    public class FooImpl extends Mixin.Impl implements Foo {

        public FooImpl() {
        }

        @Override
        public String foo() {
            return "generic foo";
        }

        public Foo getThat() {
            return this.as(Foo.class);
        }
    }

    public class BarImpl implements Bar {
        @Override
        public String bar() {
            return "generic bar";
        }
    }

    @Test
    public void testSpecificImplementation() {
        MixinClass<FooBar> fooBarClass = Mixins.createClass(FooBar.class, Foo.class, Bar.class);

        FooBar fooBar = fooBarClass.newInstance(new AbstractMixinConfigurer<FooBar>() {
            @Override
            protected void configure() {
                implement(Foo.class).with(new FooImpl());
                implement(Bar.class).with(new BarImpl());
            }
        });

        assertThat(fooBar.foo(), equalTo("generic foo"));
        assertThat(fooBar.bar(), equalTo("generic bar"));
    }

    @Test(expected = IncompleteImplementationException.class)
    public void testInterfaceWithNoImplementation() {
        MixinClass<FooBar> fooBarClass = Mixins.createClass(FooBar.class);

        fooBarClass.newInstance(new AbstractMixinConfigurer<FooBar>() {
            @Override
            protected void configure() {
                implement(Foo.class);
                implement(Bar.class).with(new BarImpl());
            }
        });
    }

    @Test(expected = IncompleteImplementationException.class)
    public void testIncompleteImplementation() {
        MixinClass<FooBar> fooBarClass = Mixins.createClass(FooBar.class);

        fooBarClass.newInstance(new AbstractMixinConfigurer<FooBar>() {
            @Override
            protected void configure() {
                implement(Bar.class).with(new BarImpl());
            }
        });
    }

    @Test
    public void testInvocationHandlerImplementation() {
        // given
        MixinClass<FooBar> fooBarClass = Mixins.createClass(FooBar.class);
        class TestInvocationHandler extends AbstractInvocationHandler {

            Object proxy;

            @Override
            protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
                this.proxy = proxy;
                return Defaults.defaultValue(method.getReturnType());
            }
        };
        final TestInvocationHandler handler = new TestInvocationHandler();

        // when
        FooBar foobar = fooBarClass.newInstance(new AbstractMixinConfigurer<FooBar>() {
            @Override
            protected void configure() {
                InstanceProvider<Bar> barProxyProvider = InstanceProviders.adapt(handler, Bar.class);
                implement(Foo.class).with(new FooImpl());
                implement(Bar.class).with(barProxyProvider);
            }
        });
        foobar.bar();

        // then
        assertThat(handler.proxy, sameInstance((Object) foobar));
    }

    @Test
    public void testMixinInitialization() {
        // given
        MixinClass<Mixin> mixinClass = Mixins.createClass(Mixin.class, FooBar.class);

        // when
        final Foo foo = new FooImpl();
        // this will initialize foo with the proxy
        Mixin mixin = mixinClass.newInstance(new AbstractMixinConfigurer<Mixin>() {
            @Override
            protected void configure() {
                implement(Object.class).with(new Object());
                implement(Foo.class).with(foo);
                implement(Bar.class).with(new BarImpl());
                assertThat(foo.as(Foo.class), is(foo));
            }
        });
        assertThat((Foo) mixin, not(sameInstance(foo)));
        assertThat(foo.as(Foo.class), sameInstance(mixin));
        assertThat(mixin.as(Foo.class), sameInstance(mixin));
    }

}
