package com.slamur.plagiarism.service.impl.comparison;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.slamur.plagiarism.model.IdsPair;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.utils.IOUtils;

import static com.slamur.plagiarism.utils.CsvUtils.SEPARATOR;

public class ComparisonRepository {

    private static final String COMPARISONS_FILE_NAME_FORMAT = "%s_%.2f.txt";

    private final File comparisonsDirectory;

    public ComparisonRepository(File comparisonsDirectory) {
        this.comparisonsDirectory = comparisonsDirectory;
    }

    public Map<IdsPair, Double> loadSimilarities(double similarityFilter,
                                                 String problemName) throws IOException {
        File comparisonsFile = new File(
                comparisonsDirectory,
                String.format(COMPARISONS_FILE_NAME_FORMAT, problemName, similarityFilter)
        );

        if (!comparisonsFile.exists()) return Collections.emptyMap();

        return IOUtils.loadFromFile(comparisonsFile, (in) -> {
            Map<IdsPair, Double> similarities = new HashMap<>();

            in.lines().forEach(line -> {
                StringTokenizer tok = new StringTokenizer(line, SEPARATOR);

                String leftId = tok.nextToken();
                String rightId = tok.nextToken();
                double similarity = Double.parseDouble(tok.nextToken());

                similarities.put(new IdsPair(leftId, rightId), similarity);
            });

            return similarities;
        });
    }

    public void saveSimilarities(Map<Comparison, Double> similarities,
                                 double similarityFilter) throws IOException {

        Map<String, Map<IdsPair, Double>> problemToSimilarities = new HashMap<>();
        similarities.forEach((comparison, similarity) -> {
            var problemName = comparison.getProblemName();

            var problemSimilarities = problemToSimilarities.computeIfAbsent(
                    problemName, (name) -> new HashMap<>()
            );

            problemSimilarities.put(comparison.toIds(), similarity);
        });

        for (var e : problemToSimilarities.entrySet()) {
            String problemName = e.getKey();
            var problemSimilarities = e.getValue();
            saveSimilarities(problemSimilarities, similarityFilter, problemName);
        }
    }

    public void saveSimilarities(Map<IdsPair, Double> similarities,
                                 double similarityFilter,
                                 String problemName) throws IOException {
        File comparisonsFile = new File(
                comparisonsDirectory,
                String.format(COMPARISONS_FILE_NAME_FORMAT, problemName, similarityFilter)
        );

        try (PrintWriter out = new PrintWriter(
                new BufferedWriter(new FileWriter(comparisonsFile, true)))
        ) {
            similarities.forEach(
                    (ids, similarity) -> out.println(
                            ids.getLeftId() + SEPARATOR
                            + ids.getRightId() + SEPARATOR
                            + similarity
                    )
            );
        }
    }
}
