package org.example.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.types.ResolvedType;
import org.example.config.ConstructorInfo;

import java.util.List;
import java.util.Optional;

public class ConstructorAnalyzer {

    private static Optional<String> resolveVariableType(NameExpr nameExpr, CompilationUnit cu) {
        // Trova la dichiarazione della variabile nel contesto della CompilationUnit
        return cu.findAll(VariableDeclarator.class).stream()
                .filter(declarator -> declarator.getNameAsString().equals(nameExpr.getNameAsString()))
                .map(declarator -> declarator.getType().asString())
                .findFirst();
    }

    private static void argAsNameExpr(NameExpr arg, CompilationUnit cu, List<String> parameterTypes) {
        System.out.println("Argomento è una variabile: " + arg.getName());

        Optional<String> result = resolveVariableType(arg, cu);
        result.ifPresentOrElse(
                type -> System.out.println("Tipo risolto: " + type),
                () -> System.out.println("Impossibile risolvere il tipo per: " + arg.getName())
        );

        parameterTypes.add(arg.calculateResolvedType().describe());
    }

    private static void argAsLiteralExpr(LiteralExpr arg, List<String> parameterTypes) {
        System.out.println("Argomento è un valore letterale: " + arg);
        parameterTypes.add(arg.calculateResolvedType().describe());
    }

    private static void argAsObjectCreationExpr(ObjectCreationExpr arg, List<String> parameterTypes,
                                                CompilationUnit cu, VariableResolverVisitor variableResolver) {
        System.out.println("Trovato un altro ObjectCreationExpr, di tipo: " + arg.getType());
        analyzeConstructorDetails(arg, parameterTypes, cu, variableResolver);
        parameterTypes.add(arg.getType().resolve().describe());
    }

    private static void argAsMethodCallExpr(MethodCallExpr arg, CompilationUnit cu, List<String> parameterTypes) {
        System.out.println("Method: " + arg);
        System.out.println("Method returned type: " + arg.resolve().getReturnType().describe());
        arg.getScope().ifPresentOrElse(
                scope -> {
                    if (scope.isNameExpr()) {  // Controlla se è un NameExpr prima del cast
                        NameExpr instance = scope.asNameExpr();
                        Optional<String> result = resolveVariableType(instance, cu);
                        result.ifPresentOrElse(
                                obj -> {
                                    System.out.println("Tipo risolto: " + obj);
                                    String objAndMethod = obj + ".".concat(arg.getNameAsString()) + "()";
                                    parameterTypes.add(objAndMethod);
                                    parameterTypes.add(arg.resolve().getReturnType().describe());
                                },
                                () -> {
                                    System.out.println("Impossibile risolvere il tipo per: " + instance.getName());
                                    parameterTypes.add(null);
                                }
                        );
                    } else {
                        System.out.println("Scope non è un NameExpr: " + scope);
                    }
                },
                () -> System.out.println("Method call has no scope") // Caso in cui non c'è un scope
        );
    }

    private static void argAsFieldAccessExpr(FieldAccessExpr arg, List<String> parameterTypes) {
        FieldAccessExpr fieldAccessExpr = arg.asFieldAccessExpr();
        NameExpr className = fieldAccessExpr.getScope().asNameExpr();
        String field = fieldAccessExpr.getNameAsString();
        String classAndField = className.toString() + ".".concat(field);
        parameterTypes.add(classAndField);
        parameterTypes.add(fieldAccessExpr.resolve().getType().describe());
    }

    public static void analyzeConstructorDetails(ObjectCreationExpr creationExpr, List<String> parameterTypes,
                                           CompilationUnit cu, VariableResolverVisitor variableResolver) {

        String currentType = creationExpr.getType().resolve().describe();

        System.out.println("Analizzando costruttore: " + currentType);
        System.out.println("Parametri: " + creationExpr.getArguments());

        for (Expression arg: creationExpr.getArguments()) {

            ResolvedType argType = arg.calculateResolvedType();
            System.out.println("argType: " + argType);
            System.out.println("argType describe: " + argType.describe());

            if (arg instanceof NameExpr) {
                argAsNameExpr(arg.asNameExpr(), cu, parameterTypes);
            } else if (arg instanceof LiteralExpr) {
                argAsLiteralExpr(arg.asLiteralExpr(), parameterTypes);
            } else if (arg instanceof ObjectCreationExpr) {
                argAsObjectCreationExpr(arg.asObjectCreationExpr(), parameterTypes, cu, variableResolver);
            } else if (arg instanceof MethodCallExpr) {
                argAsMethodCallExpr(arg.asMethodCallExpr(), cu, parameterTypes);
            } else if (arg instanceof FieldAccessExpr) {
                argAsFieldAccessExpr(arg.asFieldAccessExpr(), parameterTypes);
            } else {
                System.out.println("Argomento di tipo sconosciuto: " + arg.getClass().getSimpleName());
            }
        }
    }

    public static boolean matchesConstructor(ConstructorInfo constructor, List<String> constructorArgs) {
        return constructor.getParameterTypes().size() == constructorArgs.size() &&
                constructor.getParameterTypes().equals(constructorArgs);
    }

}
