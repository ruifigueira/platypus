package io.platypus;


/**
 *
 * @author rui.figueira
 *
 * @param <T>
 */
public interface MixinFactory {

    /**
     *
     * @param class1
     * @return
     */
    <T> T newInstance(Class<T> clazz);

    Object newInstance();
}
