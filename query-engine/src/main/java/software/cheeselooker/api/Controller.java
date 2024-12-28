@RestController
@RequestMapping("/api")
public class Controller {

    private final CommonQueryEngine queryEngine;

    // Constructor que recibe el QueryEngine ya inicializado
    public Controller(CommonQueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }

    @GetMapping("/search")
    public ResponseEntity<String> searchWordsGet(@RequestParam String words) {
        try {
            // Usa el método search del motor
            String result = queryEngine.search(words);  // Asegúrate de que este método exista en CommonQueryEngine
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error en la búsqueda: " + e.getMessage());
        }
    }
}
