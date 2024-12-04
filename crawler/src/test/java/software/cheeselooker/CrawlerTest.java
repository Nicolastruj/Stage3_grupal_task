package software.cheeselooker;

import org.junit.jupiter.api.Test;
import software.cheeselooker.control.Command;
import software.cheeselooker.control.CrawlerCommand;
import software.cheeselooker.implementations.ReaderFromWeb;
import software.cheeselooker.implementations.StoreInDatalake;
import software.cheeselooker.ports.ReaderFromWebInterface;
import software.cheeselooker.ports.StoreInDatalakeInterface;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class CrawlerTest {
    //TODO check if ids are correct.

    @Test
    void downloadOneBook() throws IOException {
        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        Path datalakePath = projectRoot.resolve("data/datalake").normalize();
        Path metadataPath = projectRoot.resolve("data/metadata/metadata.csv").normalize();

        EnvironmentStats stats = prepareTestEnvironment(datalakePath, metadataPath);

        ReaderFromWebInterface reader = new ReaderFromWeb();
        StoreInDatalakeInterface store = new StoreInDatalake(metadataPath.toString());
        Command crawlerCommand = new CrawlerCommand(datalakePath.toString(), metadataPath.toString(), reader, store);

        crawlerCommand.download(1);

        File[] files = datalakePath.toFile().listFiles();
        assertNotNull(files, "No files found in the datalake.");
        assertEquals(stats.existingBookCount + 1, files.length, "Expected one additional file in the datalake.");

        List<String> metadata = Files.readAllLines(metadataPath);
        assertEquals(stats.existingMetadataCount + 1, metadata.size(), "Metadata should contain one new entry.");
    }

    @Test
    void downloadMoreThanOneBook() throws IOException {
        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        Path datalakePath = projectRoot.resolve("data/datalake").normalize();
        Path metadataPath = projectRoot.resolve("data/metadata/metadata.csv").normalize();

        EnvironmentStats stats = prepareTestEnvironment(datalakePath, metadataPath);

        ReaderFromWebInterface reader = new ReaderFromWeb();
        StoreInDatalakeInterface store = new StoreInDatalake(metadataPath.toString());
        Command crawlerCommand = new CrawlerCommand(datalakePath.toString(), metadataPath.toString(), reader, store);

        crawlerCommand.download(3);

        File[] files = datalakePath.toFile().listFiles();
        assertNotNull(files, "No files found in the datalake.");
        assertEquals(stats.existingBookCount + 3, files.length, "Expected three additional files in the datalake.");

        List<String> metadata = Files.readAllLines(metadataPath);
        assertEquals(stats.existingMetadataCount + 3, metadata.size(), "Metadata should contain three new entries.");
    }

    private EnvironmentStats prepareTestEnvironment(Path datalakePath, Path metadataPath) throws IOException {
        int existingMetadataCount = 0;
        if (Files.exists(metadataPath)) {
            List<String> existingMetadata = Files.readAllLines(metadataPath);
            existingMetadataCount = existingMetadata.size(); // Exclude header
        } else {
            Files.createDirectories(metadataPath.getParent());
        }

        int existingBookCount = 0;
        if (Files.exists(datalakePath)) {
            for (File file : Objects.requireNonNull(datalakePath.toFile().listFiles())) {
                existingBookCount++;
            }
        } else {
            Files.createDirectories(datalakePath);
        }

        return new EnvironmentStats(existingMetadataCount, existingBookCount);
    }

    private static class EnvironmentStats {
        int existingMetadataCount;
        int existingBookCount;

        EnvironmentStats(int existingMetadataCount, int existingBookCount) {
            this.existingMetadataCount = existingMetadataCount;
            this.existingBookCount = existingBookCount;
        }
    }
}
