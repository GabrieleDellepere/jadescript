package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ActivateStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.behaviour.BehaviourType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import jadescript.core.behaviours.OneShot;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;

/**
 * Created on 09/03/18.
 */
@Singleton
public class ActivateStatementSemantics
    extends StatementSemantics<ActivateStatement> {


    public ActivateStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<ActivateStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        Maybe<RValueExpression> behaviour =
            input.__(ActivateStatement::getExpression);
        Maybe<RValueExpression> period =
            input.__(ActivateStatement::getPeriod);
        Maybe<RValueExpression> delay =
            input.__(ActivateStatement::getDelay);
        Maybe<RValueExpression> start =
            input.__(ActivateStatement::getStartTime);
        String methodName = "activate";

        List<ExpressionWriter> params = new ArrayList<>();

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        StaticState runningState = state;
        final String compiledBehaviour = rves.compile(
            behaviour,
            runningState,
            acceptor
        );
        runningState = rves.advance(
            behaviour,
            runningState
        );

        params.add(w.expr(CompilationHelper.compileAgentReference()));
        if (delay.isPresent()) {
            methodName += "_after";
            final String delayCompiled = rves.compile(
                delay,
                runningState,
                acceptor
            );
            runningState = rves.advance(delay, runningState);
            params.add(w.expr(delayCompiled));
        }

        if (start.isPresent()) {
            methodName += "_at";
            final String compiledStart = rves.compile(
                start,
                runningState,
                acceptor
            );
            runningState = rves.advance(start, runningState);
            params.add(w.expr(compiledStart));
        }

        if (period.isPresent()) {
            methodName += "_every";
            final String compiledPeriod = rves.compile(
                period,
                runningState,
                acceptor
            );
            runningState = rves.advance(period, runningState);
            params.add(w.expr(compiledPeriod));
        }


        acceptor.accept(w.callStmnt(
            compiledBehaviour + "." + methodName,
            params
        ));

        return runningState;
    }


    @Override
    public StaticState validateStatement(
        Maybe<ActivateStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return state;
        }
        Maybe<RValueExpression> expr =
            input.__(ActivateStatement::getExpression);
        Maybe<RValueExpression> period =
            input.__(ActivateStatement::getPeriod);
        Maybe<RValueExpression> delay =
            input.__(ActivateStatement::getDelay);
        Maybe<RValueExpression> start =
            input.__(ActivateStatement::getStartTime);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        final boolean behaviourCheck = rves.validate(expr, state, acceptor);
        StaticState runningState = state;
        IJadescriptType exprType = rves.inferType(expr, state);
        if (behaviourCheck == VALID) {
            validationHelper.assertExpectedType(
                module.get(BuiltinTypeProvider.class).anyBehaviour(),
                exprType,
                "InvalidBehaviourExpressionType",
                expr,
                acceptor
            );
            runningState = rves.advance(expr, state);
        }


        validationHelper.assertCanUseAgentReference(expr, acceptor);


        final Optional<IJadescriptType> agentType = module.get(
            ContextManager.class).currentContext().actAs(
            AgentAssociated.class
        ).findFirst().flatMap(agentAssociated ->
            agentAssociated.computeAllAgentAssociations()
                .sorted()
                .findFirst()
                .map(AgentAssociation::getAgent)
        );

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);


        if (agentType.isPresent()
            && exprType instanceof BehaviourType
            && behaviourCheck == VALID) {
            final IJadescriptType forAgentType =
                ((BehaviourType) exprType).getForAgentType().ignoreBound();
            validationHelper.asserting(
                comparator
                    .checkIs(superTypeOrEqual(), forAgentType, agentType.get()),
                "InvalidBehaviourActivation",
                "An agent of type '" + agentType.get().getFullJadescriptName() +
                    "' can not activate a behaviour " +
                    "designed for agents of type '" +
                    forAgentType.getFullJadescriptName() + "'.",
                expr,
                acceptor
            );
        }


        if (period.isPresent()) {
            boolean periodCheck = rves.validate(period, runningState, acceptor);
            if (periodCheck == VALID) {
                IJadescriptType periodType = rves.inferType(
                    period,
                    runningState
                );
                validationHelper.assertExpectedType(
                    builtins.duration(),
                    periodType,
                    "InvalidPeriodType",
                    period,
                    acceptor
                );
                runningState = rves.advance(period, runningState);
            }
            validationHelper.asserting(
                !comparator.compare(OneShot.class, exprType)
                    .is(superTypeOrEqual()),
                "InvalidEveryClause",
                "Can not apply 'every' clause to the activation of a" +
                    " one-shot behaviour",
                period,
                acceptor
            );
        }


        if (delay.isPresent()) {
            boolean delayCheck = rves.validate(delay, runningState, acceptor);
            if (delayCheck == VALID) {
                validationHelper.assertExpectedType(
                    builtins.duration(),
                    rves.inferType(delay, runningState),
                    "InvalidDelayType",
                    delay,
                    acceptor
                );
                runningState = rves.advance(delay, runningState);
            }
        }

        if (start.isPresent()) {
            boolean startCheck = rves.validate(start, runningState, acceptor);
            if (startCheck == VALID) {
                validationHelper.assertExpectedType(
                    builtins.timestamp(),
                    rves.inferType(start, runningState),
                    "InvalidDelayType",
                    start,
                    acceptor
                );
            }
            runningState = rves.advance(start, runningState);
        }

        return runningState;
    }


}
