package software.cheeselooker.ports;

import java.util.List;
import java.util.Map;

public interface Output {
    void displayResults(List<Map<String, Object>> results, String input);
}
