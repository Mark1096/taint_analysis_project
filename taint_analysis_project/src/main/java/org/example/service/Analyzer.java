package org.example.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.github.javaparser.ParserConfiguration;
import org.example.config.ConfigLoader;
import java.io.FileInputStream;
import java.util.*;

public class Analyzer {

    private static ConfigLoader configLoader;
    private final String[] commandLineArgs;
    private static CompilationUnit cu;
    private static Map<String, String> sanitizationMapping;

    public Analyzer(ConfigLoader loader, String[] args, String configFilePath) {
        configLoader = loader;
        this.commandLineArgs = args;
        sanitizationMapping = InputSanitizerMapping.creationMapping(configFilePath);
    }

    public void analyze(String sourceFilePath) throws IOException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(Paths.get("src/main/java")));

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));

        JavaParser javaParser = new JavaParser(parserConfiguration);

        FileInputStream in = new FileInputStream(sourceFilePath);
        cu = javaParser.parse(in).getResult().orElseThrow();

        // Inizializza il VariableResolverVisitor e visita il CompilationUnit
        VariableResolverVisitor variableResolver = new VariableResolverVisitor();
        variableResolver.visit(cu, null);

        // Usa MethodCallVisitor con il resolver integrato
        MethodCallVisitor methodCallVisitor = new MethodCallVisitor();
        methodCallVisitor.visit(cu, null);

        // Scrivi il codice modificato in un nuovo file
        String outputPath = "src/main/java/org/example/destination/ClassToAnalyze2.java";
        Files.write(Paths.get(outputPath), cu.toString().getBytes());

        System.out.println("File aggiornato scritto in: " + outputPath);

        analyzeCommandLineArgs();
    }

    private void analyzeCommandLineArgs() {
        boolean isTrusted = configLoader.isSourceTrusted("commandLineArgs");
        System.out.println("Trusted status for commandLineArgs: " + isTrusted);

        if (!isTrusted) {
            for (String arg : commandLineArgs) {
                if (!isValid(arg)) {
                    System.out.println("Warning: Invalid command line argument: " + arg);
                } else {
                    System.out.println("Valid command line argument: " + arg);
                }
            }
        }
    }

    private boolean isValid(String arg) {
        return arg != null && arg.matches("[a-zA-Z0-9]+");
    }

    private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {

        private final VariableResolverVisitor variableResolver;

        public MethodCallVisitor() {
            this.variableResolver = new VariableResolverVisitor();
        }

        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {

            System.out.println("Metodo da analizzare: " + methodCall.getName());

            // Verifica se la chiamata a metodo è un argomento di un'altra espressione
            if (isNestedMethodCall(methodCall)) {
                System.out.println("Il metodo: " + methodCall.getName() + " è un argomento di un construttore o di un altro metodo!");
                System.out.println("-------------------------------------------");
                return; // Ignora questo metodo, è un argomento di un'altra chiamata
            }

            methodCall.getScope().ifPresent(scope -> {
                try {

                    ResolvedType resolvedType = scope.calculateResolvedType();

                    if (resolvedType.isReferenceType()) {

                        // Integra VariableResolverVisitor per trovare la variabile
                        variableResolver.visit(cu, null);

                        String currentMethod = methodCall.getNameAsString() + "()";
                        String className = scope.calculateResolvedType().describe();
                        List<String> constructorParameterTypes = new ArrayList<>();
                        boolean staticMethod = false;

                        ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();

                        if (!resolvedMethod.isStatic()) {
                            // Trova la dichiarazione della variabile
                            String variableName = scope.toString();

                            ResolvedType variableType = variableResolver.getResolvedVariableType(variableName);
                            if (variableType != null) {
                                System.out.println("Tipo della variabile '" + variableName + "': " + variableType.describe());
                            }

                            // Analizza il costruttore e i parametri
                            analyzeConstructor(scope, constructorParameterTypes);
                        }
                        else {
                            System.out.println("Il metodo è statico.");
                            staticMethod = true;
                        }

                        compareWithConfigurationData(constructorParameterTypes, className, methodCall, staticMethod);
                    }
                    System.out.println("-------------------------------------------");
                } catch (Exception e) {
                    System.err.println("Error resolving class for method call: " + e.getMessage());
                }
            });

            super.visit(methodCall, arg);
        }

        private boolean isNestedMethodCall(MethodCallExpr methodCall) {
            return methodCall.getParentNode()
                    .map(parent -> parent instanceof MethodCallExpr || parent instanceof ObjectCreationExpr)
                    .orElse(false);
        }

        private void analyzeConstructor(Expression scope, List<String> constructorParameterTypes) {
            if (scope.isNameExpr()) {

                NameExpr nameExpr = scope.asNameExpr();

                try {
                    // Trova la dichiarazione originale nello scope corretto
                    Optional<VariableDeclarator> variableNode = findVariableNodeInScope(nameExpr);
                    variableNode.ifPresent(variable -> analyzeVariableInitializer(variable, constructorParameterTypes));
                } catch (UnsolvedSymbolException e) {
                    System.err.println("Impossibile risolvere il simbolo per: " + nameExpr.getNameAsString());
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }

        // Metodo per trovare la dichiarazione di una variabile nello scope corretto
        private Optional<VariableDeclarator> findVariableNodeInScope(NameExpr nameExpr) {
            String variableName = nameExpr.getNameAsString();

            // Cerca nello scope locale (blocco di codice)
            Optional<BlockStmt> blockScope = nameExpr.findAncestor(BlockStmt.class);
            if (blockScope.isPresent()) {
                Optional<VariableDeclarator> localVariable = blockScope.get().findAll(VariableDeclarator.class).stream()
                        .filter(v -> v.getNameAsString().equals(variableName))
                        .findFirst();
                if (localVariable.isPresent()) {
                    return localVariable;
                }
            }

            // Cerca nello scope del metodo
            Optional<MethodDeclaration> methodScope = nameExpr.findAncestor(MethodDeclaration.class);
            if (methodScope.isPresent()) {
                return methodScope.get().getBody()
                        .flatMap(body -> body.findAll(VariableDeclarator.class).stream()
                                .filter(v -> v.getNameAsString().equals(variableName))
                                .findFirst());
            }

            // Cerca nei campi della classe
            Optional<ClassOrInterfaceDeclaration> classScope = nameExpr.findAncestor(ClassOrInterfaceDeclaration.class);
            if (classScope.isPresent()) {
                // Cerca campi dichiarati nella classe
                Optional<VariableDeclarator> fieldVariable = classScope.get().findAll(FieldDeclaration.class).stream()
                        .flatMap(field -> field.getVariables().stream())
                        .filter(v -> v.getNameAsString().equals(variableName))
                        .findFirst();
                if (fieldVariable.isPresent()) {
                    return fieldVariable;
                }

                // Controlla se il campo è inizializzato nel costruttore
                Optional<ConstructorDeclaration> constructor = classScope.get().findFirst(ConstructorDeclaration.class);
                if (constructor.isPresent()) {
                    Optional<VariableDeclarator> initializedInConstructor = constructor.get().getBody()
                            .findAll(AssignExpr.class).stream()
                            .filter(assign -> assign.getTarget().isNameExpr())
                            .filter(assign -> assign.getTarget().asNameExpr().getNameAsString().equals(variableName))
                            .map(AssignExpr::getValue)
                            .filter(Expression::isObjectCreationExpr)
                            .map(Expression::asObjectCreationExpr)
                            .map(expr -> new VariableDeclarator(expr.getType(), variableName, expr))
                            .findFirst();
                    if (initializedInConstructor.isPresent()) {
                        return initializedInConstructor;
                    }
                }
            }
            return Optional.empty();
        }

        private void analyzeVariableInitializer(VariableDeclarator variable, List<String> constructorParameterTypes) {
            variable.getInitializer().ifPresent(initializer -> {
                if (initializer.isObjectCreationExpr()) {
                    ObjectCreationExpr creationExpr = initializer.asObjectCreationExpr();
                    System.out.println("Oggetto creato: " + creationExpr);
                    analyzeConstructorDetails(creationExpr, constructorParameterTypes);
                    Collections.reverse(constructorParameterTypes);
                }
            });
        }

        public static Optional<String> resolveVariableType(NameExpr nameExpr, CompilationUnit cu) {
            // Trova la dichiarazione della variabile nel contesto della CompilationUnit
            return cu.findAll(VariableDeclarator.class).stream()
                    .filter(declarator -> declarator.getNameAsString().equals(nameExpr.getNameAsString()))
                    .map(declarator -> declarator.getType().asString())
                    .findFirst();
        }

        private void insertSanitizeMethod(MethodCallExpr methodCall, String source) {

            if (sanitizationMapping.containsKey(source)) {
                String value = sanitizationMapping.get(source);
                String sanitizedCall = value.concat("(" + methodCall.toString() + ")");

                // Sostituisce l'espressione corrente con quella nuova
                Expression sanitizedExpression = StaticJavaParser.parseExpression(sanitizedCall);
                methodCall.replace(sanitizedExpression);

            } else {
                System.out.println("Chiave '" + source + "' non trovata nella mappa.");
            }
        }

        private void compareWithConfigurationData(List<String> parameterTypes, String className,
                                                  MethodCallExpr methodCall, boolean staticMethod) {

            String currentMethod = methodCall.getNameAsString().concat("()");
            // Continua il confronto con la configurazione
            var constructorDetails = configLoader.getSourceDetailsForResolvedType(className, currentMethod, parameterTypes, staticMethod);

            if (constructorDetails != null && !constructorDetails.isTrusted()) {
                System.out.println("Warning: Object created with untrusted constructor:");
                System.out.println("  Class: " + className);
                System.out.println("  Parameters: " + parameterTypes);
                System.out.println("  Source: " + constructorDetails.getName());
                System.out.println("  Trusted: " + constructorDetails.isTrusted());
                insertSanitizeMethod(methodCall, constructorDetails.getName());
            }
        }

        private void analyzeConstructorDetails(ObjectCreationExpr creationExpr, List<String> parameterTypes) {

            String currentType = creationExpr.getType().resolve().describe();

            System.out.println("Analizzando costruttore: " + currentType);
            System.out.println("Parametri: " + creationExpr.getArguments());

            for (Expression arg: creationExpr.getArguments()) {

                ResolvedType argType = arg.calculateResolvedType();

                if (arg instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr) arg;
                    System.out.println("Argomento è una variabile: " + nameExpr.getName());

                    Optional<String> result = resolveVariableType(nameExpr, cu);
                    result.ifPresentOrElse(
                            type -> System.out.println("Tipo risolto: " + type),
                            () -> System.out.println("Impossibile risolvere il tipo per: " + nameExpr.getName())
                    );

                    ResolvedType resolvedType = variableResolver.getResolvedVariableType(arg.asNameExpr().getNameAsString());
                    if (resolvedType != null) {
                        parameterTypes.add(resolvedType.describe());
                    } else {
                        parameterTypes.add(argType.describe());
                    }
                } else if (arg instanceof LiteralExpr) {
                    LiteralExpr literalExpr = (LiteralExpr) arg;
                    System.out.println("Argomento è un valore letterale: " + literalExpr);
                    parameterTypes.add(arg.calculateResolvedType().describe());
                } else if (arg instanceof ObjectCreationExpr) {
                    ObjectCreationExpr nestedExpr = arg.asObjectCreationExpr();
                    System.out.println("Trovato un altro ObjectCreationExpr, di tipo: " + nestedExpr.getType());
                    analyzeConstructorDetails(nestedExpr, parameterTypes);
                    parameterTypes.add(nestedExpr.getType().resolve().describe());
                } else if (arg instanceof MethodCallExpr) {
                    MethodCallExpr methodCallExpr = (MethodCallExpr) arg;
                    System.out.println("Method: " + methodCallExpr);
                    System.out.println("Method returned type: " + methodCallExpr.resolve().getReturnType().describe());
                    NameExpr instance = (methodCallExpr.getScope().get()).asNameExpr();
                    Optional<String> result = resolveVariableType(instance, cu);
                    result.ifPresentOrElse(
                            obj -> {
                                System.out.println("Tipo risolto: " + obj);
                                String objAndMethod = obj + ".".concat(methodCallExpr.getNameAsString()) + "()";
                                parameterTypes.add(objAndMethod);
                                parameterTypes.add(methodCallExpr.resolve().getReturnType().describe());
                            },
                            () -> {
                                System.out.println("Impossibile risolvere il tipo per: " + instance.getName());
                                parameterTypes.add(null);
                            }
                    );
                } else if (arg instanceof FieldAccessExpr) {
                    FieldAccessExpr fieldAccessExpr = arg.asFieldAccessExpr();
                    NameExpr className = fieldAccessExpr.getScope().asNameExpr();
                    String field = fieldAccessExpr.getNameAsString();
                    String classAndField = className.toString() + ".".concat(field);
                    parameterTypes.add(classAndField);
                    parameterTypes.add(fieldAccessExpr.resolve().getType().describe());
                } else {
                    System.out.println("Argomento di tipo sconosciuto: " + arg.getClass().getSimpleName());
                }
            }
        }
    }

    private static class VariableResolverVisitor extends VoidVisitorAdapter<Void> {

        private final Map<String, ResolvedType> resolvedVariables = new HashMap<>();

        public void visit(VariableDeclarationExpr declaration, Void arg) {
            for (VariableDeclarator var : declaration.getVariables()) {
                try {
                    ResolvedType resolvedType = var.getType().resolve();
                    resolvedVariables.put(var.getNameAsString(), resolvedType);
                } catch (Exception e) {
                    System.err.println("Error resolving type for variable " + var.getNameAsString() + ": " + e.getMessage());
                }
            }
            super.visit(declaration, arg);
        }

        /**
         * Risolve il tipo di una variabile a runtime.
         */
        public ResolvedType getResolvedVariableType(String variableName) {
            return resolvedVariables.getOrDefault(variableName, null);
        }
    }

}
