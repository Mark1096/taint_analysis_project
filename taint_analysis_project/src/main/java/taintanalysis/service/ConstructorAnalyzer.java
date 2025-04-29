package taintanalysis.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import taintanalysis.config.ConstructorInfo;

import java.util.List;
import java.util.Optional;

/**
 * <h1> ConstructorAnalyzer </h1>
 *
 * This class checks the parameters passed to the constructor of the instance invoking the method to be analyzed.
 */
public class ConstructorAnalyzer {

    private final static ConstructorAnalyzer obj = new ConstructorAnalyzer();

    private ConstructorAnalyzer() {
    }

    /**
     * Returns the only instance of the class.
     *
     * @return constructor analyzer
     */
    public static ConstructorAnalyzer getInstance() {
        return obj;
    }

    /**
     * Find the variable declaration in the context of the CompilationUnit.
     *
     * @param nameExpr the name expr
     * @param cu the cu
     * @return optional string
     */
    private Optional<String> resolveVariableType(NameExpr nameExpr, CompilationUnit cu) {
        return cu.findAll(VariableDeclarator.class).stream()
                .filter(declarator -> declarator.getNameAsString().equals(nameExpr.getNameAsString()))
                .map(declarator -> declarator.getType().asString())
                .findFirst();
    }

    /**
     * Adds the type extrapolated from the class argument NameExpr to the parameterTypes list.
     *
     * @param arg the arg
     * @param parameterTypes the parameter types
     */
    private void argAsNameExpr(NameExpr arg, List<String> parameterTypes) {
        parameterTypes.add(arg.calculateResolvedType().describe());
    }

    /**
     * Adds the type extrapolated from the class argument LiteralExpr to the parameterTypes list.
     *
     * @param arg the arg
     * @param parameterTypes the parameter types
     */
    private void argAsLiteralExpr(LiteralExpr arg, List<String> parameterTypes) {
        parameterTypes.add(arg.calculateResolvedType().describe());
    }

    /**
     * Adds the type extrapolated from the class argument ObjectCreationExpr to the parameterTypes list.
     *
     * @param arg the arg
     * @param parameterTypes the parameter types
     * @param cu the cu
     */
    private void argAsObjectCreationExpr(ObjectCreationExpr arg, List<String> parameterTypes, CompilationUnit cu) {
        analyzeConstructorDetails(arg, parameterTypes, cu);
        parameterTypes.add(arg.getType().resolve().describe());
    }

    /**
     * Adds the type extrapolated from the class argument MethodCallExpr to the parameterTypes list.
     *
     * @param arg the arg
     * @param cu the cu
     * @param parameterTypes the parameter types
     */
    private void argAsMethodCallExpr(MethodCallExpr arg, CompilationUnit cu, List<String> parameterTypes) {
        arg.getScope().ifPresentOrElse(
                scope -> {
                    if (scope.isNameExpr()) {
                        NameExpr instance = scope.asNameExpr();
                        Optional<String> result = resolveVariableType(instance, cu);
                        result.ifPresentOrElse(
                                obj -> {
                                    String objAndMethod = obj + ".".concat(arg.getNameAsString()) + "()";
                                    parameterTypes.add(objAndMethod);
                                    parameterTypes.add(arg.resolve().getReturnType().describe());
                                },
                                () -> {
                                    System.out.println("Unable to solve type for: " + instance.getName());
                                    parameterTypes.add(null);
                                }
                        );
                    } else {
                        System.out.println("Scope is not a NameExpr: " + scope);
                    }
                },
                () -> System.out.println("Method call has no scope")
        );
    }

    /**
     * Adds the type extrapolated from the class argument FieldAccessExpr to the parameterTypes list.
     *
     * @param arg the arg
     * @param parameterTypes the parameter types
     */
    private void argAsFieldAccessExpr(FieldAccessExpr arg, List<String> parameterTypes) {
        FieldAccessExpr fieldAccessExpr = arg.asFieldAccessExpr();
        NameExpr className = fieldAccessExpr.getScope().asNameExpr();
        String field = fieldAccessExpr.getNameAsString();
        String classAndField = className.toString() + ".".concat(field);
        parameterTypes.add(classAndField);
        parameterTypes.add(fieldAccessExpr.resolve().getType().describe());
    }

    /**
     * It analyses the arguments passed to the constructor and inserts their type into the parameterTypes list.
     *
     * @param creationExpr the creation expr
     * @param parameterTypes the parameter types
     * @param cu the cu
     */
    public void analyzeConstructorDetails(ObjectCreationExpr creationExpr, List<String> parameterTypes, CompilationUnit cu) {
        for (Expression arg : creationExpr.getArguments()) {
            if (arg instanceof NameExpr) {
                argAsNameExpr(arg.asNameExpr(), parameterTypes);
            } else if (arg instanceof LiteralExpr) {
                argAsLiteralExpr(arg.asLiteralExpr(), parameterTypes);
            } else if (arg instanceof ObjectCreationExpr) {
                argAsObjectCreationExpr(arg.asObjectCreationExpr(), parameterTypes, cu);
            } else if (arg instanceof MethodCallExpr) {
                argAsMethodCallExpr(arg.asMethodCallExpr(), cu, parameterTypes);
            } else if (arg instanceof FieldAccessExpr) {
                argAsFieldAccessExpr(arg.asFieldAccessExpr(), parameterTypes);
            } else {
                System.out.println("Argument of unknown type:" + arg.getClass().getSimpleName());
            }
        }
    }

    /**
     * It performs checks on the size and types of parameters passed,
     * to both the external source constructor in the user code and the constructor in the configuration file.
     *
     * @param constructor the constructor
     * @param constructorArgs the constructor args
     * @return boolean
     */
    public boolean matchesConstructor(ConstructorInfo constructor, List<String> constructorArgs) {
        return constructor.getParameterTypes().size() == constructorArgs.size() &&
                constructor.getParameterTypes().equals(constructorArgs);
    }

}
