package software.cheeselooker.implementations;

import software.cheeselooker.exceptions.CrawlerException;
import software.cheeselooker.ports.StoreInDatalakeInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreInDatalake implements StoreInDatalakeInterface {

    private final Path metadataPath;
    private final AtomicInteger customIdCounter;

    public StoreInDatalake(String metadataFilePath) {
        this.metadataPath = Paths.get(metadataFilePath);
        this.customIdCounter = new AtomicInteger(loadLastCustomId() + 1);
    }

    @Override
    public int saveBook(InputStream bookStream, String title, String downloadDirectory) throws CrawlerException {
        int customId = customIdCounter.getAndIncrement();
        String bookFileName = sanitizeFileName(title) + "_" + customId + ".txt";
        Path filePath = Paths.get(downloadDirectory, bookFileName);

        saveBookInDatalake(bookStream, filePath);

        return customId;
    }

    private static void saveBookInDatalake(InputStream bookStream, Path filePath) throws CrawlerException {
        try {
            Files.createDirectories(filePath.getParent());
            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = bookStream.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            throw new CrawlerException("Failed to save book: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveMetadata(int customId, int gutenbergId, String title, String author, String url) throws CrawlerException {
        String metadataEntry = customId + "," + gutenbergId + "," + title + "," + author + "," + url + "\n";

        try (FileWriter writer = new FileWriter(metadataPath.toFile(), true)) {
            writer.write(metadataEntry);
        } catch (IOException e) {
            throw new CrawlerException("Failed to write metadata: " + e.getMessage(), e);
        }
    }

    private int loadLastCustomId() {
        int lastId = 0;

        if (Files.exists(metadataPath)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(metadataPath.toFile()))) {
                String lastLine = getLastLine(reader);

                if (lastLine != null) {
                    String[] fields = lastLine.split(",");
                    lastId = Integer.parseInt(fields[0].trim());
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Failed to read last custom ID: " + e.getMessage());
            }
        }
        return lastId;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[<>:\"/|?*]", "_");
    }

    private static String getLastLine(BufferedReader reader) throws IOException {
        String lastLine = null;
        String line;
        while ((line = reader.readLine()) != null) {
            lastLine = line;
        }
        return lastLine;
    }
}
