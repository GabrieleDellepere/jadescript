package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.strictSubType;
import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class TypeLatticeComputer {

    private final LazyInit<TypeComparator> comparator;
    private final LazyInit<BuiltinTypeProvider> builtins;
    private final LazyInit<JvmTypeHelper> jvm;
    private final LazyInit<TypeSolver> solver;


    public TypeLatticeComputer(SemanticsModule module) {
        this.comparator = lazyInit(() -> {
            return module.get(TypeComparator.class);
        });

        this.builtins = lazyInit(() -> {
            return module.get(BuiltinTypeProvider.class);
        });

        this.jvm = lazyInit(() -> {
            return module.get(JvmTypeHelper.class);
        });

        this.solver = lazyInit(() -> {
            return module.get(TypeSolver.class);
        });
    }


    public Maybe<OntologyType> getOntologyGLB(
        Maybe<OntologyType> mt1,
        Maybe<OntologyType> mt2,
        List<Maybe<OntologyType>> mts
    ) {
        //XXX: Change this for ontology Multi-inheritance
        Maybe<OntologyType> result = getOntologyGLB(mt1, mt2);
        for (Maybe<OntologyType> mt : mts) {
            if (result.isNothing()) {
                return nothing();
            }
            result = getOntologyGLB(result, mt);
        }
        return result;
    }


    public Maybe<OntologyType> getOntologyGLB(
        Maybe<OntologyType> mt1,
        Maybe<OntologyType> mt2
    ) {
        //XXX: Change this for ontology Multi-inheritance
        if (mt1.isNothing()) {
            return nothing();
        }
        if (mt2.isNothing()) {
            return nothing();
        }
        final OntologyType t1 = mt1.toNullable();
        final OntologyType t2 = mt2.toNullable();
        if (t1.isSuperOrEqualOntology(t2)) {
            return some(t2);
        } else if (t2.isSuperOrEqualOntology(t1)) {
            return some(t1);
        } else {
            return nothing();
        }
    }


    public IJadescriptType getGLB(
        @Nullable String errorMessage,
        IJadescriptType t0,
        IJadescriptType... ts
    ) {
        if (ts.length == 0) {
            return t0;
        } else if (ts.length == 1) {
            return getLUB(t0, ts[0], errorMessage);
        } else {
            return Arrays.stream(ts).reduce(
                t0,
                (a, b) -> this.getGLB(a, b, errorMessage)
            );
        }
    }


    public IJadescriptType getGLB(
        IJadescriptType t1,
        IJadescriptType t2,
        @Nullable String errorMessage
    ) {
        final TypeRelationship comparison = comparator.get().compare(t1, t2);
        if (superTypeOrEqual().matches(comparison)) {
            return t2;
        } else if (strictSubType().matches(comparison)) {
            return t1;
        } else {
            final String msg = errorMessage != null
                ? errorMessage
                : "Could not find common subtype for types '" + t1 + "' and '" +
                t2 + "'";

            return builtins.get().nothing(msg);
        }

    }


    public IJadescriptType getLUB(
        IJadescriptType t1,
        IJadescriptType t2,
        @Nullable String errorMessage
    ) {
        final TypeRelationship comparison = comparator.get().compare(t1, t2);

        if (superTypeOrEqual().matches(comparison)) {
            return t1;
        }

        if (strictSubType().matches(comparison)) {
            return t2;
        }


        final Optional<IJadescriptType> lub = t1.allSupertypesBFS()
            .filter(st1 -> comparator.get()
                .compare(st1, t2).is(superTypeOrEqual()))
            .findFirst();

        if (lub.isPresent()) {
            return lub.get();
        }

        if (t1.asJvmTypeReference().getType() instanceof JvmDeclaredType
            && t2.asJvmTypeReference().getType() instanceof JvmDeclaredType) {
            List<JvmTypeReference> parentChainOfA =
                jvm.get().getParentChainIncluded(t1.asJvmTypeReference());
            for (JvmTypeReference candidateCommonParent : parentChainOfA) {
                if (jvm.get().isAssignable(
                    candidateCommonParent,
                    t2.asJvmTypeReference(),
                    false
                )) {
                    return solver.get().fromJvmTypeReference(
                        candidateCommonParent
                    ).ignoreBound();
                }
            }
        }

        final String msg = errorMessage != null
            ? errorMessage
            : "Could not find common supertype of types '" + t1 + "' and '" +
            t2 + "'";

        return builtins.get().any(msg);
    }


}
