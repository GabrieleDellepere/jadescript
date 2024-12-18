package it.unipr.ailab.jadescript.semantics.expression;


import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.SubPattern;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.TupledExpressions;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.someStream;


/**
 * Semantics for the expression made of a parentheses-delimited list of
 * comma-separated expressions, a.k.a. "tuple".
 * This semantics class assumes that a tuple has at least 2 elements. Code
 * that uses this semantics has to make sure that this is the case.
 * Please note that the type of the tuple is parameterized on the types of
 * its elements, and its length is known at compile time.
 * For example, (1, "hello") is a binary tuple with type signature (integer,
 * text).
 * There is no 1-length tuple, as it would be just a parenthesized expression.
 * There is no 0-length tuple, as the language does not provide (at the
 * moment) any 'unit'-like data type (as procedure declarations have no return
 * type specification).
 * Moreover, the maximum supported number of elements in a tuple is 20 (this
 * is enforced by the validator). This was done to simplify the compilation
 * process and because statically-sized tuples are rarely that large.
 */
public class TupleExpressionSemantics
    extends AssignableExpressionSemantics<TupledExpressions> {

    public TupleExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    private static String PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
        int position,
        int size
    ) {
        return "Cannot infer the type of the element in postion " + position +
            " in the pattern. " +
            "The tuple pattern contains unbound terms, and the missing " +
            "information cannot be retrieved from the input value type. " +
            "Suggestion: make sure that the input is narrowed to a valid " +
            size + "-sized tuple type.";
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<TupledExpressions> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<TupledExpressions> input,
        StaticState state
    ) {
        return subExpressionsAdvanceAll(input, state);
    }


    @Override
    protected boolean validateInternal(
        Maybe<TupledExpressions> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        MaybeList<RValueExpression> exprs =
            input.__toList(TupledExpressions::getTuples);
        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        boolean result = VALID;
        final boolean validateTupleSize = validateTupleSize(
            input,
            acceptor,
            exprs.size()
        );
        result = result && validateTupleSize;
        StaticState runningState = state;
        for (Maybe<RValueExpression> expr : exprs) {
            final boolean exprValidation = rves.validate(
                expr,
                runningState,
                acceptor
            );
            result = result && exprValidation;
            runningState = rves.advance(expr, runningState);
        }
        return result;
    }


    private boolean validateTupleSize(
        Maybe<TupledExpressions> input,
        ValidationMessageAcceptor acceptor,
        int size
    ) {
        return module.get(ValidationHelper.class).asserting(
            size <= 20,
            "TupleTooBig",
            "Tuples with more than 20 elements are not supported.",
            SemanticsUtils.extractEObject(input),
            acceptor
        );
    }


    @Override
    public void compileAssignmentInternal(
        Maybe<TupledExpressions> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        // Cannot assign to a tuple
    }


    @Override
    protected IJadescriptType assignableTypeInternal(
        Maybe<TupledExpressions> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).nothing("");
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<TupledExpressions> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        // Cannot assign to a tuple
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<TupledExpressions> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        // Cannot assign to a tuple
        return VALID;
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<TupledExpressions> input,
        ValidationMessageAcceptor acceptor
    ) {
        // Cannot assign to a tuple
        return errorNotLvalue(input, acceptor);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<TupledExpressions> input) {
        // Cannot assign to a tuple
        return false;
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<TupledExpressions> input
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        return someStream(input.__(TupledExpressions::getTuples))
            .filter(Maybe::isPresent)
            .map(e -> new SemanticsBoundToExpression<>(rves, e));
    }


    @Override
    protected String compileInternal(
        Maybe<TupledExpressions> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final Integer initialCapacity =
            input.__(TupledExpressions::getSize).orElse(2);
        MaybeList<RValueExpression> exprs =
            input.__toList(TupledExpressions::getTuples);
        List<String> elements = new ArrayList<>(initialCapacity);
        List<TypeArgument> types = new ArrayList<>(initialCapacity);
        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        StaticState runningState = state;
        for (Maybe<RValueExpression> expr : exprs) {
            elements.add(rves.compile(expr, runningState, acceptor));
            types.add(rves.inferType(expr, runningState));
            runningState = rves.advance(expr, runningState);
        }
        return TupleType.compileNewInstance(elements, types);
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<TupledExpressions> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> exprs =
            input.__toList(TupledExpressions::getTuples);

        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final List<TypeArgument> typeArguments = new ArrayList<>();
        StaticState runningState = state;
        for (Maybe<RValueExpression> expr : exprs) {
            IJadescriptType iJadescriptType =
                rves.inferType(expr, runningState);
            typeArguments.add(iJadescriptType);
            runningState = rves.advance(expr, runningState);
        }
        return module.get(BuiltinTypeProvider.class).tuple(typeArguments);
    }


    @Override
    protected boolean mustTraverse(Maybe<TupledExpressions> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<TupledExpressions> input) {
        return Optional.empty();
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        return subExpressionsAnyTypelyHoled(input, state);
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        MaybeList<RValueExpression> terms = input.getPattern()
            .__toList(TupledExpressions::getTuples);

        PatternType patternType = inferPatternType(input, state);

        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());

        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        final List<PatternMatcher> subResults = new ArrayList<>(elementCount);

        StaticState runningState = state;
        for (int i = 0; i < elementCount; i++) {
            Maybe<RValueExpression> term = terms.get(i);

            IJadescriptType termType;
            if (solvedPatternType instanceof TupleType) {
                final List<IJadescriptType> elementTypes =
                    ((TupleType) solvedPatternType).getElementTypes();
                if (elementTypes.size() > i) {
                    termType = elementTypes.get(i);
                } else {
                    termType = builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                            i,
                            elementCount
                        )
                    );
                }
            } else {
                termType = builtins.any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                        i,
                        elementCount
                    )
                );
            }

            SubPattern<RValueExpression, TupledExpressions> termSubpattern =
                input.subPattern(
                    termType,
                    __ -> term.toNullable(),
                    "_tupleelem" + i
                );
            final PatternMatcher subResult =
                rves.compilePatternMatch(
                    termSubpattern,
                    runningState,
                    acceptor
                );

            subResults.add(subResult);

            runningState = rves.advancePattern(termSubpattern, runningState);

            runningState = rves.assertDidMatch(termSubpattern, runningState);
        }

        Function<Integer, String> compiledSubInputs = (i) -> {
            if (i < 0 || i >= elementCount) {
                return "/* Index out of bounds */";
            } else {
                return TupleType.compileStandardGet("__x", i);
            }
        };

        return input.createCompositeMethodOutput(
            solvedPatternType,
            List.of("__x.getLength() == " + elementCount),
            compiledSubInputs,
            subResults
        );
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        if (!isTypelyHoled(input, state)) {
            return PatternType.simple(inferType(input.getPattern(), state));
        }

        return PatternType.holed(inputType -> {
            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);

            final RValueExpressionSemantics rves = module.get(
                RValueExpressionSemantics.class);
            List<TypeArgument> elementTypes = new ArrayList<>();
            MaybeList<RValueExpression> exprs =
                input.getPattern().__toList(TupledExpressions::getTuples);
            final List<IJadescriptType> inputElementTypes;
            if (inputType instanceof TupleType) {
                inputElementTypes = ((TupleType) inputType).getElementTypes();
            } else {
                inputElementTypes = List.of();
            }

            StaticState runningState = state;
            for (int i = 0; i < exprs.size(); i++) {
                Maybe<RValueExpression> expr = exprs.get(i);
                final IJadescriptType inputElementType;
                if (i < inputElementTypes.size()) {
                    inputElementType = inputElementTypes.get(i);
                } else {
                    final String message =
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                            i,
                            exprs.size()
                        );
                    inputElementType = builtins.any(message);
                }

                final SubPattern<RValueExpression, TupledExpressions>
                    termSubpattern = input.subPattern(
                    inputElementType,
                    (__) -> expr.toNullable(),
                    "_tupleelem" + i
                );
                elementTypes.add(
                    rves.inferPatternType(termSubpattern, runningState)
                        .solve(inputElementType)
                );
                runningState = rves.advancePattern(
                    termSubpattern,
                    runningState
                );
            }
            return builtins.tuple(elementTypes);
        });

    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        MaybeList<RValueExpression> terms = input.getPattern()
            .__toList(TupledExpressions::getTuples);

        IJadescriptType solvedPatternType = inferPatternType(input, state)
            .solve(input.getProvidedInputType());

        int elementCount = terms.size();

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        boolean sizeCheck = validateTupleSize(
            input.getPattern(),
            acceptor,
            elementCount
        );

        boolean allElemsCheck = VALID;
        StaticState runningState = state;
        for (int i = 0; i < terms.size(); i++) {
            final Maybe<RValueExpression> term = terms.get(i);
            IJadescriptType termType;
            if (solvedPatternType instanceof TupleType) {
                final List<IJadescriptType> elementTypes =
                    ((TupleType) solvedPatternType).getElementTypes();
                if (i < elementTypes.size()) {
                    termType = elementTypes.get(i);
                } else {
                    termType = builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                            i,
                            elementCount
                        )
                    );
                }
            } else {
                termType = builtins.any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                        i,
                        elementCount
                    )
                );
            }
            final SubPattern<RValueExpression, TupledExpressions>
                termSubpattern = input.subPattern(
                termType,
                __ -> term.toNullable(),
                "_tupleelem" + i
            );
            boolean elemCheck = rves.validatePatternMatch(
                termSubpattern,
                runningState,
                acceptor
            );
            allElemsCheck = allElemsCheck && elemCheck;
            runningState = rves.advancePattern(termSubpattern, runningState);
            runningState = rves.assertDidMatch(termSubpattern, runningState);
        }

        return sizeCheck && allElemsCheck;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> terms = input.getPattern()
            .__toList(TupledExpressions::getTuples);

        IJadescriptType solvedPatternType = inferPatternType(input, state)
            .solve(input.getProvidedInputType());
        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);


        StaticState runningState = state;
        for (int i = 0; i < terms.size(); i++) {
            final Maybe<RValueExpression> term = terms.get(i);
            IJadescriptType termType;
            if (solvedPatternType instanceof TupleType) {
                final List<IJadescriptType> elementTypes =
                    ((TupleType) solvedPatternType).getElementTypes();
                if (elementTypes.size() > i) {
                    termType = elementTypes.get(i);
                } else {
                    termType = builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                            i,
                            elementCount
                        )
                    );
                }
            } else {
                termType = builtins.any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                        i,
                        elementCount
                    )
                );
            }
            final SubPattern<RValueExpression, TupledExpressions>
                termSubpattern = input.subPattern(
                termType,
                __ -> term.toNullable(),
                "_tupleelem" + i
            );
            runningState = rves.advancePattern(termSubpattern, runningState);
            if (i < terms.size() - 1) {
                runningState = rves.assertDidMatch(
                    termSubpattern,
                    runningState
                );
            }
        }

        return runningState;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> terms = input.getPattern()
            .__toList(TupledExpressions::getTuples);

        IJadescriptType solvedPatternType = inferPatternType(input, state)
            .solve(input.getProvidedInputType());
        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);


        StaticState runningState = state;
        for (int i = 0; i < terms.size(); i++) {
            final Maybe<RValueExpression> term = terms.get(i);
            IJadescriptType termType;
            if (solvedPatternType instanceof TupleType) {
                final List<IJadescriptType> elementTypes =
                    ((TupleType) solvedPatternType).getElementTypes();
                if (elementTypes.size() > i) {
                    termType = elementTypes.get(i);
                } else {
                    termType = builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                            i,
                            elementCount
                        )
                    );
                }
            } else {
                termType = builtins.any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                        i,
                        elementCount
                    )
                );
            }
            final SubPattern<RValueExpression, TupledExpressions>
                termSubpattern = input.subPattern(
                termType,
                __ -> term.toNullable(),
                "_tupleelem" + i
            );
            runningState = rves.advancePattern(termSubpattern, runningState);
            runningState = rves.assertDidMatch(termSubpattern, runningState);
        }

        return runningState;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<TupledExpressions> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<TupledExpressions> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<TupledExpressions> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<TupledExpressions> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> terms = input.getPattern()
            .__toList(TupledExpressions::getTuples);

        IJadescriptType solvedPatternType = inferPatternType(input, state)
            .solve(input.getProvidedInputType());
        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        StaticState runningState = state;
        for (int i = 0; i < terms.size(); i++) {
            final Maybe<RValueExpression> term = terms.get(i);
            IJadescriptType termType;
            if (solvedPatternType instanceof TupleType) {
                final List<IJadescriptType> elementTypes =
                    ((TupleType) solvedPatternType).getElementTypes();
                if (elementTypes.size() > i) {
                    termType = elementTypes.get(i);
                } else {
                    termType = builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                            i,
                            elementCount
                        )
                    );
                }
            } else {
                termType = builtins.any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(
                        i,
                        elementCount
                    )
                );
            }
            final SubPattern<RValueExpression, TupledExpressions>
                termSubpattern = input.subPattern(
                termType,
                __ -> term.toNullable(),
                "_tupleelem" + i
            );

            if (!rves.isPredictablePatternMatchSuccess(
                termSubpattern,
                runningState
            )) {
                return false;
            }

            runningState = rves.advancePattern(termSubpattern, runningState);
            runningState = rves.assertDidMatch(termSubpattern, runningState);
        }

        return true;
    }

}
