package com.slamur.plagiarism.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;

public class IOUtils {
    public static final String RUSSIAN_ENCODING = "cp1251";

    public static InputStreamReader createReader(InputStream in) throws UnsupportedEncodingException {
        return new InputStreamReader(in, RUSSIAN_ENCODING);
    }

    public static List<File> getFiles(File directory) {
        return Optional.ofNullable(directory.listFiles())
                .map(Arrays::asList)
                .orElse(emptyList());
    }

    public static void saveToFile(File file,
                                  Consumer<PrintWriter> writeStrategy)
            throws FileNotFoundException, UnsupportedEncodingException {
        saveToFile(file, writeStrategy, IOUtils.RUSSIAN_ENCODING);
    }

    public static void saveToFile(File file,
                                  Consumer<PrintWriter> writeStrategy,
                                  String encoding)
            throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter out = new PrintWriter(file, encoding)) {
            writeStrategy.accept(out);
        }
    }

    @FunctionalInterface
    public interface ReadStrategy<T> {

        T readFrom(BufferedReader in) throws IOException;
    }

    public static <T> T loadFromFile(File file, ReadStrategy<T> readStrategy)
            throws IOException {
        return readFrom(new FileInputStream(file), readStrategy);
    }

    public static <T> T readFrom(InputStream inputStream, ReadStrategy<T> readStrategy) throws IOException {
        return readFrom(createReader(inputStream), readStrategy);
    }

    public static <T> T readFrom(Reader reader, ReadStrategy<T> readStrategy) throws IOException {
        try (BufferedReader in = new BufferedReader(reader)) {
            return readStrategy.readFrom(in);
        }
    }
}
