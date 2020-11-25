package com.slamur.plagiarism.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class VerificationService implements Service {

    private String jury;
    private final ObservableList<Cluster> clusters;
    private final Map<Comparison, Cluster> clusterByComparison;
    private final Map<Comparison, Status> statusByComparison;
    private final Map<Participant, Cluster[]> clustersByParticipant;

    VerificationService() {
        this.clusters = FXCollections.observableArrayList();
        this.clusterByComparison = new HashMap<>();
        this.statusByComparison = new HashMap<>();
        this.clustersByParticipant = new HashMap<>();
    }

    @Override
    public void initialize() {
        var properties = PropertiesService.appProperties;

        this.jury = properties.getProperty("jury");
        if (null == jury) {
            jury = "Jury " + this.hashCode();
        }
    }

    public void setJuryComment(Cluster cluster, String comment) {
        cluster.setComment(jury, comment);
    }

    public String getJuryComment(Cluster cluster) {
        return cluster.getComment(jury);
    }

    public Optional<Comparison> getComparison(Cluster cluster, Participant left, Participant right) {
        if (null == left || null == right) return Optional.empty();

        return Optional.of(
                new Comparison(left, right, cluster.getProblemId())
        );
    }

    public Status getStatus(Comparison comparison) {
        return statusByComparison.getOrDefault(comparison, Status.NOT_SEEN);
    }

    public void setStatus(Comparison comparison, Status status) {
        if (getStatus(comparison).equals(status)) return;
        statusByComparison.put(comparison, status);

        Participant left = comparison.left, right = comparison.right;
        int problemId = comparison.problemId;

        var leftCluster = getCluster(left, problemId);
        var rightCluster = getCluster(right, problemId);

        if (Status.IGNORED == status) {
            removeWeakEdge(left, leftCluster, right, rightCluster);
        } else {
            if (null == leftCluster) {
                leftCluster = createCluster(left, problemId);
            }

            if (null == rightCluster) {
                rightCluster = createCluster(right, problemId);
            }

            var resultCluster = mergeClusters(leftCluster, rightCluster);
            clusterByComparison.put(comparison, resultCluster);

            if (Status.PLAGIAT == status) {
                resultCluster.setStrongEdge(left, right);
            } else {
                resultCluster.setWeakEdge(left, right);
            }
        }
    }

    private Cluster getCluster(Participant participant, int problemId) {
        var participantClusters = clustersByParticipant.get(participant);
        return Optional.ofNullable(participantClusters)
                .map(c -> c[problemId])
                .orElse(null);
    }

    private Cluster createCluster(Participant participant, int problemId) {
        var cluster = new Cluster(problemId);
        clusters.add(cluster);

        cluster.addParticipant(participant);
        setCluster(participant, cluster);

        return cluster;
    }

    private void setCluster(Participant participant, Cluster cluster) {
        var clusters = clustersByParticipant.get(participant);
        if (null == clusters) {
            clustersByParticipant.put(participant,
                    clusters = new Cluster[Services.contest().getProblemsCount()]
            );
        }

        clusters[cluster.getProblemId()] = cluster;
    }

    private Cluster mergeClusters(Cluster leftCluster, Cluster rightCluster) {
        if (leftCluster == rightCluster) {
            return leftCluster;
        }

        if (leftCluster.size() < rightCluster.size()) {
            return mergeClusters(rightCluster, leftCluster);
        }

        // size of left >= right
        leftCluster.mergeWith(rightCluster);

        for (Participant participant : rightCluster.getParticipants()) {
            setCluster(participant, leftCluster);
        }

        clusters.remove(rightCluster);

        return leftCluster;
    }

    private void removeWeakEdge(Participant left, Cluster leftCluster,
                                Participant right, Cluster rightCluster) {
        if (null == leftCluster || null == rightCluster) {
            return;
        }

        if (leftCluster != rightCluster) {
            return;
        }

        rightCluster = leftCluster.removeWeakEdge(left, right);
        if (null != rightCluster) {
            clusters.add(rightCluster);

            for (var participant : rightCluster.getParticipants()) {
                setCluster(participant, rightCluster);
            }
        }
    }

    public Optional<Cluster> getCluster(Comparison comparison) {
        return Optional.ofNullable(clusterByComparison.get(comparison));
    }

    public Predicate<Comparison> isNot(Status status) {
        return (comparison) -> statusByComparison.getOrDefault(comparison, Status.NOT_SEEN) != status;
    }

    public ObservableList<Cluster> getClusters() {
        return clusters;
    }
}
