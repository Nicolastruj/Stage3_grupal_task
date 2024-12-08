package software.cheeselooker.control;

import software.cheeselooker.exceptions.QueryEngineException;
import software.cheeselooker.model.QueryEngine;
import software.cheeselooker.ports.Input;
import software.cheeselooker.ports.Output;

import java.util.List;
import java.util.Map;


public class SearchEngineCommand implements Command {
    private final Input input;
    private final Output outputInterface;
    private final QueryEngine queryEngine;

    public SearchEngineCommand(Input inputInterface, Output outputInterface, QueryEngine queryEngine) {
        this.input = inputInterface;
        this.outputInterface = outputInterface;
        this.queryEngine = queryEngine;
    }

    @Override
    public void execute() throws QueryEngineException {
        System.out.println("\nWelcome to the Search Engine!");
        System.out.println("If you want to exit the search engine, type 'EXIT'");

        processSearch();
    }

    private void processSearch() throws QueryEngineException {
        while (true) {
            String searchInput = input.getSearchText();
            String[] words = searchInput.split("\\s+");

            if (exitMode(words)) return;
            if (emptyInput(words)) continue;

            getResults(words, searchInput);
        }
    }

    private void getResults(String[] words, String searchInput) throws QueryEngineException {
        try {
            List<Map<String, Object>> wordResults = queryEngine.query(words);
            outputInterface.displayResults(wordResults, searchInput);

        } catch (Exception e) {
            System.err.println("An error occurred while processing the query: " + e.getMessage());
            throw new QueryEngineException(e.getMessage(), e);
        }
    }

    private static boolean emptyInput(String[] words) {
        if (words[0].trim().isEmpty()) {
            System.out.println("Empty query entered. Please try again.");
            return true;
        }
        return false;
    }

    private static boolean exitMode(String[] words) {
        if ("EXIT".equalsIgnoreCase(words[0])) {
            System.out.println("Exiting the search engine. Have a great day!");
            return true;
        }
        return false;
    }


}
