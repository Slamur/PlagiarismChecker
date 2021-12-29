package com.slamur.plagiarism.model.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.solution.Solution;

public class Clique {

    private final List<Solution> solutions;

    Clique() {
        this.solutions = new ArrayList<>();
    }

    public List<Solution> getSolutions() {
        return Collections.unmodifiableList(solutions);
    }

    public void addSolution(Solution solution) {
        solutions.add(solution);
    }

    public int size() {
        return solutions.size();
    }

    public void mergeWith(Clique otherClique) {
        solutions.addAll(otherClique.solutions);
    }

    public String participantsToText() {
        var builder = new StringBuilder();

        solutions.stream()
                .map(Solution::getParticipant)
                .map(Participant::toText)
                .forEach(text -> builder.append(text).append("\n"));

        return builder.toString();
    }

    public String solutionsToText() {
        var builder = new StringBuilder();

        solutions.forEach(solution -> {
                    var participant = solution.getParticipant();
                    builder.append(participant.login)
                            .append("\t")
                            .append(solution)
                            .append("\n");
                });

        return builder.toString();
    }

    @Override
    public String toString() {
        return participantsToText() + "\n" +
                solutionsToText() + "\n";
    }
}
