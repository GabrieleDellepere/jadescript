package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.Matches;
import it.unipr.ailab.jadescript.jadescript.Pattern;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.HandlerWhenExpressionContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.PatternMatchHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.LocalClassStatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 2019-08-18.
 */
@Singleton
public class MatchesExpressionSemantics extends ExpressionSemantics<Matches> {


    public MatchesExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        final LValueExpressionSemantics lves =
            module.get(LValueExpressionSemantics.class);
        final StaticState afterLeft = upes.advance(inputExpr, state);
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);
        final Maybe<ExpressionDescriptor> inputExprDesc =
            upes.describeExpression(inputExpr, state);

        final PatternMatchHelper patternMatchHelper = module.get(
            PatternMatchHelper.class);

        PatternMatchInput<LValueExpression> pmi;
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression

            pmi = patternMatchHelper.handlerHeader(
                inputExprType,
                pattern,
                inputExprDesc
            );
        } else {
            pmi = patternMatchHelper.matchesExpression(
                inputExprType,
                pattern,
                inputExprDesc
            );
        }

        return lves.advancePattern(
            pmi,
            afterLeft
        );

    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Matches> input,
        StaticState state
    ) {

        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        final StaticState afterLeft = upes.advance(inputExpr, state);
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);

        final Maybe<ExpressionDescriptor> inputExprDesc =
            upes.describeExpression(inputExpr, state);

        final PatternMatchHelper patternMatchHelper =
            module.get(PatternMatchHelper.class);

        PatternMatchInput<LValueExpression> pmi;
        if (handlerHeaderContext.isPresent()) {
            pmi = patternMatchHelper.handlerHeader(
                inputExprType,
                pattern,
                inputExprDesc
            );
        } else {
            pmi = patternMatchHelper.matchesExpression(
                inputExprType,
                pattern,
                inputExprDesc
            );
        }


        final LValueExpressionSemantics lves =
            module.get(LValueExpressionSemantics.class);


        final Maybe<ExpressionDescriptor> leftDescriptor =
            upes.describeExpression(
                inputExpr,
                state
            );
        final IJadescriptType solvedPatternType = lves.inferPatternType(
            pmi,
            afterLeft
        ).solve(inputExprType);

        return lves
            .assertDidMatch(pmi, afterLeft)
            .assertFlowTypingUpperBound(leftDescriptor, solvedPatternType);
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Matches> input
    ) {
        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        return Stream.of(unary.<SemanticsBoundToExpression<?>>extract(x ->
            new SemanticsBoundToExpression<>(
                module.get(UnaryPrefixExpressionSemantics.class),
                x
            )));
    }


    @Override
    protected String compileInternal(
        Maybe<Matches> input, StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        final String compiledInputExpr = upes.compile(
            inputExpr,
            state,
            acceptor
        );
        final StaticState afterLeft = upes.advance(inputExpr, state);
        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);
        PatternMatchHelper patternMatchHelper =
            module.get(PatternMatchHelper.class);
        LValueExpressionSemantics lves =
            module.get(LValueExpressionSemantics.class);

        final Maybe<ExpressionDescriptor> inputExprDesc =
            upes.describeExpression(inputExpr, state);

        final PatternMatchInput<LValueExpression> patternMatchInput;


        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            patternMatchInput = patternMatchHelper.handlerHeader(
                inputExprType,
                pattern,
                inputExprDesc
            );
        } else {
            patternMatchInput = patternMatchHelper.matchesExpression(
                inputExprType,
                pattern,
                inputExprDesc
            );

        }

        String localClassName =
            patternMatchHelper.getPatternMatcherClassName(pattern);

        final String variableName =
            patternMatchHelper.getPatternMatcherVariableName(pattern);


        final PatternMatcher matcher = lves.compilePatternMatch(
            patternMatchInput, afterLeft, acceptor
        );


        final LocalClassStatementWriter localClass =
            PatternMatchHelper.w.localClass(localClassName);

        localClass.addMember(patternMatchHelper.getSelfField(pattern));

        matcher.getAllWriters().forEach(localClass::addMember);

        acceptor.accept(localClass);
        acceptor.accept(PatternMatchHelper.w.variable(
            localClassName,
            variableName,
            PatternMatchHelper.w.expr("new " + localClassName + "()")
        ));


        return matcher.compilePatternMatchExpression(compiledInputExpr);
    }




    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        return nothing();
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Matches> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).boolean_();
    }


    @Override
    protected boolean mustTraverse(Maybe<Matches> input) {
        final Maybe<Pattern> pattern = input.__(Matches::getPattern);
        final boolean isMatches =
            input.__(Matches::isMatches).orElse(false);
        return !isMatches || pattern.isNothing();
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<Matches> input) {

        final Maybe<UnaryPrefix> unary = input.__(Matches::getUnaryExpr);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(UnaryPrefixExpressionSemantics.class), unary
            ));
        } else {
            return Optional.empty();
        }
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return true;
    }


    @Override
    protected boolean validateInternal(
        Maybe<Matches> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<UnaryPrefix> inputExpr = input.__(Matches::getUnaryExpr);
        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);
        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        final boolean validatedInputExpr =
            upes.validate(inputExpr, state, acceptor);
        if (validatedInputExpr == INVALID) {
            return INVALID;
        }

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);
        final Maybe<ExpressionDescriptor> inputExprDesc =
            upes.describeExpression(inputExpr, state);

        StaticState afterInputExpr = upes.advance(inputExpr, state);

        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();


        PatternMatchInput<LValueExpression> pmi;
        final PatternMatchHelper patternMatchHelper =
            module.get(PatternMatchHelper.class);
        if (handlerHeaderContext.isPresent()) {
            //We are in a handler header, probably in a when-expression
            pmi = patternMatchHelper.handlerHeader(
                inputExprType,
                pattern,
                inputExprDesc
            );
        } else {
            pmi = patternMatchHelper.matchesExpression(
                inputExprType,
                pattern,
                inputExprDesc
            );
        }

        return module.get(LValueExpressionSemantics.class)
            .validatePatternMatch(
                pmi,
                afterInputExpr,
                acceptor
            );
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<Matches> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Matches> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<Matches> input,
        StaticState state
    ) {

        final Maybe<UnaryPrefix> inputExpr =
            input.__(Matches::getUnaryExpr);

        final UnaryPrefixExpressionSemantics upes =
            module.get(UnaryPrefixExpressionSemantics.class);
        if (!upes.isWithoutSideEffects(inputExpr, state)) {
            return false;
        }

        final Maybe<LValueExpression> pattern =
            input.__(Matches::getPattern).__(i -> (LValueExpression) i);

        final IJadescriptType inputExprType = upes.inferType(inputExpr, state);
        final Maybe<ExpressionDescriptor> inputExprDesc =
            upes.describeExpression(inputExpr, state);
        final StaticState afterInputExpr = upes.advance(inputExpr, state);

        final Optional<HandlerWhenExpressionContext> handlerHeaderContext =
            module.get(ContextManager.class)
                .currentContext()
                .actAs(HandlerWhenExpressionContext.class)
                .findFirst();

        final PatternMatchHelper pmh = module.get(PatternMatchHelper.class);
        final PatternMatchInput<LValueExpression> pmi;
        if (handlerHeaderContext.isPresent()) {
            pmi = pmh.handlerHeader(
                inputExprType,
                pattern,
                inputExprDesc
            );
        } else {
            pmi = pmh.matchesExpression(
                inputExprType,
                pattern,
                inputExprDesc
            );
        }
        return module.get(LValueExpressionSemantics.class)
            .isPatternEvaluationWithoutSideEffects(pmi, afterInputExpr);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<Matches> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Matches> input) {
        // MATCHES EXPRESSION CANNOT BE USED AS PATTERN ITSELF
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Matches> input,
        StaticState state
    ) {
        return false;
    }

}
