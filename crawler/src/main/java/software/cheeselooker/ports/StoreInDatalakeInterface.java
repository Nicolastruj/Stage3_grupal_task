package software.cheeselooker.ports;

import software.cheeselooker.exceptions.CrawlerException;

import java.io.InputStream;

public interface StoreInDatalakeInterface {
    void saveBook(InputStream bookStream, String title, int bookId, String downloadDirectory) throws CrawlerException;

    void saveMetadata(int gutenbergId, String title, String author, String url) throws CrawlerException;
}

