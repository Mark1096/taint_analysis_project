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
import org.example.config.Source;

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

    private JavaParser parserSetting() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(Paths.get("src/main/java")));

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));

        return new JavaParser(parserConfiguration);
    }

    public void analyze(String sourceFilePath) throws IOException {
        JavaParser javaParser = parserSetting();
        FileInputStream in = new FileInputStream(sourceFilePath);
        cu = javaParser.parse(in).getResult().orElseThrow();

        // Usa MethodCallVisitor con il resolver integrato
        MethodCallVisitor methodCallVisitor = new MethodCallVisitor();
        methodCallVisitor.visit(cu, null);

        // Scrivi il codice modificato in un nuovo file
        String outputPath = "src/main/java/org/example/destination/ClassToAnalyze2.java";
        Files.write(Paths.get(outputPath), cu.toString().getBytes());

        System.out.println("File aggiornato scritto in: " + outputPath);

        analyzeCommandLineArgs();
    }

    private boolean isValid(String arg) {
        return arg != null && arg.matches("[a-zA-Z0-9]+");
    }

    private void analyzeCommandLineArgs() {
        boolean isTrusted = configLoader.isSourceTrusted("commandLineArgs");
        System.out.println("Trusted status for commandLineArgs: " + isTrusted);

        if (!isTrusted) {
            Arrays.stream(commandLineArgs)
                    .forEach(arg -> System.out.println(isValid(arg)
                            ? "Valid command line argument: " + arg
                            : "Warning: Invalid command line argument: " + arg));
        }
    }

    @SuppressWarnings("unchecked")
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
                return;
            }

            methodCall.getScope().ifPresent(scope -> {
                try {

                    ResolvedType resolvedType = scope.calculateResolvedType();

                    if (resolvedType.isReferenceType()) {

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
                            constructorScope(scope, constructorParameterTypes);
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

        private void constructorScope(Expression scope, List<String> constructorParameterTypes) {
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

        private Optional<VariableDeclarator> checkIntoLocalScope(NameExpr nameExpr) {
            // Cerca nello scope locale (blocco di codice)
            Optional<BlockStmt> blockScope = nameExpr.findAncestor(BlockStmt.class.asSubclass(BlockStmt.class));

            if (blockScope.isPresent()) {
                Optional<VariableDeclarator> localVariable = blockScope.get().findAll(VariableDeclarator.class).stream()
                        .filter(v -> v.getNameAsString().equals(nameExpr.getNameAsString()))
                        .findFirst();
                if (localVariable.isPresent()) {
                    return localVariable;
                }
            }
            return Optional.empty();
        }

        private Optional<VariableDeclarator> checkIntoMethodScope(NameExpr nameExpr) {
            // Cerca nello scope del metodo
            Optional<MethodDeclaration> methodScope = nameExpr.findAncestor(MethodDeclaration.class);
            return methodScope.flatMap(methodDeclaration -> methodDeclaration.getBody()
                    .flatMap(body -> body.findAll(VariableDeclarator.class).stream()
                            .filter(v -> v.getNameAsString().equals(nameExpr.getNameAsString()))
                            .findFirst()));
        }

        private Optional<VariableDeclarator> checkIntoConstructor(NameExpr nameExpr, ClassOrInterfaceDeclaration classScope) {
            Optional<ConstructorDeclaration> constructor = classScope.findFirst(ConstructorDeclaration.class);
            if (constructor.isPresent()) {
                Optional<VariableDeclarator> initializedInConstructor = constructor.get().getBody()
                        .findAll(AssignExpr.class).stream()
                        .filter(assign -> assign.getTarget().isNameExpr())
                        .filter(assign -> assign.getTarget().asNameExpr().getNameAsString().equals(nameExpr.getNameAsString()))
                        .map(AssignExpr::getValue)
                        .filter(Expression::isObjectCreationExpr)
                        .map(Expression::asObjectCreationExpr)
                        .map(expr -> new VariableDeclarator(expr.getType(), nameExpr.getNameAsString(), expr))
                        .findFirst();
                if (initializedInConstructor.isPresent()) {
                    return initializedInConstructor;
                }
            }
            return Optional.empty();
        }

        private Optional<VariableDeclarator> checkIntoClassFields(NameExpr nameExpr) {
            // Cerca nei campi della classe
            Optional<ClassOrInterfaceDeclaration> classScope = nameExpr.findAncestor(ClassOrInterfaceDeclaration.class);
            if (classScope.isPresent()) {
                // Cerca campi dichiarati nella classe
                Optional<VariableDeclarator> fieldVariable = classScope.get().findAll(FieldDeclaration.class).stream()
                        .flatMap(field -> field.getVariables().stream())
                        .filter(v -> v.getNameAsString().equals(nameExpr.getNameAsString()))
                        .findFirst();

                return fieldVariable.isPresent() ? fieldVariable : checkIntoConstructor(nameExpr, classScope.get());
            }
            return Optional.empty();
        }

        // Metodo per trovare la dichiarazione di una variabile nello scope corretto
        private Optional<VariableDeclarator> findVariableNodeInScope(NameExpr nameExpr) {

            return checkIntoLocalScope(nameExpr)
                    .or(() -> checkIntoMethodScope(nameExpr))
                    .or(() -> checkIntoClassFields(nameExpr));
        }

        private void analyzeVariableInitializer(VariableDeclarator variable, List<String> constructorParameterTypes) {
            variable.getInitializer().ifPresent(initializer -> {
                if (initializer.isObjectCreationExpr()) {
                    ObjectCreationExpr creationExpr = initializer.asObjectCreationExpr();
                    System.out.println("Oggetto creato: " + creationExpr);
                    ConstructorAnalyzer.analyzeConstructorDetails(creationExpr, constructorParameterTypes, cu, variableResolver);
                    Collections.reverse(constructorParameterTypes);
                }
            });
        }

        private void insertSanitizeMethod(MethodCallExpr methodCall, String source) {

            if (sanitizationMapping.containsKey(source)) {
                String sanitizedCall = sanitizationMapping.get(source)
                                                          .concat("(" + methodCall.toString() + ")");
                // Sostituisce l'espressione corrente con quella nuova
                methodCall.replace(StaticJavaParser.parseExpression(sanitizedCall));
            } else {
                System.out.println("Chiave '" + source + "' non trovata nella mappa.");
            }
        }

        private void compareWithConfigurationData(List<String> parameterTypes, String className,
                                                  MethodCallExpr methodCall, boolean staticMethod) {
            String currentMethod = methodCall.getNameAsString().concat("()");
            Source constructorDetails = configLoader.getSourceDetailsForResolvedType(className, currentMethod, parameterTypes, staticMethod);

            if (constructorDetails != null && !constructorDetails.isTrusted()) {
                System.out.println("Warning: Object created with untrusted constructor:");
                System.out.println("  Class: " + className);
                System.out.println("  Parameters: " + parameterTypes);
                System.out.println("  Source: " + constructorDetails.getName());
                System.out.println("  Trusted: " + constructorDetails.isTrusted());
                insertSanitizeMethod(methodCall, constructorDetails.getName());
            }
        }
    }
}
