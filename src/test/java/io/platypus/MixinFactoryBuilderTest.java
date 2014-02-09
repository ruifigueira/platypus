package io.platypus;

import static io.platypus.utils.Throwables.propagate;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.junit.Test;


public class MixinFactoryBuilderTest {

    private static final Method BAR_GET_FOO_METHOD = getMethod(Bar.class, "getFoo");

    public interface Foo {}
    public interface Bar {
        public Foo getFoo();
    }
    public interface FooBar extends Foo, Bar {}

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
    public void testInterfaces() {
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

    @Test
    public void testImplementWithInvocationHandler() throws Throwable {
        // given
        InvocationHandler handlerMock = mock(InvocationHandler.class);
        MixinFactory factory = new MixinFactoryBuilder()
            .implement(Bar.class).with(handlerMock)
            .build();

        // when
        Bar instance = factory.newInstance(Bar.class);
        instance.getFoo();

        // then
        verify(handlerMock).invoke(instance, BAR_GET_FOO_METHOD, null);
    }

    @Test
    public void testImplementWithInstance() throws Throwable {
        // given
        Bar barMock = mock(Bar.class);
        MixinFactory factory = new MixinFactoryBuilder()
            .implement(Bar.class).with(InstanceProviders.ofInstance(barMock))
            .build();

        // when
        Bar instance = factory.newInstance(Bar.class);
        instance.getFoo();

        // then
        verify(barMock).getFoo();
    }

    protected static Method getMethod(Class<?> clazz, String methodName, Class<?> ... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (SecurityException e) {
            throw propagate(e);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }
}
