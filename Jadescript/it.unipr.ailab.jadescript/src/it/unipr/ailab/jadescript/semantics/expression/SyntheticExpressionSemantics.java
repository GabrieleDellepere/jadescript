package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Created on 01/11/2018.
 */
@Singleton
public class SyntheticExpressionSemantics extends ExpressionSemantics<SyntheticExpression> {

    public SyntheticExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<SyntheticExpression> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(Maybe<SyntheticExpression> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.compile();
        }
        return "";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<SyntheticExpression> input, StaticState state) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.inferType(module.get(TypeHelper.class));
        }
        return module.get(TypeHelper.class).ANY;
    }

    @Override
    protected boolean mustTraverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.mustTraverse();
        }
        return false;
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<SyntheticExpression> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<SyntheticExpression> input,
                                          StaticState state) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<SyntheticExpression> input) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.traverse();
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(PatternMatchInput<SyntheticExpression> input, StaticState state) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.isPatternEvaluationPure();
        }
        return true;
    }

    @Override
    protected boolean validateInternal(Maybe<SyntheticExpression> input, StaticState state, ValidationMessageAcceptor acceptor) {
        final SyntheticExpression.SemanticsMethods customSemantics = input.__(SyntheticExpression::getSemanticsMethods)
                .toOpt().orElseGet(SyntheticExpression.SemanticsMethods::new); //empty methods if null
        final Maybe<SyntheticExpression.SyntheticType> type = input.__(SyntheticExpression::getSyntheticType);
        if (type.toNullable() == SyntheticExpression.SyntheticType.CUSTOM) {
            return customSemantics.validate(acceptor);
        }
        return VALID;
    }

    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<SyntheticExpression> input, StaticState state, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<SyntheticExpression> input, StaticState state) {
        return PatternType.empty(module);
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<SyntheticExpression> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }




    @Override
    protected boolean isAlwaysPureInternal(Maybe<SyntheticExpression> input,
                                           StaticState state) {
        return true;
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<SyntheticExpression> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<SyntheticExpression> input,
                                      StaticState state) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<SyntheticExpression> input,
                                            StaticState state) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<SyntheticExpression> input,
                                        StaticState state) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<SyntheticExpression> input) {
        return false;
    }
}
