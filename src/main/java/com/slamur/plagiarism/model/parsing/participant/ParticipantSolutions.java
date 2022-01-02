package com.slamur.plagiarism.model.parsing.participant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.utils.DateTimeUtils;

public class ParticipantSolutions {

    private final List<Solution> allSolutions;
    private final Map<String, Solution> problemToBestSolution;

    public ParticipantSolutions() {
        this.allSolutions = new ArrayList<>();
        this.problemToBestSolution = new HashMap<>();
    }

    public void addSolution(Solution solution) {
        allSolutions.add(solution);

        // compare with old
        Solution oldSolution = problemToBestSolution.get(solution.problemName);
        if (solution.compareTo(oldSolution) > 0) {
            problemToBestSolution.put(solution.problemName, solution);
        }
    }

    public List<Solution> getAllSolutions() {
        return Collections.unmodifiableList(allSolutions);
    }

    public List<Solution> getSolutions(boolean onlyBest,
                                       int minimalProblemsToCompare,
                                       List<String> interestingProblems) {
        var resultSolutions = onlyBest
                ? new ArrayList<>(problemToBestSolution.values())
                : allSolutions;

        var problemsCount = resultSolutions.stream()
                .map(Solution::getProblemName)
                .distinct()
                .count();

        if (problemsCount < minimalProblemsToCompare) {
            return Collections.emptyList();
        }

        return resultSolutions.stream()
                .filter(solution -> interestingProblems.contains(solution.getProblemName()))
                .collect(Collectors.toUnmodifiableList());
    }

    public ParticipantResult getParticipantResults(
            Contest contest,
            Predicate<Solution> isPlagiatPredicate
    ) {
        if (allSolutions.isEmpty()) {
            return new ParticipantResult(
                    null,
                    Collections.emptyMap()
            );
        }

        var participant = allSolutions.stream().findFirst()
                .map(Solution::getParticipant)
                .orElseThrow();

        var startDateTime = allSolutions.stream()
                .filter(solution -> solution.verdict == Verdict.VIRTUAL_START)
                .map(Solution::getDateTime)
                .findAny().orElse(contest.getStartDateTime());

        Map<String, ProblemResult> problemResults = contest.getProblems().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        (problemName) -> getProblemResult(
                                problemName,
                                isPlagiatPredicate,
                                startDateTime,
                                contest.getPenaltyTime()
                        )
                ));

        return new ParticipantResult(
                participant,
                problemResults
        );
    }

    private ProblemResult getProblemResult(String problemName,
                                           Predicate<Solution> isPlagiatPredicate,
                                           LocalDateTime startDateTime,
                                           long penaltyTime) {

        var bestSolution = problemToBestSolution.get(problemName);
        return Optional.ofNullable(bestSolution)
                .filter(isPlagiatPredicate.negate())
                .map(solution -> {
                    var time = Duration.between(
                            startDateTime,
                            solution.dateTime
                    );

                    var tries = (int)allSolutions.stream()
                            .filter(otherSolution ->
                                    problemName.equals(otherSolution.getProblemName())
                            ).filter(otherSolution ->
                                    otherSolution.getDateTime().compareTo(solution.getDateTime()) < 0
                            ).filter(otherSolution ->
                                    !otherSolution.verdict.equals(Verdict.CE)
                            ).count();

                    long timeMinutes = DateTimeUtils.toCeilingMinutes(time);
                    long penalty = timeMinutes + penaltyTime * tries;

                    return new ProblemResult(
                            solution.score,
                            time,
                            tries,
                            penalty
                    );
                }).orElse(ProblemResult.NOT_TRIED);
    }
}
