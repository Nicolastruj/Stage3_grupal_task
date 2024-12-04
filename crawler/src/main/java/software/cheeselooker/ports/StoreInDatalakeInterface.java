package software.cheeselooker.ports;

import software.cheeselooker.exceptions.CrawlerException;
import java.io.InputStream;

public interface StoreInDatalakeInterface {

    /**
     * Saves the book content to a file in the specified directory and returns a custom ID for the book.
     *
     * @param bookStream The InputStream of the book content.
     * @param title The title of the book.
     * @param downloadDirectory The directory where the book should be saved.
     * @return A unique custom ID assigned to the saved book.
     * @throws CrawlerException if there is an error saving the book.
     */
    int saveBook(InputStream bookStream, String title, String downloadDirectory) throws CrawlerException;

    /**
     * Saves metadata information about a book to the metadata file.
     *
     * @param customId The custom ID of the book.
     * @param gutenbergId The Gutenberg ID of the book.
     * @param title The title of the book.
     * @param author The author of the book.
     * @param url The URL for downloading the book.
     * @throws CrawlerException if there is an error writing to the metadata file.
     */
    void saveMetadata(int customId, int gutenbergId, String title, String author, String url) throws CrawlerException;
}

