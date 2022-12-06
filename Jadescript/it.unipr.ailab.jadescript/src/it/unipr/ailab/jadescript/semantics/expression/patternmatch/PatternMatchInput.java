package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeRelationship;
import it.unipr.ailab.maybe.Either;
import it.unipr.ailab.maybe.Maybe;

import java.util.function.Function;

public abstract class PatternMatchInput<
        T,
        U extends PatternMatchOutput.Unification,
        N extends PatternMatchOutput.TypeNarrowing> {

    protected final SemanticsModule module;
    private final PatternMatchMode patternMatchMode;
    private final Maybe<T> pattern;
    private final String termID;

    private final String rootPatternMatchVariableName;

    public PatternMatchInput(
            SemanticsModule module,
            PatternMatchMode patternMatchMode,
            Maybe<T> pattern,
            String termID,
            String rootPatternMatchVariableName
    ) {
        this.module = module;
        this.patternMatchMode = patternMatchMode;
        this.pattern = pattern;
        this.termID = termID;
        this.rootPatternMatchVariableName = rootPatternMatchVariableName;
    }

    public PatternMatchMode getMode() {
        return patternMatchMode;
    }

    public String getRootPatternMatchVariableName() {
        return rootPatternMatchVariableName;
    }

    public String getTermID() {
        return termID;
    }

    public Maybe<T> getPattern() {
        return pattern;
    }

    public abstract <R> PatternMatchInput<R, U, N> mapPattern(
            Function<T, R> function
    );


    public abstract IJadescriptType providedInputType();


    public <T2> SubPattern<T2, T, U, N> subPattern(
            Maybe<RValueExpression> inputExpression,
            Function<T, T2> extractSubpattern,
            String idSuffix
    ) {
        return new SubPattern<>(
                module,
                inputExpression,
                this,
                pattern.__(extractSubpattern),
                idSuffix
        );
    }

    public <T2> SubPattern<T2, T, U, N> subPattern(
            IJadescriptType providedInputType,
            Function<T, T2> extractSubpattern,
            String idSuffix
    ) {
        return new SubPattern<>(
                module,
                providedInputType,
                this,
                pattern.__(extractSubpattern),
                idSuffix
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, ?, ?> createEmptyCompileOutput() {
        return new PatternMatchOutput<>(
                new PatternMatchSemanticsProcess.IsCompilation.AsEmpty(this),
                getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                        ? PatternMatchOutput.EMPTY_UNIFICATION
                        : PatternMatchOutput.NoUnification.INSTANCE,
                getMode().getNarrowsTypeOfInput() == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                        ? new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).ANY)
                        : PatternMatchOutput.NoNarrowing.INSTANCE
        );
    }

    public PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> createEmptyValidationOutput() {
        return new PatternMatchOutput<>(
                PatternMatchSemanticsProcess.IsValidation.INSTANCE,
                getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                        ? PatternMatchOutput.EMPTY_UNIFICATION
                        : PatternMatchOutput.NoUnification.INSTANCE,
                getMode().getNarrowsTypeOfInput() == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                        ? new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).ANY)
                        : PatternMatchOutput.NoNarrowing.INSTANCE
        );
    }

    public static class WhenMatchesStatement<T>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public WhenMatchesStatement(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_FREE_VARS,
                    TypeRelationship.Related.class,
                    PatternMatchMode.PatternApplicationPurity.IMPURE_OK,
                    PatternMatchMode.Unification.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.STATEMENT_GUARD
            ), pattern, termID, rootPatternMatchVariableName);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R> WhenMatchesStatement<R> mapPattern(Function<T, R> function) {
            return new WhenMatchesStatement<>(
                    module,
                    inputExpr,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }


        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }


    }

    public static class HandlerHeader<T>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final IJadescriptType contentUpperBound;
        private final String referenceToContent;

        public HandlerHeader(
                SemanticsModule module,
                IJadescriptType contentUpperBound,
                String referenceToContent,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_FREE_VARS,
                    TypeRelationship.SubtypeOrEqual.class,
                    PatternMatchMode.PatternApplicationPurity.HAS_TO_BE_PURE,
                    PatternMatchMode.Unification.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.FEATURE_HEADER
            ), pattern, termID, rootPatternMatchVariableName);
            this.referenceToContent = referenceToContent;
            this.contentUpperBound = contentUpperBound;
        }

        @Override
        public <R> HandlerHeader<R> mapPattern(Function<T, R> function) {
            return new HandlerHeader<>(
                    module,
                    contentUpperBound,
                    referenceToContent,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }

        @Override
        public IJadescriptType providedInputType() {
            return contentUpperBound;
        }


    }

    public static class MatchesExpression<T>
            extends PatternMatchInput<T, PatternMatchOutput.NoUnification, PatternMatchOutput.WithTypeNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public MatchesExpression(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_NONVAR_HOLES_ONLY,
                    TypeRelationship.Related.class,
                    PatternMatchMode.PatternApplicationPurity.IMPURE_OK,
                    PatternMatchMode.Unification.WITHOUT_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE,
                    PatternMatchMode.PatternLocation.BOOLEAN_EXPRESSION
            ), pattern, termID, rootPatternMatchVariableName);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R> MatchesExpression<R> mapPattern(Function<T, R> function) {
            return new MatchesExpression<>(
                    module,
                    inputExpr,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }

        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }


    }

    public static class AssignmentDeconstruction<T>
            extends PatternMatchInput<T, PatternMatchOutput.DoesUnification, PatternMatchOutput.NoNarrowing> {
        private final Maybe<RValueExpression> inputExpr;

        public AssignmentDeconstruction(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                Maybe<T> pattern,
                String termID,
                String rootPatternMatchVariableName
        ) {
            super(module, new PatternMatchMode(
                    PatternMatchMode.HolesAndGroundness.ACCEPTS_NONVAR_HOLES_ONLY,
                    TypeRelationship.SupertypeOrEqual.class,
                    PatternMatchMode.PatternApplicationPurity.IMPURE_OK,
                    PatternMatchMode.Unification.WITH_VAR_DECLARATION,
                    PatternMatchMode.NarrowsTypeOfInput.DOES_NOT_NARROW_TYPE,
                    PatternMatchMode.PatternLocation.ROOT_OF_ASSIGNED_EXPRESSION
            ), pattern, termID, rootPatternMatchVariableName);
            this.inputExpr = inputExpr;
        }

        @Override
        public <R> AssignmentDeconstruction<R> mapPattern(Function<T, R> function) {
            return new AssignmentDeconstruction<>(
                    module,
                    inputExpr,
                    getPattern().__(function),
                    getTermID(),
                    getRootPatternMatchVariableName()
            );
        }


        @Override
        public IJadescriptType providedInputType() {
            return module.get(RValueExpressionSemantics.class).inferType(inputExpr);
        }


    }


    public static class SubPattern<T, RT,
            U extends PatternMatchOutput.Unification,
            N extends PatternMatchOutput.TypeNarrowing> extends PatternMatchInput<T, U, N> {

        private final PatternMatchInput<RT, U, N> rootInput;
        private final String suffixID;

        private final Either<
                Maybe<RValueExpression>,
                IJadescriptType> inputInfo;

        private SubPattern(
                SemanticsModule module,
                Either<Maybe<RValueExpression>, IJadescriptType> inputInfo,
                PatternMatchInput<RT, U, N> rootInput,
                Maybe<T> pattern,
                String suffixID
        ) {
            super(module, new PatternMatchMode(
                    rootInput.getMode().getHolesAndGroundness(),
                    // Subpatterns always have a "related" requirement, except when in assignment/declarations.
                    rootInput.getMode().getPatternLocation() == PatternMatchMode.PatternLocation.ROOT_OF_ASSIGNED_EXPRESSION
                            ? TypeRelationship.SupertypeOrEqual.class
                            : TypeRelationship.Related.class,
                    rootInput.getMode().getPatternApplicationPurity(),
                    rootInput.getMode().getUnification(),
                    rootInput.getMode().getNarrowsTypeOfInput(),
                    PatternMatchMode.PatternLocation.SUB_PATTERN
            ), pattern, rootInput.termID + suffixID, rootInput.getRootPatternMatchVariableName());
            this.rootInput = rootInput;
            this.suffixID = suffixID;
            this.inputInfo = inputInfo;
        }

        public SubPattern(
                SemanticsModule module,
                Maybe<RValueExpression> inputExpr,
                PatternMatchInput<RT, U, N> rootInput,
                Maybe<T> pattern,
                String suffixID
        ) {
            this(module, new Either.Left<>(inputExpr), rootInput, pattern, suffixID);
        }


        public SubPattern(
                SemanticsModule module,
                IJadescriptType inputType,
                PatternMatchInput<RT, U, N> rootInput,
                Maybe<T> pattern,
                String suffixID
        ) {
            this(module, new Either.Right<>(inputType), rootInput, pattern, suffixID);
        }

        public PatternMatchInput<RT, U, N> getRootInput() {
            return rootInput;
        }

        @Override
        public <R> SubPattern<R, RT, U, N> mapPattern(Function<T, R> function) {
            return new SubPattern<>(module, inputInfo, rootInput, getPattern().__(function), suffixID);
        }

        @Override
        public IJadescriptType providedInputType() {
            if (inputInfo instanceof Either.Left) {
                return module.get(RValueExpressionSemantics.class).inferType(
                        ((Either.Left<Maybe<RValueExpression>, IJadescriptType>) inputInfo).getLeft());
            } else if (inputInfo instanceof Either.Right) {
                return (((Either.Right<Maybe<RValueExpression>, IJadescriptType>) inputInfo).getRight());
            }

            return null; // Not possible by Either design.
        }


    }

}