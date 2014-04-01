package io.platypus;

import com.google.common.reflect.TypeToken;

/**
 *
 * @author rui.figueira
 *
 */
public interface MixinFactory {

    /**
     *
     * @param class1
     * @return
     */
    <T> T newInstance(Class<T> clazz);
    <T> T newInstance(TypeToken<T> typeToken);

    Object newInstance();

}
