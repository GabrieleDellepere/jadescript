package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.MethodCallSemantics;
import it.unipr.ailab.jadescript.semantics.PatternMatchUnifiedVariable;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchMode;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.ProxyEObject;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.SingleIdentifier;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Either;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;
import static it.unipr.ailab.maybe.Maybe.some;


/**
 * Created on 26/08/18.
 */
@Singleton
public class SingleIdentifierExpressionSemantics
    extends AssignableExpressionSemantics<SingleIdentifier> {


    public SingleIdentifierExpressionSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    public boolean isThisReference(Maybe<SingleIdentifier> input) {
        return input.__(SingleIdentifier::getIdent).wrappedEquals(THIS);
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        Maybe<Either<NamedSymbol, CallableSymbol>> resolved =
            resolve(input, state);
        final String ident = input.__(SingleIdentifier::getIdent)
            .extract(nullAsEmptyString);
        if (resolved.isNothing() || ident.isBlank()) {
            return Maybe.nothing();
        } else if (resolved.toNullable() instanceof Either.Left) {
            return some(new ExpressionDescriptor.PropertyChain(ident));
        } else /*if(resolved.toNullable() instanceof Either.Right)*/ {
            return module.get(MethodCallSemantics.class).describeExpression(
                MethodCall.methodCall(input),
                state
            );
        }
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        Maybe<Either<NamedSymbol, CallableSymbol>> resolved =
            resolve(input, state);
        final String ident = input.__(SingleIdentifier::getIdent)
            .extract(nullAsEmptyString);

        if (resolved.isNothing() || ident.isBlank()
            || resolved.toNullable() instanceof Either.Left) {
            return state;
        } else /*if(resolved.toNullable() instanceof Either.Right)*/ {
            return module.get(MethodCallSemantics.class).advance(
                MethodCall.methodCall(input),
                state
            );
        }
    }


    public Maybe<NamedSymbol> resolveAsNamedSymbol(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        //TODO cached resolution?
        return input.__(SingleIdentifier::getIdent).__(
            identSafe -> state.searchAs(
                NamedSymbol.Searcher.class,
                s -> s.searchName(identSafe, null, null)
            ).findAny().orElse(null)
        );
    }


    public Maybe<Either<NamedSymbol, CallableSymbol>> resolve(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        //TODO cached resolution?
        Maybe<NamedSymbol> named = resolveAsNamedSymbol(input, state);
        if (named.isPresent()) {
            return some(new Either.Left<>(named.toNullable()));
        }
        final MethodCallSemantics mcs = module.get(MethodCallSemantics.class);
        Maybe<? extends CallableSymbol> callable = mcs.resolve(
            MethodCall.methodCall(input),
            state,
            true
        );
        return callable.__(Either.Right::new);
    }


    public boolean resolves(Maybe<SingleIdentifier> input, StaticState state) {
        //TODO cached resolution?
        return resolve(input, state).isPresent();
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<SingleIdentifier> input
    ) {
        return Stream.empty();
    }


    @Override
    protected String compileInternal(
        Maybe<SingleIdentifier> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return "";
        Maybe<String> ident = input.__(SingleIdentifier::getIdent);
        if (ident.wrappedEquals(THIS)) {
            return Util.getOuterClassThisReference(
                input.__(ProxyEObject::getProxyEObject)
            ).orElse("");
        }

        //TODO move here all special identifiers from Primary?
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved =
            resolve(input, state);

        if (resolved.isNothing()) {
            return "/*UNRESOLVED NAME:*/" + ident.toNullable();
        } else if (resolved.toNullable() instanceof Either.Left) {
            final NamedSymbol variable = ((Either.Left<NamedSymbol,
                CallableSymbol>) resolved.toNullable()).getLeft();
            if (variable instanceof UserVariable) {
                UserVariable userVariable = (UserVariable) variable;
                userVariable.notifyReadUsage();
                module.get(CompilationHelper.class)
                    .lateBindingContext() //TODO rethink the latebinding system
                    .pushVariable(userVariable);
                return w.varPlaceholder(variable.hashCode());
            } else {
                return variable.compileRead("");
            }
        } else /*if (resolved.toNullable() instanceof Either.Right)*/ {
            return module.get(MethodCallSemantics.class)
                .compile(MethodCall.methodCall(input), state, acceptor);
        }
    }


    public void compileAssignmentInternal(
        Maybe<SingleIdentifier> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return;
        final Maybe<String> ident = input.__(SingleIdentifier::getIdent);

        final Maybe<NamedSymbol> variable = resolveAsNamedSymbol(
            input,
            state
        );
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (variable.isPresent()) {
            String adaptedExpression = compiledExpression;
            if (typeHelper.implicitConversionCanOccur(
                exprType,
                variable.toNullable().writingType()
            )) {
                adaptedExpression = typeHelper.compileImplicitConversion(
                    compiledExpression,
                    exprType,
                    variable.toNullable().writingType()
                );
            }

            if (variable.toNullable() instanceof UserVariable) {
                final UserVariable userVariable =
                    (UserVariable) variable.toNullable();
                userVariable.notifyWriteUsage();
                module.get(CompilationHelper.class).lateBindingContext()
                    .pushVariable(userVariable);
                //TODO ?
//                     acceptor.accept(w.varAssPlaceholder(userVariable.name
//                     (), w.expr(adaptedExpression)));
                acceptor.accept(w.assign(
                    w.varPlaceholder(userVariable.hashCode()),
                    w.expr(adaptedExpression)
                ));
            } else {
                acceptor.accept(
                    w.simpleStmt(variable.toNullable().compileWrite(
                        "", adaptedExpression
                    ))
                );
            }
        } else {
            // Inferred declaration
            String name = ident.orElse("");


            final UserVariable newDeclared = new UserVariable(
                name,
                exprType,
                true
            );

            //NOT NEEDED:
//            final StaticState newState = state.assertNamedSymbol(newDeclared);

            module.get(CompilationHelper.class).lateBindingContext()
                .pushVariable(newDeclared);//TODO this should be in state...

            acceptor.accept(w.varDeclPlaceholder(
                exprType.compileToJavaTypeReference(),
                name,
                w.expr(compiledExpression)
            ));
        }
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<SingleIdentifier> input,
        IJadescriptType exprType,
        StaticState state
    ) {
        if (input == null) return state;
        final Maybe<String> ident = input.__(SingleIdentifier::getIdent);

        final Maybe<NamedSymbol> variable = resolveAsNamedSymbol(
            input,
            state
        );
        if (variable.isPresent()) {
            if (variable.toNullable() instanceof UserVariable) {
                //TODO update state for assignment
                final UserVariable userVariable =
                    (UserVariable) variable.toNullable();
                userVariable.notifyWriteUsage();
                module.get(CompilationHelper.class).lateBindingContext()
                    .pushVariable(userVariable);

            }

            return state;
        } else {
            // Inferred declaration
            String name = ident.orElse("");

            final UserVariable newDeclared = new UserVariable(
                name,
                exprType,
                true
            );
            module.get(CompilationHelper.class).lateBindingContext()
                .pushVariable(newDeclared);//TODO integrate it in state

            return state.assertNamedSymbol(newDeclared);
        }
    }


    protected IJadescriptType inferTypeInternal(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<String> ident = input.__(SingleIdentifier::getIdent);
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved =
            resolve(input, state);
        if (resolved.isNothing()) {
            return module.get(TypeHelper.class).BOTTOM.apply(
                "Cannot infer the type of the expression. Reason: cannot " +
                    "resolve name '" + ident.toNullable() + "'"
            );
        } else if (resolved.toNullable() instanceof Either.Left) {
            return (
                (Either.Left<NamedSymbol, CallableSymbol>) resolved.toNullable()
            ).getLeft().readingType();
        } else /*if(resolved.toNullable() instanceof Either.Right)*/ {
            return module.get(MethodCallSemantics.class).inferType(
                MethodCall.methodCall(input), state);
        }

    }


    @Override
    protected boolean mustTraverse(Maybe<SingleIdentifier> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(Maybe<SingleIdentifier> input) {
        return Optional.empty();
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved =
            resolve(input, state);
        //Considering it pure if it is not resolved and if it is a named
        // symbol (i.e. not a call to a function without
        // parentheses).
        if (resolved.isNothing()) {
            return true;
        }

        if (resolved.toNullable() instanceof Either.Left) {
            return true;
        }

        return (
            (Either.Right<NamedSymbol, CallableSymbol>) resolved.toNullable()
        ).getRight().isPure();

    }


    @Override
    protected boolean isHoledInternal(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        return !resolves(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        return !resolves(input, state);
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<SingleIdentifier> input,
        StaticState state
    ) {
        return !resolves(input, state);
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<SingleIdentifier> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        final String identifier = input.getPattern()
            .__(SingleIdentifier::getIdent).orElse("");
        if (isUnbound(input.getPattern(), state)) {
            if (input.getMode().getUnification()
                == PatternMatchMode.Unification.WITH_VAR_DECLARATION) {
                IJadescriptType solvedPatternType =
                    inferPatternType(input, state)
                        .solve(input.getProvidedInputType());

                //update of state is omitted on purpose

                return input.createFieldAssigningMethodOutput(
                    solvedPatternType,
                    identifier
                );
            } else {
                return input.createEmptyCompileOutput();
            }
        } else {
            IJadescriptType solvedPatternType = inferPatternType(input, state)
                .solve(input.getProvidedInputType());
            String tempCompile = compile(input.getPattern(), state, acceptor);
            String compiledFinal;
            if (tempCompile.startsWith(
                input.getRootPatternMatchVariableName()
            )) {
                compiledFinal = identifier;
            } else {
                compiledFinal = tempCompile;
            }
            return input.createSingleConditionMethodOutput(
                solvedPatternType,
                "java.util.Objects.equals(__x," + compiledFinal + ")"
            );
        }
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<SingleIdentifier> input,
        StaticState state
    ) {
        final String identifier = input.getPattern()
            .__(SingleIdentifier::getIdent).orElse("");

        if (isUnbound(input.getPattern(), state)) {
            if (input.getMode().getUnification()
                == PatternMatchMode.Unification.WITH_VAR_DECLARATION) {
                IJadescriptType solvedPatternType =
                    inferPatternType(input, state)
                        .solve(input.getProvidedInputType());
                String localClassName = "__PatternMatcher" + input.getPattern()
                    .__(ProxyEObject::getProxyEObject)
                    .__(Objects::hashCode)
                    .orElse(0);

                final PatternMatchUnifiedVariable deconstructedVariable
                    = new PatternMatchUnifiedVariable(
                    identifier,
                    solvedPatternType,
                    localClassName + "_obj."
                );

                return state.assertNamedSymbol(deconstructedVariable);
            } else {
                return state;
            }
        } else {
            //TODO update usages/reassignments?
            return state;
        }
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<SingleIdentifier> input,
        StaticState state
    ) {
        if (isTypelyHoled(input.getPattern(), state)) {
            return PatternType.holed(inputType -> inputType);
        } else {
            return PatternType.simple(inferType(input.getPattern(), state));
        }
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<SingleIdentifier> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        if (isUnbound(input.getPattern(), state)) {
            final String identifier = input.getPattern()
                .__(SingleIdentifier::getIdent).orElse("");
            boolean resolutionCheck =
                module.get(ValidationHelper.class).asserting(
                    input.getMode().getUnification() !=
                        PatternMatchMode.Unification.WITHOUT_VAR_DECLARATION,
                    "InvalidReference",
                    "Unresolved name: " + identifier,
                    input.getPattern().__(SingleIdentifier::getProxyEObject),
                    acceptor
                );

            if (input.getMode().getUnification()
                == PatternMatchMode.Unification.WITH_VAR_DECLARATION) {
                IJadescriptType solvedPatternType =
                    inferPatternType(input, state)
                        .solve(input.getProvidedInputType());

                // update of state is omitted on purpose

                if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS
                    && input.getPattern().isPresent()) {
                    module.get(ValidationHelper.class).emitInfo(
                        ISSUE_CODE_PREFIX + "Info",
                        "Inferred declaration, type: "
                            + solvedPatternType.getJadescriptName(),
                        input.getPattern(),
                        acceptor
                    );
                }

            }
            return resolutionCheck;
        } else {
            IJadescriptType typeOfNamedSymbol = inferType(
                input.getPattern(),
                state
            );
            return module.get(ValidationHelper.class).assertExpectedType(
                input.getProvidedInputType(),
                typeOfNamedSymbol,
                "UnexpectedTermType",
                input.getPattern().__(SingleIdentifier::getProxyEObject),
                acceptor
            );
        }
    }


    protected boolean validateInternal(
        Maybe<SingleIdentifier> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<String> ident = input.__(SingleIdentifier::getIdent);
        if (ident.isNothing()) {
            return VALID;
        }
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved =
            resolve(input, state);
        //TODO validate closure usage when new usage tracking is available
        return module.get(ValidationHelper.class).asserting(
            resolved.isPresent(),
            "UnresolvedSymbol",
            "Unresolved symbol: " + ident.orElse("[empty]"),
            input,
            acceptor
        );
    }


    public boolean validateAssignmentInternal(
        Maybe<SingleIdentifier> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final Maybe<String> ident = input.__(SingleIdentifier::getIdent);


        boolean rightValidation = module.get(RValueExpressionSemantics.class)
            .validate(expression, state, acceptor);

        if (ident.isNothing() || rightValidation == INVALID) {
            return INVALID;
        }
        StaticState afterRExpr = module.get(RValueExpressionSemantics.class)
            .advance(expression, state);
        IJadescriptType typeOfRExpression =
            module.get(RValueExpressionSemantics.class).inferType(
                expression, state);

        final Maybe<Either<NamedSymbol, CallableSymbol>> resolve =
            resolve(input, afterRExpr);

        if (resolve.isNothing()) {
            //validate inferred declaration
            boolean reservedNameValidation =
                module.get(ValidationHelper.class).assertNotReservedName(
                    ident,
                    input,
                    JadescriptPackage.eINSTANCE.getAssignment_Lexpr(),
                    acceptor
                );


            if (reservedNameValidation == INVALID) {
                return INVALID;
            }

            boolean typeValidation = typeOfRExpression
                .validateType(expression, acceptor);
            if (typeValidation == INVALID
                || ident.isNothing()
                || ident.toNullable().isBlank()
            ) {
                return INVALID;
            }

            String identSafe = ident.toNullable();

            final UserVariable newDeclared = new UserVariable(
                identSafe,
                typeOfRExpression,
                true
            );
            module.get(CompilationHelper.class).lateBindingContext()
                .pushVariable(newDeclared);


            //NOT NEEDED
//            final StaticState newState = state.assertNamedSymbol(newDeclared);

            if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
                module.get(ValidationHelper.class).emitInfo(
                    ISSUE_CODE_PREFIX + "Info",
                    "Inferred declaration; type: " +
                        typeOfRExpression.getJadescriptName(),
                    input,
                    acceptor
                );
            }
            return VALID;
        } else if (resolve.toNullable() instanceof Either.Left) {
            NamedSymbol variable = (
                (Either.Left<NamedSymbol, CallableSymbol>) resolve.toNullable()
            ).getLeft();

            boolean canWrite = module.get(ValidationHelper.class).asserting(
                variable.canWrite(),
                "InvalidAssignment",
                "'" + ident.orElse("[empty]") + "' is read-only.",
                input,
                acceptor
            );


            if (canWrite && variable instanceof UserVariable) {
                canWrite = module.get(ValidationHelper.class).asserting(
                    !((UserVariable) variable).isCapturedInAClosure(),
                    "AssigningToCapturedReference",
                    "This local variable is internally captured in a closure," +
                        " " +
                        "and it can not be modified in this context.",
                    input,
                    acceptor
                );
                if (canWrite) {
                    ((UserVariable) variable).notifyWriteUsage();
                }
            }


            final IJadescriptType resolvedNameType = variable.writingType();
            final boolean typeConformance =
                module.get(ValidationHelper.class).assertExpectedType(
                    resolvedNameType,
                    typeOfRExpression,
                    "InvalidAssignment",
                    input.__(ProxyEObject::getProxyEObject),
                    acceptor
                );
            return canWrite && typeConformance;
        } else /*if(resolve.toNullable() instanceof Either.Right)*/ {
            return errorNotLvalue(
                input,
                "Cannot assign a value to a name that resolves to a function " +
                    "with nullary arity.",
                acceptor
            );
        }
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<SingleIdentifier> input,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<SingleIdentifier> input) {
        return true;
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<SingleIdentifier> input,
        StaticState state
    ) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolve =
            this.resolve(input.getPattern(), state);
        if (resolve.isNothing()) {
            //Yes: probably declaring a new local variable.
            return true;
        }

        final Either<NamedSymbol, CallableSymbol> either = resolve.toNullable();
        if (either instanceof Either.Left) {
            //Resolves to a variable/property
            return true;
        } else if (either instanceof Either.Right) {
            //Resolves to a function invocation
            return module.get(MethodCallSemantics.class)
                .isPatternEvaluationPure(input.replacePattern(
                    MethodCall.methodCall(input.getPattern())
                ), state);
        } else {
            return true;
        }

    }


    public boolean validateAsStatementTraversing(
        Maybe<SingleIdentifier> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved
            = resolve(input, state);
        if (resolved.isPresent() &&
            resolved.toNullable() instanceof Either.Right) {
            // nullary function call: ok as statement
            return VALID;
        } else {
            return module.get(ValidationHelper.class).emitError(
                "InvalidStatement",
                "Not a statement.",
                input,
                acceptor
            );
        }
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<SingleIdentifier> input) {
        return true;
    }

}
