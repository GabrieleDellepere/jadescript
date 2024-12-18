package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OfNotation;
import it.unipr.ailab.jadescript.jadescript.PerformativeExpression;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableCallable;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.CompilableName;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 28/12/16.
 */
@Singleton
public class UnaryPrefixExpressionSemantics
    extends ExpressionSemantics<UnaryPrefix> {


    public UnaryPrefixExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<UnaryPrefix> input
    ) {

        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);

        Stream<Maybe<OfNotation>> inputsStream;
        if (index.isPresent()) {
            inputsStream = Stream.of(index, ofNotation);
        } else {
            inputsStream = Stream.of(ofNotation);
        }

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);

        return inputsStream
            .filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(ones, i));

    }


    @Override
    protected String compileInternal(
        Maybe<UnaryPrefix> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {


        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<String> firstOrLast = input.__(UnaryPrefix::getFirstOrLast);
        final boolean isIndexOfElemOperation =
            input.__(UnaryPrefix::isIndexOfElemOperation)
                .orElse(false);
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final boolean isDebugScope = input.__(UnaryPrefix::isDebugScope)
            .orElse(false);
        final boolean isDebugType = input.__(UnaryPrefix::isDebugInferredType)
            .orElse(false);
        final boolean isDebugDescribe =
            input.__(UnaryPrefix::isDebugDescribeExpression).orElse(false);
        final boolean isDebugSearchName =
            input.__(UnaryPrefix::isDebugSearchName)
                .orElse(false);
        final boolean isDebugSearchCall =
            input.__(UnaryPrefix::isDebugSearchCall).orElse(false);
        final Maybe<String> performativeConst =
            input.__(UnaryPrefix::getPerformativeConst)
                .__(PerformativeExpression::getPerformativeValue);

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);

        StaticState newState;
        final String indexCompiled;
        if (index.isPresent()) {
            indexCompiled = ones.compile(index, state, acceptor);
            newState = ones.advance(index, state);
        } else {
            newState = state;
            indexCompiled = "";
        }

        final String afterOp = ones.compile(ofNotation, newState, acceptor);

        if (isIndexOfElemOperation) {
            if (firstOrLast.wrappedEquals("last")) {
                return afterOp + ".lastIndexOf(" + indexCompiled + ")";
            } else { // assumes "first"
                return afterOp + ".indexOf(" + indexCompiled + ")";
            }
        }

        if (unaryPrefixOp.isPresent()) {
            return " " + unaryPrefixOp.__(op -> {
                if (op.equals("not")) {
                    return "!";
                } else {
                    return op;
                }
            }) + " " + afterOp;
        }

        if (performativeConst.isPresent()) {
            return "jadescript.lang.Performative."
                + performativeConst.__(String::toUpperCase);
        }
        if (isDebugSearchName) {
            final String searchNameMessage = getSearchNameMessage(
                state,
                input.__(UnaryPrefix::getSearchName)
            );
            System.out.println(searchNameMessage);
            return "/*" + searchNameMessage + "*/ null";
        }
        if (isDebugSearchCall) {
            final String searchCallMessage = getSearchCallMessage(
                state,
                input.__(UnaryPrefix::getSearchName)
            );
            System.out.println(searchCallMessage);
            return "/*" + searchCallMessage + "*/ null";
        }
        if (isDebugScope) {
            SourceCodeBuilder scb = new SourceCodeBuilder("");
            module.get(ContextManager.class).debugDump(scb);
            String dumpedScope = scb + "\n" + getOntologiesMessage() +
                "\n" + getAgentsMessage();
            System.out.println(dumpedScope);
        }
        if (isDebugType) {
            final IJadescriptType type = ones.inferType(ofNotation, state);
            System.out.println(
                "Type of '" + CompilationHelper.sourceToText(ofNotation) +
                    "' " + CompilationHelper.sourceToLocationText(ofNotation) +
                    " is: " + type.getDebugPrint()
            );
        }
        if (isDebugDescribe) {
            final Maybe<ExpressionDescriptor> descriptorMaybe =
                ones.describeExpression(ofNotation, state);
            System.out.println("Expression descriptor: " + descriptorMaybe);
        }
        return afterOp;

    }


    private String getOntologiesMessage() {
        return "[DEBUG] Searching all ontology associations: \n\n" +
            module.get(ContextManager.class).currentContext()
                .actAs(OntologyAssociationComputer.class)
                .findFirst()
                .orElse(OntologyAssociationComputer.EMPTY_ONTOLOGY_ASSOCIATIONS)
                .computeAllOntologyAssociations()
                .map(oa -> {
                    SourceCodeBuilder scb = new SourceCodeBuilder();
                    oa.debugDump(scb);
                    return scb.toString();
                })
                .collect(Collectors.joining(";\n")) +
            "\n\n****** End Searching ontology associations in scope ******";
    }


    private String getAgentsMessage() {
        return "[DEBUG] Searching all agent associations: \n\n" +
            module.get(ContextManager.class).currentContext()
                .actAs(AgentAssociationComputer.class)
                .findFirst()
                .orElse(AgentAssociationComputer.EMPTY_AGENT_ASSOCIATIONS)
                .computeAllAgentAssociations()
                .map(aa -> {
                    SourceCodeBuilder scb = new SourceCodeBuilder();
                    aa.debugDump(scb);
                    return scb.toString();
                })
                .collect(Collectors.joining(";\n")) +
            "\n\n****** End Searching agent associations in scope ******";
    }


    private String getSearchNameMessage(
        StaticState state,
        Maybe<String> identifier
    ) {

        final String target = identifier.isNothing()
            ? "all names"
            : "name '" + identifier.orElse("") + "'";


        return "[DEBUG]Searching " + target + " in scope: \n\n" +
            state.searchAs(
                CompilableName.Namespace.class,
                s -> {
                    Stream<? extends CompilableName> result;
                    if (identifier.isPresent()) {
                        result = s.compilableNames(identifier.orElse(""));
                    } else {
                        result = s.compilableNames(null);
                    }
                    return result;
                }
            ).map(ns -> {
                SourceCodeBuilder scb = new SourceCodeBuilder("");
                ns.debugDumpName(scb);
                return " - " + scb;
            }).collect(Collectors.joining(";\n")) +
            "\n\n****** End Searching " + target + " in scope ******";
    }


    private String getSearchCallMessage(
        StaticState state,
        Maybe<String> identifier
    ) {
        final String target = identifier.isNothing()
            ? "all callables"
            : "callable with name '" + identifier.orElse("") + "'";

        return "[DEBUG]Searching " + target + " in scope: \n\n" +
            state.searchAs(
                CompilableCallable.Namespace.class,
                s -> {
                    Stream<? extends CompilableCallable> result;
                    if (identifier.isPresent()) {
                        result = s.compilableCallables(identifier.orElse(""));
                    } else {
                        result = s.compilableCallables(null);
                    }
                    return result;
                }
            ).map(cc -> {
                SourceCodeBuilder scb = new SourceCodeBuilder("");
                cc.debugDumpCallable(scb);
                return " - " + scb;
            }).collect(Collectors.joining(";\n")) +
            "\n\n****** End Searching " + target + " in scope ******";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        if (input == null) {
            return builtins.any("");
        }

        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<String> performativeConst =
            input.__(UnaryPrefix::getPerformativeConst)
                .__(PerformativeExpression::getPerformativeValue);
        final boolean isIndexOfElemOperation =
            input.__(UnaryPrefix::isIndexOfElemOperation).orElse(false);
        final boolean isDebugSearchName =
            input.__(UnaryPrefix::isDebugSearchName).orElse(false);
        final boolean isDebugSearchCall =
            input.__(UnaryPrefix::isDebugSearchCall).orElse(false);

        if (isDebugSearchName || isDebugSearchCall) {
            return builtins.text();
        }

        if (unaryPrefixOp.isPresent()) {
            String op = unaryPrefixOp.orElse("");
            switch (op) {
                case "+":
                case "-":
                    //it could be floating point or integer
                    return module.get(OfNotationExpressionSemantics.class)
                        .inferType(ofNotation, state);
                case "not":
                    //it has to be boolean
                    return builtins.boolean_();
                default:
                    return builtins.any("");
            }
        }

        if (performativeConst.isPresent()) {
            return builtins.performative();
        }

        if (isIndexOfElemOperation) {
            final IJadescriptType collectionType =
                module.get(OfNotationExpressionSemantics.class)
                    .inferType(ofNotation, state);
            if (collectionType.category().isList()) {
                return builtins.integer();
            } else {
                return builtins.any("");
            }
        }

        return module.get(OfNotationExpressionSemantics.class)
            .inferType(ofNotation, state);
    }


    @Override
    protected boolean mustTraverse(Maybe<UnaryPrefix> input) {
        return input.__(UnaryPrefix::getUnaryPrefixOp).isNothing()
            && !input.__(UnaryPrefix::isIndexOfElemOperation).orElse(false)
            && input.__(UnaryPrefix::getPerformativeConst)
            .__(PerformativeExpression::getPerformativeValue).isNothing()
            && !input.__(UnaryPrefix::isDebugSearchName).orElse(false)
            && !input.__(UnaryPrefix::isDebugSearchCall).orElse(false)
            && !input.__(UnaryPrefix::isDebugScope).orElse(false)
            && !input.__(UnaryPrefix::isDebugInferredType).orElse(false)
            && !input.__(UnaryPrefix::isDebugDescribeExpression).orElse(false);
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<UnaryPrefix> input) {
        if (mustTraverse(input)) {
            final Maybe<OfNotation> ofNotation =
                input.__(UnaryPrefix::getOfNotation);
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(OfNotationExpressionSemantics.class),
                ofNotation
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected boolean validateInternal(
        Maybe<UnaryPrefix> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);

        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);

        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);


        final StaticState newState;
        final IJadescriptType inferredType;
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (input.__(UnaryPrefix::isIndexOfElemOperation).orElse(false)) {

            boolean indexCheck = ones.validate(index, state, acceptor);
            if (indexCheck == INVALID) {
                return INVALID;
            }

            newState = ones.advance(index, state);
            inferredType = ones.inferType(ofNotation, newState);

            boolean evr = validationHelper.asserting(
                inferredType.category().isList(),
                "InvalidIndexExpression",
                "Invalid type; expected: 'list', provided: " +
                    inferredType.getFullJadescriptName(),
                ofNotation,
                acceptor
            );

            if (evr == INVALID) {
                return INVALID;
            }

            if (inferredType.category().isList()) {
                assert inferredType instanceof ListType;

                IJadescriptType inferredIndexType = ones.inferType(
                    index,
                    state
                );

                return validationHelper.assertExpectedType(
                    ((ListType) inferredType).getElementType(),
                    inferredIndexType,
                    "InvalidIndexExpression",
                    index,
                    acceptor
                );
            }

        } else {
            newState = state;
            inferredType = ones.inferType(ofNotation, state);
        }

        if (unaryPrefixOp.isPresent()) {
            String op = unaryPrefixOp.orElse("");

            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);
            if (op.equals("+") || op.equals("-")) {
                boolean subValidation = ones.validate(
                    ofNotation,
                    newState,
                    acceptor
                );

                if (subValidation == VALID) {
                    return validationHelper.assertExpectedTypesAny(
                        List.of(
                            builtins.integer(),
                            builtins.real()
                        ),
                        inferredType,
                        "InvalidUnaryPrefix",
                        input,
                        JadescriptPackage.eINSTANCE
                            .getUnaryPrefix_OfNotation(),
                        acceptor
                    );
                }
            } else if (op.equals("not")) {
                boolean subValidation = ones.validate(
                    ofNotation,
                    newState,
                    acceptor
                );
                if (subValidation == VALID) {
                    return validationHelper.assertExpectedType(
                        builtins.boolean_(),
                        inferredType,
                        "InvalidUnaryPrefix",
                        input,
                        JadescriptPackage.eINSTANCE
                            .getUnaryPrefix_OfNotation(),
                        acceptor
                    );
                } else {
                    return subValidation;
                }
            }
        }

        if (input.__(UnaryPrefix::isDebugInferredType).orElse(false)) {
            validationHelper.emitInfo(
                "DEBUG_INFO",
                inferredType.getDebugPrint(),
                input,
                acceptor
            );
        }

        if (input.__(UnaryPrefix::isDebugDescribeExpression).orElse(false)) {
            validationHelper.emitInfo(
                "DEBUG_INFO",
                "Expression descriptor: "
                    + ones.describeExpression(ofNotation, state),
                input,
                acceptor
            );
        }

        if (input.__(UnaryPrefix::isDebugScope).orElse(false)) {
            SourceCodeBuilder scb = new SourceCodeBuilder("");
            state.debugDump(scb);
            scb.line();
            module.get(ContextManager.class).debugDump(scb);
            String dumpedScope = scb + "\n" + getOntologiesMessage() +
                "\n" + getAgentsMessage();
            validationHelper.emitInfo(
                "DEBUG_INFO",
                dumpedScope,
                input,
                acceptor
            );
        }

        if (input.__(UnaryPrefix::isDebugSearchName).orElse(false)) {
            input.safeDo(inputSafe -> {
                acceptor.acceptInfo(getSearchNameMessage(
                    state,
                    input.__(UnaryPrefix::getSearchName)
                ), inputSafe, null, -1, "DEBUG");
            });
        }

        if (input.__(UnaryPrefix::isDebugSearchCall).orElse(false)) {
            input.safeDo(inputSafe -> {
                acceptor.acceptInfo(getSearchCallMessage(
                    state,
                    input.__(UnaryPrefix::getSearchName)
                ), inputSafe, null, -1, "DEBUG");
            });
        }

        // ofn can be null if __dsn__, __dsc__ or performatives are there
        if (ofNotation.isNothing()) {
            return VALID;
        }

        return ones.validate(ofNotation, newState, acceptor);
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        return nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<OfNotation> index =
            input.__(UnaryPrefix::getIndex);

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);

        if (index.isPresent()) {
            final StaticState afterIndex = ones.advance(index, state);
            return ones.advance(ofNotation, afterIndex);
        }

        return ones.advance(ofNotation, state);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<OfNotation> index =
            input.__(UnaryPrefix::getIndex);

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);

        if (index.isPresent()) {
            return state;
        }

        //Concerning only the 'not' operator case...
        if (unaryPrefixOp.wrappedEquals("not")) {
            // if this returned true, the argument returned false
            return ones.assertReturnedFalse(ofNotation, state);
        }
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<OfNotation> index =
            input.__(UnaryPrefix::getIndex);

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);

        if (index.isPresent()) {
            return state;
        }

        //Concerning only the 'not' operator case...
        if (unaryPrefixOp.wrappedEquals("not")) {
            // if this returned false, the argument returned true
            return ones.assertReturnedTrue(ofNotation, state);
        }
        return state;
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<UnaryPrefix> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<UnaryPrefix> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return false;
    }

}
