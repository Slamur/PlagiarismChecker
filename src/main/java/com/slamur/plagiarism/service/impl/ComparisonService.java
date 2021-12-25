package com.slamur.plagiarism.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.slamur.plagiarism.model.parsing.Contest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.Solution;
import com.slamur.plagiarism.model.parsing.Verdict;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.RandomUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ComparisonService extends ServiceBase {

    private static final EnumSet<Verdict> notScanningVerdicts = EnumSet.of(Verdict.WA, Verdict.CE);
    private static final boolean onlyEqualScoreScan = true;

    private static final double solutionSimilarityFilter = 0.9;

    private static final String comparisonsFileName = "comparisons";

    private final ObservableList<Comparison> comparisons;
    private final Map<Comparison, Double> similarityByComparison;

    private File comparisonsFile;

    public ComparisonService() {
        this.comparisons = FXCollections.observableArrayList();
        this.similarityByComparison = new HashMap<>();
    }

    @Override
    protected void initializeOnly() {
        var participants = Services.contest().getParticipants();

        Contest contest = Services.contest().getContest();

        try {
            this.comparisonsFile = new File(contest.createFolder(), comparisonsFileName + ".txt");
            if (!comparisonsFile.exists()) {
                if (!comparisonsFile.createNewFile()) {
                    throw new IOException("Неудачное создание файла");
                }
            }
        } catch (IOException e) {
            AlertUtils.warning("Проблема с кешированием сравнений", e);
        }

        loadComparisons(participants);
        generateComparisons(participants);
    }

    private void loadComparisons(List<Participant> participants) {
        try (BufferedReader in = new BufferedReader(new FileReader(comparisonsFile))){
            Map<String, Participant> participantsById = new HashMap<>();
            for (Participant participant : participants) {
                participantsById.put(participant.id, participant);
            }

            in.lines().forEach(line -> {
                StringTokenizer tok = new StringTokenizer(line, " -()");

                String leftId = tok.nextToken();
                String rightId = tok.nextToken();
                int problemId = tok.nextToken().charAt(0) - 'A';

                double similarity = Double.parseDouble(tok.nextToken());

                var left = participantsById.get(leftId);
                var right = participantsById.get(rightId);

                if (null != left && null != right) {
                    var comparison = new Comparison(
                            left, right, problemId
                    );

                    saveResult(comparison, similarity);
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveResult(Comparison comparison, double similarity) {
        similarityByComparison.put(comparison, similarity);
    }

    private void generateComparisons(List<Participant> participants) {
        List<Comparison> notCachedBefore = new ArrayList<>();

        int problemsCount = Services.contest().getProblemsCount();

        for (int leftIndex = 0; leftIndex < participants.size(); ++leftIndex) {
            for (int rightIndex = leftIndex + 1; rightIndex < participants.size(); ++rightIndex) {
                for (int problemIndex = 0; problemIndex < problemsCount; ++problemIndex) {
                    var comparison = compare(
                            participants.get(leftIndex),
                            participants.get(rightIndex),
                            problemIndex
                    );

                    if (null != comparison) {
                        notCachedBefore.add(comparison);
                    }
                }
            }
        }

        if (null != comparisonsFile) {
            try (PrintWriter out = new PrintWriter(
                    new BufferedWriter(new FileWriter(comparisonsFile, true)))
            ) {
                notCachedBefore.forEach(comparison -> out.println(comparison + " " + similarityByComparison.get(comparison)));
            } catch (IOException e) {
                AlertUtils.warning("Не смогли кешировать сравнения", e);
            }
        }
    }

    public Comparison compare(Participant left, Participant right, int problemIndex) {
        var leftSolution = left.problemToBestSolution[problemIndex];
        var rightSolution = right.problemToBestSolution[problemIndex];

        if (null == leftSolution || null == rightSolution) {
            return null;
        }

        if (notScanningVerdicts.contains(leftSolution.verdict)
                || notScanningVerdicts.contains(rightSolution.verdict)) {
            return null;
        }

        if (onlyEqualScoreScan) {
            if (leftSolution.score != rightSolution.score) {
                return null;
            }
        }

        var comparison = new Comparison(left, right, problemIndex);
        comparisons.add(comparison);

        if (!similarityByComparison.containsKey(comparison)) {
            double similarity = Solution.calculateSimilarity(leftSolution, rightSolution, solutionSimilarityFilter);
            saveResult(comparison, similarity);

            return comparison;
        } else {
            return null;
        }
    }

    public ObservableList<Comparison> ordered() {
        List<List<Comparison>> problemToComparisons = IntStream.range(0, Services.contest().getProblemsCount())
                .mapToObj(problemId -> new ArrayList<Comparison>())
                .collect(Collectors.toList());

        comparisons.forEach(
                comparison -> problemToComparisons.get(comparison.problemId).add(comparison)
        );

        var orderedComparisons = FXCollections.<Comparison>observableArrayList();

        problemToComparisons
                .forEach(problemComparisons -> {
                    RandomUtils.shuffle(problemComparisons, Services.properties().getJury().hashCode());
                    orderedComparisons.addAll(problemComparisons);
                });

        return orderedComparisons;
    }

    public ObservableList<Comparison> filtered(Predicate<Comparison> predicate) {
        return ordered().filtered(predicate);
    }

    public Predicate<Comparison> moreThan(double minSimilarity) {
        return (comparison) ->
                similarityByComparison.getOrDefault(comparison, 0.0) >= minSimilarity;
    }

    public Predicate<Comparison> forProblem(List<Integer> expectedProblems) {
        return (comparison) ->
                expectedProblems.contains(comparison.problemId);
    }

    public Predicate<Comparison> withAuthorsOf(Comparison source) {
        return (comparison) ->
                source.left == comparison.left && source.right == comparison.right;
    }

    public Predicate<Comparison> withParticipant(String participantId) {
        return (comparison) ->
                comparison.left.id.equals(participantId) || comparison.right.id.equals(participantId);
    }
}

