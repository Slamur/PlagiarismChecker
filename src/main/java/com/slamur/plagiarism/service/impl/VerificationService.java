package com.slamur.plagiarism.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.IdsPair;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.IOUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import static java.util.Collections.emptySet;

public class VerificationService extends ServiceBase {

    private static final String verificationFileName = "verification";

    private File verificationFile;

    private final ObservableList<Cluster> clusters;
    private final Map<Comparison, Status> comparisonToStatus;
    private final Map<Solution, Cluster> solutionToCluster;

    public VerificationService() {
        this.clusters = FXCollections.observableArrayList();
        this.comparisonToStatus = new HashMap<>();
        this.solutionToCluster = new HashMap<>();
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

    public Optional<Comparison> getComparison(Solution left, Solution right) {
        if (null == left || null == right) return Optional.empty();
        if (left.equals(right)) return Optional.empty();

        return Optional.of(
                new Comparison(left, right)
        );
    }

    synchronized public void setStatus(Comparison comparison, Status status) {
        if (getStatus(comparison).equals(status)) return;
        comparisonToStatus.put(comparison, status);

        Solution left = comparison.left, right = comparison.right;

        var leftCluster = getCluster(left);
        var rightCluster = getCluster(right);

        if (Status.IGNORED == status) {
            removeWeakEdge(left, leftCluster, right, rightCluster);
        } else {
            if (null == leftCluster) {
                leftCluster = createCluster(left);
            }

            if (null == rightCluster) {
                rightCluster = createCluster(right);
            }

            var resultCluster = mergeClusters(leftCluster, rightCluster);

            // FIXME separate plagiat and auto
            if (Status.PLAGIAT == status || Status.AUTOPLAGIAT == status) {
                resultCluster.setStrongEdge(left, right);
            } else {
                resultCluster.setWeakEdge(left, right);
            }
        }
    }

    synchronized public Status getStatus(Comparison comparison) {
        return comparisonToStatus.getOrDefault(comparison, Status.NOT_SEEN);
    }

    public boolean isPlagiat(Solution solution) {
        return Optional.ofNullable(getCluster(solution))
                .map(cluster -> getCliqueSolutions(cluster, solution))
                .map(solutions -> solutions.size() > 1)
                .orElse(false);
    }

    private List<Solution> getCliqueSolutions(Cluster cluster, Solution solution) {
        return Optional.ofNullable(cluster)
                .orElseGet(() -> new Cluster(solution))
                .getClique(solution)
                .getSolutions();
    }

    public Status getExpectedStatus(Comparison comparison) {
        var left = comparison.left;
        var right = comparison.right;

        Cluster leftCluster = getCluster(left);
        Cluster rightCluster = getCluster(right);

        if (leftCluster != null && leftCluster == rightCluster) {
            var leftClique = leftCluster.getClique(left);
            var rightClique = rightCluster.getClique(right);

            return (leftClique == rightClique) ? Status.PLAGIAT : Status.UNKNOWN;
        } else {
            var leftSolutions = getCliqueSolutions(leftCluster, left);
            var rightSolutions = getCliqueSolutions(rightCluster, right);

            for (var leftSolution : leftSolutions) {
                for (var rightSolution : rightSolutions) {
                    if (Status.IGNORED == getStatus(new Comparison(leftSolution, rightSolution))) {
                        return Status.IGNORED;
                    }
                }
            }

            return Status.NOT_SEEN;
        }
    }

    private Cluster createCluster(Solution solution) {
        var cluster = new Cluster(solution);
        addCluster(cluster);
        return cluster;
    }

    public void addCluster(Cluster cluster) {
        clusters.add(cluster);
        cluster.getSolutions().forEach(solution -> setCluster(solution, cluster));
    }

    private Cluster getCluster(Solution solution) {
        return solutionToCluster.get(solution);
    }

    private void setCluster(Solution solution, Cluster cluster) {
        solutionToCluster.put(solution, cluster);
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

        rightCluster.getSolutions().forEach(
                solution -> setCluster(solution, leftCluster)
        );

        clusters.remove(rightCluster);

        return leftCluster;
    }

    private void removeWeakEdge(Solution left, Cluster leftCluster,
                                Solution right, Cluster rightCluster) {
        if (null == leftCluster || null == rightCluster) {
            return;
        }

        if (leftCluster != rightCluster) {
            return;
        }

        leftCluster.removeWeakEdge(left, right).ifPresent(this::addCluster);
    }

    public Optional<Cluster> getCluster(Comparison comparison) {
        var left = comparison.left;
        var right = comparison.right;

        var leftCluster = getCluster(left);
        var rightCluster = getCluster(right);

        Cluster cluster = (leftCluster == rightCluster)
                ? leftCluster
                : null;

        return Optional.ofNullable(cluster);
    }

    public ObservableList<Cluster> getClusters() {
        return clusters;
    }

    public void saveDataToFile() throws IOException {
        File contestDirectory = Services.contest().getDirectory();

        this.verificationFile = new File(contestDirectory, verificationFileName + ".txt");
        try (PrintWriter out = new PrintWriter(verificationFile, IOUtils.RUSSIAN_ENCODING)) {
            out.println(comparisonToStatus.size());

            for (var comparisonStatusEntry : comparisonToStatus.entrySet()) {
                out.println(comparisonStatusEntry.getKey().toText() + "\t" + comparisonStatusEntry.getValue());
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
        File contestDirectory = Services.contest().getDirectory();

        this.verificationFile = new File(contestDirectory, verificationFileName + ".txt");
        if (!verificationFile.exists()) {
            return;
        }

        loadPatchFrom(verificationFile);
    }

    private static Status parseStatus(String statusString, Supplier<String> errorMessageSupplier) throws IOException {
        try {
            return Status.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            throw new IOException(errorMessageSupplier.get());
        }
    }

    public void loadPatchFrom(File file) throws IOException {
        IOUtils.loadFromFile(file, (in) -> {
            Map<String, Solution> idToSolution = new HashMap<>();

            Services.contest().getSolutions()
                    .forEach(solution -> idToSolution.put(solution.id, solution));

            loadStatuses(in, idToSolution);

            in.readLine();

            try {
                loadComments(in, idToSolution);
            } catch (IOException e) {
                throw new IOException("Ошибка при чтении комментариев из файла. Данные о вердиктах уже применены", e);
            }

            return null;
        });
    }

    private void loadComments(BufferedReader in, Map<String, Solution> idToSolution) throws IOException {
        int fileClustersCount = Integer.parseInt(in.readLine());
        for (int fileClusterIndex = 0; fileClusterIndex < fileClustersCount; ++fileClusterIndex) {
            String clusterInfoLine = in.readLine();
            String[] clusterInfoParts = clusterInfoLine.split(" ");

            String solutionId = clusterInfoParts[0];

            Solution solution = idToSolution.get(solutionId);
            if (null == solution) {
                //throw new IOException("Кластер привязан к неизвестному решению: " + clusterInfoLine);
                continue;
            }

            Cluster cluster = getCluster(solution);
            if (null == cluster) {
                cluster = createCluster(solution);
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
    }

    private void loadStatuses(BufferedReader in, Map<String, Solution> idToSolution) throws IOException {
        Map<Comparison, Status> fileComparisonToStatus = new HashMap<>();

        int fileStatusesCount = Integer.parseInt(in.readLine());
        for (int fileStatusIndex = 0; fileStatusIndex < fileStatusesCount; ++fileStatusIndex) {
            String line = in.readLine();

            var tok = new StringTokenizer(line, "-() \t");

            String leftId = tok.nextToken();
            String leftLogin = tok.nextToken();
            String rightId = tok.nextToken();
            String rightLogin = tok.nextToken();
            Status status = parseStatus(
                    tok.nextToken(),
                    () -> "Сравнение содержит некорректный вердикт: " + line
            );

            var left = idToSolution.get(leftId);
            var right = idToSolution.get(rightId);

            if (null == left) {
                //throw new IOException("Сравнение содержит неизвестный идентификатор левого решения: " + line);
                continue;
            }

            if (null == right) {
//                throw new IOException("Сравнение содержит неизвестный идентификатор правого решения: " + line);
                continue;
            }

            if (leftId.equals(rightId)) {
//                throw new IOException("Сравнение решения с самим собой: " + line);
                continue;
            }

            fileComparisonToStatus.put(new Comparison(left, right), status);
        }

        for (var fileEntry : fileComparisonToStatus.entrySet()) {
            var comparison = fileEntry.getKey();
            var fileStatus = fileEntry.getValue();

            var actualStatus = getStatus(comparison);

            if (actualStatus.priority < fileStatus.priority) {
                setStatus(comparison, fileStatus);
            }
        }

        clusters.setAll(
                clusters.stream().filter(cluster -> cluster.size() > 1).collect(Collectors.toList())
        );
    }

    public void saveReportToFile() throws IOException {
        File reportFile = new File(Services.contest().getDirectory(), "report.txt");
        try (PrintWriter out = new PrintWriter(reportFile, IOUtils.RUSSIAN_ENCODING)) {
            clusters.stream()
                    .filter(cluster -> cluster.size() > 1)
                    .forEach(cluster -> out.println(cluster.toText()));
        }
    }

    public void saveBansToFile() throws IOException {
        File reportFile = new File(Services.contest().getDirectory(), "banned.txt");
        IOUtils.saveToFile(reportFile, (out) -> {
            clusters.stream().map(
                    Cluster::getSolutions
            ).flatMap(List::stream)
            .filter(this::isPlagiat)
            .map(Solution::getId)
            .forEach(out::println);
        });
    }

    public Predicate<Comparison> withStatus(List<Status> expectedStatuses) {
        return (comparison) ->
                expectedStatuses.contains(getStatus(comparison))
                || expectedStatuses.contains(getExpectedStatus(comparison));
    }

    public Predicate<Comparison> fromCluster(List<Cluster> clusters) {
        return (comparison) -> getCluster(comparison)
                .map(clusters::contains)
                .orElse(false);
    }

    private void runAutoPlagiat(int minAutoPlagiatProblemsCount,
                                double minAutoPlagiatSimilarity) {
        Map<IdsPair, Set<String>> plagiatedProblems = new HashMap<>();

        var comparisons = Services.comparisons();

        var comparisonsList = List.copyOf(comparisons.ordered());
        for (var comparison : comparisonsList) {
            var similarity = comparisons.getSimilarity(comparison);
            var status = getStatus(comparison);

            boolean plagiated = (Status.PLAGIAT == status || Status.AUTOPLAGIAT == status);
            plagiated |= similarity >= minAutoPlagiatSimilarity;

            if (plagiated) {
                var participantIds = comparison.toParticipantIds();

                var participantsPlagiatedProblems = plagiatedProblems.computeIfAbsent(
                        participantIds, (ids) -> new HashSet<>()
                );

                participantsPlagiatedProblems.add(
                        comparison.getProblemName()
                );
            }
        }

        for (var comparison : comparisonsList) {
            var participantIds = comparison.toParticipantIds();

            int participantsPlagiatedProblemsCount = plagiatedProblems.getOrDefault(
                    participantIds, emptySet()
            ).size();

            if (minAutoPlagiatProblemsCount <= participantsPlagiatedProblemsCount) {
                if (Status.NOT_SEEN == getStatus(comparison)) {
                    setStatus(comparison, Status.AUTOPLAGIAT);
                }
            }
        }
    }

    public void runAutoPlagiat(int minAutoPlagiatProblemsCount,
                               double minAutoPlagiatSimilarity,
                               Runnable callback) {
        new Thread(() -> {
            runAutoPlagiat(minAutoPlagiatProblemsCount, minAutoPlagiatSimilarity);

            callback.run();
        }).start();
    }
}
