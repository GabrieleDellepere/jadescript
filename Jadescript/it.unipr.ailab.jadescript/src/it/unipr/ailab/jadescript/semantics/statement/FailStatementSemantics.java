package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.FailStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public class FailStatementSemantics extends StatementSemantics<FailStatement> {

    public FailStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<FailStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        Maybe<RValueExpression> target = input.__(FailStatement::getTarget);
        Maybe<RValueExpression> reason = input.__(FailStatement::getReason);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final String targetCompiled =
            rves.compile(target, state, acceptor);
        final StaticState afterTarget =
            rves.advance(target, state);
        final String reasonCompiled =
            rves.compile(reason, afterTarget, acceptor);

        acceptor.accept(w.callStmnt(
            targetCompiled + ".__failBehaviour",
            w.expr(reasonCompiled)
        ));


        return rves.advance(reason, afterTarget);
    }


    @Override
    public StaticState validateStatement(
        Maybe<FailStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<RValueExpression> target = input.__(FailStatement::getTarget);
        Maybe<RValueExpression> reason = input.__(FailStatement::getReason);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        StaticState runningState = state;
        boolean targetCheck = rves.validate(target, runningState, acceptor);
        if (targetCheck == VALID) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(BuiltinTypeProvider.class).anyBehaviour(),
                rves.inferType(target, runningState),
                "InvalidFailStatement",
                target,
                acceptor
            );
            runningState = rves.advance(target, runningState);
        }

        boolean reasonCheck = rves.validate(reason, runningState, acceptor);
        if (reasonCheck == VALID) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(BuiltinTypeProvider.class).proposition(),
                rves.inferType(reason, runningState),
                "InvalidFailStatement",
                reason,
                acceptor
            );
            runningState = rves.advance(reason, runningState);
        }

        return runningState;
    }


}
