package software.cheeselooker.apps;

import software.cheeselooker.implementations.CommonQueryEngine;
import software.cheeselooker.implementations.SearchServerOutput;
import spark.Spark;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SearchServer {

    public static void main(String[] args) {
        Path bookDatalakePath = Paths.get(System.getProperty("user.dir"), "/data/datalake");
        Path invertedIndexPath = Paths.get(System.getProperty("user.dir"), "/data/datamart");
        Path metadataPath = Paths.get(System.getProperty("user.dir"), "/data/metadata/metadata.csv");

        CommonQueryEngine queryEngine = new CommonQueryEngine(
                metadataPath.toString(),
                bookDatalakePath.toString(),
                invertedIndexPath.toString()
        );

        Spark.port(8080);

        Spark.get("/documents/search", (req, res) -> {
            String query = req.queryParams("words");

            if (query == null || query.trim().isEmpty()) {
                res.status(400);
                return "Parameter 'words' required.";
            }

            System.out.println("Received query: " + query);

            String[] words = query.toLowerCase().split("\\s+");
            System.out.println("Processed words: " + Arrays.toString(words));

            try {
                List<Map<String, Object>> wordResults = queryEngine.query(words);

                String plainTextResults = SearchServerOutput.getPlainTextResults(wordResults, query);

                res.type("text/html");
                return plainTextResults;
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "Error processing the query: " + e.getMessage();
            }
        });
    }
}
