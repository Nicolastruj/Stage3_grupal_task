package software.cheeselooker.control;

import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.model.Book;
import software.cheeselooker.ports.IndexerReader;
import software.cheeselooker.ports.IndexerStore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class IndexerCommand implements Command {
    private final String indexedBooksFile;
    private final IndexerReader indexerReader;
    private final IndexerStore indexerStore;

    public IndexerCommand(IndexerReader indexerReader, IndexerStore indexerStore, String indexedBooksFile) {
        this.indexerReader = indexerReader;
        this.indexerStore = indexerStore;
        this.indexedBooksFile = indexedBooksFile;
    }

    @Override
    public void execute() throws IndexerException {
        indexLatestBooks();
        System.out.println("Indexation finished.");
    }

    private void indexLatestBooks() throws IndexerException {
        Path bookPath = Paths.get(indexerReader.getPath());
        Path tempTrayPath = Paths.get(System.getProperty("user.dir"), "tempTrayPath");
        Optional<String> lastIndexedId = getLastIndexedBookId();

        moveNewBooksToTempTray(tempTrayPath, bookPath, lastIndexedId);
        List<Book> booksToIndex = indexBooksFrom(tempTrayPath);

        if (!booksToIndex.isEmpty()) {
            String maxBookId = booksToIndex.stream()
                    .map(Book::bookId)
                    .max(String::compareTo)
                    .orElse("");
            saveLastIndexedBookId(maxBookId);
        }

        deleteTempTray(tempTrayPath);
    }

    private Optional<String> getLastIndexedBookId() {
        Path filePath = Paths.get(indexedBooksFile);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            return Files.lines(filePath).findFirst();
        } catch (IOException e) {
            System.err.println("Error reading indexed books file: " + e.getMessage());
            return Optional.empty();
        }
    }

    private void moveNewBooksToTempTray(Path tempTray, Path bookPath, Optional<String> lastIndexedId) throws IndexerException {
        createIfNotExists(tempTray);
        copyNewBooksToTempTray(bookPath, tempTray, lastIndexedId);
    }

    private static void createIfNotExists(Path tempTray) throws IndexerException {
        if (!Files.exists(tempTray)) {
            try {
                Files.createDirectories(tempTray);
            } catch (IOException e) {
                throw new IndexerException(e.getMessage(), e);
            }
        }
    }

    private void copyNewBooksToTempTray(Path bookPath, Path tempTray, Optional<String> lastIndexedId) throws IndexerException {
        try (Stream<Path> paths = Files.walk(bookPath)) {
            List<Path> newBooks = paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        if (lastIndexedId.isEmpty()) return true;

                        String bookId = extractBookId(path);

                        try {
                            int bookIdInt = Integer.parseInt(bookId);
                            int lastIndexedIdInt = Integer.parseInt(lastIndexedId.get());
                            return bookIdInt > lastIndexedIdInt;
                        } catch (NumberFormatException e) {
                            return bookId.compareTo(lastIndexedId.get()) > 0;
                        }
                    })
                    .sorted(Comparator.comparing(this::extractBookId))
                    .toList();


            for (Path sourcePath : newBooks) {
                Path destinationPath = tempTray.resolve(sourcePath.getFileName());
                System.out.println("copying " + destinationPath);
                try {
                    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new IndexerException(e.getMessage(), e);
        }
    }

    private String extractBookId(Path path) {
        String fileName = path.getFileName().toString();
        String[] parts = fileName.split("_");
        if (parts.length == 2) {
            return parts[1].replace(".txt", "");
        }
        return fileName;
    }

    private List<Book> indexBooksFrom(Path tempTrayPath) throws IndexerException {
        List<Book> books = indexerReader.read(String.valueOf(tempTrayPath));
        List<Book> indexedBooks = new ArrayList<>();

        for (Book book : books) {
            int tiempo1 = (int) System.currentTimeMillis();
            System.out.println("Indexando libro " + book.bookId());
            indexerStore.index(book);
            indexedBooks.add(book);
            int tiempo2 = (int) (System.currentTimeMillis() - tiempo1);
            System.out.println("libro" + book.bookId() + "indexado en un tiempo de " + tiempo2);
        }

        return indexedBooks;
    }

    private void saveLastIndexedBookId(String bookId) throws IndexerException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(indexedBooksFile))) {
            writer.write(bookId);
        } catch (IOException e) {
            throw new IndexerException("Failed to save last indexed book ID", e);
        }
    }

    private static void deleteTempTray(Path tempTrayPath) throws IndexerException {
        try {
            deleteFilesFrom(tempTrayPath);
            Files.deleteIfExists(tempTrayPath);
        } catch (IOException e) {
            throw new IndexerException("Failed to delete the temp tray", e);
        }
    }

    private static void deleteFilesFrom(Path tempTrayPath) throws IndexerException {
        if (Files.isDirectory(tempTrayPath)) {
            try (Stream<Path> files = Files.walk(tempTrayPath)) {
                files.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Error deleting file: " + path);
                            }
                        });
            } catch (IOException e) {
                throw new IndexerException("Failed to walk the directory to delete files", e);
            }
        }
    }
}
