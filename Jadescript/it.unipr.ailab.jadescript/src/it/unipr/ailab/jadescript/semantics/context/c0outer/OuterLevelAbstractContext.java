package it.unipr.ailab.jadescript.semantics.context.c0outer;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.search.ModuleGlobalLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.CallableMember;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedOperation;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import jadescript.lang.JadescriptGlobalFunction;
import jadescript.lang.JadescriptGlobalProcedure;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmTypeNamespace.CTOR_INTERNAL_NAME;
import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

/**
 * Base class for File and Module contexts.
 */
public abstract class OuterLevelAbstractContext extends Context
    implements RawTypeReferenceSolverContext {

    public OuterLevelAbstractContext(SemanticsModule module) {
        super(module);
    }


    protected Stream<? extends CallableMember>
    getCallableStreamFromDeclaredType(
        JvmTypeReference methodJVMClassRef,
        JvmDeclaredType type,
        Predicate<String> name,
        Predicate<IJadescriptType> returnType,
        BiPredicate<Integer, Function<Integer, String>> parameterNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>>
            parameterTypes
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        IJadescriptType jadescriptType = typeHelper.jtFromJvmTypeRef(
            methodJVMClassRef
        );
        Stream<? extends CallableMember> result;
        if (typeHelper.isAssignable(
            jade.core.behaviours.Behaviour.class,
            methodJVMClassRef
        )) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = searchBehaviourCtorFunction(
                namespace,
                name,
                returnType,
                parameterNames,
                parameterTypes
            );
        } else if (typeHelper.isAssignable(
            jade.content.onto.Ontology.class,
            methodJVMClassRef
        )) {
            Stream<Integer> ontoStream = Stream.of(0);
            ontoStream = safeFilter(
                ontoStream,
                (__) -> methodJVMClassRef.getSimpleName(),
                name
            );
            ontoStream = safeFilter(
                ontoStream,
                (__) -> jadescriptType,
                returnType
            );
            ontoStream = safeFilter(
                ontoStream,
                (__) -> 0,
                (__) -> i -> "",
                parameterNames
            );
            ontoStream = safeFilter(
                ontoStream,
                (__) -> 0,
                (__) -> i -> typeHelper.NOTHING,
                parameterTypes
            );
            result = ontoStream
                .map((__) -> ontoInstanceAsCallable(
                        methodJVMClassRef.getSimpleName(),
                        jadescriptType
                    )
                );
        } else if (typeHelper.isAssignable(
            JadescriptGlobalFunction.class,
            methodJVMClassRef
        ) || typeHelper.isAssignable(
            JadescriptGlobalProcedure.class,
            methodJVMClassRef
        )) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = namespace.searchCallable(
                name,
                returnType,
                parameterNames,
                parameterTypes
            ).map(m -> SymbolUtils.setDereferenceByCtor(m, jadescriptType));
        } else {
            result = Stream.empty();
        }

        return result.map(cs -> SymbolUtils.changeLocation(
            cs,
            new ModuleGlobalLocation(
                type.getPackageName() != null ? type.getPackageName() : ""
            )
        ));
    }


    protected Stream<? extends CallableMember>
    getCallableStreamFromDeclaredType(
        JvmTypeReference methodJVMClassRef,
        JvmDeclaredType type,
        String name,
        Predicate<IJadescriptType> returnType,
        BiPredicate<Integer, Function<Integer, String>> parameterNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        IJadescriptType jadescriptType =
            typeHelper.jtFromJvmTypeRef(methodJVMClassRef);
        Stream<? extends CallableMember> result;
        if (typeHelper.isAssignable(
            jade.core.behaviours.Behaviour.class,
            methodJVMClassRef
        )) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = searchBehaviourCtorFunction(
                namespace,
                name,
                returnType,
                parameterNames,
                parameterTypes
            );
        } else if (typeHelper.isAssignable(
            jade.content.onto.Ontology.class,
            methodJVMClassRef
        )) {
            Stream<Integer> ontoStream = Stream.of(0);
            ontoStream = safeFilter(
                ontoStream,
                (__) -> methodJVMClassRef.getSimpleName(),
                name::equals
            );
            ontoStream = safeFilter(
                ontoStream,
                (__) -> jadescriptType,
                returnType
            );
            ontoStream = safeFilter(
                ontoStream,
                (__) -> 0,
                (__) -> i -> "",
                parameterNames
            );
            ontoStream = safeFilter(
                ontoStream,
                (__) -> 0,
                (__) -> i -> typeHelper.NOTHING,
                parameterTypes
            );
            result = ontoStream.map((__) -> ontoInstanceAsCallable(
                methodJVMClassRef.getSimpleName(),
                jadescriptType
            ));
        } else if (typeHelper.isAssignable(
            JadescriptGlobalFunction.class,
            methodJVMClassRef
        ) || typeHelper.isAssignable(
            JadescriptGlobalProcedure.class,
            methodJVMClassRef
        )) {
            JvmTypeNamespace namespace = new JvmTypeNamespace(module, type);
            result = namespace.searchCallable(
                name,
                returnType,
                parameterNames,
                parameterTypes
            ).map(m -> SymbolUtils.setDereferenceByCtor(m, jadescriptType));
        } else {
            result = Stream.empty();
        }
        return result.map(cs -> SymbolUtils.changeLocation(
            cs,
            new ModuleGlobalLocation(
                type.getPackageName() != null ? type.getPackageName() : ""
            )
        ));
    }


    protected Stream<? extends CallableMember> getCallableStreamFromFQName(
        String fqName,
        String name,
        Predicate<IJadescriptType> returnType,
        BiPredicate<Integer, Function<Integer, String>> parameterNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        JvmTypeReference methodJVMClassRef =
            module.get(TypeHelper.class).typeRef(
                fqName);
        if (methodJVMClassRef.getType() instanceof JvmDeclaredType) {
            final JvmDeclaredType type =
                (JvmDeclaredType) methodJVMClassRef.getType();
            return getCallableStreamFromDeclaredType(
                methodJVMClassRef,
                type,
                name,
                returnType,
                parameterNames,
                parameterTypes
            );
        }
        return Stream.empty();
    }


    private CallableMember ontoInstanceAsCallable(
        String name,
        IJadescriptType ontoType
    ) {
        final String ontoTypeCompiled = ontoType.compileToJavaTypeReference();
        return new ContextGeneratedOperation(
            true,
            name,
            ontoType,
            List.of(),
            (__1, __2) -> "((" + ontoTypeCompiled + ") " +
                ontoTypeCompiled + ".getInstance())",
            (__1, __2) -> "((" + ontoTypeCompiled + ") " +
                ontoTypeCompiled + ".getInstance())"
        );
    }


    private Stream<? extends CallableMember> searchBehaviourCtorFunction(
        CallableMember.Namespace namespace,
        String name,
        Predicate<IJadescriptType> returnType,
        BiPredicate<Integer, Function<Integer, String>> parameterNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return searchBehaviourCtorFunction(
            namespace,
            name::equals,
            returnType,
            parameterNames,
            parameterTypes
        );
    }


    private Stream<? extends CallableMember> searchBehaviourCtorFunction(
        CallableMember.Namespace namespace,
        Predicate<String> name,
        Predicate<IJadescriptType> returnType,
        BiPredicate<Integer, Function<Integer, String>> parameterNames,
        BiPredicate<Integer, Function<Integer, IJadescriptType>> parameterTypes
    ) {
        return namespace.searchCallable(//Searching locally (not in supertypes)
            CTOR_INTERNAL_NAME,
            returnType,
            parameterNames,
            parameterTypes
        ).filter(callable ->
            name.test(callable.returnType()
                .asJvmTypeReference().getSimpleName())
        );
    }

}
