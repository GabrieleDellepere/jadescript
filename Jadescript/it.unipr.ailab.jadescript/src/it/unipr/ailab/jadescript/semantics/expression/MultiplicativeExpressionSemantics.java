package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Matches;
import it.unipr.ailab.jadescript.jadescript.Multiplicative;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit.ImplicitConversionsHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.equal;
import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;

/**
 * Created on 28/12/16.
 */
@Singleton
public class MultiplicativeExpressionSemantics
    extends ExpressionSemantics<Multiplicative> {


    public MultiplicativeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        final MaybeList<Matches> matches =
            input.__toList(Multiplicative::getMatches);
        return advanceAssociative(matches, state);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return state;
    }


    private String associativeCompile(
        MaybeList<Matches> operands,
        MaybeList<String> operators,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (operands.isEmpty()) {
            return "";
        }
        final MatchesExpressionSemantics mes =
            module.get(MatchesExpressionSemantics.class);

        final String op0c = mes.compile(operands.get(0), state, acceptor);
        final StaticState op0s = mes.advance(operands.get(0), state);
        IJadescriptType t = mes.inferType(operands.get(0), state);
        String result = op0c;
        StaticState runningState = op0s;
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            result = compilePair(
                result,
                runningState,
                t,
                operators.get(i - 1),
                operands.get(i),
                acceptor
            );
            runningState = advancePair(
                runningState,
                operands.get(i)
            );
            t = inferPair(
                t,
                operators.get(i - 1),
                operands.get(i),
                runningState
            );
        }
        return result;
    }


    private StaticState advanceAssociative(
        MaybeList<Matches> operands,
        StaticState state
    ) {
        final MatchesExpressionSemantics mes =
            module.get(MatchesExpressionSemantics.class);

        StaticState runningState = mes.advance(operands.get(0), state);
        for (int i = 1; i < operands.size(); i++) {
            runningState = advancePair(
                runningState,
                operands.get(i)
            );
        }
        return runningState;
    }


    private StaticState advancePair(
        StaticState op1s,
        Maybe<Matches> op2
    ) {
        return module.get(MatchesExpressionSemantics.class).advance(op2, op1s);
    }


    private IJadescriptType associativeInfer(
        MaybeList<Matches> operands,
        MaybeList<String> operators,
        StaticState state
    ) {
        if (operands.isEmpty()) {
            return module.get(BuiltinTypeProvider.class).nothing(
                "No operands found."
            );
        }
        IJadescriptType t = module.get(MatchesExpressionSemantics.class)
            .inferType(operands.get(0), state);
        StaticState runningState = module.get(MatchesExpressionSemantics.class)
            .advance(operands.get(0), state);
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            t = inferPair(
                t,
                operators.get(i - 1),
                operands.get(i),
                runningState
            );
            runningState = advancePair(runningState, operands.get(i));
        }
        return t;
    }


    private boolean isDuration(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.duration(), type)
            .is(superTypeOrEqual());
    }


    private boolean isNumber(IJadescriptType type) {
        return isInteger(type) || isReal(type);
    }


    private boolean isInteger(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.integer(), type)
            .is(superTypeOrEqual());
    }


    private boolean isReal(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.real(), type)
            .is(superTypeOrEqual());
    }


    private String compilePair(
        String op1c,
        StaticState state,
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Matches> op2,
        BlockElementAcceptor acceptor
    ) {
        IJadescriptType t2 =
            module.get(MatchesExpressionSemantics.class).inferType(op2, state);
        String op2c =
            module.get(MatchesExpressionSemantics.class).compile(
                op2,
                state,
                acceptor
            );

        if (isDuration(t1) && isDuration(t2)) {
            if (op.wrappedEquals("/")) {
                return "(float) jadescript.lang.Duration.divide(" + op1c +
                    ", " + op2c + ")";
            }
        } else if ((isNumber(t1) && isDuration(t2))
            || (isDuration(t1) && isNumber(t2))) {
            if (op.wrappedEquals("*")) {
                return "jadescript.lang.Duration.multiply(" + op1c +
                    ", " + op2c + ")";
            } else if (op.wrappedEquals("/")) {
                return "jadescript.lang.Duration.divide(" + op1c +
                    ", " + op2c + ")";
            }
        }

        String c1 = op1c;
        String c2 = op2c;

        ImplicitConversionsHelper implicits =
            module.get(ImplicitConversionsHelper.class);

        if (implicits.implicitConversionCanOccur(t1, t2)) {
            c1 = implicits.compileImplicitConversion(c1, t1, t2);
        }

        if (implicits.implicitConversionCanOccur(t2, t1)) {
            c2 = implicits.compileImplicitConversion(c2, t2, t1);
        }
        return c1 + " " + op.orElse("*") + " " + c2;

    }


    private IJadescriptType inferPair(
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Matches> op2,
        StaticState state
    ) {
        IJadescriptType t2 =
            module.get(MatchesExpressionSemantics.class).inferType(
                op2,
                state
            );
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final ImplicitConversionsHelper implicits =
            module.get(ImplicitConversionsHelper.class);

        final TypeComparator comparator = module.get(TypeComparator.class);

        if (isDuration(t1) && isDuration(t2)) {
            if (op.wrappedEquals("/")) {
                return builtins.real();
            }
        } else if ((isNumber(t1) && isDuration(t2))
            || (isDuration(t1) && isNumber(t2))) {
            if (op.wrappedEquals("*") || op.wrappedEquals("/")) {
                return builtins.duration();
            }
        }
        if (implicits.implicitConversionCanOccur(t1, t2)) {
            t1 = t2;
        }

        if (implicits.implicitConversionCanOccur(t2, t1)) {
            t2 = t1;
        }


        if (comparator.compare(t1, t2).is(equal())) {
            return t2;
        }

        return builtins.real();
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Multiplicative> input
    ) {
        final MatchesExpressionSemantics mes =
            module.get(MatchesExpressionSemantics.class);
        return Maybe.someStream(input.__(Multiplicative::getMatches))
            .filter(Maybe::isPresent)
            .map(x -> new SemanticsBoundToExpression<>(mes, x));
    }


    @Override
    protected String compileInternal(
        Maybe<Multiplicative> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (input == null) {
            return "";
        }
        final MaybeList<Matches> matches =
            input.__toList(Multiplicative::getMatches);
        final MaybeList<String> multiplicativeOps =
            input.__toList(Multiplicative::getMultiplicativeOp);
        return associativeCompile(matches, multiplicativeOps, state, acceptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        final MaybeList<Matches> matches =
            input.__toList(Multiplicative::getMatches);
        final MaybeList<String> multiplicativeOps =
            input.__toList(Multiplicative::getMultiplicativeOp);
        return associativeInfer(matches, multiplicativeOps, state);
    }


    @Override
    protected boolean mustTraverse(Maybe<Multiplicative> input) {
        final MaybeList<Matches> matches =
            input.__toList(Multiplicative::getMatches);
        return matches.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<Multiplicative> input) {
        if (mustTraverse(input)) {
            final MaybeList<Matches> matches =
                input.__toList(Multiplicative::getMatches);

            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(MatchesExpressionSemantics.class),
                matches.get(0)
            ));

        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return true;
    }


    public boolean validateAssociative(
        MaybeList<Matches> operands,
        MaybeList<String> operators,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (operands.isEmpty()) {
            return VALID;
        }
        final MatchesExpressionSemantics mes =
            module.get(MatchesExpressionSemantics.class);
        IJadescriptType t = mes.inferType(operands.get(0), state);
        Maybe<Matches> prevOperand = operands.get(0);
        boolean prevOperandCheck = mes.validate(prevOperand, state, acceptor);
        if (prevOperandCheck == INVALID) {
            return INVALID;
        }
        StaticState runningState = mes.advance(prevOperand, state);
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            Maybe<Matches> currentOperand = operands.get(i);
            boolean pairValidation = validatePair(
                prevOperand,
                t,
                operators.get(i - 1),
                currentOperand,
                runningState,
                acceptor
            );
            if (pairValidation == INVALID) {
                return pairValidation;
            }
            t = inferPair(
                t,
                operators.get(i - 1),
                operands.get(i),
                runningState
            );
            runningState = advancePair(
                runningState,
                currentOperand
            );
            prevOperand = currentOperand;
        }
        return VALID;
    }


    private boolean validatePairTypes(
        Maybe<Matches> op1,
        IJadescriptType t1,
        Maybe<Matches> op2,
        IJadescriptType t2,
        Maybe<String> operator,
        ValidationMessageAcceptor acceptor
    ) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        ValidationHelper vh = module.get(ValidationHelper.class);

        List<IJadescriptType> durationIntReal = List.of(
            builtins.integer(),
            builtins.real(),
            builtins.duration()
        );
        boolean t1Expected = vh.assertExpectedTypesAny(
            durationIntReal,
            t1,
            "InvalidMultiplicativeOperation",
            op1,
            acceptor
        );
        boolean t2Expected = vh.assertExpectedTypesAny(
            durationIntReal,
            t2,
            "InvalidMultiplicativeOperation",
            op2,
            acceptor
        );

        if (t1Expected == INVALID || t2Expected == INVALID) {
            return INVALID;
        }

        if (isDuration(t1)) {
            if (operator.wrappedEquals("*")) {
                return vh.assertExpectedTypesAny(
                    List.of(
                        builtins.integer(),
                        builtins.real()
                    ),
                    t2,
                    "InvalidMultiplicativeOperation",
                    op2,
                    acceptor
                );
            }

            if (operator.wrappedEquals("/")) {
                // always ok
                return VALID;
            }

            // assuming operator is '%'
            op1.safeDo(op1safe -> {
                acceptor.acceptError(
                    "Invalid operation " + operator.orElse("") +
                        " for duration type",
                    op1safe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidMultiplicativeOperation"
                );
            });
            return INVALID;
        }

        if (isNumber(t1)) {
            if (operator.wrappedEquals("*")) {
                // always ok
                return VALID;
            }

            if (operator.wrappedEquals("/")
                || operator.wrappedEquals("%")) {
                return vh.assertExpectedTypesAny(
                    List.of(
                        builtins.real(),
                        builtins.integer()
                    ),
                    t2,
                    "InvalidMultiplicativeOperation",
                    op2,
                    acceptor
                );
            }
        }

        return VALID;
    }


    public boolean validatePair(
        Maybe<Matches> op1,
        IJadescriptType t1,
        Maybe<String> op,
        Maybe<Matches> op2,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        boolean op2Validation = module.get(MatchesExpressionSemantics.class)
            .validate(op2, state, acceptor);

        if (op2Validation == INVALID) {
            return INVALID;
        }

        IJadescriptType t2 = module.get(MatchesExpressionSemantics.class)
            .inferType(op2, state);


        return validatePairTypes(op1, t1, op2, t2, op, acceptor);
    }


    @Override
    protected boolean validateInternal(
        Maybe<Multiplicative> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final MaybeList<Matches> operands =
            input.__toList(Multiplicative::getMatches);
        final MaybeList<String> operators =
            input.__toList(Multiplicative::getMultiplicativeOp);
        return validateAssociative(operands, operators, state, acceptor);
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<Multiplicative> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<Multiplicative> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Multiplicative> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Multiplicative> input,
        StaticState state
    ) {
        return false;
    }

}
