package org.junit.runner;

import org.junit.internal.Classes;
import org.junit.internal.reflection.ObjectFactory;
import org.junit.runner.FilterFactory.FilterNotCreatedException;
import org.junit.runner.manipulation.Filter;

/**
 * Utility class whose methods create a {@link FilterFactory}.
 */
public class FilterFactories {
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    /**
     * Creates a {@link Filter}.
     *
     * A filter specification is of the form "package.of.FilterFactory=args-to-filter-factory" or
     * "package.of.FilterFactory".
     *
     * @param filterSpec The filter specification
     * @throws org.junit.runner.FilterFactory.FilterNotCreatedException
     */
    public static Filter createFilterFromFilterSpec(Description description, String filterSpec)
            throws FilterFactory.FilterNotCreatedException {
        String[] tuple;

        if (filterSpec.contains("=")) {
            tuple = filterSpec.split("=", 2);
        } else {
            tuple = new String[]{ filterSpec, "" };
        }

        return createFilter(tuple[0], new FilterFactoryParams(tuple[1]));
    }

    /**
     * Creates a {@link Filter}.
     *
     * @param filterFactoryFqcn The fully qualified class name of the {@link FilterFactory}
     * @param params The arguments to the {@link FilterFactory}
     * @throws org.junit.runner.FilterFactory.FilterNotCreatedException
     */
    public static Filter createFilter(String filterFactoryFqcn, FilterFactoryParams params)
            throws FilterFactory.FilterNotCreatedException {
        FilterFactory filterFactory = createFilterFactory(filterFactoryFqcn);

        return filterFactory.createFilter(params);
    }

    /**
     * Creates a {@link Filter}.
     *
     * @param filterFactoryClass The class of the {@link FilterFactory}
     * @param params             The arguments to the {@link FilterFactory}
     * @throws org.junit.runner.FilterFactory.FilterNotCreatedException
     *
     */
    public static Filter createFilter(Class<? extends FilterFactory> filterFactoryClass, FilterFactoryParams params)
            throws FilterFactory.FilterNotCreatedException {
        FilterFactory filterFactory = createFilterFactory(filterFactoryClass);

        return filterFactory.createFilter(params);
    }

    static FilterFactory createFilterFactory(String filterFactoryFqcn) throws FilterNotCreatedException {
        Class<? extends FilterFactory> filterFactoryClass;

        try {
            filterFactoryClass = Classes.getClass(filterFactoryFqcn).asSubclass(FilterFactory.class);
        } catch (Exception e) {
            throw new FilterNotCreatedException(e);
        }

        return createFilterFactory(filterFactoryClass);
    }

    static FilterFactory createFilterFactory(Class<? extends FilterFactory> filterFactoryClass)
            throws FilterNotCreatedException {
        try {
            return OBJECT_FACTORY.createObjectWithClass(filterFactoryClass);
        } catch (Exception e) {
            throw new FilterNotCreatedException(e);
        }
    }
}
