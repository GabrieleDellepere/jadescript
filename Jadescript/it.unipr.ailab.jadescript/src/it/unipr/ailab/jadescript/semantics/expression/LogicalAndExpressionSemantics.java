package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.EqualityComparison;
import it.unipr.ailab.jadescript.jadescript.LogicalAnd;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.someStream;


/**
 * Created on 28/12/16.
 */
@Singleton
public class LogicalAndExpressionSemantics
    extends ExpressionSemantics<LogicalAnd> {


    public LogicalAndExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<LogicalAnd> input
    ) {
        final EqualityComparisonExpressionSemantics eces = module.get(
            EqualityComparisonExpressionSemantics.class);
        return someStream(input.__(LogicalAnd::getEqualityComparison))
            .filter(Maybe::isPresent)
            .map(sbte -> new SemanticsBoundToExpression<>(
                eces,
                sbte
            ));
    }


    @Override
    protected String compileInternal(
        Maybe<LogicalAnd> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        if (input == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        MaybeList<EqualityComparison> equs =
            input.__toList(LogicalAnd::getEqualityComparison);

        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        StaticState newState = state;
        for (int i = 0; i < equs.size(); i++) {
            Maybe<EqualityComparison> equ = equs.get(i);

            final String operandCompiled = eces.compile(
                equ,
                newState,
                acceptor
            );

            newState = eces.advance(equ, newState);


            newState = eces.assertReturnedTrue(
                equ, newState
            );

            if (i != 0) {
                result.append(" && ").append(operandCompiled);
            } else {
                result = new StringBuilder(operandCompiled);
            }
        }
        return result.toString();
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        MaybeList<EqualityComparison> equs =
            input.__toList(LogicalAnd::getEqualityComparison);

        if (equs.isEmpty()) {
            return state;
        }

        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        // Contains the intermediate states, where the last evaluation returned
        // false
        List<StaticState> shortCircuitAlternatives
            = new ArrayList<>(equs.size());


        StaticState allTrueState = state;
        for (Maybe<EqualityComparison> equ : equs) {
            final StaticState newState = eces.advance(equ, allTrueState);

            shortCircuitAlternatives.add(
                eces.assertReturnedFalse(equ, newState)
            );

            allTrueState = eces.assertReturnedTrue(equ, newState);
        }

        // The result state is the intersection of the
        // "all true" state with each "one false" short-circuited state

        return allTrueState.intersectAllAlternatives(shortCircuitAlternatives);
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        // If it is asserted that the overall expression is true, it
        // means that all the operands returned true, and we can
        // incorporate the consequences, one after the other, in the state.

        MaybeList<EqualityComparison> equs =
            input.__toList(LogicalAnd::getEqualityComparison);

        if (equs.isEmpty()) {
            return state;
        }

        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        for (var equ : equs) {
            state = eces.assertReturnedTrue(equ, state);
        }

        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        // If it is asserted that the overall expression is false, it
        // means that one non-empty sub-sequence of the operands returned true
        // until one operand returned false. Each sub-sequence case is
        // used to compute an alternative final state, wich is used to
        // compute the overall final state with an intersection.

        MaybeList<EqualityComparison> equs =
            input.__toList(LogicalAnd::getEqualityComparison);

        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        List<StaticState> alternatives = new ArrayList<>();

        StaticState runningState = state;

        for (int i = 0; i < equs.size(); i++) {
            Maybe<EqualityComparison> equ = equs.get(i);
            alternatives.add(
                eces.assertReturnedFalse(equ, runningState)
            );
            if (i < equs.size() - 1) { //exclude last
                runningState = eces.assertReturnedTrue(equ, runningState);
            }
        }

        return StaticState.intersectAllAlternatives(alternatives, () -> state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).boolean_();
    }


    @Override
    protected boolean mustTraverse(Maybe<LogicalAnd> input) {
        MaybeList<EqualityComparison> equs =
            input.__toList(LogicalAnd::getEqualityComparison);
        return equs.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<LogicalAnd> input) {
        if (mustTraverse(input)) {
            MaybeList<EqualityComparison> equs =
                input.__toList(LogicalAnd::getEqualityComparison);

            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(EqualityComparisonExpressionSemantics.class),
                equs.get(0)
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean validateInternal(
        Maybe<LogicalAnd> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        MaybeList<EqualityComparison> equs =
            input.__toList(LogicalAnd::getEqualityComparison);
        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        boolean result = VALID;
        StaticState newState = state;
        for (Maybe<EqualityComparison> equ : equs) {
            boolean equValidation = eces.validate(equ, newState, acceptor);
            if (equValidation == VALID) {
                IJadescriptType type = eces.inferType(equ, newState);
                final boolean operandType =
                    module.get(ValidationHelper.class).assertExpectedType(
                        module.get(BuiltinTypeProvider.class).boolean_(),
                        type,
                        "InvalidOperandType",
                        equ,
                        acceptor
                    );
                result = result && operandType;
            } else {
                result = INVALID;
            }
            newState = eces.advance(equ, newState);


            newState = eces.assertReturnedTrue(
                equ,
                newState
            );

        }
        return result;

    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<LogicalAnd> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<LogicalAnd> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return false;
    }


}
