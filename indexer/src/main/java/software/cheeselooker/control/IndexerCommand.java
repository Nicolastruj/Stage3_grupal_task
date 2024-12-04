package software.cheeselooker.control;

import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.model.Book;
import software.cheeselooker.ports.IndexerReader;
import software.cheeselooker.ports.IndexerStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class IndexerCommand implements Command{
    private final IndexerReader indexerReader;
    private final IndexerStore indexerStore;

    public IndexerCommand(IndexerReader indexerReader, IndexerStore indexerStore) {
        this.indexerReader = indexerReader;
        this.indexerStore = indexerStore;
    }

    @Override
    public void execute() throws IndexerException {
        indexLatestBooks();
        System.out.println("Indexation finished.");
    }

    private void indexLatestBooks() throws IndexerException {
        Path bookPath = Paths.get(indexerReader.getPath());
        Path tempTrayPath = Paths.get(System.getProperty("user.dir"), "tempTrayPath");
        moveLatestBooksToTempTray(tempTrayPath, bookPath);
        indexBooksFrom(tempTrayPath);
        deleteTempTray(tempTrayPath);
    }

    private void moveLatestBooksToTempTray(Path tempTray, Path bookPath) throws IndexerException {
        createIfNotExists(tempTray);
        copyLatestBooksToTempTray(bookPath, tempTray);
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

    private void copyLatestBooksToTempTray(Path bookPath, Path tempTray) throws IndexerException {
        try (Stream<Path> paths = Files.walk(bookPath)) {
            paths.filter(Files::isRegularFile)
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .limit(5)
                    .forEach(sourcePath -> {
                        Path destinationPath = tempTray.resolve(sourcePath.getFileName());
                        try {
                            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    });
        } catch (IOException e) {
            throw new IndexerException(e.getMessage(), e);
        }
    }

    private void indexBooksFrom(Path tempTrayPath) throws IndexerException {
        List<Book> books = indexerReader.read(String.valueOf(tempTrayPath));

        for (Book book : books) {
            indexerStore.index(book);
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
