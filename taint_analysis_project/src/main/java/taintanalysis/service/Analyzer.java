package taintanalysis.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.maven.shared.utils.StringUtils;
import taintanalysis.config.ConfigLoader;
import taintanalysis.error.ErrorException;

import java.nio.file.Paths;
import java.util.Arrays;

import static taintanalysis.utils.FileUtils.getFileInputStream;
import static taintanalysis.utils.FileUtils.writeOutputFile;

//@Slf4j
public class Analyzer {

    private final ConfigLoader configLoader;
    private final String[] commandLineArgs;

    public Analyzer(String[] args) {
        configLoader = ConfigLoader.getInstance();
        this.commandLineArgs = args;
    }

    private JavaParser parserSetting() {
        var combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(Paths.get("src/main/java")));

        var parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));

        return new JavaParser(parserConfiguration);
    }

    public void analyze(String sourceFilePath) {
        System.out.println("Path in arrivo ad Analyzer: " + sourceFilePath);

        JavaParser javaParser = parserSetting();
        CompilationUnit cu = javaParser.parse(getFileInputStream(sourceFilePath))
                .getResult()
                .orElseThrow();

        // Usa MethodCallVisitor con il resolver integrato
        var methodCallVisitor = new MethodCallVisitor(cu);
        methodCallVisitor.visit(cu, null);

        writeOutputFile(sourceFilePath, cu.toString());

        analyzeCommandLineArgs();
    }

    private boolean isValid(String arg) {
        return StringUtils.isNotBlank(arg) && arg.matches("[a-zA-Z0-9]+");
    }

    // TODO: Understanding how to deal with input text at the security level
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

}
