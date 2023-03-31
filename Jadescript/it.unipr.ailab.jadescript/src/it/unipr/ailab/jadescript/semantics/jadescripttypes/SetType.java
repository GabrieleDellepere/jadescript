package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.jadescript.ExtendingFeature;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import jadescript.util.JadescriptSet;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.helpers.TypeHelper.builtinPrefix;
import static it.unipr.ailab.maybe.Maybe.some;

public class SetType extends ParametricType
    implements EmptyCreatable, DeclaresOntologyAdHocClass {

    private final Map<String, Property> properties = new HashMap<>();
    private final List<Operation> operations = new ArrayList<>();
    private boolean initializedProperties = false;


    public SetType(
        SemanticsModule module,
        TypeArgument elementType
    ) {
        super(
            module,
            builtinPrefix + "Set",
            "Set",
            "SET",
            "of",
            "",
            "",
            "",
            Collections.singletonList(elementType),
            Collections.singletonList(module.get(TypeHelper.class).ANY)
        );

    }


    public static String getAdHocSetClassName(IJadescriptType elementType) {
        return "__SetClass_" + elementType.compileToJavaTypeReference().replace(
            ".",
            "_"
        );
    }


    public IJadescriptType getElementType() {
        return getTypeArguments().get(0).ignoreBound();
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(
            JadescriptSet.class,
            getTypeArguments().stream()
                .map(TypeArgument::asJvmTypeReference)
                .collect(Collectors.toList())
        );
    }


    @Override
    public void addBultinProperty(Property prop) {
        properties.put(prop.name(), prop);
    }


    private void initBuiltinProperties() {
        if (initializedProperties) {
            return;
        }
        this.addBultinProperty(
            Property.readonlyProperty(
                "size",
                module.get(TypeHelper.class).INTEGER,
                getLocation(),
                Property.compileGetWithCustomMethod("size")
            )
        );
        operations.add(Operation.operation(
            module.get(TypeHelper.class).VOID,
            "__add",
            Map.of("element", getElementType()),
            List.of("element"),
            getLocation(),
            false,
            (receiver, args) -> {
                final String s;
                if (args.size() >= 1) {
                    s = args.get(0);
                } else {
                    s = "/*internal error: missing arguments*/";
                }
                return receiver + ".add(" + s + ")";
            },
            (receiver, namedArgs) -> {
                return receiver + ".add(" + namedArgs.get("element") + ")";
            }
        ));

        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "contains",
            Map.of("o", getElementType()),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "containsAll",
            Map.of("o", this),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "containsAll",
            Map.of(
                "o",
                module.get(TypeHelper.class).LIST.apply(Arrays.asList(
                    getElementType()))
            ),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "containsAny",
            Map.of("o", this),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).BOOLEAN,
            "containsAny",
            Map.of(
                "o",
                module.get(TypeHelper.class).LIST.apply(
                    Arrays.asList(getElementType())
                )
            ),
            List.of("o"),
            getLocation(),
            true
        ));
        operations.add(Operation.operation(
            module.get(TypeHelper.class).VOID,
            "clear",
            Map.of(),
            List.of(),
            getLocation(),
            false
        ));
        this.initializedProperties = true;
    }


    @Override
    public boolean isSlottable() {
        return getTypeArguments().stream()
            .map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::isSlottable);
    }


    @Override
    public boolean isSendable() {
        return getTypeArguments().stream()
            .map(TypeArgument::ignoreBound)
            .allMatch(IJadescriptType::isSendable);
    }


    @Override
    public boolean isReferrable() {
        return true;
    }


    @Override
    public boolean hasProperties() {
        return true;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return getElementType().getDeclaringOntology();
    }


    @Override
    public boolean isCollection() {
        return true;
    }


    @Override
    public String getSlotSchemaName() {
        return "\"" + SetType.getAdHocSetClassName(getElementType()) + "\"";
    }


    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
            module,
            Maybe.nothing(),
            new ArrayList<>(getBuiltinProperties().values()),
            operations,
            getLocation()
        );
    }


    @Override
    public String compileNewEmptyInstance() {
        return "new jadescript.util.JadescriptSet<" +
            getElementType().compileToJavaTypeReference() + ">()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    @Override
    public Maybe<IJadescriptType> getElementTypeIfCollection() {
        return some(getElementType());
    }


    private Map<String, Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
    }


    @Override
    public void declareAdHocClass(
        EList<JvmMember> members,
        Maybe<ExtendingFeature> feature,
        HashMap<String, String> generatedSpecificClasses,
        List<StatementWriter> addSchemaWriters,
        List<StatementWriter> describeSchemaWriters,
        TypeExpression slotTypeExpression,
        Function<TypeExpression, String> schemaNameForSlotProvider,
        SemanticsModule module
    ) {
        if (feature.isNothing()) {
            return;
        }

        final ExtendingFeature featureSafe = feature.toNullable();

        IJadescriptType elementType = this.getElementType();
        String className = getAdHocSetClassName(elementType);

        if (generatedSpecificClasses.containsKey(className)) {
            return;
        }

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        members.add(jvmTB.toClass(
            featureSafe,
            className,
            itClass -> {
                itClass.setStatic(true);
                itClass.setVisibility(JvmVisibility.PUBLIC);
                final TypeHelper typeHelper =
                    module.get(TypeHelper.class);

                itClass.getSuperTypes().add(typeHelper.typeRef(
                    JadescriptSet.class,
                    elementType.asJvmTypeReference()
                ));


                itClass.getMembers().add(jvmTB.toMethod(
                    featureSafe,
                    "__fromSet",
                    typeHelper.typeRef(className),
                    itMeth -> {
                        itMeth.setVisibility(JvmVisibility.PUBLIC);
                        itMeth.setStatic(true);
                        itMeth.getParameters().add(jvmTB.toParameter(
                            featureSafe,
                            "set",
                            typeHelper.typeRef(
                                JadescriptSet.class,
                                elementType.asJvmTypeReference()
                            )
                        ));

                        module.get(CompilationHelper.class).createAndSetBody(
                            itMeth,
                            scb -> {
                                final String typeName =
                                    typeHelper.noGenericsTypeName(
                                        elementType.compileToJavaTypeReference()
                                    );
                                scb.line(className + " result = " +
                                    "new " + className + "();");
                                scb.line(
                                    "java.util.List<" + typeName +
                                        "> elements = new java.util" +
                                        ".ArrayList<>();"
                                );
                                scb.line("set.forEach(elements::add);");
                                scb.line("result.setElements(elements);");
                                scb.line("return result;");
                            }
                        );
                    }
                ));
            }
        ));

        generatedSpecificClasses.put(className, getCategoryName());

        addSchemaWriters.add(SemanticsConsts.w.simpleStmt(
            "add(new jade.content.schema.ConceptSchema(\"" +
                className + "\"), " + className + ".class);"));

        describeSchemaWriters.add(new StatementWriter() {
            @Override
            public void writeSonnet(SourceCodeBuilder scb) {
                EList<TypeExpression> typeParameters = slotTypeExpression
                    .getCollectionTypeExpression().getTypeParameters();

                if (typeParameters == null || typeParameters.size() != 1) {
                    return;
                }

                scb.add(
                    "jadescript.content.onto.Ontology" +
                        ".__populateSetSchema(" +
                        "(jade.content.schema.TermSchema) " +
                        "getSchema(" + schemaNameForSlotProvider
                        .apply(typeParameters.get(0)) + "), " +
                        "(jade.content.schema.ConceptSchema) " +
                        "getSchema(\"" + className + "\"));");

            }
        });
    }


    @Override
    public String getAdHocClassName() {
        return getAdHocSetClassName(getElementType());
    }


    @Override
    public String getConverterToAdHocClassMethodName() {
        return "__fromSet";
    }

}
