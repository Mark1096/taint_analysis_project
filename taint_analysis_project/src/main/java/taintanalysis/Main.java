package taintanalysis;

import com.github.javaparser.ast.CompilationUnit;
import taintanalysis.error.ErrorCode;
import taintanalysis.service.MethodCallVisitor;
import taintanalysis.utils.FileUtils;
import java.util.List;

import static taintanalysis.error.ErrorCode.generateErrorException;
import static taintanalysis.utils.FileUtils.*;

/**
 * <h1> Taint Analysis </h1>
 *
 * This program aims to analyze Java files, inspecting their source code and identifying areas where data from external sources is being used,
 * mitigating the risk by applying input sanitization methods.
 */
public class Main {

    /**
     * This is the main method from which methods to analyze user files will be called.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        List<String> sourcesList = FileUtils.getSourcesList();

        if (sourcesList.isEmpty())
            throw generateErrorException(ErrorCode.JAVA_FILE_NOT_FOUND);

        for (String fileName : sourcesList) {
            CompilationUnit cu = retrieveCompilationUnit(SOURCE_BASE_PATH + fileName);
            var methodCallVisitor = new MethodCallVisitor(cu);
            methodCallVisitor.visit(cu, null);
            writeOutputFile(SOURCE_BASE_PATH + fileName, cu.toString());
        }

    }
}
