package software.cheeselooker.implementations;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import software.cheeselooker.exceptions.CrawlerException;
import software.cheeselooker.ports.ReaderFromWebInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ReaderFromWeb implements ReaderFromWebInterface {

    @Override
    public InputStream downloadBookStream(int bookId) throws CrawlerException {
        String bookUrl = "https://www.gutenberg.org/files/" + bookId + "/" + bookId + "-0.txt";
        try {
            URL url = new URL(bookUrl);
            HttpURLConnection connection = getHttpURLConnection(url);

            int responseCode = connection.getResponseCode();
            return getResultFromHttpURLConnection(responseCode, connection);
        } catch (IOException e) {
            throw new CrawlerException("Error connecting to URL: " + bookUrl, e);
        }
    }

    private static InputStream getResultFromHttpURLConnection(int responseCode, HttpURLConnection connection)
            throws IOException, CrawlerException {
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return null;
        } else {
            throw new CrawlerException("HTTP error code: " + responseCode);
        }
    }

    private static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        return connection;
    }

    @Override
    public String[] getTitleAndAuthor(int bookId) throws CrawlerException {
        String bookUrl = "https://www.gutenberg.org/ebooks/" + bookId;
        try {
            Document doc = Jsoup.connect(bookUrl).get();
            Element h1Element = doc.selectFirst("h1");

            return getBookMetadataFrom(h1Element);
        } catch (IOException e) {
            throw new CrawlerException("Error retrieving title and author: " + e.getMessage(), e);
        }
    }

    private static String[] getBookMetadataFrom(Element h1Element) throws CrawlerException {
        if (h1Element != null) {
            String titleAndAuthor = h1Element.text();
            String[] parts = titleAndAuthor.split(" by ", 2);
            String title = getBookAuthor(parts[0]);
            String author = getBookAuthor((parts.length > 1 ? parts[1] : "Unknown Author"));

            return new String[]{title, author};
        } else {
            throw new CrawlerException("Title and author not found.");
        }
    }

    private static String getBookAuthor(String author) {
        return author
                .replaceAll("[/:*?\"<>|]", "")
                .replace(",", ";");
    }
}



