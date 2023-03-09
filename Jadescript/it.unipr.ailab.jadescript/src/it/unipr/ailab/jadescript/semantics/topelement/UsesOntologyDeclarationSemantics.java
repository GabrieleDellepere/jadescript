package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.GlobalFunctionOrProcedure;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.UsesOntologyElement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import jade.content.ContentManager;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 27/04/18.
 */
@Singleton
public abstract class UsesOntologyDeclarationSemantics
    <T extends UsesOntologyElement>
    extends ExtendingDeclarationSemantics<T>
    implements OntologyAssociatedDeclarationSemantics<T> {

    public UsesOntologyDeclarationSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public List<IJadescriptType> getUsedOntologyTypes(Maybe<T> input) {
        return getUsedOntologiesTypeRefs(input).stream()
            .map(module.get(TypeHelper.class)::jtFromJvmTypeRef)
            .collect(Collectors.toList());
    }


    @Override
    public void validate(Maybe<T> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;

        final List<Maybe<JvmTypeReference>> ontologies = Maybe.toListOfMaybes(
            input.__(UsesOntologyElement::getOntologies)
        );

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        for (int i = 0; i < ontologies.size(); i++) {
            Maybe<JvmTypeReference> ontologyTypeRef = ontologies.get(i);
            IJadescriptType ontology = ontologyTypeRef
                .__(typeHelper::jtFromJvmTypeRef)
                .orElseGet(() -> typeHelper.ANY);

            validationHelper.assertExpectedType(
                jade.content.onto.Ontology.class,
                ontology,
                "InvalidOntologyType",
                input,
                JadescriptPackage.eINSTANCE.getUsesOntologyElement_Ontologies(),
                i,
                acceptor
            );
        }

        super.validate(input, acceptor);

    }


    /**
     * Infers a {@link UsesOntologyElement}s, i.e., agents or behaviours.<br />
     * Generated code for the ontology: {@code private Onto _ontology0 =
     * (Onto) Onto.getInstance();}<br />
     * Generated code for the codec: {@code public Codec _codec0 = new
     * SLCodec();}
     */
    @Override
    public void populateMainMembers(
        Maybe<T> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        if (input == null) {
            return;
        }

        List<JvmTypeReference> ontologyTypes = getUsedOntologiesTypeRefs(input);

        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        input.safeDo(inputsafe -> {
            for (final JvmTypeReference ontologyType : ontologyTypes) {
                members.add(jvmTypesBuilder.toField(
                    inputsafe,
                    CompilationHelper.extractOntologyVarName(ontologyType),
                    ontologyType,
                    itField -> {
                        itField.setVisibility(JvmVisibility.PUBLIC);

                        compilationHelper.createAndSetInitializer(
                            itField,
                            scb -> {
                                String ontologyName =
                                    ontologyType.getQualifiedName('.');

                                scb.line(
                                    "(" + ontologyName + ") " +
                                        ontologyName + ".getInstance()"
                                );
                            }


                        );
                    }
                ));
            }


            members.add(jvmTypesBuilder.toMethod(
                inputsafe,
                "__registerOntologies",
                typeHelper.VOID.asJvmTypeReference(),
                itMethod -> {
                    itMethod.getParameters().add(
                        jvmTypesBuilder.toParameter(
                            inputsafe,
                            "cm",
                            typeHelper.typeRef(ContentManager.class)
                        )
                    );
                    compilationHelper.createAndSetBody(itMethod, scb -> {
                        if (!(inputsafe instanceof GlobalFunctionOrProcedure)) {
                            scb.line("super.__registerOntologies(cm);");
                        }

                        for (JvmTypeReference ontologyType : ontologyTypes) {
                            scb.line("cm.registerOntology(" +
                                ontologyType.getQualifiedName('.')
                                + ".getInstance());");
                        }
                    });
                }
            ));


            members.add(jvmTypesBuilder.toField(
                inputsafe,
                CODEC_VAR_NAME,
                typeHelper.typeRef(jade.content.lang.Codec.class),
                itField -> {
                    itField.setVisibility(JvmVisibility.PUBLIC);
                    compilationHelper.createAndSetInitializer(
                        itField,
                        scb -> scb.line(
                            "new jade.content.lang.leap.LEAPCodec" +
                                "()")
                    );
                }
            ));
        });

        super.populateMainMembers(input, members, itClass);
    }


    @NotNull
    private List<JvmTypeReference> getUsedOntologiesTypeRefs(Maybe<T> input) {
        List<Maybe<JvmTypeReference>> ontologies =
            Maybe.toListOfMaybes(input.__(UsesOntologyElement::getOntologies));

        List<JvmTypeReference> ontologyTypes = ontologies.stream()
            .filter(Maybe::isPresent)
            .map(Maybe::toNullable)
            .collect(Collectors.toCollection(ArrayList::new));
        if (ontologyTypes.isEmpty()) {
            final TypeHelper typeHelper = module.get(TypeHelper.class);
            ontologyTypes.add(typeHelper.typeRef(
                jadescript.content.onto.Ontology.class
            ));
        }
        return ontologyTypes;
    }


}
