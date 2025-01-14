package software.cheeselooker.apps;

import software.cheeselooker.implementations.CommonQueryEngine;
import software.cheeselooker.implementations.SearchServerOutput;
import spark.Spark;

import java.nio.file.Path;
import java.nio.file.Paths;
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
                return "<html><body><h2>Parameter 'words' required.</h2></body></html>";
            }

            String[] words = query.toLowerCase().split("\\s+");

            try {
                List<Map<String, Object>> wordResults = queryEngine.query(words);

                String htmlResults = SearchServerOutput.getPlainTextResults(wordResults, query);

                res.type("text/html");
                return htmlResults;
            } catch (Exception e) {
                res.status(500);
                return "<html><body><h2>Error processing the query: " + e.getMessage() + "</h2></body></html>";
            }
        });
    }
}
