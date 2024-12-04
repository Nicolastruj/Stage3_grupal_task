package software.cheeselooker.ports;

import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.model.Book;

public interface IndexerStore {
    void index(Book book) throws IndexerException;
}
