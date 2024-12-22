package software.cheeselooker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SearchEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineApplication.class, args);
    }
}

//Para la salud de la API (GET): http://localhost:8080/api/search/parallel
//Para la búsqueda (POST): http://localhost:8080/api/search/query