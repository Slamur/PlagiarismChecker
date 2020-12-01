package com.slamur.plagiarism.model.verification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.utils.ModelUtils;

public class Cluster {

    public static final String SEPARATOR = "===========================================";

    private final int problemId;

    private final List<Clique> cliques;
    private final Map<Participant, Clique> participantToClique;

    private final Map<String, String> comments;

    private final Map<Participant, List<Participant>> weakConnections;
    private final Map<Participant, List<Participant>> strongConnections;

    public Cluster(int problemId) {
        this.problemId = problemId;

        this.cliques = new ArrayList<>();
        this.participantToClique = new LinkedHashMap<>();

        this.comments = new HashMap<>();

        this.weakConnections = new HashMap<>();
        this.strongConnections = new HashMap<>();
    }

    public void addParticipant(Participant participant) {
        Clique clique = new Clique(problemId);
        clique.getParticipants().add(participant);

        weakConnections.put(participant, new ArrayList<>());
        strongConnections.put(participant, new ArrayList<>());

        addClique(clique);
    }

    private void addClique(Clique clique) {
        cliques.add(clique);
        clique.getParticipants().forEach(
                participant -> participantToClique.put(participant, clique)
        );
    }

    public int size() {
        return participantToClique.size();
    }

    public int getProblemId() {
        return problemId;
    }

    synchronized public void setComment(String author, String comment) {
        comments.put(author, comment);
    }

    public String getComment(String author) {
        return comments.getOrDefault(author, "");
    }

    public List<Participant> getParticipants() {
        return new ArrayList<>(participantToClique.keySet());
    }

    public void mergeWith(Cluster otherCluster) {
        cliques.addAll(otherCluster.cliques);
        participantToClique.putAll(otherCluster.participantToClique);

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

    public String problemToText() {
        return ModelUtils.getProblemName(problemId) + "\n";
    }

    public String toText() {
        var builder = new StringBuilder();

        builder.append(problemToText()).append("\n");

        for (Clique clique : cliques) {
            builder.append(clique).append("\n");
        }

        builder.append("\n");

        builder.append(commentsToText(false)).append("\n");

        builder.append(SEPARATOR).append("\n\n");

        return builder.toString();
    }

    @Override
    public String toString() {
        int firstParticipantId = getParticipants().stream().findFirst().map(p -> p.id).orElse(-1);
        return firstParticipantId + " " + ModelUtils.getProblemName(problemId);
    }

    public void mergeCliques(Participant left, Participant right) {
        var leftClique = participantToClique.get(left);
        var rightClique = participantToClique.get(right);

        if (leftClique == rightClique) return;

        if (leftClique.size() < rightClique.size()) {
            var tmpClique = leftClique;
            leftClique = rightClique;
            rightClique = tmpClique;
        }

        leftClique.mergeWith(rightClique);
        for (Participant participant : rightClique.getParticipants()) {
            participantToClique.put(participant, leftClique);
        }
        cliques.remove(rightClique);
    }

    public void setStrongEdge(Participant left, Participant right) {
        if (strongConnections.get(left).contains(right)) return;

        strongConnections.get(left).add(right);
        strongConnections.get(right).add(left);

        mergeCliques(left, right);
    }

    private void divideClique(Participant left, Participant right) {
        var leftClique = participantToClique.get(left);
        var rightClique = participantToClique.get(right);

        if (leftClique != rightClique) return;

        var cliqueParticipants = leftClique.getParticipants();

        List<Participant> dividedParticipants = getDividedPart(left, cliqueParticipants);
        if (dividedParticipants == null) {
            return;
        }

        rightClique = new Clique(problemId);
        rightClique.getParticipants().addAll(dividedParticipants);
        addClique(rightClique);
    }

    private List<Participant> getDividedPart(Participant startParticipant, List<Participant> allParticipants) {
        List<Participant> leftConnections = new ArrayList<>();
        leftConnections.add(startParticipant);

        for (int index = 0; index < leftConnections.size(); ++index) {
            var from = leftConnections.get(index);

            for (var to : strongConnections.get(from)) {
                if (leftConnections.contains(to)) continue;
                leftConnections.add(to);
            }
        }

        if (leftConnections.size() == allParticipants.size()){
            // clique is not divided
            return null;
        }

        List<Participant> rightConnections = new ArrayList<>();
        for (var participant : allParticipants) {
            if (!leftConnections.contains(participant)) {
                rightConnections.add(participant);
            }
        }

        if (leftConnections.size() < rightConnections.size()) {
            rightConnections = leftConnections;
        }
        return rightConnections;
    }

    public void setWeakEdge(Participant left, Participant right) {
        if (!strongConnections.get(left).contains(right)) return;

        strongConnections.get(left).remove(right);
        strongConnections.get(right).remove(left);

        divideClique(left, right);
    }

    public Cluster removeWeakEdge(Participant left, Participant right) {
        // on case it was strong
        setWeakEdge(left, right);

        List<Participant> dividedParticipants = getDividedPart(left, getParticipants());
        if (null == dividedParticipants) {
            // cluster was not divided
            return null;
        }

        Cluster dividedCluster = new Cluster(problemId);

        Set<Clique> dividedCliques = new HashSet<>();
        for (var participant : dividedParticipants) {
            dividedCliques.add(participantToClique.remove(participant));

            var participantWeaks = weakConnections.remove(participant);
            var participantStrongs = strongConnections.remove(participant);

            dividedCluster.weakConnections.put(participant, participantWeaks);
            dividedCluster.strongConnections.put(participant, participantStrongs);
        }

        for (var clique : dividedCliques) {
            dividedCluster.addClique(clique);
        }

        return dividedCluster;
    }

    public boolean areStrongConnected(Participant left, Participant right) {
        return participantToClique.get(left) == participantToClique.get(right);
    }
}
