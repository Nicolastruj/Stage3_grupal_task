package software.cheeselooker.implementations;

import software.cheeselooker.ports.Output;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SearchServerOutput implements Output {

    @Override
    public void displayResults(List<Map<String, Object>> results, String input) {
        if (!results.isEmpty()) {
            System.out.println("\nResults for '" + input + "':");
            for (Map<String, Object> result : results) {
                print(result);
            }
        } else {
            System.out.println("\nSorry! No results found for that word.");
        }
    }

    private static void print(Map<String, Object> result) {
        System.out.println("Book Name: " + result.get("book_name"));
        System.out.println("Author: " + result.get("author_name"));
        System.out.println("URL: " + result.get("URL"));
        System.out.println("Total Occurrences: " + result.get("total_occurrences"));
        System.out.println("Paragraphs:");
        List<String> paragraphs = (List<String>) result.get("paragraphs");
        for (String paragraph : paragraphs) {
            System.out.println(paragraph + "\n\n");
        }
    }

    public static String getPlainTextResults(List<Map<String, Object>> results, String input) {
        StringBuilder sb = new StringBuilder();
        Set<String> wordsToHighlight = Arrays.stream(input.toLowerCase().split("\\s+"))
                .collect(Collectors.toSet());

        sb.append("<html><head><style>")
                .append("body { font-family: Arial, sans-serif; margin: 20px; padding: 20px; background-color: #f4f4f4; }")
                .append(".result-container { display: flex; flex-wrap: wrap; gap: 20px; }")
                .append(".result-card { display: flex; width: 100%; background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 20px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); }")
                .append(".book-info { flex: 1; padding-right: 20px; }")
                .append(".book-cover { width: 250px; height: 100%; max-height: 400px; object-fit: contain; }")
                .append(".result-card h3 { margin-top: 0; color: #333; font-size: 18px; }")
                .append(".result-card a { color: #0066cc; text-decoration: none; }")
                .append(".result-card a:hover { text-decoration: underline; }")
                .append(".highlight { background-color: yellow; font-weight: bold; }")
                .append(".paragraphs { max-height: 200px; overflow-y: auto; padding: 10px 0; border-top: 1px solid #ddd; }")
                .append(".paragraphs ul { list-style-type: disc; padding-left: 20px; }")
                .append(".paragraphs li { padding: 5px 0; }")
                .append("</style></head><body>");

        if (!results.isEmpty()) {
            sb.append("<h1>Search Results for '").append(input).append("'</h1>");
            sb.append("<div class='result-container'>");

            for (Map<String, Object> result : results) {
                String url = (String) result.get("URL");

                String bookId = extractBookIdFromUrl(url);

                sb.append("<div class='result-card'>")
                        .append("<div class='book-info'>")
                        .append("<h3>").append(highlightWords((String) result.get("book_name"), wordsToHighlight)).append("</h3>")
                        .append("<p><strong>Author:</strong> ").append(highlightWords((String) result.get("author_name"), wordsToHighlight)).append("</p>")
                        .append("<p><strong>Total Occurrences:</strong> ").append(result.get("total_occurrences")).append("</p>")
                        .append("<p><strong>URL:</strong> <a href='").append(result.get("URL")).append("' target='_blank'>")
                        .append(result.get("URL")).append("</a></p>")
                        .append("<div class='paragraphs'>");

                List<String> paragraphs = (List<String>) result.get("paragraphs");
                if (paragraphs != null && !paragraphs.isEmpty()) {
                    sb.append("<strong>Paragraphs:</strong><ul>");
                    for (String paragraph : paragraphs) {
                        sb.append("<li>").append(highlightWords(paragraph, wordsToHighlight)).append("</li>");
                    }
                    sb.append("</ul>");
                } else {
                    sb.append("<p>No paragraphs available</p>");
                }

                sb.append("</div></div>")
                        .append("<img class='book-cover' src='https://www.gutenberg.org/cache/epub/")
                        .append(bookId)
                        .append("/pg")
                        .append(bookId)
                        .append(".cover.medium.jpg' alt='Book Cover'>")
                        .append("</div>");
            }
            sb.append("</div>");
        } else {
            sb.append("<h2>No results found for '").append(input).append("'.</h2>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String highlightWords(String text, Set<String> wordsToHighlight) {
        if (text == null || wordsToHighlight == null || wordsToHighlight.isEmpty()) {
            return text;
        }

        text = StringEscapeUtils.escapeHtml4(text);

        for (String word : wordsToHighlight) {
            text = text.replaceAll("(?i)\\b" + word + "\\b",
                    "<span class='highlight'>" + word + "</span>");
        }
        return text;
    }

    // Extraer el book_id de la URL del libro
    private static String extractBookIdFromUrl(String url) {
        if (url != null && url.matches("https://www.gutenberg.org/files/\\d+/\\d+-0.txt")) {
            return url.split("/")[4];
        }
        return null;
    }
}
