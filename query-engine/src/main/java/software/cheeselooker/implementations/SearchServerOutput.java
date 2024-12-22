package software.cheeselooker.implementations;

import software.cheeselooker.ports.Output;

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

        if (!results.isEmpty()) {
            sb.append("\nResults for '").append(input).append("':\n");
            for (Map<String, Object> result : results) {
                sb.append(formatResult(result, wordsToHighlight)).append("\n");
            }
        } else {
            sb.append("\nSorry! No results found for that word.");
        }
        return sb.toString();
    }

    private static String formatResult(Map<String, Object> result, Set<String> wordsToHighlight) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div>");
        sb.append("<strong>Book Name:</strong> ").append(highlightWords((String) result.get("book_name"), wordsToHighlight)).append("<br>");
        sb.append("<strong>Author:</strong> ").append(highlightWords((String) result.get("author_name"), wordsToHighlight)).append("<br>");
        sb.append("<strong>URL:</strong> <a href='").append(result.get("URL")).append("'>").append(result.get("URL")).append("</a><br>");
        sb.append("<strong>Total Occurrences:</strong> ").append(result.get("total_occurrences")).append("<br>");
        sb.append("<strong>Paragraphs:</strong><br>");

        List<String> paragraphs = (List<String>) result.get("paragraphs");
        for (String paragraph : paragraphs) {
            sb.append("<p>").append(highlightWords(paragraph, wordsToHighlight)).append("</p>");
        }

        sb.append("</div>");
        return sb.toString();
    }



    private static String highlightWords(String text, Set<String> wordsToHighlight) {
        if (text == null || wordsToHighlight == null || wordsToHighlight.isEmpty()) {
            return text;
        }

        for (String word : wordsToHighlight) {
            text = text.replaceAll("(?i)\\b" + word + "\\b", "<span style='color:blue; font-weight:bold;'>" + word + "</span>");
        }
        return text;
    }


}
