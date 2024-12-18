package it.unipr.ailab.jadescript.semantics.jadescripttypes.index;


import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.message.MessageType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.ParametricTypeSchema;
import it.unipr.ailab.maybe.utils.LazyInit;
import it.unipr.ailab.maybe.utils.Utils;
import jadescript.lang.Performative;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class TypeIndex {

    private final LazyInit<BuiltinTypeProvider> builtinTypeProvider;
    private final LazyInit<JvmTypeHelper> jvmTypeHelper;


    private final Map<String, Supplier<? extends IJadescriptType>>
        typeTable = new HashMap<>();

    private final Map<String,
        Supplier<ParametricTypeSchema<? extends IJadescriptType>>>
        parametricTypeTable = new HashMap<>();

    private final Map<String, Performative> messageClassToPerformativeMap
        = new HashMap<>();

    private final Map<Performative,
        Supplier<ParametricTypeSchema<? extends MessageType>>>
        performativeToMessageSubtypeMap = new HashMap<>();


    private final Map<String, Integer> expectedTypeParameters =
        new HashMap<>();

    private boolean initialized = false;


    public TypeIndex(SemanticsModule module) {

        this.builtinTypeProvider = LazyInit.lazyInit(
            () -> module.get(BuiltinTypeProvider.class)
        );

        this.jvmTypeHelper =
            LazyInit.lazyInit(() -> module.get(JvmTypeHelper.class));
    }


    private void doInitialize() {
        final BuiltinTypeProvider provider =
            builtinTypeProvider.get();

        for (
            Field declaredField : BuiltinTypeProvider.class.getDeclaredFields()
        ) {
            if (!isPackagePrivate(declaredField)) {
                continue;
            }

            final @Nullable BuiltinType builtinTypeAnnotation =
                declaredField.getAnnotation(BuiltinType.class);

            final @Nullable MessageBuiltinType messageAnnotation =
                declaredField.getAnnotation(MessageBuiltinType.class);

            if (builtinTypeAnnotation == null) {
                continue;
            }

            if (!Supplier.class.isAssignableFrom(declaredField.getType())) {
                throw new RuntimeException("Wrong type of the builtin-type " +
                    "field '" + declaredField.getName() + "'.");
            }

            final boolean isParametric =
                builtinTypeAnnotation.variadic()
                    || builtinTypeAnnotation.maxArgs() > 0;

            if (isParametric) {
                registerBuiltinParametricType(
                    provider,
                    declaredField,
                    builtinTypeAnnotation,
                    messageAnnotation
                );
            } else {
                registerBuiltinType(
                    provider,
                    declaredField,
                    builtinTypeAnnotation
                );
            }
        }

        this.initialized = true;
    }


    @SuppressWarnings("unchecked")
    private void registerBuiltinType(
        BuiltinTypeProvider provider,
        Field declaredField,
        @NotNull BuiltinType builtinTypeAnnotation
    ) {
        for (Class<?> jvmClass : builtinTypeAnnotation.value()) {
            final String fqn = JvmTypeHelper.noGenericsTypeName(
                jvmTypeHelper.get().typeRef(jvmClass)
                    .getQualifiedName('.')
            );

            try {
                typeTable.put(
                    fqn,
                    (Supplier<? extends IJadescriptType>)
                        declaredField.get(provider)
                );
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }
    }


    @SuppressWarnings("unchecked")
    private void registerBuiltinParametricType(
        BuiltinTypeProvider provider,
        Field declaredField,
        @NotNull BuiltinType builtinTypeAnnotation,
        @Nullable MessageBuiltinType messageAnnotation
    ) {
        for (Class<?> jvmClass : builtinTypeAnnotation.value()) {
            final JvmTypeReference typeRef =
                jvmTypeHelper.get().typeRef(jvmClass);
            final String fqn = JvmTypeHelper.noGenericsTypeName(
                typeRef.getQualifiedName('.')
            );

            final String simpleName = JvmTypeHelper.noGenericsTypeName(
                typeRef.getSimpleName()
            );


            try {
                final Supplier<ParametricTypeSchema<? extends IJadescriptType>>
                    parametricTypeSchemaSupplier =
                    (Supplier<ParametricTypeSchema<? extends IJadescriptType>>)
                        declaredField.get(provider);
                parametricTypeTable.put(
                    fqn,
                    parametricTypeSchemaSupplier
                );
                expectedTypeParameters.put(
                    fqn,
                    builtinTypeAnnotation.minArgs()
                );

                if (messageAnnotation != null) {
                    final Performative performative =
                        Performative.fromCode(messageAnnotation.value());

                    messageClassToPerformativeMap.put(simpleName, performative);
                    performativeToMessageSubtypeMap.put(
                        performative,
                        Utils.castSupplier(parametricTypeSchemaSupplier)
                    );
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }
    }


    private boolean isPackagePrivate(Field field) {
        return !Modifier.isPrivate(field.getModifiers())
            && !Modifier.isPublic(field.getModifiers())
            && !Modifier.isProtected(field.getModifiers());
    }


    private void initializeIfRequired() {
        if (!this.initialized) {
            doInitialize();
        }
    }


    /*package-private*/ Map<String, Supplier<? extends IJadescriptType>>
    getTypeTable() {
        initializeIfRequired();
        return this.typeTable;
    }


    /*package-private*/ Map<
        String,
        Supplier<ParametricTypeSchema<? extends IJadescriptType>>
        > getParametricTypeTable() {
        initializeIfRequired();
        return this.parametricTypeTable;
    }


    /*package-private*/ Map<String, Performative>
    getMessageClassToPerformativeMap() {
        initializeIfRequired();
        return messageClassToPerformativeMap;
    }


    /*package-private*/ Map<
        Performative, Supplier<ParametricTypeSchema<? extends MessageType>>
        > getPerformativeToMessageSubtypeMap() {
        initializeIfRequired();
        return performativeToMessageSubtypeMap;
    }


    /*package-private*/ Map<String, Integer> getExpectedTypeParameters() {
        initializeIfRequired();
        return expectedTypeParameters;
    }


    public void store(
        JvmTypeReference input,
        IJadescriptType output
    ) {
        initializeIfRequired();
        typeTable.put(
            input.getQualifiedName('.'),
            () -> output
        );
    }

}
