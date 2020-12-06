package com.slamur.plagiarism.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.IOUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class VerificationService extends ServiceBase {

    private static final String verificationFileName = "verification";

    private File verificationFile;

    private final ObservableList<Cluster> clusters;
    private final Map<Comparison, Cluster> comparisonToCluster;
    private final Map<Comparison, Status> comparisonToStatus;
    private final Map<Participant, Cluster[]> participantToClusters;

    public VerificationService() {
        this.clusters = FXCollections.observableArrayList();
        this.comparisonToCluster = new HashMap<>();
        this.comparisonToStatus = new HashMap<>();
        this.participantToClusters = new HashMap<>();
    }

    @Override
    protected void initializeOnly() {
        try {
            restoreDataFromFile();
        } catch (IOException e) {
            AlertUtils.warning(
                    "Ошибка при восстановлении данных", e
            );
        }
    }

    public void setJuryComment(Cluster cluster, String comment) {
        String jury = Services.properties().getJury();
        cluster.setComment(jury, comment);
    }

    public String getJuryComment(Cluster cluster) {
        String jury = Services.properties().getJury();
        return cluster.getComment(jury);
    }

    public Optional<Comparison> getComparison(Cluster cluster, Participant left, Participant right) {
        if (null == left || null == right) return Optional.empty();
        if (left.equals(right)) return Optional.empty();

        return Optional.of(
                new Comparison(left, right, cluster.getProblemId())
        );
    }

    synchronized public Status getStatus(Comparison comparison) {
        return comparisonToStatus.getOrDefault(comparison, Status.NOT_SEEN);
    }

    synchronized public void setStatus(Comparison comparison, Status status) {
        if (getStatus(comparison).equals(status)) return;
        comparisonToStatus.put(comparison, status);

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
            comparisonToCluster.put(comparison, resultCluster);

            if (Status.PLAGIAT == status) {
                resultCluster.setStrongEdge(left, right);
            } else {
                resultCluster.setWeakEdge(left, right);
            }
        }
    }

    private Cluster getCluster(Participant participant, int problemId) {
        var participantClusters = participantToClusters.get(participant);
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
        var clusters = participantToClusters.get(participant);
        if (null == clusters) {
            participantToClusters.put(participant,
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
        return Optional.ofNullable(comparisonToCluster.get(comparison));
    }

    public Predicate<Comparison> isNot(Status status) {
        return (comparison) -> comparisonToStatus.getOrDefault(comparison, Status.NOT_SEEN) != status;
    }

    public ObservableList<Cluster> getClusters() {
        return clusters;
    }

    public void saveDataToFile() throws IOException {
        File contestFolder = Services.contest().getFolder();

        this.verificationFile = new File(contestFolder, verificationFileName + ".txt");
        try (PrintWriter out = new PrintWriter(verificationFile, IOUtils.RUSSIAN_ENCODING)) {
            out.println(comparisonToStatus.size());

            for (var comparisonStatusEntry : comparisonToStatus.entrySet()) {
                out.println(comparisonStatusEntry.getKey() + "\t" + comparisonStatusEntry.getValue());
            }

            out.println(Cluster.SEPARATOR);

            out.println(clusters.size());

            for (Cluster cluster : clusters) {
                out.println(cluster.toString());
                out.print(cluster.commentsToText(true));
                out.println(Cluster.SEPARATOR);
            }
        }
    }

    public void restoreDataFromFile() throws IOException {
        File contestFolder = Services.contest().getFolder();

        this.verificationFile = new File(contestFolder, verificationFileName + ".txt");
        if (!verificationFile.exists()) {
            return;
        }

        loadPatchFrom(verificationFile);
    }

    public void loadPatchFrom(File file) throws IOException {
        int problemsCount = Services.contest().getProblemsCount();
        Map<Integer, Participant> idToParticipant = new HashMap<>();

        Services.contest().getParticipants()
                .forEach(p -> idToParticipant.put(p.id, p));

        try (BufferedReader in = new BufferedReader(
                IOUtils.createReader(new FileInputStream(file))
        )) {
            Map<Comparison, Status> fileComparisonToStatus = new HashMap<>();

            int fileStatusesCount = Integer.parseInt(in.readLine());
            for (int fileStatusIndex = 0; fileStatusIndex < fileStatusesCount; ++fileStatusIndex) {
                String line = in.readLine();
                try {
                    var tok = new StringTokenizer(line, "-() \t");

                    int leftId = Integer.parseInt(tok.nextToken());
                    int rightId = Integer.parseInt(tok.nextToken());
                    int problemId = tok.nextToken().charAt(0) - 'A';
                    Status status = Status.valueOf(tok.nextToken());

                    var left = idToParticipant.get(leftId);
                    var right = idToParticipant.get(rightId);

                    if (null == left) {
                        throw new IOException("Сравнение содержит неверный идентификатор левого участника: " + line);
                    }

                    if (null == right) {
                        throw new IOException("Сравнение содержит неверный идентификатор правого участника: " + line);
                    }

                    if (leftId == rightId) {
                        throw new IOException("Сравнение участника с самим собой: " + line);
                    }

                    if (problemId < 0 || problemsCount <= problemId) {
                        throw new IOException("Сравнение содержит некорректный идентификатор задачи: " + line);
                    }

                    fileComparisonToStatus.put(new Comparison(left, right, problemId), status);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Сравнение содержит некорректный вердикт: " + line);
                }
            }

            for (var fileEntry : fileComparisonToStatus.entrySet()) {
                var comparison = fileEntry.getKey();
                var fileStatus = fileEntry.getValue();

                var actualStatus = getStatus(comparison);

                if (actualStatus.priority < fileStatus.priority) {
                    setStatus(comparison, fileStatus);
                }
            }

            in.readLine();

            try {
                int fileClustersCount = Integer.parseInt(in.readLine());
                for (int fileClusterIndex = 0; fileClusterIndex < fileClustersCount; ++fileClusterIndex) {
                    String clusterInfoLine = in.readLine();
                    String[] clusterInfoParts = clusterInfoLine.split(" ");

                    int participantId = Integer.parseInt(clusterInfoParts[0]);
                    int problemId = clusterInfoParts[1].charAt(0) - 'A';

                    Participant participant = idToParticipant.get(participantId);
                    if (null == participant) {
                        throw new IOException("Кластер привязан к некорректному участнику: " + clusterInfoLine);
                    }

                    if (problemId < 0 || problemsCount <= problemId) {
                        throw new IOException("Кластер содержит некорректный идентификатор задачи: " + clusterInfoLine);
                    }

                    Cluster cluster = getCluster(participant, problemId);
                    if (null == cluster) {
                        cluster = createCluster(participant, problemId);
                    }

                    int commentsCount = Integer.parseInt(in.readLine());
                    for (int clusterCommentIndex = 0; clusterCommentIndex < commentsCount; ++clusterCommentIndex) {
                        String commentLine = in.readLine();

                        int authorDotsIndex = commentLine.indexOf(":");
                        if (authorDotsIndex < 0) {
                            throw new IOException(
                                    String.format(
                                            "Коммент к кластеру %s не содержит указания автора: %s",
                                            clusterInfoLine,
                                            commentLine
                                    )
                            );
                        }

                        String author = commentLine.substring(0, authorDotsIndex);
                        String comment = commentLine.substring(authorDotsIndex + 1).trim();

                        cluster.setComment(author, comment);
                    }

                    in.readLine();
                }
            } catch (IOException e) {
                throw new IOException("Ошибка при чтении комментариев из файла. Данные о вердиктах уже применены", e);
            }
        }
    }

    public void saveReportToFile() throws IOException {
        File reportFile = new File(Services.contest().getFolder(), "report.txt");
        try (PrintWriter out = new PrintWriter(reportFile, IOUtils.RUSSIAN_ENCODING)) {
            for (Cluster cluster : clusters) {
                out.println(cluster.toText());
            }
        }
    }
}
