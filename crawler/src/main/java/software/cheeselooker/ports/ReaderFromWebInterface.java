package software.cheeselooker.ports;


import software.cheeselooker.exceptions.CrawlerException;
import java.io.InputStream;

public interface ReaderFromWebInterface {

    /**
     * Downloads the book content as an InputStream given the book ID.
     *
     * @param bookId The ID of the book to download.
     * @return InputStream of the book content, or null if the book is not found (404).
     * @throws CrawlerException if there is an error other than 404.
     */
    InputStream downloadBookStream(int bookId) throws CrawlerException;

    /**
     * Retrieves the title and author of a book given the book ID.
     *
     * @param bookId The ID of the book to retrieve information for.
     * @return A String array containing title and author, or throws CrawlerException if not found.
     * @throws CrawlerException if there is an error retrieving title and author.
     */
    String[] getTitleAndAuthor(int bookId) throws CrawlerException;
}

