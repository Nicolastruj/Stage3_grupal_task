package software.cheeselooker.api;

import org.springframework.web.bind.annotation.*;
import software.cheeselooker.exceptions.QueryEngineException;
import software.cheeselooker.control.SearchEngineCommand;
import software.cheeselooker.implementations.CommonQueryEngine;
import software.cheeselooker.implementations.SearchInput;
import software.cheeselooker.implementations.SearchOutput;
import software.cheeselooker.ports.Input;
import software.cheeselooker.ports.Output;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchEngineControllerApi {

    private final CommonQueryEngine queryEngine;

    public SearchEngineControllerApi() {
        Path bookDatalakePath = Paths.get(System.getProperty("user.dir"), "/data/datalake");
        Path invertedIndexPath = Paths.get(System.getProperty("user.dir"), "/data/datamart");
        Path metadataPath = Paths.get(System.getProperty("user.dir"), "/data/metadata/metadata.csv");

        Input input = new SearchInput();
        Output output = new SearchOutput();
        this.queryEngine = new CommonQueryEngine(
                metadataPath.toString(),
                bookDatalakePath.toString(),
                invertedIndexPath.toString()
        );
    }

    @PostMapping("/query")
    public Map<String, Object> search(@RequestBody SearchRequest request) throws QueryEngineException {
        // Dividir la consulta en palabras
        String[] words = request.getQuery().split("\\s+");

        // Ejecutar el comando de búsqueda
        SearchEngineCommand searchEngineCommand = new SearchEngineCommand(new SearchInput(), new SearchOutput(), queryEngine);
        searchEngineCommand.execute(); // Ejecutamos el comando (esto es lo que hará la búsqueda)

        // Obtener los resultados de la búsqueda
        List<Map<String, Object>> results = queryEngine.query(words);

        // Si no hay resultados, devolvemos un mensaje indicando que no se encontraron resultados
        if (results.isEmpty()) {
            return Map.of("message", "No results found for your query");
        }

        // Si hay resultados, los devolvemos
        return Map.of("results", results);
    }


    @GetMapping("/health")
    public String healthCheck() {
        return "Search Engine API is running";
    }
}
