package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 27/12/16.
 */
@Singleton
public class TernaryConditionalExpressionSemantics extends ExpressionSemantics<TernaryConditional> {


    public TernaryConditionalExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<TernaryConditional> input) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);

        return Stream.of(
                condition.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(LogicalOrExpressionSemantics.class),
                        x
                )),
                expression1.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(RValueExpressionSemantics.class),
                        x
                )),
                expression2.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(RValueExpressionSemantics.class),
                        x
                ))
        );
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<TernaryConditional> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<TernaryConditional> input,
                                          StaticState state) {
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        ExpressionTypeKB kb1 = module.get(RValueExpressionSemantics.class).advance(expression1, );
        ExpressionTypeKB kb2 = module.get(RValueExpressionSemantics.class).advance(expression2, );
        return module.get(TypeHelper.class).mergeByLUB(kb1, kb2);
    }

    @Override
    protected String compileInternal(Maybe<TernaryConditional> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        String part1 = module.get(LogicalOrExpressionSemantics.class).compile(condition, , acceptor);
        String part2 = module.get(RValueExpressionSemantics.class).compile(expression1, , acceptor);
        String part3 = module.get(RValueExpressionSemantics.class).compile(expression2, , acceptor);
        return "((" + part1 + ") ? (" + part2 + ") : (" + part3 + "))";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<TernaryConditional> input, StaticState state) {
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        IJadescriptType type1 = module.get(RValueExpressionSemantics.class).inferType(expression1, );
        IJadescriptType type2 = module.get(RValueExpressionSemantics.class).inferType(expression2, );
        return module.get(TypeHelper.class).getLUB(type1, type2);

    }


    @Override
    protected boolean mustTraverse(Maybe<TernaryConditional> input) {
        return !input.__(TernaryConditional::isConditionalOp).extract(nullAsFalse);
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<TernaryConditional> input) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(LogicalOrExpressionSemantics.class),
                    condition
            ));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(PatternMatchInput<TernaryConditional> input, StaticState state) {
        return subPatternEvaluationsAllPure(input, state);
    }

    @Override
    protected boolean validateInternal(Maybe<TernaryConditional> input, StaticState state, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        boolean conditionValidation = module.get(LogicalOrExpressionSemantics.class)
                .validate(condition, , acceptor);

        if (conditionValidation == INVALID) {
            return INVALID;
        }

        IJadescriptType type = module.get(LogicalOrExpressionSemantics.class).inferType(condition, );
        final boolean validConditionType = module.get(ValidationHelper.class).assertExpectedType(
                Boolean.class,
                type,
                "InvalidCondition",
                input,
                JadescriptPackage.eINSTANCE.getTernaryConditional_Condition(),
                acceptor
        );
        conditionValidation = conditionValidation && validConditionType;

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        boolean expr1Validation = rves.validate(expression1, , acceptor);
        boolean expr2Validation = rves.validate(expression2, , acceptor);

        if (expr2Validation == INVALID || expr1Validation == INVALID) {
            return INVALID;
        }

        final IJadescriptType computedType = inferType(input, );
        final boolean commonParentTypeValidation = module.get(ValidationHelper.class).assertion(
                !computedType.isErroneous(),
                "TernaryConditionalInvalidType",
                "Can not find a valid common parent type of the types of the two branches.",
                input,
                acceptor
        );

        return conditionValidation && commonParentTypeValidation;
    }

    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<TernaryConditional> input, StaticState state, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<TernaryConditional> input, StaticState state) {
        return PatternType.empty(module);
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<TernaryConditional> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<TernaryConditional> input,
                                           StaticState state) {
        return subPatternEvaluationsAllPure(input, state);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<TernaryConditional> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<TernaryConditional> input,
                                      StaticState state) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<TernaryConditional> input,
                                            StaticState state) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<TernaryConditional> input,
                                        StaticState state) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<TernaryConditional> input) {
        return false;
    }
}
