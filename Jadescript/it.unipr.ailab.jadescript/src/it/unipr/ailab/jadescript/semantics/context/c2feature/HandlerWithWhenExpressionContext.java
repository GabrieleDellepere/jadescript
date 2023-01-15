package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public abstract class HandlerWithWhenExpressionContext
    extends EventHandlerContext {

    public HandlerWithWhenExpressionContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer,
        String eventType
    ) {
        super(module, outer, eventType);
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is HandlerWithWhenExpressionContext {");
        scb.close("}");
    }


}
