package io.platypus;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ CONSTRUCTOR })
@Retention(RUNTIME)
public @interface InjectProxy {
}
