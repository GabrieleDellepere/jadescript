package it.unipr.ailab.jadescript.semantics.jadescripttypes.agent;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UserDefinedType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.namespace.AgentTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Collections;

public class UserDefinedAgentType
    extends UserDefinedType<BaseAgentType>
    implements AgentType {

    private final Maybe<IJadescriptType> superType;


    public UserDefinedAgentType(
        SemanticsModule module,
        JvmTypeReference jvmType,
        BaseAgentType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
        this.superType = Maybe.nothing();
    }


    public UserDefinedAgentType(
        SemanticsModule module,
        JvmTypeReference jvmType,
        Maybe<IJadescriptType> superType,
        BaseAgentType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
        this.superType = superType;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }


    @Override
    public AgentTypeNamespace namespace() {
        return new AgentTypeNamespace(
            module,
            this,
            Collections.emptyList(),
            Collections.emptyList()
        );
    }


    public AgentType getSuperAgentType() {
        if (superType.isPresent()
            && superType.toNullable() instanceof AgentType) {
            return ((AgentType) superType.toNullable());
        }

        final JvmType type = asJvmTypeReference().getType();
        if (type instanceof JvmDeclaredType) {
            final IJadescriptType result =
                module.get(TypeSolver.class).fromJvmTypeReference(
                    ((JvmDeclaredType) type).getExtendedClass()
                ).ignoreBound();
            if (result instanceof AgentType) {
                return ((AgentType) result);
            }
        }
        return module.get(BuiltinTypeProvider.class).agent();
    }

}
