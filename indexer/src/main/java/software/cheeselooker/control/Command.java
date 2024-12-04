package software.cheeselooker.control;

import software.cheeselooker.exceptions.IndexerException;

public interface Command {
    void execute() throws IndexerException;
}
