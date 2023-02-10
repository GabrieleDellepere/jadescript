package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AidLiteral;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OfNotation;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.newsys.member.NameMember;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableList;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;
import static it.unipr.ailab.maybe.Maybe.some;


/**
 * Created on 01/04/18.
 */
@Singleton
public class OfNotationExpressionSemantics
    extends AssignableExpressionSemantics<OfNotation> {


    public OfNotationExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    private String generateMethodName(
        String propName,
        IJadescriptType prevType,
        boolean isAssignment
    ) {
        if (propName.equals("size") || propName.equals("length")) {
            if (module.get(TypeHelper.class).TEXT.isSupEqualTo(prevType)) {
                return "length";
            } else {
                return "size";
            }
        } else {
            return (isAssignment ? "set" : "get")
                + Strings.toFirstUpper(propName);
        }
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<OfNotation> input
    ) {
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);

        return Stream.of(input.__(OfNotation::getAidLiteral))
            .filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(ales, i));
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(
            input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        List<String> result = properties.stream()
            .filter(Maybe::isPresent)
            .map(Maybe::toNullable)
            .collect(Collectors.toCollection(ArrayList::new));


        final Maybe<ExpressionDescriptor> descriptorMaybe =
            module.get(AidLiteralExpressionSemantics.class)
                .describeExpression(aidLiteral, state);

        if (descriptorMaybe.isNothing()) {
            return Maybe.nothing();
        }

        ExpressionDescriptor descriptor = descriptorMaybe.toNullable();

        if (!(descriptor instanceof ExpressionDescriptor.PropertyChain)) {
            return Maybe.nothing();
        }

        final ImmutableList<String> otherProperties =
            ((ExpressionDescriptor.PropertyChain) descriptor).getProperties();

        for (String otherProperty : otherProperties) {
            result.add(otherProperty);
        }


        return some(new ExpressionDescriptor.PropertyChain(
            ImmutableList.from(result)
        ));
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return subExpressionsAdvanceAll(input, state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected String compileInternal(
        Maybe<OfNotation> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(
            OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);
        StringBuilder r = new StringBuilder(
            module.get(AidLiteralExpressionSemantics.class)
                .compile(aidLiteral, state, acceptor)
        );
        IJadescriptType prev = module.get(AidLiteralExpressionSemantics.class)
            .inferType(
                aidLiteral,
                state
            );
        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);
        for (int i = properties.size() - 1; i >= 0; i--) {
            String propName = properties.get(i).extract(nullAsEmptyString);
            Optional<? extends NameMember> property =
                prev.namespace().searchAs(
                    NameMember.Namespace.class,
                    s -> s.searchName(propName, null, null)
                ).findFirst();
            if (property.isPresent()) {
                r = new StringBuilder(property.get().compileRead(r + "."));
            } else {
                r.append(".").append(generateMethodName(
                    propName,
                    prev,
                    false
                )).append("()");
            }

            prev = inferTypeProperty(some(propName), prev);
        }
        return r.toString();
    }


    @Override
    public void compileAssignmentInternal(
        Maybe<OfNotation> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(
            OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);
        if (properties.isEmpty()) {
            ales.compileAssignment(
                aidLiteral,
                compiledExpression,
                exprType,
                state,
                acceptor
            );
            return;
        }

        StringBuilder sb = new StringBuilder(ales.compile(
            aidLiteral,
            state,
            acceptor
        ));
        IJadescriptType prevType = ales.inferType(aidLiteral, state);
        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);
        for (int i = properties.size() - 1; i >= 0; i--) {
            String propName = properties.get(i).extract(nullAsEmptyString);
            IJadescriptType currentPropType = inferTypeProperty(
                some(propName),
                prevType
            );

            Optional<? extends NameMember> property =
                prevType.namespace().searchAs(
                    NameMember.Namespace.class,
                    s -> s.searchName(propName, null, null)
                ).findFirst();

            final String rExprConverted = module.get(TypeHelper.class)
                .compileWithEventualImplicitConversions(
                    compiledExpression,
                    exprType,
                    currentPropType
                );
            if (property.isPresent()) {
                if (i == 0) {
                    sb = new StringBuilder(property.get().compileWrite(
                        sb + ".",
                        rExprConverted
                    ));
                } else {
                    sb = new StringBuilder(property.get().compileRead(
                        sb + "."
                    ));
                }
            } else {
                if (i == 0) {
                    sb.append(".").append(generateMethodName(
                        propName,
                        prevType,
                        true
                    )).append("(");
                    sb.append(rExprConverted).append(")");
                } else {
                    sb.append(".").append(generateMethodName(
                        propName,
                        prevType,
                        false
                    )).append("()");
                }
            }


            prevType = inferTypeProperty(some(propName), prevType);
        }
        acceptor.accept(w.simpleStmt(sb.toString()));
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<OfNotation> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        final Maybe<ExpressionDescriptor> descriptor = describeExpression(
            input,
            state
        );

        return ales.advance(aidLiteral, state)
            .assertAssigned(descriptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(
            input.__(OfNotation::getProperties)
        );
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        List<Maybe<String>> props = new ArrayList<>(properties);
        IJadescriptType prevType =
            module.get(AidLiteralExpressionSemantics.class).inferType(
                aidLiteral, state);
        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);
        for (int i = props.size() - 1; i >= 0; i--) {
            Maybe<String> prop = props.get(i);
            prevType = inferTypeProperty(prop, prevType);
        }
        return prevType;
    }


    @Override
    protected boolean mustTraverse(Maybe<OfNotation> input) {
        final List<Maybe<String>> properties = Maybe.toListOfMaybes(
            input.__(OfNotation::getProperties));
        return properties.isEmpty();
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<OfNotation> input) {
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToAssignableExpression<>(
                module.get(AidLiteralExpressionSemantics.class),
                aidLiteral
            ));
        }
        return Optional.empty();
    }


    public IJadescriptType inferTypeProperty(
        Maybe<String> prop,
        IJadescriptType prevType
    ) {
        String propSafe = prop.extract(nullAsEmptyString);
        return prevType.namespace().searchAs(
                NameMember.Namespace.class,
                s -> s.searchName(propSafe, null, null)
            ).findFirst()
            .map(NameMember::readingType)
            .orElseGet(() ->
                module.get(TypeHelper.class).BOTTOM.apply(
                    "Could not resolve property '" + propSafe + "' of value " +
                        "of type "
                        + prevType.getJadescriptName()
                )
            );
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<OfNotation> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;

        final List<Maybe<String>> properties = Maybe.toListOfMaybes(input.__(
            OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);
        boolean subValidation = rves
            .validate(expression, state, acceptor);

        if (subValidation == INVALID) {
            return subValidation;
        }
        IJadescriptType typeOfRExpression = rves.inferType(expression, state);
        StaticState afterRExpression = rves.advance(expression, state);


        List<Maybe<String>> props = new ArrayList<>(properties);
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);


        boolean subExprCheck = ales.validate(
            aidLiteral,
            afterRExpression,
            acceptor
        );
        if (subExprCheck == INVALID) {
            return INVALID;
        }
        IJadescriptType prevType = ales.inferType(aidLiteral, afterRExpression);
        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);

        for (int i = props.size() - 1; i >= 0; i--) {
            Maybe<String> prop = props.get(i);
            boolean result = module.get(ValidationHelper.class)
                .assertPropertiesOfTypeAccessible(
                    input,
                    "Cannot access properties of values of type '"
                        + prevType.getJadescriptName() + "'",
                    prevType,
                    acceptor
                );
            if (i == 0) {
                final boolean assignProperty = validateAssignProperty(
                    input,
                    acceptor,
                    typeOfRExpression,
                    i,
                    prop,
                    prevType
                );
                result = result && assignProperty;
            } else {
                final boolean propertyCheck = validateProperty(
                    input,
                    acceptor,
                    i,
                    prop,
                    prevType
                );
                result = result && propertyCheck;
            }
            if (result == VALID) {
                prevType = inferTypeProperty(prop, prevType);
            } else {
                return INVALID;
            }
        }

        return VALID;
    }


    public boolean validateProperty(
        Maybe<OfNotation> input,
        ValidationMessageAcceptor acceptor,
        int index,
        Maybe<String> propmaybe,
        IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return VALID;
        }

        String prop = propmaybe.toNullable();

        return module.get(ValidationHelper.class).asserting(
            prevType.namespace().searchAs(
                NameMember.Namespace.class,
                s -> s.searchName(prop, null, null)
            ).findFirst().isPresent(),
            "InvalidOfNotation",
            "Cannot resolve property '" + prop + "' in value of type " +
                prevType.getJadescriptName() + ".",
            input,
            JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
            index,
            acceptor
        );

    }


    public boolean validateAssignProperty(
        Maybe<OfNotation> input,
        ValidationMessageAcceptor acceptor,
        IJadescriptType typeOfRExpression,
        int index,
        Maybe<String> propmaybe,
        IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return VALID;
        }

        String prop = propmaybe.toNullable();

        Optional<? extends NameMember> foundProperty =
            prevType.namespace().searchAs(
                NameMember.Namespace.class,
                s -> s.searchName(prop, null, null)
            ).findFirst();


        boolean result = module.get(ValidationHelper.class).asserting(
            foundProperty.isPresent(),
            "InvalidOfNotation",
            "Can not find property '" + prop + "' in type " +
                prevType.getJadescriptName() + ".",
            input,
            JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
            index,
            acceptor
        );

        if (result == VALID && foundProperty.isPresent()) {
            final boolean canWriteCheck = module.get(ValidationHelper.class)
                .asserting(
                    foundProperty.get().canWrite(),
                    "InvalidOfNotation",
                    "Cannot assign to read-only property '" + prop + "'",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
                );
            result = result && canWriteCheck;
        }

        if (result == VALID && foundProperty.isPresent()) {
            final boolean typeConformanceCheck =
                module.get(ValidationHelper.class).assertExpectedType(
                    foundProperty.get().readingType(),
                    typeOfRExpression,
                    "InvalidOfNotation",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
                );
            result = result && typeConformanceCheck;
        }

        return result;

    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<OfNotation> input,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<OfNotation> input) {
        return true;
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean validateInternal(
        Maybe<OfNotation> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;

        final List<Maybe<String>> properties =
            Maybe.toListOfMaybes(input.__(OfNotation::getProperties));
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        final boolean subValidation =
            module.get(AidLiteralExpressionSemantics.class)
                .validate(aidLiteral, state, acceptor);

        if (subValidation == INVALID) {
            return subValidation;
        }

        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);

        IJadescriptType afterLastOfType = module.get(
            AidLiteralExpressionSemantics.class).inferType(aidLiteral, state);
        List<Maybe<String>> props = new ArrayList<>(properties);
        if (!props.isEmpty()) {
            IJadescriptType prevType = afterLastOfType;
            for (int i = props.size() - 1; i >= 0; i--) { //reverse
                Maybe<String> prop = props.get(i);
                boolean result = VALID;
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                if (!prop.wrappedEquals("length")
                    || !typeHelper.TEXT.isSupEqualTo(prevType)) {
                    result = module.get(ValidationHelper.class)
                        .assertPropertiesOfTypeAccessible(
                            input,
                            "Cannot access properties of values of type '" +
                                prevType.getJadescriptName() + "'.",
                            prevType,
                            acceptor
                        );
                }
                final boolean propertyCheck = validateProperty(
                    input,
                    acceptor,
                    i,
                    prop,
                    prevType
                );
                result = result && propertyCheck;
                if (result == VALID) {
                    prevType = inferTypeProperty(prop, prevType);
                } else {
                    return result;
                }
            }
        }

        return VALID;

    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<OfNotation> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }

}
