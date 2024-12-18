package it.unipr.ailab.jadescript.semantics.context.symbol;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.FeatureWithSlots;
import it.unipr.ailab.jadescript.jadescript.SlotDeclaration;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.GlobalCallable;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.some;

public class OntologyElementConstructor implements GlobalCallable {

    private final Maybe<String> ontoFQName;
    private final String name;
    private final LazyInit<IJadescriptType> type;
    private final Map<String, IJadescriptType> parameterTypesByName;
    private final List<String> parameterNames;
    private final SearchLocation sourceLocation;


    private OntologyElementConstructor(
        Maybe<String> ontoFQName,
        String name,
        LazyInit<IJadescriptType> type,
        Map<String, IJadescriptType> parameterTypesByName,
        List<String> parameterNames,
        SearchLocation sourceLocation
    ) {
        this.ontoFQName = ontoFQName;
        this.name = name;
        this.type = type;
        this.parameterTypesByName = parameterTypesByName;
        this.parameterNames = parameterNames;
        this.sourceLocation = sourceLocation;
    }


    public static OntologyElementConstructor fromJvmStaticOperation(
        SemanticsModule module,
        JvmTypeNamespace jvmTypeNamespace,
        JvmOperation operation,
        String ontoFQName,
        SearchLocation location
    ) {
        List<JvmFormalParameter> parameters = operation.getParameters();
        if (parameters == null) {
            parameters = List.of();
        }

        List<String> paramNames = new ArrayList<>();
        Map<String, IJadescriptType> paramNamesToTypes = new HashMap<>();

        final IJadescriptType anyAE =
            module.get(BuiltinTypeProvider.class).anyAgentEnv();


        for (JvmFormalParameter parameter : parameters) {
            if (parameter == null) {
                continue;
            }

            final String paramName = parameter.getName();
            final JvmTypeReference paramTypeRef =
                parameter.getParameterType();
            if (paramName == null || paramTypeRef == null) {
                continue;
            }

            if (paramName.equals(SemanticsConsts.AGENT_ENV)) {
                continue;
            }

            final IJadescriptType solvedType =
                jvmTypeNamespace.resolveType(paramTypeRef).ignoreBound();

            final TypeComparator comparator = module.get(TypeComparator.class);

            if (comparator.compare(anyAE, solvedType).is(superTypeOrEqual())) {
                continue;
            }

            paramNames.add(paramName);
            paramNamesToTypes.put(paramName, solvedType);
        }

        return new OntologyElementConstructor(
            some(ontoFQName),
            operation.getSimpleName(),
            new LazyInit<>(() -> jvmTypeNamespace.resolveType(
                operation.getReturnType()
            ).ignoreBound()),
            paramNamesToTypes,
            paramNames,
            location == null
                ? jvmTypeNamespace.currentLocation()
                : location
        );
    }


    public static Maybe<OntologyElementConstructor> fromFeature(
        SemanticsModule module,
        Maybe<ExtendingFeature> f,
        Maybe<String> ontoFQName,
        SearchLocation currentLocation
    ) {
        if (f.isNothing()) {
            return Maybe.nothing();
        }

        final ExtendingFeature ontologyElement = f.toNullable();

        final List<String> paramNames;
        final Map<String, IJadescriptType> paramTypesByName;
        if (ontologyElement instanceof FeatureWithSlots) {

            final TypeExpressionSemantics typeExpressionSemantics =
                module.get(TypeExpressionSemantics.class);
            final MaybeList<SlotDeclaration> slots =
                f.__toList(i -> ((FeatureWithSlots) i).getSlots());
            paramNames = new ArrayList<>(slots.size());
            paramTypesByName = new HashMap<>(slots.size());

            for (Maybe<SlotDeclaration> slot : slots) {
                String name = slot.__(SlotDeclaration::getName).orElse("");
                Maybe<TypeExpression> typeExpr =
                    slot.__(SlotDeclaration::getType);
                if (typeExpr.isPresent() && !name.isBlank()) {
                    paramNames.add(name);
                    paramTypesByName.put(
                        name,
                        typeExpressionSemantics.toJadescriptType(typeExpr)
                    );
                }
            }
        } else {
            paramNames = List.of();
            paramTypesByName = Map.of();
        }

        return some(new OntologyElementConstructor(
            ontoFQName,
            ontologyElement.getName() == null ? "" : ontologyElement.getName(),
            new LazyInit<>(() -> {
                final TypeSolver typeSolver = module.get(TypeSolver.class);
                final BuiltinTypeProvider builtins =
                    module.get(BuiltinTypeProvider.class);
                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);

                return some(
                    compilationHelper.getFullyQualifiedName(ontologyElement)
                )
                    .__(fqn -> fqn.toString("."))
                    .nullIf(String::isBlank)
                    .__(typeSolver::fromFullyQualifiedName)
                    .orElse(builtins.anyOntologyElement());

            }),
            paramTypesByName,
            paramNames,
            currentLocation
        ));
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public IJadescriptType returnType() {
        return type.get();
    }


    @Override
    public Map<String, IJadescriptType> parameterTypesByName() {
        return parameterTypesByName;
    }


    @Override
    public List<String> parameterNames() {
        return parameterNames;
    }


    @Override
    public List<IJadescriptType> parameterTypes() {
        final Map<String, IJadescriptType> ptbn = parameterTypesByName();
        return parameterNames().stream()
            .filter(ptbn::containsKey)
            .map(ptbn::get)
            .collect(Collectors.toList());
    }


    @Override
    public boolean isWithoutSideEffects() {
        return true;
    }


    @Override
    public String compileInvokeByArity(
        List<String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return javaMethodName() +
            "(" + String.join(" ,", compiledRexprs) + ")";
    }


    @Override
    public String compileInvokeByName(
        Map<String, String> compiledRexprs,
        BlockElementAcceptor acceptor
    ) {
        return javaMethodName() + "(" + String.join(
            " ,",
            CallSemantics.sortToMatchParamNames(
                compiledRexprs,
                parameterNames()
            )
        ) + ")";
    }


    private String javaMethodName() {
        if (ontoFQName.isNothing()) {
            return name;
        }

        return ontoFQName.toNullable() + "." + name;
    }


    @Override
    public SearchLocation sourceLocation() {
        return sourceLocation;
    }

}
