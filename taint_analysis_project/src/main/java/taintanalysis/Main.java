package taintanalysis;

import taintanalysis.error.ErrorCode;
import taintanalysis.service.Analyzer;
import taintanalysis.utils.FileUtils;
import java.util.List;

import static taintanalysis.error.ErrorCode.generateErrorException;
import static taintanalysis.utils.FileUtils.SOURCE_BASE_PATH;

public class Main {

    public static void main(String[] args) throws Exception {

        List<String> sourcesList = FileUtils.getSourcesList();

        if (sourcesList.isEmpty()) {
            throw generateErrorException(ErrorCode.JAVA_FILE_NOT_FOUND);
        }

        // TODO : Aggiungere log tramite slf4j e configurazione di logging patter (vedi houseofpizza -> "# logging level")
        System.out.println("sourcesList: " + sourcesList);

        //Analyzer analyzer = new Analyzer(loader, args);
        Analyzer analyzer = new Analyzer(args);

        // Analisi del file sorgente specificato
        for (String fileName : sourcesList) {
            analyzer.analyze(SOURCE_BASE_PATH + fileName);
        }

    }
}
