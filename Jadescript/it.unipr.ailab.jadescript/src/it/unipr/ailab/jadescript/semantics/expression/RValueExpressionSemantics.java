package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TernaryConditional;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * Created on 27/12/16.
 */
@Singleton
public class RValueExpressionSemantics
    extends ExpressionSemantics<RValueExpression> {


    public RValueExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<RValueExpression> input
    ) {
        return Stream.empty();
    }


    @Override
    protected String compileInternal(
        Maybe<RValueExpression> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return "";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<RValueExpression> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).any(
            "Detected semantics class is RValueExpressionSemantics."
        );
    }


    @Override
    protected boolean mustTraverse(Maybe<RValueExpression> input) {
        return input.isInstanceOf(SyntheticExpression.class)
            || input.isInstanceOf(TernaryConditional.class);
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<RValueExpression> input) {
        if (input.isInstanceOf(SyntheticExpression.class)) {
            return Optional.of(
                new SemanticsBoundToExpression<>(
                    module.get(SyntheticExpressionSemantics.class),
                    input.__((i -> (SyntheticExpression) i))
                ));
        }
        if (input.isInstanceOf(TernaryConditional.class)) {
            return Optional.of(
                new SemanticsBoundToExpression<>(
                    module.get(TernaryConditionalExpressionSemantics.class),
                    input.__((i -> (TernaryConditional) i))
                ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean validateInternal(
        Maybe<RValueExpression> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<RValueExpression> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<RValueExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<RValueExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<RValueExpression> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<RValueExpression> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<RValueExpression> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<RValueExpression> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<RValueExpression> input,
        StaticState state
    ) {
        return false;
    }


    public boolean isThis(Maybe<RValueExpression> expression) {
        return CompilationHelper.sourceToLocationText(expression)
            .__(String::trim)
            .__(s -> s.equals(THIS))
            .orElse(false);
    }

}
