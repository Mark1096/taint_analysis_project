package taintanalysis.service;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.HashMap;
import java.util.Map;

import static taintanalysis.error.ErrorCode.generateException;

public class VariableResolverVisitor extends VoidVisitorAdapter<Void> {

    private final Map<String, ResolvedType> resolvedVariables = new HashMap<>();

    public void visit(VariableDeclarationExpr declaration, Void arg) {
        for (VariableDeclarator var : declaration.getVariables()) {
            try {
                ResolvedType resolvedType = var.getType().resolve();
                resolvedVariables.put(var.getNameAsString(), resolvedType);
            } catch (Exception e) {
                //System.err.println("Error resolving type for variable " + var.getNameAsString() + ": " + e.getMessage());
                throw generateException(e);
            }
        }
        super.visit(declaration, arg);
    }

    public ResolvedType getResolvedVariableType(String variableName) {
        return resolvedVariables.getOrDefault(variableName, null);
    }

}
