package com.nfl.glitr.util;

import com.nfl.glitr.annotation.*;
import graphql.execution.batched.Batched;
import graphql.execution.batched.BatchedDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtil {

    private final static Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);
    public final static String NAME_PREFIX = "class ";


    public static  String getClassName(Type type) {
        String fullName = type.toString();
        if (fullName.startsWith(NAME_PREFIX)) {
            return fullName.substring(NAME_PREFIX.length());
        }
        return fullName;
    }

    public static Class getClassFromType(Type type) {
        try {
            return Class.forName(getClassName(type));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines if the given method is eligible for inclusion in the GraphQL Schema
     * @param method to inspect
     * @return true if the method name starts with `get` or `is` and false otherwise or if the method or the corresponding field is annotated with GlitrIgnore
     */
    public static Boolean eligibleMethod(Method method) {
        Annotation glitrIgnore = method.getAnnotation(GlitrIgnore.class);
        if (glitrIgnore != null) {
            return false;
        }

        String methodName = method.getName();
        String fieldName = sanitizeMethodName(methodName);

        Field field;
        try {
            field = method.getDeclaringClass().getDeclaredField(fieldName);
            Annotation fieldAnnotations = field.getAnnotation(GlitrIgnore.class);
            if (fieldAnnotations != null) {
                return false;
            }
        } catch (NoSuchFieldException e) {
            // there is no field existing for the given method
            logger.debug("Field not found: {}", fieldName);
        }

        return (methodName.startsWith("is") || methodName.startsWith("get"))
                && method.getDeclaringClass() != Object.class
                && (!Map.class.isAssignableFrom(method.getReturnType()));
    }

    /**
     * Returns an alphabetically sorted Map of the method names to the actual Method and referencing class
     * @param clazz to inspect
     * @return method map
     */
    public static Map<String, Pair<Method, Class>> getMethodMap(Class clazz) {
        List<Method> methods = Arrays.asList(clazz.getMethods());
        methods.stream()
                .filter(method -> Collections.frequency(methods, method.getName()) > 1)
                .forEach(duplicateMethodName -> { throw new IllegalArgumentException("Method name duplicate for the given field [" + duplicateMethodName.getName() + "] in class [" + clazz.getSimpleName() + "]"); });

        return new TreeMap<>(methods.stream()
                .filter(ReflectionUtil::eligibleMethod)
                .collect(Collectors.toMap(Method::getName, eligibleMethod -> Pair.of(eligibleMethod, eligibleMethod.getDeclaringClass()))));
    }

    /**
     * Strip a string from the prefix `get` or `is` and un-capitalize.
     * @param name usually a getter name. e.g: `getTitle`
     * @return sanitized name `title`
     */
    public static String sanitizeMethodName(String name) {

        String sanitized;
        if (name.startsWith("is")) {
            sanitized = name.substring(2);
        } else if (name.startsWith("get")) {
            sanitized = name.substring(3);
        } else {
            sanitized = name;
        }

        return StringUtils.uncapitalize(sanitized);
    }

    public static Type getActualTypeArgumentFromType(ParameterizedType parameterizedType) {
        Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();

        if (fieldArgTypes.length > 1) {
            throw new IllegalArgumentException("Type can only have one generic argument: " + parameterizedType);
        }

        return fieldArgTypes[0];
    }

    public static GlitrArgument[] getArgumentsFromAnnotations(Map<Class, Annotation> annotationMap) {
        GlitrArgument[] singleAnnotationGlitrArguments = annotationMap.containsKey(GlitrArgument.class) ? new GlitrArgument[] {(GlitrArgument)annotationMap.get(GlitrArgument.class)}  : new GlitrArgument[0];
        GlitrArgument[] groupedAnnotationGlitrArguments = annotationMap.containsKey(GlitrArguments.class) ? ((GlitrArguments)annotationMap.get(GlitrArguments.class)).value() : new GlitrArgument[0];
        return ArrayUtils.addAll(singleAnnotationGlitrArguments, groupedAnnotationGlitrArguments);
    }

    public static GlitrArgument[] getArgumentsFromMethod(Method method) {
        GlitrArgument[] singleAnnotationGlitrArguments = method.isAnnotationPresent(GlitrArgument.class) ? method.getAnnotationsByType(GlitrArgument.class) : new GlitrArgument[0];
        GlitrArgument[] groupedAnnotationGlitrArguments = method.isAnnotationPresent(GlitrArguments.class) ? method.getAnnotation(GlitrArguments.class).value() : new GlitrArgument[0];
        return ArrayUtils.addAll(singleAnnotationGlitrArguments, groupedAnnotationGlitrArguments);
    }

    public static GlitrArgument[] getArgumentsFromField(Field field) {
        if (field == null) {
            return new GlitrArgument[0];
        }
        GlitrArgument[] singleAnnotationGlitrArguments = field.isAnnotationPresent(GlitrArgument.class) ? field.getAnnotationsByType(GlitrArgument.class) : new GlitrArgument[0];
        GlitrArgument[] groupedAnnotationGlitrArguments = field.isAnnotationPresent(GlitrArguments.class) ? field.getAnnotation(GlitrArguments.class).value() : new GlitrArgument[0];
        return ArrayUtils.addAll(singleAnnotationGlitrArguments, groupedAnnotationGlitrArguments);
    }

    /**
     * Tells whether a given data fetcher supports batching
     *  {code @Batched} on the get method or instance of {code BatchedDataFetcher}
     *
     * @param supplied data fetcher
     * @return true if batched, false otherwise
     */
    public static boolean isDataFetcherBatched(DataFetcher supplied) {
        if (supplied instanceof BatchedDataFetcher) {
            return true;
        }

        try {
            Method getMethod = supplied.getClass().getMethod("get", DataFetchingEnvironment.class);
            Batched batched = getMethod.getAnnotation(Batched.class);
            if (batched != null) {
                return true;
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        return false;
    }

    /**
     * Retrieves the GraphQL documentation description value if present on an element
     * @param element that may be annotated with the GlitrDescription
     * @return GlitrDescription.value if present or GlitrDescription.DEFAULT_DESCRIPTION otherwise
     */
    public static String getDescriptionFromAnnotatedElement(AnnotatedElement element) {
        return element.isAnnotationPresent(GlitrDescription.class) ? element.getAnnotation(GlitrDescription.class).value() : GlitrDescription.DEFAULT_DESCRIPTION;
    }

    public static boolean isAnnotatedElementNullable(AnnotatedElement element) {
        return !element.isAnnotationPresent(GlitrNonNull.class);
    }
}
