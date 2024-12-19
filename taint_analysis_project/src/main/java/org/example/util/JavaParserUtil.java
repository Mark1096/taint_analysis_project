package org.example.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class JavaParserUtil {

    public static boolean isMethodCallingSource(MethodCallExpr methodCall, String sourceName) {
        // Logica per verificare se un metodo chiama una sorgente di dati
        return methodCall.getNameAsString().equals(sourceName);
    }
}

