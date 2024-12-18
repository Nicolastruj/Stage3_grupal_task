package software.cheeselooker.control;

import software.cheeselooker.exceptions.QueryEngineException;

public interface Command {
    void execute() throws QueryEngineException;
}
