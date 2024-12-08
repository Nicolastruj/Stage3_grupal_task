package software.cheeselooker.implementations;

import software.cheeselooker.exceptions.QueryEngineException;
import software.cheeselooker.model.Book;
import software.cheeselooker.model.QueryEngine;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonQueryEngine implements QueryEngine {
    private final String metadataPath;
    private final String bookFolder;
    private final String indexFolder;

    public CommonQueryEngine(String metadataPath, String bookFolder, String indexFolder) {
        this.metadataPath = metadataPath;
        this.bookFolder = bookFolder;
        this.indexFolder = indexFolder;
    }

    @Override
    public List<Map<String, Object>> query(String[] words) throws QueryEngineException {
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> commonBooks = null;

        for (String word : words) {
            Map<String, List<Integer>> wordOccurrences = getIndexedInfoOf(word);
            if (wordOccurrences == null || wordOccurrences.isEmpty()) {
                return Collections.emptyList();
            }

            commonBooks = getCommonBooks(commonBooks, wordOccurrences);
        }

        if (commonBooks == null || commonBooks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Book> metadataMap = loadMetadata(metadataPath);
        getResults(words, commonBooks, metadataMap, results);

        return results;
    }

    private Map<String, List<Integer>> getIndexedInfoOf(String word) throws QueryEngineException {
        word = word.trim();
        return loadIndexedWordInfo(word);
    }

    private Map<String, List<Integer>> loadIndexedWordInfo(String word) throws QueryEngineException {
        String wordFilePath = constructWordFilePath(word, indexFolder);
        File file = new File(wordFilePath);

        if (!file.exists()) {
            return null;
        }

        return getWordIndex(wordFilePath);
    }

    private String constructWordFilePath(String word, String indexFolder) {
        int depth = Math.min(word.length(), 3);
        StringBuilder pathBuilder = new StringBuilder(indexFolder);
        for (int i = 0; i < depth; i++) {
            pathBuilder.append("/").append(word.charAt(i));
        }
        pathBuilder.append("/").append(word).append(".csv");
        return pathBuilder.toString();
    }

    private static Map<String, List<Integer>> getWordIndex(String wordFilePath) throws QueryEngineException {
        Map<String, List<Integer>> wordIndex = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(wordFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                getWordInfo(line, wordIndex);
            }
        } catch (IOException e) {
            throw new QueryEngineException("Error reading index file", e);
        }
        return wordIndex;
    }

    private static void getWordInfo(String line, Map<String, List<Integer>> wordIndex) {
        String[] parts = line.split(",");
        if (parts.length < 2) return;
        String bookId = parts[0];
        List<Integer> positions = getWordPositions(parts);
        wordIndex.put(bookId, positions);
    }

    private static List<Integer> getWordPositions(String[] parts) {
        List<Integer> positions = new ArrayList<>();
        for (String pos : parts[1].split(";")) {
            positions.add(Integer.parseInt(pos));
        }
        return positions;
    }

    private static Set<String> getCommonBooks(Set<String> commonBooks, Map<String, List<Integer>> wordOccurrences) {
        if (commonBooks == null) {
            commonBooks = new HashSet<>(wordOccurrences.keySet());
        } else {
            commonBooks.retainAll(wordOccurrences.keySet());
        }
        return commonBooks;
    }

    public Map<String, Book> loadMetadata(String metadataPath) throws QueryEngineException {
        try (BufferedReader reader = new BufferedReader(new FileReader(metadataPath))) {
            return readMetadata(reader);
        } catch (FileNotFoundException e) {
            throw new QueryEngineException(e.getMessage(), e);
        } catch (IOException e) {
            throw new QueryEngineException("Error reading metadata file", e);
        }
    }

    private Map<String, Book> readMetadata(BufferedReader reader) throws IOException {
        Map<String, Book> metadata = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 4) {
                metadata.put(parts[0], new Book(parts[0], parts[2], parts[3], parts[4]));
            }
        }
        return metadata;
    }

    private void getResults(String[] words, Set<String> commonBooks, Map<String, Book> metadataMap, List<Map<String, Object>> results) throws QueryEngineException {
        for (String bookId : commonBooks) {
            Book metadata = metadataMap.get(bookId);
            if (metadataNotExists(bookId, metadata)) continue;

            String bookPath = String.format("%s/%s_%s.txt", bookFolder, metadata.name(), bookId);
            Map<String, Object> extractedData = new ParagraphExtractor().findParagraphs(bookPath, words);
            List<String> paragraphs = (List<String>) extractedData.get("paragraphs");
            int occurrences = (int) extractedData.get("occurrences");
            addResultsInfo(paragraphs, metadata, occurrences, results);
        }
    }

    private static boolean metadataNotExists(String bookId, Book metadata) {
        if (metadata == null) {
            System.out.println("Metadata for book ID '" + bookId + "' not found.");
            return true;
        }
        return false;
    }

    private static void addResultsInfo(List<String> paragraphs, Book metadata, int occurrences, List<Map<String, Object>> results) {
        if (!paragraphs.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("book_id", metadata.id());
            result.put("book_name", metadata.name());
            result.put("author_name", metadata.author());
            result.put("URL", metadata.url());
            result.put("paragraphs", paragraphs);
            result.put("total_occurrences", occurrences);
            results.add(result);
        }
    }

    public static class ParagraphExtractor {


        public Map<String, Object> findParagraphs(String bookPath, String[] searchWords) throws QueryEngineException {
            Map<String, Object> result = new HashMap<>();
            List<String> relevantParagraphs = new ArrayList<>();
            int totalOccurrences = 0;

            Map<String, Pattern> wordPatterns = createWordPatterns(searchWords);

            try (BufferedReader reader = new BufferedReader(new FileReader(bookPath))) {
                String[] paragraphs = splitTextIntoParagraphs(readFileContent(reader));

                for (String paragraph : paragraphs) {
                    totalOccurrences += processParagraph(paragraph, wordPatterns, relevantParagraphs);
                }
            } catch (FileNotFoundException e) {
                throw new QueryEngineException("Error: Book file not found: " + bookPath, e);
            } catch (IOException e) {
                throw new QueryEngineException("Error reading book file", e);
            }

            result.put("paragraphs", relevantParagraphs);
            result.put("occurrences", totalOccurrences);
            return result;
        }

        private Map<String, Pattern> createWordPatterns(String[] searchWords) {
            Map<String, Pattern> wordPatterns = new HashMap<>();
            for (String word : searchWords) {
                wordPatterns.put(word, Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE));
            }
            return wordPatterns;
        }

        private String[] splitTextIntoParagraphs(String text) {
            return text.split("\\n\\n");
        }

        private String readFileContent(BufferedReader reader) throws IOException {
            StringBuilder textBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                textBuilder.append(line).append("\n");
            }
            return textBuilder.toString();
        }

        private int processParagraph(String paragraph, Map<String, Pattern> wordPatterns, List<String> relevantParagraphs) {
            boolean paragraphAdded = false;
            int occurrencesInParagraph = 0;

            for (Pattern pattern : wordPatterns.values()) {
                Matcher matcher = pattern.matcher(paragraph);
                if (matcher.find()) {
                    occurrencesInParagraph += countAndHighlightOccurrences(matcher, relevantParagraphs, paragraphAdded);
                    paragraphAdded = true;
                }
            }
            return occurrencesInParagraph;
        }

        private int countAndHighlightOccurrences(Matcher matcher, List<String> relevantParagraphs, boolean paragraphAdded) {
            int occurrences = 0;
            StringBuilder highlightedBuffer = new StringBuilder();

            do {
                occurrences++;
                highlightMatch(matcher, highlightedBuffer);
            } while (matcher.find());

            matcher.appendTail(highlightedBuffer);
            if (!paragraphAdded) {
                relevantParagraphs.add(highlightedBuffer.toString().trim());
            }
            return occurrences;
        }

        private void highlightMatch(Matcher matcher, StringBuilder highlightedBuffer) {
            matcher.appendReplacement(highlightedBuffer, "\033[34m" + matcher.group() + "\033[0m");
        }


    }

}
