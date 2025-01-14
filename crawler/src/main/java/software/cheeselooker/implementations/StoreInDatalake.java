package software.cheeselooker.implementations;

import software.cheeselooker.exceptions.CrawlerException;
import software.cheeselooker.ports.StoreInDatalakeInterface;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StoreInDatalake implements StoreInDatalakeInterface {

    private final Path metadataPath;

    public StoreInDatalake(String metadataFilePath) {
        this.metadataPath = Paths.get(metadataFilePath);
    }

    @Override
    public void saveBook(InputStream bookStream, String title, int bookId, String downloadDirectory) throws CrawlerException {
        String bookFileName = sanitizeFileName(title) + "_" + bookId + ".txt";
        Path filePath = Paths.get(downloadDirectory, bookFileName);

        saveBookInDatalake(bookStream, filePath);

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
    public void saveMetadata(int gutenbergId, String title, String author, String url) throws CrawlerException {
        String metadataEntry = gutenbergId + "," + title + "," + author + "," + url + "\n";
        try {
            Files.createDirectories(metadataPath.getParent());
        } catch (IOException e) {
            throw new CrawlerException("Failed to create metadata directory: " + e.getMessage(), e);
        }
        try (FileWriter writer = new FileWriter(metadataPath.toFile(), true)) {
            writer.write(metadataEntry);
        } catch (IOException e) {
            throw new CrawlerException("Failed to write metadata: " + e.getMessage(), e);
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[<>:\"/|?*]", "_");
    }

}