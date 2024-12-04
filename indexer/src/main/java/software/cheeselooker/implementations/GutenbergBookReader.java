package software.cheeselooker.implementations;

import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.model.Book;
import software.cheeselooker.ports.IndexerReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GutenbergBookReader implements IndexerReader {

    private final String path;
    private List<Book> books;

    public GutenbergBookReader(String path) {
        this.path = path;
    }

    @Override
    public List<Book> read(String trayPath) throws IndexerException {
        File folder = new File(trayPath);
        File[] files = folder.listFiles();
        return getBooksFrom(files);
    }

    private List<Book> getBooksFrom(File[] files) throws IndexerException {
        books = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (isTextFile(file)) {
                    Book book = createBookFromFile(file);
                    if (book != null) {
                        books.add(book);
                    }
                }
            }
        } else {
            System.out.println("The folder does not contain any files or could not be accessed.");
        }
        return books;
    }

    private boolean isTextFile(File file) {
        return file.isFile() && file.getName().endsWith(".txt");
    }

    private Book createBookFromFile(File file) throws IndexerException {
        String fileName = file.getName();
        String[] parts = fileName.split("_");
        if (parts.length == 2) {
            String bookId = parts[1].replace(".txt", "");
            String content = readFileContent(file);

            return new Book(bookId, content);
        }
        return null;
    }


    private String readFileContent(File file) throws IndexerException {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            throw new IndexerException(e.getMessage(), e);
        }
    }

    public List<Book> getBooks() {
        return books;
    }

    public String getPath() {
        return path;
    }
}
