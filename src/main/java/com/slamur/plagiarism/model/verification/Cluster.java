package com.slamur.plagiarism.model.verification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.slamur.plagiarism.model.parsing.solution.Solution;

public class Cluster {

    public static final String SEPARATOR = "===========================================";

    private final String problemName;

    private final List<Clique> cliques;
    private final Map<Solution, Clique> solutionToClique;

    private final Map<String, String> comments;

    private final Map<Solution, List<Solution>> weakConnections;
    private final Map<Solution, List<Solution>> strongConnections;

    public Cluster(Solution startSolution) {
        this(startSolution.problemName);
        addSolution(startSolution);
    }

    private Cluster(String problemName) {
        this.problemName = problemName;

        this.cliques = new ArrayList<>();
        this.solutionToClique = new LinkedHashMap<>();

        this.comments = new HashMap<>();

        this.weakConnections = new HashMap<>();
        this.strongConnections = new HashMap<>();
    }

    public void addSolution(Solution solution) {
        if (solutionToClique.containsKey(solution)) return;

        Clique clique = new Clique();
        clique.addSolution(solution);

        weakConnections.put(solution, new ArrayList<>());
        strongConnections.put(solution, new ArrayList<>());

        addClique(clique);
    }

    private void addClique(Clique clique) {
        cliques.add(clique);
        clique.getSolutions().forEach(
                solution -> solutionToClique.put(solution, clique)
        );
    }

    public int size() {
        return solutionToClique.size();
    }

    public String getProblemName() {
        return problemName;
    }

    synchronized public void setComment(String author, String comment) {
        comments.put(author, comment);
    }

    public String getComment(String author) {
        return comments.getOrDefault(author, "");
    }

    public List<Solution> getSolutions() {
        return new ArrayList<>(solutionToClique.keySet());
    }

    public void mergeWith(Cluster otherCluster) {
        cliques.addAll(otherCluster.cliques);
        solutionToClique.putAll(otherCluster.solutionToClique);

        strongConnections.putAll(otherCluster.strongConnections);
        weakConnections.putAll(otherCluster.weakConnections);

        for (var mergedCommentEntry : otherCluster.comments.entrySet()) {
            String author = mergedCommentEntry.getKey();
            String otherComment = mergedCommentEntry.getValue();

            String mergedComment = comments.getOrDefault(author, "");
            if (mergedComment.length() > 0) {
                mergedComment += "\n";
            }

            mergedComment += otherComment;
            comments.put(author, mergedComment);
        }
    }

    public String commentsToText(boolean printCommentsCount) {
        var builder = new StringBuilder();

        if (printCommentsCount) {
            builder.append(comments.size()).append("\n");
        }

        for (var commentEntry : comments.entrySet()) {
            builder.append(commentEntry.getKey()).append(": ")
                    .append(commentEntry.getValue()).append("\n");
        }

        return builder.toString();
    }

    public String toText() {
        var builder = new StringBuilder();

        builder.append(problemName).append("\n");

        for (Clique clique : cliques) {
            builder.append(clique).append("\n");
        }

        builder.append("\n");

        builder.append(commentsToText(false)).append("\n");

        builder.append(SEPARATOR).append("\n\n");

        return builder.toString();
    }

    public void mergeCliques(Solution left, Solution right) {
        var leftClique = solutionToClique.get(left);
        var rightClique = solutionToClique.get(right);

        if (leftClique == rightClique) return;

        if (leftClique.size() < rightClique.size()) {
            var tmpClique = leftClique;
            leftClique = rightClique;
            rightClique = tmpClique;
        }

        leftClique.mergeWith(rightClique);
        for (var solution : rightClique.getSolutions()) {
            solutionToClique.put(solution, leftClique);
        }
        cliques.remove(rightClique);
    }

    public void setStrongEdge(Solution left, Solution right) {
        if (strongConnections.get(left).contains(right)) return;

        strongConnections.get(left).add(right);
        strongConnections.get(right).add(left);

        mergeCliques(left, right);
    }

    private void divideClique(Solution left, Solution right) {
        var leftClique = solutionToClique.get(left);
        var rightClique = solutionToClique.get(right);

        if (leftClique != rightClique) return;

        var cliqueSolutions = leftClique.getSolutions();

        List<Solution> dividedSolutions = getDividedPart(left, cliqueSolutions);
        if (dividedSolutions.isEmpty()) {
            // clique was not divided
            return;
        }

        rightClique = new Clique();
        dividedSolutions.forEach(rightClique::addSolution);
        addClique(rightClique);
    }

    private List<Solution> getDividedPart(Solution startSolution, List<Solution> allSolutions) {
        List<Solution> leftConnections = new ArrayList<>();
        leftConnections.add(startSolution);

        for (int index = 0; index < leftConnections.size(); ++index) {
            var from = leftConnections.get(index);

            for (var to : strongConnections.get(from)) {
                if (leftConnections.contains(to)) continue;
                leftConnections.add(to);
            }
        }

        List<Solution> rightConnections = allSolutions.stream()
                .filter(solution -> !leftConnections.contains(solution))
                .collect(Collectors.toList());

        if (leftConnections.size() < rightConnections.size()) {
            rightConnections = leftConnections;
        }

        return rightConnections;
    }

    public void setWeakEdge(Solution left, Solution right) {
        if (!strongConnections.get(left).contains(right)) return;

        strongConnections.get(left).remove(right);
        strongConnections.get(right).remove(left);

        divideClique(left, right);
    }

    public Optional<Cluster> removeWeakEdge(Solution left, Solution right) {
        // in case it was strong
        setWeakEdge(left, right);

        var dividedSolutions = getDividedPart(left, getSolutions());
        if (dividedSolutions.isEmpty()) {
            // cluster was not divided
            return Optional.empty();
        }

        Cluster dividedCluster = new Cluster(problemName);

        Set<Clique> dividedCliques = new HashSet<>();
        for (var solution : dividedSolutions) {
            var clique = solutionToClique.remove(solution);
            dividedCliques.add(clique);

            var solutionWeaks = weakConnections.remove(solution);
            var solutionStrongs = strongConnections.remove(solution);

            dividedCluster.weakConnections.put(solution, solutionWeaks);
            dividedCluster.strongConnections.put(solution, solutionStrongs);
        }

        for (var clique : dividedCliques) {
            cliques.remove(clique);
            dividedCluster.addClique(clique);
        }

        return Optional.of(dividedCluster);
    }

    public Clique getClique(Solution solution) {
        return solutionToClique.get(solution);
    }

    private Stream<String> toParticipantIds() {
        return getSolutions().stream()
                .map(Solution::getParticipant)
                .map(participant -> participant.id);
    }

    @Override
    public String toString() {
        String minParticipantId = toParticipantIds()
                .min(String::compareTo)
                .orElse("-1");

        return minParticipantId + " " + problemName;
    }

    private String toIdString() {
        String allParticipantIds = toParticipantIds()
                .sorted()
                .collect(Collectors.joining("|"));

        return allParticipantIds + " " + problemName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Cluster other)) return false;

        return this.toIdString().equals(other.toIdString());
    }

    @Override
    public int hashCode() {
        return toIdString().hashCode();
    }
}
