package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.ThrowStatement;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public class ThrowStatementSemantics
    extends StatementSemantics<ThrowStatement> {

    public ThrowStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<ThrowStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        Maybe<RValueExpression> expr = input.__(ThrowStatement::getReason);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final String compiledExpr = rves.compile(expr, state, acceptor);
        StaticState afterExpr = rves.advance(expr, state);


        acceptor.accept(w.callStmnt(
            EXCEPTION_THROWER_NAME + ".__throw",
            w.expr(compiledExpr)
        ));

        return afterExpr.invalidateUntilExitOperation();
    }


    @Override
    public StaticState validateStatement(
        Maybe<ThrowStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return state;
        }

        Maybe<RValueExpression> reason = input.__(ThrowStatement::getReason);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        boolean reasonCheck = rves.validate(reason, state, acceptor);

        if (reasonCheck == INVALID) {
            return state;
        }
        IJadescriptType reasonType = rves.inferType(reason, state);

        StaticState afterExpr = rves.advance(reason, state);

        module.get(ValidationHelper.class).assertExpectedType(
            module.get(BuiltinTypeProvider.class).proposition(),
            reasonType,
            "InvalidThrowArgument",
            reason,
            acceptor
        );

        return afterExpr.invalidateUntilExitOperation();
    }


}
