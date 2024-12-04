package software.cheeselooker.ports;

import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.model.Book;

import java.util.List;

public interface IndexerReader {
    List<Book> read(String path) throws IndexerException;

    String getPath() throws IndexerException;
}
