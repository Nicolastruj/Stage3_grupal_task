package software.cheeselooker.ports;


import software.cheeselooker.exceptions.CrawlerException;

import java.io.InputStream;

public interface ReaderFromWebInterface {
    InputStream downloadBookStream(int bookId) throws CrawlerException;

    String[] getTitleAndAuthor(int bookId) throws CrawlerException;
}

