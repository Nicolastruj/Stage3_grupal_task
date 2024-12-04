package software.cheeselooker.implementations;

import software.cheeselooker.exceptions.IndexerException;
import software.cheeselooker.model.Book;
import software.cheeselooker.ports.IndexerStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ExpandedHierarchicalCsvStore implements IndexerStore {

    private static Path invertedIndexPath;
    private static final int maxDepth = 3;
    private static final Set<String> stopWords = new HashSet<>();

    public ExpandedHierarchicalCsvStore(Path invertedIndexPath, Path stopWordsFilePath) {
        ExpandedHierarchicalCsvStore.invertedIndexPath = invertedIndexPath;
        loadStopWords(stopWordsFilePath);
    }

    @Override
    public void index(Book book) throws IndexerException {
        String content = book.content();
        String bookId = book.bookId();
        String[] words = content.split("\\W+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (!word.isEmpty() && !stopWords.contains(word)) {
                indexWord(bookId, i, word);
            }
        }
    }

    private static void loadStopWords(Path stopWordsFilePath) {
        try (BufferedReader reader = Files.newBufferedReader(stopWordsFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading stop words from file: " + stopWordsFilePath, e);
        }
    }

    public static void indexWord(String bookId, int position, String word) throws IndexerException {
        Path currentPath = invertedIndexPath;

        try {
            currentPath = getHierarchicalDirectoryPath(word, currentPath);
            Path filePath = getWordFilePath(word, currentPath);
            writeWordInfoToFile(bookId, position, filePath);

        } catch (IOException e) {
            throw new IndexerException(e.getMessage(), e);
        }
    }

    private static Path getHierarchicalDirectoryPath(String word, Path currentPath) throws IndexerException {
        int depth = Math.min(maxDepth, word.length());
        for (int i = 0; i < depth; i++) {
            String letter = String.valueOf(word.charAt(i));
            currentPath = currentPath.resolve(letter);
            createDirectoryIfNotExists(currentPath);
        }
        return currentPath;
    }

    private static void createDirectoryIfNotExists(Path currentPath) throws IndexerException {
        if (!Files.exists(currentPath)) {
            try {
                Files.createDirectories(currentPath);
            } catch (IOException e) {
                throw new IndexerException(e.getMessage(), e);
            }
        }
    }

    private static Path getWordFilePath(String word, Path currentPath) {
        return currentPath.resolve(word + ".csv");
    }

    private static void writeWordInfoToFile(String bookId, int position, Path filePath) throws IOException {
        String csvEntry = bookId + "," + position + "\n";
        Files.write(filePath, csvEntry.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }
}
