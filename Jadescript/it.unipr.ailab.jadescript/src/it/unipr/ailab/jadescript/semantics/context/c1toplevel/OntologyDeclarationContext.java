package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.WithSupertype;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmDeclaredType;

import java.util.stream.Stream;

public class OntologyDeclarationContext
        extends TopLevelDeclarationContext
        implements WithSupertype, OntologyAssociated {
    private final JvmDeclaredType ontology;
    private final LazyValue<IJadescriptType> ontoType;
    private final LazyValue<TypeNamespace> ontoNamespace;

    public OntologyDeclarationContext(
            SemanticsModule module,
            FileContext outer,
            JvmDeclaredType ontology
    ) {
        super(module, outer);
        this.ontology = ontology;
        this.ontoType = new LazyValue<>(() ->
            module.get(TypeHelper.class).jtFromJvmType(this.ontology));
        this.ontoNamespace = new LazyValue<>(() ->
            this.ontoType.get().namespace());
    }

    public String getOntologyName(){
        return ontoType.get().getJadescriptName();
    }

    @Override
    public Maybe<Searcheable> superTypeSearcheable() {
        return ontoNamespace.get().superTypeSearcheable();
    }

    @Override
    public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
        return Stream.empty();
    }

    @Override
    public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
        return Stream.of(new OntologyAssociation(ontoType.get(),
            OntologyAssociation.O.INSTANCE));
    }

    @Override
    public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
        return Stream.empty();
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is OntologyDeclarationContext {");
        scb.line("ontologyType = " + ontoType.get().getDebugPrint());
        scb.close("}");
        debugDumpOntologyAssociations(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<init ontology " + getOntologyName() + ">";
    }

    @Override
    public boolean canUseAgentReference() {
        return false;
    }
}
