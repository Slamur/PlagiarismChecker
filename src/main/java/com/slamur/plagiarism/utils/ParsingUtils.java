package com.slamur.plagiarism.utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ParsingUtils {

    public static String parseField(Document document, String fieldName) {
        String nodeText = document.getElementsByTag("p")
                .stream()
                .filter(element -> element.text().contains(fieldName))
                .findFirst()
                .map(Element::text)
                .orElseThrow();

        return parseField(nodeText);
    }

    public static String parseField(String text) {
        int startQuotesIndex = text.indexOf(":");
        int endQuotesIndex = text.length();

        return text.substring(startQuotesIndex + 1, endQuotesIndex).trim();
    }
}
