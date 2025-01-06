package software.cheeselooker.apps;

import software.cheeselooker.control.Command;
import software.cheeselooker.control.SearchEngineCommand;
import software.cheeselooker.exceptions.QueryEngineException;
import software.cheeselooker.implementations.CommonQueryEngine;
import software.cheeselooker.implementations.SearchInput;
import software.cheeselooker.implementations.SearchOutput;
import software.cheeselooker.ports.Input;
import software.cheeselooker.ports.Output;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MainCommonQueryEngine {
    public static void main(String[] args) throws QueryEngineException {
        Path bookDatalakePath = Paths.get(System.getProperty("user.dir"), "/data/datalake");
        Path invertedIndexPath = Paths.get(System.getProperty("user.dir"), "/data/datamart");
        Path metadataPath = Paths.get(System.getProperty("user.dir"), "/data/metadata/metadata.csv");

        Input input = new SearchInput();
        Output output = new SearchOutput();
        CommonQueryEngine queryEngine = new CommonQueryEngine(
                metadataPath.toString(),
                bookDatalakePath.toString(),
                invertedIndexPath.toString()
        );
        Command searchEngineCommand = new SearchEngineCommand(input, output, queryEngine);

        try {
            searchEngineCommand.execute();
        } catch (QueryEngineException e) {
            throw new QueryEngineException(e.getMessage(), e);
        }
    }
}
