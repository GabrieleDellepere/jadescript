package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.AgentDeclarationContext;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import jade.wrapper.ContainerController;
import jadescript.java.JadescriptAgentController;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 27/04/18.
 */
@Singleton
public class AgentDeclarationSemantics
    extends UsesOntologyTopLevelDeclarationSemantics<Agent>
    implements AgentAssociatedDeclarationSemantics<Agent> {

    public AgentDeclarationSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public IJadescriptType getAssociatedAgentType(
        Maybe<Agent> input,
        JvmDeclaredType beingDeclared
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final TypeSolver typeSolver = module.get(TypeSolver.class);

        return typeHelper.beingDeclaredAgentType(
            beingDeclared,
            Maybe.someStream(input.__(Agent::getSuperTypes))
                .findFirst()
                .orElse(nothing())
                .__(JvmParameterizedTypeReference::getType)
                .require(t -> t instanceof JvmDeclaredType)
                .__(t -> (JvmDeclaredType) t)
                .__(typeSolver::fromJvmType)
        );
    }


    @Override
    protected void prepareAndEnterContext(
        Maybe<Agent> input,
        JvmDeclaredType jvmDeclaredType
    ) {

        final ContextManager contextManager = module.get(ContextManager.class);
        contextManager.enterTopLevelDeclaration((module, outer) ->
            new AgentDeclarationContext(
                module,
                outer,
                getUsedOntologyTypes(input),
                jvmDeclaredType
            )
        );

        contextManager.enterProceduralFeatureContainer(
            module.get(TypeSolver.class).fromJvmTypePermissive(jvmDeclaredType),
            input
        );

    }


    @Override
    protected void exitContext(Maybe<Agent> input) {
        //from ProceduralFeatureContainerContext:
        module.get(ContextManager.class).exit();

        //from AgentDeclarationContext:
        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateOnEdit(
        Maybe<Agent> input,
        ValidationMessageAcceptor acceptor
    ) {
        // Keep super call at the end
        super.validateOnEdit(input, acceptor);
    }


    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(Maybe<Agent> input) {
        return Collections.singletonList(
            module.get(BuiltinTypeProvider.class).agent()
        );
    }


    @Override
    public Optional<IJadescriptType> defaultSuperType(Maybe<Agent> input) {
        return Optional.ofNullable(
            module.get(BuiltinTypeProvider.class).agent()
        );
    }


    @Override
    public void populateMainMembers(
        Maybe<Agent> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        super.populateMainMembers(input, members, itClass);

        if (input.isNothing()) {
            return;
        }


        final Agent inputSafe = input.toNullable();
        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);


        populateAgentAssociatedMembers(
            input,
            members,
            module,
            itClass
        );

        members.add(jvmTB.toMethod(
            inputSafe,
            THE_AGENT,
            jvm.typeRef(itClass),
            itMethod -> compilationHelper.createAndSetBody(
                itMethod,
                scb -> {
                    w.returnStmnt(w.expr("this")).writeSonnet(scb);
                }
            )
        ));

        members.add(jvmTB.toMethod(
            inputSafe,
            "setup",
            builtins.javaVoid().asJvmTypeReference(),
            itMethod -> {
                itMethod.setVisibility(JvmVisibility.PROTECTED);
                compilationHelper.createAndSetBody(itMethod, scb -> {
                    w.callStmnt("super.setup").writeSonnet(scb);
                    w.callStmnt("__initializeAgentEnv").writeSonnet(scb);
                    w.callStmnt("__initializeProperties").writeSonnet(scb);
                    w.callStmnt("this.__onCreate").writeSonnet(scb);
                });
            }
        ));

        members.add(jvmTB.toMethod(
            inputSafe,
            "__registerCodecs",
            builtins.javaVoid().asJvmTypeReference(),
            itMethod -> {
                itMethod.setVisibility(JvmVisibility.PROTECTED);
                itMethod.getParameters().add(
                    jvmTB.toParameter(
                        inputSafe,
                        "cm",
                        jvm.typeRef(jade.content.ContentManager.class)
                    )
                );

                compilationHelper.createAndSetBody(itMethod, scb -> {
                    w.callStmnt(
                        "super.__registerCodecs",
                        w.expr("cm")
                    ).writeSonnet(scb);
                    w.callStmnt(
                        "cm.registerLanguage",
                        w.expr(CODEC_VAR_NAME)
                    ).writeSonnet(scb);
                });
            }
        ));


        Maybe<OnCreateHandler> createHandler = someStream(
            input.__(FeatureContainer::getFeatures)
        ).filter(maybeF ->
                //f instanceof OnCreateHandler
                maybeF.__(f -> f instanceof OnCreateHandler)
                    .orElse(false)
            ).map(maybeF -> maybeF.__(f -> (OnCreateHandler) f)).findAny()
            .orElse(Maybe.nothing());


        input.__(NamedElement::getName).safeDo(nameSafe -> {
            addCreateMethod(members, inputSafe, createHandler);
        });


        if (createHandler.isNothing()) {
            addDefaultOnCreate(input, members);
        }


    }


    private void addDefaultOnCreate(
        Maybe<Agent> input,
        EList<JvmMember> members
    ) {
        if (input.isNothing()) {
            return;
        }

        final Agent agent = input.toNullable();

        members.add(module.get(JvmTypesBuilder.class).toMethod(
            agent,
            "__onCreate",
            module.get(BuiltinTypeProvider.class)
                .javaVoid().asJvmTypeReference(),
            itMethod -> {
                itMethod.setVisibility(JvmVisibility.PRIVATE);
                module.get(CompilationHelper.class).createAndSetBody(
                    itMethod,
                    scb -> {
                        scb.line("// do nothing;");
                    }
                );
            }
        ));
    }


    private void addCreateMethod(
        EList<JvmMember> members,
        Agent inputSafe,
        Maybe<OnCreateHandler> createHandler
    ) {
        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final TypeExpressionSemantics tes = module.get(
            TypeExpressionSemantics.class);

        final JvmTypeReference ctrlrType =
            jvm.typeRef(JadescriptAgentController.class);

        members.add(jvmTB.toMethod(inputSafe, "create", ctrlrType, itMethod -> {
            itMethod.setVisibility(JvmVisibility.PUBLIC);
            itMethod.setStatic(true);


            final Maybe<String> inputFQN =
                some(compilationHelper.getFullyQualifiedName(inputSafe))
                    .__(fqn -> fqn.toString("."))
                    .nullIf(String::isBlank);


            if (createHandler.isPresent()) {

                // get feature, get params, compile into create's params
                OnCreateHandler createHandlerSafe =
                    createHandler.toNullable();

                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    "_container",
                    jvm.typeRef(ContainerController.class)
                ));
                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    "_agentName",
                    jvm.typeRef(String.class)
                ));

                itMethod.getExceptions().add(jvm.typeRef(
                    jade.wrapper.StaleProxyException.class
                ));

                List<ExpressionWriter> createArgs = new ArrayList<>();
                createArgs.add(w.expr("_container"));
                createArgs.add(w.expr("_agentName"));
                createArgs.add(w.expr(
                    inputFQN.orElse("jadescript.core.Agent") + ".class"
                ));

                for (FormalParameter parameter :
                    createHandlerSafe.getParameters()) {
                    if (parameter == null) {
                        continue;
                    }

                    final String paramName = parameter.getName();

                    if (paramName == null) {
                        continue;
                    }


                    final TypeExpression paramTypeRef = parameter.getType();

                    if (paramTypeRef == null) {
                        continue;
                    }

                    final IJadescriptType paramType =
                        tes.toJadescriptType(some(paramTypeRef));

                    itMethod.getParameters().add(jvmTB.toParameter(
                        parameter,
                        paramName,
                        paramType.asJvmTypeReference()
                    ));

                    createArgs.add(w.expr(paramName));
                }

                compilationHelper.createAndSetBody(
                    itMethod,
                    scb -> w.returnStmnt(w.callExpr(
                        "jadescript.java.JadescriptAgentController" +
                            ".createRaw",
                        createArgs.toArray(new ExpressionWriter[0])
                    )).writeSonnet(scb)

                );
            } else {
                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    "_container",
                    jvm.typeRef(ContainerController.class)
                ));

                itMethod.getParameters().add(jvmTB.toParameter(
                    inputSafe,
                    "_agentName",
                    jvm.typeRef(String.class)
                ));

                itMethod.getExceptions().add(
                    jvm.typeRef(jade.wrapper.StaleProxyException.class)
                );

                List<ExpressionWriter> createArgs = new ArrayList<>();
                createArgs.add(w.expr("_container"));
                createArgs.add(w.expr("_agentName"));
                createArgs.add(w.expr(
                    inputFQN.orElse("jadescript.core.Agent") + ".class"
                ));


                compilationHelper.createAndSetBody(
                    itMethod,
                    scb -> w.returnStmnt(w.callExpr(
                        "jadescript.java.JadescriptAgentController" +
                            ".createRaw",
                        createArgs.toArray(new ExpressionWriter[0])
                    )).writeSonnet(scb)
                );
            }
        }));
    }

}
