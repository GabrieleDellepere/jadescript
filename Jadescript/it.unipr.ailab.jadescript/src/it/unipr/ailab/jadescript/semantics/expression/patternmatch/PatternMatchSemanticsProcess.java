package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;
import it.unipr.ailab.sonneteer.classmember.FieldWriter;
import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.ReturnStatementWriter;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
import it.unipr.ailab.sonneteer.statement.controlflow.TryCatchWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public interface PatternMatchSemanticsProcess {

    static final WriterFactory w = WriterFactory.getInstance();

    enum IsValidation implements PatternMatchSemanticsProcess {
        INSTANCE
    }

    abstract class IsCompilation implements PatternMatchSemanticsProcess {


        protected final PatternMatchInput<?, ?, ?> patternMatchInput;
        protected final List<PatternMatchOutput<? extends IsCompilation, ?, ?>> subResults = new ArrayList<>();


        public IsCompilation(PatternMatchInput<?, ?, ?> patternMatchInput) {
            this.patternMatchInput = patternMatchInput;
        }

        public IsCompilation addSubResult(PatternMatchOutput<? extends IsCompilation, ?, ?> subResult) {
            subResults.add(subResult);
            return this;
        }

        public IsCompilation addSubResults(List<PatternMatchOutput<? extends IsCompilation, ?, ?>> subResults) {
            this.subResults.addAll(subResults);
            return this;
        }


        public Stream<? extends ClassMemberWriter> getAllWriters() {
            return subResults.stream()
                    .flatMap(o -> o.getProcessInfo().getWriters());
        }

        public abstract Stream<? extends ClassMemberWriter> getWriters();

        public abstract String compileOperationInvocation(String input);

        public static List<StatementWriter> compileAdaptType(SemanticsModule module, String adaptType) {
            final ReturnStatementWriter returnFalse = w.returnStmnt(w.False);

            final VariableDeclarationWriter declareX = w.variable(adaptType, "__x");//initialized later
            final TryCatchWriter checkXType = w.tryCatch(w.block()
                            .addStatement(w.ifStmnt(
                                    w.expr("__objx instanceof " + module.get(TypeHelper.class)
                                            .noGenericsTypeName(adaptType)),
                                    w.block().addStatement(w.assign("__x", w.expr("(" + adaptType + ") __objx")))
                            ).setElseBranch(w.block().addStatement(returnFalse))))
                    .addCatchBranch("java.lang.ClassCastException", "ignored", w.block()
                            .addStatement(returnFalse));
            return Arrays.asList(declareX, checkXType);
        }

        public static abstract class AsMethod extends IsCompilation {
            protected final List<StatementWriter> compiledAdaptType;


            public AsMethod(
                    PatternMatchInput<?, ?, ?> patternMatchInput,
                    IJadescriptType solvedPatternType
            ) {
                super(patternMatchInput);
                compiledAdaptType = compileAdaptType(
                        patternMatchInput.module,
                        solvedPatternType.compileToJavaTypeReference()
                );
            }

            @Override
            public String compileOperationInvocation(String input) {
                return patternMatchInput.getTermID() + "(" + input + ")";
            }
        }

        public static class AsCompositeMethod extends AsMethod {
            private final Function<Integer, String> compiledSubInputs;

            private final List<String> additionalPreconditions;


            public AsCompositeMethod(
                    PatternMatchInput<?, ?, ?> patternMatchInput,
                    IJadescriptType solvedPatternType,
                    List<String> additionalPreconditions,
                    Function<Integer, String> compiledSubInputs
            ) {
                super(patternMatchInput, solvedPatternType);
                this.compiledSubInputs = compiledSubInputs;
                this.additionalPreconditions = additionalPreconditions;

            }

            public AsCompositeMethod(
                    PatternMatchInput<?, ?, ?> patternMatchInput,
                    IJadescriptType solvedPatternType,
                    List<String> additionalPreconditions,
                    Function<Integer, String> compiledSubInputs,
                    List<PatternMatchOutput<? extends IsCompilation, ?, ?>> subResults
            ) {
                super(patternMatchInput, solvedPatternType);
                this.compiledSubInputs = compiledSubInputs;
                this.additionalPreconditions = additionalPreconditions;
                this.subResults.addAll(subResults);
            }

            public AsCompositeMethod(
                    PatternMatchInput<?, ?, ?> patternMatchInput,
                    IJadescriptType solvedPatternType,
                    Function<Integer, String> compiledSubInputs
            ) {
                super(patternMatchInput, solvedPatternType);
                this.compiledSubInputs = compiledSubInputs;
                this.additionalPreconditions = List.of();
            }

            public AsCompositeMethod(
                    PatternMatchInput<?, ?, ?> patternMatchInput,
                    IJadescriptType solvedPatternType,
                    Function<Integer, String> compiledSubInputs,
                    List<PatternMatchOutput<? extends IsCompilation, ?, ?>> subResults
            ) {
                super(patternMatchInput, solvedPatternType);
                this.compiledSubInputs = compiledSubInputs;
                this.additionalPreconditions = List.of();
                this.subResults.addAll(subResults);
            }

            public MethodWriter generatedMethod() {
                MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", patternMatchInput.getTermID())
                        .addParameter(w.param("java.lang.Object", "__objx"));
                m.getBody().addStatements(compiledAdaptType);
                StringBuilder sb = new StringBuilder("true");
                for (String additionalPrecondition : additionalPreconditions) {
                    sb.append(" && ");
                    sb.append(additionalPrecondition);
                }
                for (int i = 0; i < subResults.size(); i++) {
                    PatternMatchOutput<? extends IsCompilation, ?, ?> subResult = subResults.get(i);
                    sb.append(" && ");
                    sb.append(subResult.getProcessInfo().compileOperationInvocation(compiledSubInputs.apply(i)));
                }
                m.getBody().addStatement(w.returnStmnt(w.expr(sb.toString())));
                return m;
            }

            @Override
            public Stream<? extends ClassMemberWriter> getWriters() {
                return Stream.of(generatedMethod());
            }

        }

        public static class AsSingleConditionMethod extends AsMethod {

            private final String condition;

            public AsSingleConditionMethod(
                    PatternMatchInput<?, ?, ?> patternMatchInput,
                    IJadescriptType solvedPatternType,
                    String condition
            ) {
                super(patternMatchInput, solvedPatternType);
                this.condition = condition;
            }

            public MethodWriter generatedWriter() {
                MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", patternMatchInput.getTermID())
                        .addParameter(w.param("java.lang.Object", "__objx"));
                m.getBody().addStatements(compiledAdaptType);
                m.getBody().addStatement(w.returnStmnt(w.expr(condition)));
                return m;
            }

            @Override
            public Stream<? extends ClassMemberWriter> getWriters() {
                return Stream.of(generatedWriter());
            }
        }

        public static abstract class AsInlineCondition extends IsCompilation {


            public AsInlineCondition(PatternMatchInput<?, ?, ?> patternMatchInput) {
                super(patternMatchInput);
            }

            @Override
            public Stream<? extends ClassMemberWriter> getWriters() {
                return Stream.empty();
            }

            @Override
            public abstract String compileOperationInvocation(String input);
        }

        public static class AsEmpty extends IsCompilation {

            public AsEmpty(PatternMatchInput<?, ?, ?> patternMatchInput) {
                super(patternMatchInput);
            }

            @Override
            public Stream<? extends ClassMemberWriter> getWriters() {
                return Stream.empty();
            }

            @Override
            public String compileOperationInvocation(String input) {
                return input;
            }
        }

        public static class AsFieldAssigningMethod extends AsMethod {

            private final IJadescriptType solvedPatternType;
            private final String name;

            public AsFieldAssigningMethod(
                    PatternMatchInput<?, ?, ?> patternMatchInput,
                    IJadescriptType solvedPatternType,
                    String name
            ) {
                super(patternMatchInput, solvedPatternType);
                this.solvedPatternType = solvedPatternType;
                this.name = name;
            }

            public FieldWriter generatedField() {
                return w.field(
                        Visibility.PUBLIC,
                        false,
                        false,
                        solvedPatternType.compileToJavaTypeReference(),
                        name
                );
            }

            public MethodWriter generatedMethod() {
                MethodWriter m = w.method(Visibility.PUBLIC, false, false, "boolean", patternMatchInput.getTermID())
                        .addParameter(w.param("java.lang.Object", "__objx"));
                m.getBody().addStatements(
                        compiledAdaptType
                ).addStatement(
                        w.assign(name, w.expr("__x"))
                ).addStatement(
                        w.returnStmnt(w.expr("true"))
                );
                return m;
            }

            @Override
            public Stream<? extends ClassMemberWriter> getWriters() {
                return Stream.of(generatedField(), generatedMethod());
            }

        }
    }


}