package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnDeactivateHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;

public class OnDeactivateHandlerSemantics
    extends FeatureSemantics<OnDeactivateHandler> {

    public OnDeactivateHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnDeactivateHandler> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared
    ) {
        Maybe<QualifiedName> containerName = input
            .__(EcoreUtil2::getContainerOfType, TopElement.class)
            .__(module.get(CompilationHelper.class)::getFullyQualifiedName);
        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        input.safeDo(handlerSafe -> members.add(jvmTypesBuilder.toMethod(
            handlerSafe,
            "doOnDeactivate",
            module.get(TypeHelper.class).typeRef(void.class),
            itMethod -> {
                fillDoOnDeactivateMethod(
                    input,
                    containerName,
                    savedContext,
                    jvmTypesBuilder,
                    itMethod
                );
            }
        )));
    }


    private void fillDoOnDeactivateMethod(
        Maybe<OnDeactivateHandler> input,
        Maybe<QualifiedName> containerName,
        SavedContext savedContext,
        JvmTypesBuilder jvmTypesBuilder,
        JvmOperation itMethod
    ) {
        jvmTypesBuilder.setDocumentation(
            itMethod,
            containerName.__(QualifiedName::toString).extract(
                nullAsEmptyString) + " doOnDeactivate"
        );
        itMethod.setVisibility(JvmVisibility.PUBLIC);

        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        if (body.isPresent()) {
            module.get(CompilationHelper.class).createAndSetBody(
                itMethod,
                scb -> {
                    w.callStmnt("super.doOnDeactivate").writeSonnet(scb);
                    module.get(ContextManager.class).restore(
                        savedContext);
                    module.get(ContextManager.class).enterProceduralFeature(
                        OnDeactivateHandlerContext::new);

                    StaticState state =
                        StaticState.beginningOfOperation(module);


                    final PSR<SourceCodeBuilder> blockPSR =
                        module.get(CompilationHelper.class)
                            .compileBlockToNewSCB(state, body);


                    scb.add(encloseInGeneralHandlerTryCatch(blockPSR.result()));

                    module.get(ContextManager.class).exit();
                }
            );
        }
    }


    @Override
    public void validateFeature(
        Maybe<OnDeactivateHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        module.get(ContextManager.class).enterProceduralFeature(
            OnDeactivateHandlerContext::new);


        StaticState state = StaticState.beginningOfOperation(module);

        module.get(BlockSemantics.class).validate(body, state, acceptor);

        module.get(ContextManager.class).exit();
    }

}
