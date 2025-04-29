package taintanalysis.aspect;

import com.github.javaparser.ast.expr.Expression;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import taintanalysis.config.Source;
import java.util.List;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.CompilationUnit;

/**
 * <h1> LoggingAspect </h1>
 *
 * This class serves as an aspect to manage the application logs.
 */
@Aspect
public class LoggingAspect {

    /**
     * Print the method name to be analyzed.
     *
     * @param joinPoint the join point
     */
    @Before("execution(void analyzeMethodCall(MethodCallExpr, Void))")
    public void logMethodVisit(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        System.out.println("Method to be analyzed: " + ((MethodCallExpr) args[0]).getName());
    }

    /**
     * Print the attributes of the untrusted source.
     *
     * @param joinPoint the join point
     * @param result the result
     */
    @AfterReturning(
            pointcut = "execution(Source getSourceDetailsForResolvedType(String, String, List<String>, boolean))",
            returning = "result"
    )
    public void logUntrustedSourcesAttributes(JoinPoint joinPoint, Source result) {
        Object[] args = joinPoint.getArgs();
        if (result != null && !result.isTrusted()) {
            System.out.println("Warning: Object created with untrusted constructor:");
            System.out.println("  Class: " + args[0]);
            System.out.println("  Method: " + args[1]);
            System.out.println("  Source: " + result.getName());
            System.out.println("  Trusted: " + result.isTrusted());
        }
        else {
            System.out.println("Safe method.");
        }
    }

    /**
     * Print the method that is nested in another one.
     *
     * @param joinPoint the join point
     * @param result the result
     */
    @AfterReturning(
            pointcut = "execution(boolean isNestedMethodCall(MethodCallExpr))",
            returning = "result"
    )
    public void logNestedCall(JoinPoint joinPoint, boolean result) {
        Object[] args = joinPoint.getArgs();
        if (result) {
            System.out.println("The method: " + args[0] + " is an argument of a constructor or another method!");
        }
    }

    /**
     * Print the type of the parameter enclosed in the expression.
     *
     * @param joinPoint the join point
     */
    @Before("execution(void argAsNameExpr(NameExpr, List<String>)) || " +
            "execution(void argAsLiteralExpr(LiteralExpr, List<String>)) ||" +
            "execution(void argAsObjectCreationExpr(ObjectCreationExpr, List<String>, CompilationUnit)) ||" +
            "execution(void argAsMethodCallExpr(MethodCallExpr, CompilationUnit, List<String>)) ||" +
            "execution(void argAsFieldAccessExpr(FieldAccessExpr, List<String>))")
    public void logExprArg(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        System.out.println("Parameter type: " + ((Expression) args[0]).calculateResolvedType().describe());
    }

    /**
     * Prints a message to indicate that the method is static.
     *
     * @param result the result
     */
    @AfterReturning(
            pointcut = "execution(boolean isStatic())",
            returning = "result"
    )
    public void logStaticMethod(boolean result) {
        if(result)
            System.out.println("The method is static.");
    }

    /**
     * Print a separator between the analysis of one method and the other one.
     */
    @After("execution(void taintanalysis.service.MethodCallVisitor.visit(MethodCallExpr, Void))")
    public void logAnalysisSeparator() {
        System.out.println("-------------------------------------------");
    }

    /**
     * Prints the list of user files to be analyzed.
     *
     * @param result the result
     */
    @AfterReturning(
            pointcut = "execution(List<String> getSourcesList())",
            returning = "result"
    )
    public void logSourcesList(List<String> result) {
        if(!result.isEmpty())
            System.out.println("sourcesList: " + result);
    }

    /**
     * Print the contents of ObjectCreationExpr after conversion.
     *
     * @param result the result
     */
    @AfterReturning(
            pointcut = "execution(ObjectCreationExpr taintanalysis..*.convertToObjectCreationExpr(..))",
            returning = "result"
    )
    public void afterConversion(ObjectCreationExpr result) {
        System.out.println("Object created: " + result);
    }

}
