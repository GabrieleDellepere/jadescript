package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.JadescriptTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.utils.LazyInit;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class RealType extends BasicType {

    public RealType(
        SemanticsModule module
    ) {
        super(module, builtinPrefix + "real",
            "real",
            "jade.content.onto.BasicOntology.FLOAT",
            module.get(JvmTypeHelper.class).typeRef(Float.class),
            "0.0f"
        );
    }


    private final LazyInit<JadescriptTypeNamespace.Empty> namespace =
        lazyInit(() ->
            new JadescriptTypeNamespace.Empty(module, getLocation())
        );


    @Override
    public TypeNamespace namespace() {
        return this.namespace.get();
    }

}
