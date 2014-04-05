package io.platypus.internal;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

// Based on org.apache.commons.lang.ClassUtils.getAllInterfaces(Class)
public class Classes {

    /**
     *
     * @return all interfaces plus root
     */
    public static Set<Class<?>> getInterfacesClosure(Class<?> cls) {
        Set<Class<?>> closure = Sets.newLinkedHashSet();
        closure.add(cls);
        closure.addAll(getAllInterfaces(cls));
        return ImmutableSet.copyOf(closure);
    }

    /**
     * <p>Gets a <code>Set</code> of all interfaces implemented by the given
     * class and its superclasses.</p>
     *
     * <p>The order is determined by looking through each interface in turn as
     * declared in the source file and following its hierarchy up. Then each
     * superclass is considered in the same way. Duplicates are ignored,
     * so the order is maintained.</p>
     *
     * @param cls  the class to look up, may be <code>null</code>
     * @return the <code>List</code> of interfaces in order,
     *  <code>null</code> if null input
     */
    public static Set<Class<?>> getAllInterfaces(Class<?> cls) {
        if (cls == null) {
            return null;
        }

        // a LinkHashSet will mantain the order
        Set<Class<?>> interfacesFound = Sets.newLinkedHashSet();
        getAllInterfaces(cls, interfacesFound);

        return ImmutableSet.copyOf(interfacesFound);
    }

    /**
     * Get the interfaces for the specified class.
     *
     * @param cls  the class to look up, may be <code>null</code>
     * @param interfacesFound the <code>Set</code> of interfaces for the class
     */
    private static void getAllInterfaces(Class<?> cls, Collection<Class<?>> interfacesFound) {
        while (cls != null) {
            Class<?>[] interfaces = cls.getInterfaces();

            for (int i = 0; i < interfaces.length; i++) {
                interfacesFound.add(interfaces[i]);
                getAllInterfaces(interfaces[i], interfacesFound);
            }

            cls = cls.getSuperclass();
         }
     }
}
