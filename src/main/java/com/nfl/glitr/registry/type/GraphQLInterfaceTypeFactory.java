package com.nfl.glitr.registry.type;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.registry.datafetcher.query.batched.CompositeDataFetcherFactory;
import com.nfl.glitr.util.ReflectionUtil;
import graphql.schema.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * Factory implementation for the creation of {@link GraphQLInterfaceType}
 */
public class GraphQLInterfaceTypeFactory implements DelegateTypeFactory {

    private final TypeRegistry typeRegistry;


    public GraphQLInterfaceTypeFactory(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    @Override
    public GraphQLOutputType create(Class clazz) {
        return createInterfaceType(clazz);
    }

    /**
     * Creates the {@link GraphQLInterfaceType} dynamically for the given interface
     *
     * @param clazz class to be introspected
     * @return {@link GraphQLInterfaceType} object exposed via graphQL
     */
    private GraphQLInterfaceType createInterfaceType(Class clazz) {
        List<GraphQLFieldDefinition> fields = Arrays.stream(clazz.getMethods())
                .filter(ReflectionUtil::eligibleMethod)
                .sorted(Comparator.comparing(Method::getName))
                .map(method -> getGraphQLFieldDefinition(clazz, method))
                .collect(Collectors.toList());

        return newInterface()
                .name(clazz.getSimpleName())
                .description(ReflectionUtil.getDescriptionFromAnnotatedElement(clazz))
                .typeResolver(typeRegistry)
                .fields(fields)
                .build();
    }

    private GraphQLFieldDefinition getGraphQLFieldDefinition(Class clazz, Method method) {
        String name = ReflectionUtil.sanitizeMethodName(method.getName());
        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);

        Type fieldType = GenericTypeReflector.getExactReturnType(method, clazz);
        GraphQLType type = typeRegistry.convertToGraphQLOutputType(fieldType, name, true);
        if (type instanceof GraphQLTypeReference) {
            typeRegistry.getRegistry().putIfAbsent((Class) fieldType, type);
        }

        boolean nullable = ReflectionUtil.isAnnotatedElementNullable(method);
        if (!nullable || name.equals("id")) {
            type = new GraphQLNonNull(type);
        }

        return newFieldDefinition()
                .name(name)
                .description(description)
                .dataFetcher(CompositeDataFetcherFactory.create(Collections.singletonList(new PropertyDataFetcher(name))))
                .type((GraphQLOutputType) type)
                .build();
    }
}
