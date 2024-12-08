package software.cheeselooker.model;

import software.cheeselooker.exceptions.QueryEngineException;

import java.util.List;
import java.util.Map;

public interface QueryEngine {
    List<Map<String, Object>> query(String[] words) throws QueryEngineException;
}
