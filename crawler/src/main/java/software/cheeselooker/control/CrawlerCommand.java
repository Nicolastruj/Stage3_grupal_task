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

            // Intentamos obtener un lock para el libro
            String bookKey = String.valueOf(nextId);
            bookMap.lock(bookKey);  // Adquirimos el lock

            try {
                // Verificamos si el libro ya ha sido descargado
                if (bookMap.containsKey(bookKey)) {
                    System.out.println("Book ID " + nextId + " has already been downloaded. Skipping.");
                    continue; // Si ya está descargado, pasamos al siguiente
                }

                String[] titleAndAuthor = reader.getTitleAndAuthor(nextId);

                if (titleAndAuthor != null) {
                    try (InputStream bookStream = reader.downloadBookStream(nextId)) {
                        if (bookStream != null) {
                            saveBook(bookStream, titleAndAuthor, nextId);
                            successfulDownloads++;
                            // Añadimos el libro al map para marcarlo como descargado
                            bookMap.put(bookKey, titleAndAuthor[0]);
                            System.out.println("Successfully downloaded book ID " + nextId);
                        } else {
                            System.out.println("Book not found: " + nextId);
                        }
                    } catch (IOException e) {
                        System.err.println("Error downloading book ID " + nextId + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("Failed to retrieve title and author for book ID " + nextId);
                }
            } catch (CrawlerException e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                // Liberamos el lock para que otras instancias puedan acceder al libro
                bookMap.unlock(bookKey);
            }

            try {
                Thread.sleep(1000); // Pausar entre descargas
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void saveBook(InputStream bookStream, String[] titleAndAuthor, int nextId) throws CrawlerException {
        int customId = store.saveBook(bookStream, titleAndAuthor[0], datalakePath);
        store.saveMetadata(customId, nextId, titleAndAuthor[0], titleAndAuthor[1],
                "https://www.gutenberg.org/files/" + nextId + "/" + nextId + "-0.txt");

        // Aquí puedes realizar la lógica de agregar el libro al IMap si es necesario
        bookMap.put(String.valueOf(nextId), titleAndAuthor[0]);  // Este es solo un ejemplo, ajusta según tu lógica
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
}
