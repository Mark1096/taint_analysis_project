package taintanalysis.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import taintanalysis.config.ConfigLoader;
import taintanalysis.config.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static taintanalysis.error.ErrorCode.generateException;

/**
 * <h1> MethodCallVisitor </h1>
 *
 * This class is part of the JavaParser AST and is used to solve the search for methods.
 */
public class MethodCallVisitor extends VoidVisitorAdapter<Void> {

    private final ConfigLoader configLoader;
    private final CompilationUnit cu;

    public MethodCallVisitor(CompilationUnit cu) {
        this.cu = cu;
        configLoader = ConfigLoader.getInstance();
    }

    /**
     * It inspects source code methods for data from external sources.
     *
     * @param methodCall the method call
     * @param arg the arg
     */
    @Override
    public void visit(MethodCallExpr methodCall, Void arg) {
        analyzeMethodCall(methodCall, arg);
    }

    /**
     * It analyses the input method, checking the type of data passed to the input source.
     *
     * @param methodCall the method call
     * @param arg the arg
     */
    private void analyzeMethodCall(MethodCallExpr methodCall, Void arg) {

        if (isNestedMethodCall(methodCall))
            return;

        methodCall.getScope().ifPresent(scope -> {
            try {
                if (scope.calculateResolvedType().isReferenceType()) {
                    String className = scope.calculateResolvedType().describe();
                    List<String> constructorParameterTypes = new ArrayList<>();
                    boolean staticMethod = false;

                    if (!(methodCall.resolve().isStatic())) {
                        constructorScope(scope, constructorParameterTypes);
                    } else {
                        staticMethod = true;
                    }

                    compareWithConfigurationData(constructorParameterTypes, className, methodCall, staticMethod);
                }
            } catch (Exception e) {
                throw generateException(e);
            }
        });
        super.visit(methodCall, arg);
    }

    /**
     * Check whether the method to be analyzed is nested in another method.
     *
     * @param methodCall the method call
     * @return boolean
     */
    private boolean isNestedMethodCall(MethodCallExpr methodCall) {
        return methodCall.getParentNode()
                .map(parent -> parent instanceof MethodCallExpr || parent instanceof ObjectCreationExpr)
                .orElse(false);
    }

    /**
     * It searches the variable declaration at any scope level and analyses the constructor parameters.
     *
     * @param scope the scope
     * @param constructorParameterTypes the constructor parameter types
     */
    private void constructorScope(Expression scope, List<String> constructorParameterTypes) {
        if (scope.isNameExpr()) {
            NameExpr nameExpr = scope.asNameExpr();
            try {
                Optional<VariableDeclarator> variableNode = findVariableNodeInScope(nameExpr);
                variableNode.ifPresent(variable -> analyzeVariableInitializer(variable, constructorParameterTypes));
            } catch (UnsolvedSymbolException e) {
                throw generateException(e);
            }
        }
    }

    /**
     * Look for the variable declaration within the scope of the block.
     *
     * @param nameExpr the name expr
     * @return optional variable declarator
     */
    private Optional<VariableDeclarator> checkIntoLocalScope(NameExpr nameExpr) {
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

    /**
     * Look for the variable declaration within the method of the class in which the current method is being analyzed.
     *
     * @param nameExpr the name expr
     * @return optional variable declarator
     */
    private Optional<VariableDeclarator> checkIntoMethodScope(NameExpr nameExpr) {
        Optional<MethodDeclaration> methodScope = nameExpr.findAncestor(MethodDeclaration.class);
        return methodScope.flatMap(methodDeclaration -> methodDeclaration.getBody()
                .flatMap(body -> body.findAll(VariableDeclarator.class).stream()
                        .filter(v -> v.getNameAsString().equals(nameExpr.getNameAsString()))
                        .findFirst()));
    }

    /**
     * Look for the variable declaration in the instantiation of the object, then check the constructor.
     *
     * @param nameExpr the name expr
     * @param classScope the class scope
     * @return optional variable declarator
     */
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

    /**
     * Look for the variable declaration in the fields of the class.
     *
     * @param nameExpr the name expr
     * @return optional variable declarator
     */
    private Optional<VariableDeclarator> checkIntoClassFields(NameExpr nameExpr) {
        Optional<ClassOrInterfaceDeclaration> classScope = nameExpr.findAncestor(ClassOrInterfaceDeclaration.class);
        if (classScope.isPresent()) {
            Optional<VariableDeclarator> fieldVariable = classScope.get().findAll(FieldDeclaration.class).stream()
                    .flatMap(field -> field.getVariables().stream())
                    .filter(v -> v.getNameAsString().equals(nameExpr.getNameAsString()))
                    .findFirst();
            return fieldVariable.isPresent() ? fieldVariable : checkIntoConstructor(nameExpr, classScope.get());
        }
        return Optional.empty();
    }

    /**
     * Method for finding the declaration of a variable in the correct scope.
     *
     * @param nameExpr the name expr
     * @return optional variable declarator
     */
    private Optional<VariableDeclarator> findVariableNodeInScope(NameExpr nameExpr) {
        return checkIntoLocalScope(nameExpr)
                .or(() -> checkIntoMethodScope(nameExpr))
                .or(() -> checkIntoClassFields(nameExpr));
    }

    /**
     * Converts an expression into an ObjectCreationExpr.
     *
     * @param expr the expr
     * @return object creation expr
     */
    private ObjectCreationExpr convertToObjectCreationExpr(Expression expr) {
        return expr.asObjectCreationExpr();
    }

    /**
     * Analyses the instantiation of the object provided as input,
     * obtaining the types of the parameters passed to the constructor.
     *
     * @param variable the variable
     * @param constructorParameterTypes the constructor parameter types
     */
    private void analyzeVariableInitializer(VariableDeclarator variable, List<String> constructorParameterTypes) {
        variable.getInitializer().ifPresent(initializer -> {
            if (initializer.isObjectCreationExpr()) {
                ObjectCreationExpr creationExpr = convertToObjectCreationExpr(initializer);
                ConstructorAnalyzer.getInstance().analyzeConstructorDetails(creationExpr, constructorParameterTypes, cu);
                Collections.reverse(constructorParameterTypes);
            }
        });
    }

    /**
     * It applies input sanitization where an untrusted external source has been detected in the user's source code.
     *
     * @param methodCall the method call
     * @param source the source
     */
    private void insertSanitizeMethod(MethodCallExpr methodCall, String source) {
        var sanitizationMapping = new InputSanitizer().creationMapping();
        if (sanitizationMapping.containsKey(source)) {
            String sanitizedCall = sanitizationMapping.get(source).concat("(" + methodCall.toString() + ")");
            methodCall.replace(StaticJavaParser.parseExpression(sanitizedCall));
        } else {
            System.out.println("Key '" + source + "' not found in the map.");
        }
    }

    /**
     * It compares the source information of the configuration file with that obtained from the analysis of the current method.
     *
     * @param parameterTypes the parameter types
     * @param className the class name
     * @param methodCall the method call
     * @param staticMethod the static method
     */
    private void compareWithConfigurationData(List<String> parameterTypes, String className,
                                              MethodCallExpr methodCall, boolean staticMethod) {
        String currentMethod = methodCall.getNameAsString().concat("()");
        Source constructorDetails = configLoader.getSourceDetailsForResolvedType(className, currentMethod, parameterTypes, staticMethod);

        if (constructorDetails != null && !constructorDetails.isTrusted()) {
            insertSanitizeMethod(methodCall, constructorDetails.getName());
        }
    }
}
