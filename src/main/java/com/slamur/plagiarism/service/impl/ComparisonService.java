package com.slamur.plagiarism.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.IdsPair;
import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.solution.Language;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.SolutionProgram;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.service.impl.comparison.ComparisonRepository;
import com.slamur.plagiarism.service.impl.contest.ContestRepository;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.RandomUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ComparisonService extends ServiceBase {

    private static final EnumSet<Verdict> notScanningVerdicts = EnumSet.of(Verdict.WA, Verdict.CE, Verdict.PE, Verdict.RE, Verdict.TL);
    private static final boolean onlyEqualScoreScan = true;

    // TODO make it comparison parameter
    private static final double solutionSimilarityFilter = 0.7;
    private static final double minimalSimilarityLimit = 0.5;

    private final ObservableList<Comparison> comparisons;

    private ComparisonRepository comparisonRepository;
    private final Map<IdsPair, Double> similarities;

    public ComparisonService() {
        this.comparisons = FXCollections.observableArrayList();
        this.similarities = new HashMap<>();
    }

    @Override
    protected void initializeOnly() {
        var solutions = Services.contest().getSolutions();

        Contest contest = Services.contest().getContest();

        try {
            var contestRepository = ContestRepository.createRepository(contest);
            this.comparisonRepository = contestRepository.createComparisonRepository();
        } catch (IOException e) {
            AlertUtils.warning("Проблема с созданием репозитория сравнений", e);
        }

        try {
            for (String problemName : contest.getProblems()) {
                similarities.putAll(
                        comparisonRepository.loadSimilarities(solutionSimilarityFilter, problemName)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.warning("Проблема с загрузкой сравнений", e);
        }

        generateComparisons(solutions);
    }

    private void generateComparisons(List<Solution> solutions) {
        System.out.printf("Количество решений %d%n", solutions.size());

        int blockSize = 10;

        for (int leftIndex = 0; leftIndex < solutions.size(); ++leftIndex) {
            var leftSolution = solutions.get(leftIndex);

            if (leftIndex % blockSize == blockSize - 1) {
                System.out.printf("Начаты сравнения с %d-м решением %s (%s) %n",
                        leftIndex, leftSolution.id, leftSolution.getParticipant().login
                );
            }

            List<Comparison> notCachedBefore = new ArrayList<>();

            for (int rightIndex = leftIndex + 1; rightIndex < solutions.size(); ++rightIndex) {
                var rightSolution = solutions.get(rightIndex);

                if (isComparisonNeeded(leftSolution, rightSolution)) {
                    var comparison = new Comparison(leftSolution, rightSolution);
                    comparisons.add(comparison);

                    similarities.computeIfAbsent(comparison.toIds(), (ids) -> {
                        double similarity = SolutionProgram.calculateSimilarity(
                                leftSolution.getProgram(),
                                rightSolution.getProgram(),
                                solutionSimilarityFilter,
                                minimalSimilarityLimit
                        );

                        notCachedBefore.add(comparison);

                        return similarity;
                    });
                }
            }

            if (leftIndex % blockSize == blockSize - 1) {
                System.out.printf("Закончены сравнения с %d-м решением %s (%s) %n",
                        leftIndex, leftSolution.id, leftSolution.getParticipant().id
                );
            }

            try {
                comparisonRepository.saveSimilarities(
                        notCachedBefore.stream()
                                .collect(Collectors.toMap(
                                        Function.identity(),
                                        (comparison) -> similarities.get(comparison.toIds())
                                )),
                        solutionSimilarityFilter
                );
            } catch (IOException e) {
                AlertUtils.warning(
                        String.format(
                                "Не смогли закешировать новые сравнения с решением %s", leftSolution.id
                        ), e
                );
            }
        }
    }

    public boolean isComparisonNeeded(Solution leftSolution, Solution rightSolution) {
        if (null == leftSolution || null == rightSolution) {
            return false;
        }

        if (!leftSolution.getProblemName().equals(rightSolution.getProblemName())) {
            return false;
        }

        if (notScanningVerdicts.contains(leftSolution.verdict)
                || notScanningVerdicts.contains(rightSolution.verdict)) {
            return false;
        }

        // TODO Do we need compare two solutions of the same participant?
        if (leftSolution.getParticipant().equals(rightSolution.getParticipant())) {
            return false;
        }

        // TODO beatify later
        if (Language.TEXT == leftSolution.getProgram().language || Language.TEXT == rightSolution.getProgram().language) {
            return false;
        }

        if (onlyEqualScoreScan) {
            return leftSolution.score == rightSolution.score;
        }

        return true;
    }

    public double getSimilarity(Comparison comparison) {
        return similarities.getOrDefault(comparison.toIds(), 0.0);
    }

    public ObservableList<Comparison> ordered() {
        Map<String, List<Comparison>> problemToComparisons = Services.contest().getProblems().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        (problemName) -> new ArrayList<>()
                ));

        comparisons.forEach(
                comparison -> problemToComparisons.get(comparison.getProblemName()).add(comparison)
        );

        var orderedComparisons = FXCollections.<Comparison>observableArrayList();

        problemToComparisons
                .forEach((problemName, problemComparisons) -> {
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
                similarities.getOrDefault(comparison.toIds(), 0.0) >= minSimilarity;
    }

    public Predicate<Comparison> forProblem(Collection<String> expectedProblems) {
        return (comparison) ->
                expectedProblems.contains(comparison.getProblemName());
    }

    public Predicate<Comparison> withAuthorsOf(Comparison source) {
        return (comparison) ->
                source.toParticipantIds().equals(comparison.toParticipantIds());
    }

    public Predicate<Comparison> withParticipant(String participantId) {
        return (comparison) ->
                comparison.left.getParticipant().id.equals(participantId)
                || comparison.right.getParticipant().id.equals(participantId);
    }

    public Predicate<Comparison> withParticipants(String firstParticipantId, String secondParticipantId) {
        var participants = new IdsPair(firstParticipantId, secondParticipantId);
        return (comparison) ->
                participants.equals(comparison.toParticipantIds());
    }

    public Predicate<Comparison> withSolution(String solutionId) {
        return (comparison) ->
                comparison.left.id.equals(solutionId) || comparison.right.id.equals(solutionId);
    }

    public Predicate<Comparison> withSubstring(String substring) {
        return (comparison) ->
                comparison.left.getProgram().code.contains(substring)
                        &&
                comparison.right.getProgram().code.contains(substring);
    }
}

