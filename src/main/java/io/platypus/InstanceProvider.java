package io.platypus;

public interface InstanceProvider<T> {
    public T provide(Object proxy);
}
