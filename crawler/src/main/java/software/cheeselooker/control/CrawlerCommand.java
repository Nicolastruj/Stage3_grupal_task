package software.cheeselooker.control;

import com.hazelcast.map.IMap;
import software.cheeselooker.exceptions.CrawlerException;
import software.cheeselooker.ports.ReaderFromWebInterface;
import software.cheeselooker.ports.StoreInDatalakeInterface;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CrawlerCommand implements Command {
    private final String datalakePath;
    private final String metadataPath;
    private final ReaderFromWebInterface reader;
    private final StoreInDatalakeInterface store;
    private final IMap<String, String> bookMap;  // Aquí agregamos el campo bookMap

    // Modificar el constructor para incluir el parámetro IMap
    public CrawlerCommand(String datalakePath, String metadataPath, ReaderFromWebInterface reader,
                          StoreInDatalakeInterface store, IMap<String, String> bookMap) {
        this.datalakePath = datalakePath;
        this.metadataPath = metadataPath;
        this.reader = reader;
        this.store = store;
        this.bookMap = bookMap;
        loadBooksFromMetadata(); // Cargar metadatos al iniciar
    }

    @Override
    public void download(int numberOfBooks) {
        int lastId = obtainLastId(metadataPath);
        int successfulDownloads = 0;
        downloadLastBooks(successfulDownloads, lastId, numberOfBooks);

        System.out.println("Three books downloaded successfully.");
    }

    private void downloadLastBooks(int successfulDownloads, int lastId, int numberOfBooks) {
        while (successfulDownloads < numberOfBooks) {
            int nextId = lastId + 1;
            lastId += 1;

            try {
                String[] titleAndAuthor = reader.getTitleAndAuthor(nextId);

                if (titleAndAuthor != null) {
                    try (InputStream bookStream = reader.downloadBookStream(nextId)) {
                        if (bookStream != null) {
                            saveBook(bookStream, titleAndAuthor, nextId);
                            successfulDownloads++;
                            System.out.println("Successfully downloaded book ID " + nextId);
                        } else {
                            System.out.println("Book not found: " + nextId);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    System.out.println("Failed to retrieve title and author for book ID " + nextId);
                }
            } catch (CrawlerException e) {
                System.err.println("Error: " + e.getMessage());
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void saveBook(InputStream bookStream, String[] titleAndAuthor, int nextId) throws CrawlerException {
        if (bookMap.containsKey(String.valueOf(nextId))) {
            System.out.println("Book ID " + nextId + " is already downloaded.");
            return; // Evitar descarga duplicada
        }

        int customId = store.saveBook(bookStream, titleAndAuthor[0], datalakePath);
        store.saveMetadata(customId, nextId, titleAndAuthor[0], titleAndAuthor[1],
                "https://www.gutenberg.org/files/" + nextId + "/" + nextId + "-0.txt");

        bookMap.put(String.valueOf(nextId), titleAndAuthor[0]); // Compartir el libro descargado
        System.out.println("Book ID " + nextId + " saved and shared in Hazelcast.");
    }

    public static int obtainLastId(String metadataPath) {
        int lastGutenbergId = 0;
        Path metadataFile = Paths.get(metadataPath);

        if (!Files.exists(metadataFile) || Files.isDirectory(metadataFile)) {
            return lastGutenbergId;
        }

        lastGutenbergId = getLastGutenbergId(metadataFile, lastGutenbergId);

        return lastGutenbergId;
    }

    private static int getLastGutenbergId(Path metadataFile, int lastGutenbergId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile.toFile()))) {
            String lastLine = getLastLine(reader);

            if (lastLine != null) {
                String[] fields = lastLine.split(",");
                if (fields.length > 1) {
                    lastGutenbergId = Integer.parseInt(fields[1].trim());
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Failed to read last Gutenberg ID: " + e.getMessage());
        }
        return lastGutenbergId;
    }

    private static String getLastLine(BufferedReader reader) throws IOException {
        String line;
        String lastLine = null;

        while ((line = reader.readLine()) != null) {
            lastLine = line;
        }
        return lastLine;
    }
    private void loadBooksFromMetadata() {
        if (Files.exists(Paths.get(metadataPath))) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(metadataPath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");
                    if (fields.length > 2) {
                        String bookId = fields[1].trim();
                        String title = fields[2].trim();
                        bookMap.put(bookId, title); // Reconstruir el mapa compartido
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to load metadata: " + e.getMessage());
            }
        }
    }
}
