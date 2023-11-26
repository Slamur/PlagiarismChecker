package com.slamur.plagiarism.controller.impl;

import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.IdsPair;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.FxmlUtils;
import com.slamur.plagiarism.utils.StreamUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DiffController extends TabController {

    @FXML public VBox comparisonsVBox;

    @FXML public ListView<IdsPair> participantPairsListView;

    @FXML public ListView<Comparison> comparisonsListView;

    @FXML public HBox problemFiltersHBox;

    @FXML public HBox statusFiltersHBox;

    @FXML public Spinner<Double> minSimilaritySpinner;

    @FXML public CheckBox useFirstParticipantFilterCheckBox;

    @FXML public TextField firstParticipantIdFilterTextField;

    @FXML public CheckBox useSecondParticipantFilterCheckBox;

    @FXML public TextField secondParticipantIdFilterTextField;

    @FXML public CheckBox useSolutionFilterCheckBox;

    @FXML public TextField solutionIdFilterTextField;

    @FXML public CheckBox useSubstringFilterCheckBox;

    @FXML public TextField substringFilterTextField;

    @FXML public ListView<Cluster> clusterFiltersListView;

    @FXML public ListView<String> ipFiltersListView;

    @FXML public Button filterComparisonsButton;

    @FXML public TextArea leftParticipantInfoTextArea;

    @FXML public Label leftCodeInfoLabel;

    @FXML public TextArea leftCodeTextArea;

    @FXML public TextArea rightParticipantInfoTextArea;

    @FXML public Label rightCodeInfoLabel;

    @FXML public TextArea rightCodeTextArea;

    @FXML public Button plagiatButton;

    @FXML public Button unknownButton;

    @FXML public Button ignoreButton;

    @FXML public Label comparisonInfoLabel;

    @FXML public Label comparisonStatusLabel;

    @FXML public Button goToClusterButton;

    @FXML public CheckBox blindModeCheckBox;

    @FXML public Button prevComparisonButton;

    @FXML public Button nextComparisonButton;

    @FXML public Button showAllComparisonsForAuthorsButton;

    @FXML public TextArea clusterCommentsTextArea;

    @FXML public Spinner<Integer> minAutoPlagiatProblemsSpinner;

    @FXML public Button runAutoPlagiatButton;

    private int participantsPairIndex;

    private int comparisonIndex;
    private Comparison comparison;

    private final Map<String, CheckBox> problemToFilter;
    private final EnumMap<Status, CheckBox> statusToFilter;

    private final Map<IdsPair, ObservableList<Comparison>> participantsToComparisons;
    private final Map<String, ObservableList<Comparison>> ipToComparisons;
    private final Map<Cluster, ObservableList<Comparison>> clusterToComparisons;

    public DiffController() {
        this.problemToFilter = new HashMap<>();
        this.statusToFilter = new EnumMap<>(Status.class);
        this.participantsToComparisons = new HashMap<>();
        this.ipToComparisons = new HashMap<>();
        this.clusterToComparisons = new HashMap<>();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeComparisonInfoPart();
        initializeDiffPart();
        initializeBlindMode();
        initializeMovePart();
        initializeAutoPlagiatPart();

        this.comparison = null;
        this.comparisonIndex = -1;
        this.participantsPairIndex = -1;
    }

    private void initializeAutoPlagiatPart() {
        var contest = Services.contest();

        minAutoPlagiatProblemsSpinner.setEditable(true);

        int defaultMinAutoPlagiatProblemsCount = 2;
        minAutoPlagiatProblemsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        contest.getContest().getProblems().size(),
                        defaultMinAutoPlagiatProblemsCount
                )
        );

        runAutoPlagiatButton.setOnAction(this::runAutoPlagiatAction);
    }

    private void runAutoPlagiatAction(ActionEvent event) {
        int minAutoPlagiatProblemsCount = minAutoPlagiatProblemsSpinner.getValue();
        double minAutoPlagiatSimilarity = minSimilaritySpinner.getValue();

        Services.verification().runAutoPlagiat(
                minAutoPlagiatProblemsCount,
                minAutoPlagiatSimilarity,
                () -> Platform.runLater(
                        () -> AlertUtils.information("Автоматическая проверка на плагиат завершена")
                )
        );
    }

    private void initializeMovePart() {
        prevComparisonButton.setOnAction(event -> move(-1));
        nextComparisonButton.setOnAction(event -> moveNext());
    }

    private void moveNext() {
        move(1);
    }

    private void move(int shift) {
        List<Comparison> comparisons = comparisonsListView.getItems();
        if (comparisons.isEmpty()) return;

        if (comparisonIndex == -1) {
            comparisonIndex = 0;
        }

        int nextComparisonIndex = comparisonIndex + shift;
        if (nextComparisonIndex < 0) {
            moveParticipantsPair(-1);
        } else if (comparisons.size() <= nextComparisonIndex) {
            moveParticipantsPair(1);
        } else {
            selectComparison(nextComparisonIndex);
        }
    }

    private void moveParticipantsPair(int shift) {
        int pairsCount = participantPairsListView.getItems().size();
        int nextPairIndex = (participantsPairIndex + pairsCount + shift) % pairsCount;

        selectParticipantPairs(nextPairIndex);
        if (shift < 0) {
            List<Comparison> comparisons = comparisonsListView.getItems();
            selectComparison(comparisons.size() - 1);
        }
    }

    private void initializeBlindMode() {
        blindModeCheckBox.selectedProperty().addListener(
                (observableValue, wasBlind, nowBlind) -> updateViewForMode(nowBlind)
        );

        blindModeCheckBox.setSelected(false);
    }

    private void updateViewForMode(boolean isBlindMode) {
        comparisonsVBox.setVisible(!isBlindMode);
        leftParticipantInfoTextArea.setVisible(!isBlindMode);
        rightParticipantInfoTextArea.setVisible(!isBlindMode);

        showSelectedComparison();
    }

    private void initializeComparisonInfoPart() {
        initializeComparisonsFiltersPart();
        initializeComparisonsListView();
        initializeComparisonInfoLabel();
    }

    private void initializeComparisonsFiltersPart() {
        var contest = Services.contest();

        var problemFilters = contest.getProblems().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        CheckBox::new
                ));

        problemToFilter.putAll(problemFilters);

        problemFiltersHBox.getChildren().addAll(
                problemToFilter.values()
        );

        statusToFilter.clear();
        for (Status status : Status.values()) {
            var statusFilter = new CheckBox(status.text);
            statusToFilter.put(status, statusFilter);
        }

        statusFiltersHBox.getChildren().addAll(statusToFilter.values());

        minSimilaritySpinner.setEditable(true);
        minSimilaritySpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        0, 1, 0.9, 0.05
                )
        );

        firstParticipantIdFilterTextField.setEditable(true);

        filterComparisonsButton.setOnAction(actionEvent -> updateComparisonsListView());

        problemToFilter.values().forEach(filter -> filter.setSelected(true));
        statusToFilter.values().forEach(filter -> filter.setSelected(true));
    }

    private void initializeComparisonsListView() {
        Services.comparisons().afterInitialization(() -> Platform.runLater(() -> {
                comparisonInfoLabel.setText("Данные загружены");

                updateComparisonsListView();

                selectComparison(0);
            })
        );

        initializeComparisonsListViewSelectionModel();

        initializeParticipantPairsListViewSelectionModel();

        initializeIpListViewSelectionModel();

        initializeClustersListViewSelectionModel();
    }

    private void initializeComparisonsListViewSelectionModel() {
        var selectionModel = comparisonsListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedIndexProperty().addListener(
                (observableValue, oldComparisonIndex, newComparisonIndex)
                        -> selectComparison(newComparisonIndex.intValue())
        );

        comparisonsListView.setCellFactory(comparisonListView -> new ComparisonListCell());
    }

    private void initializeParticipantPairsListViewSelectionModel() {
        var selectionModel = participantPairsListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedIndexProperty().addListener(
                (observableValue, oldParticipantPairsIndex, newParticipantPairsIndex)
                        -> selectParticipantPairs(newParticipantPairsIndex.intValue())
        );
    }

    private void initializeIpListViewSelectionModel() {
        var selectionModel = ipFiltersListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedIndexProperty().addListener(
                (observableValue, oldIpIndex, newIpIndex)
                        -> selectIp(newIpIndex.intValue())
        );
    }

    private void initializeClustersListViewSelectionModel() {
        var selectionModel = clusterFiltersListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedIndexProperty().addListener(
                (observableValue, oldIpIndex, newIpIndex)
                        -> selectCluster(newIpIndex.intValue())
        );
    }

    private static class ComparisonListCell extends TextFieldListCell<Comparison> {

        private static final EnumMap<Status, Color> statusToColor;

        static {
            statusToColor = new EnumMap<>(Status.class);

            statusToColor.put(Status.NOT_SEEN, Color.WHITE);
            statusToColor.put(Status.IGNORED, Color.GREEN);
            statusToColor.put(Status.UNKNOWN, Color.YELLOW);
            statusToColor.put(Status.AUTOPLAGIAT, Color.BLUEVIOLET);
            statusToColor.put(Status.PLAGIAT, Color.RED);
        }

        public ComparisonListCell() {
            super();
        }

        @Override
        public void updateItem(Comparison comparison, boolean isEmpty) {
            super.updateItem(comparison, isEmpty);

            if (!isEmpty) {
                var verification = Services.verification();

                var actualStatus = verification.getStatus(comparison);
                var expectedStatus = verification.getExpectedStatus(comparison);

                var actualColor = statusToColor.get(actualStatus);
                var expectedColor = statusToColor.get(expectedStatus);

                setBorder(
                        new Border(
                                new BorderStroke(
                                        actualColor,
                                        expectedColor,
                                        expectedColor,
                                        actualColor,
                                        BorderStrokeStyle.SOLID,
                                        BorderStrokeStyle.SOLID,
                                        BorderStrokeStyle.SOLID,
                                        BorderStrokeStyle.SOLID,
                                        CornerRadii.EMPTY,
                                        new BorderWidths(5),
                                        Insets.EMPTY
                                )
                        )
                );
            } else {
                setBorder(new Border(new BorderStroke(Color.WHITE, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths.EMPTY)));
            }
        }
    }

    private Predicate<Comparison> getParticipantFilter() {
        boolean useFirstParticipant = useFirstParticipantFilterCheckBox.isSelected();
        boolean useSecondParticipant = useSecondParticipantFilterCheckBox.isSelected();

        if (useFirstParticipant && useSecondParticipant) {
            return Services.comparisons().withParticipants(
                    firstParticipantIdFilterTextField.getText(),
                    secondParticipantIdFilterTextField.getText()
            );
        } else if (useFirstParticipant) {
            return Services.comparisons().withParticipant(
                    firstParticipantIdFilterTextField.getText()
            );
        } else if (useSecondParticipant) {
            return Services.comparisons().withParticipant(
                    secondParticipantIdFilterTextField.getText()
            );
        } else {
            return (comparison) -> true;
        }
    }

    private void updateComparisonsListView() {
        double minSimilarity = minSimilaritySpinner.getValue();

        // TODO add class for valueToFilter
        var expectedProblems = problemToFilter.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableList());

        var expectedStatuses = statusToFilter.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableList());

        var comparisons = Services.comparisons();
        var verification = Services.verification();

        Predicate<Comparison> participantFilter = getParticipantFilter();

        Predicate<Comparison> solutionFilter = useSolutionFilterCheckBox.isSelected()
                ? comparisons.withSolution(solutionIdFilterTextField.getText())
                : (comparison) -> true;

        Predicate<Comparison> substringFilter = useSubstringFilterCheckBox.isSelected()
                ? comparisons.withSubstring(substringFilterTextField.getText())
                : (comparison) -> true;

        Predicate<Comparison> atLeastOneAcFilter = (comparison) ->
                Verdict.AC == comparison.left.verdict
                        || Verdict.AC == comparison.right.verdict;

        var predicate = StreamUtils.and(
                comparisons.moreThan(minSimilarity),
                comparisons.forProblem(expectedProblems),
                verification.withStatus(expectedStatuses),
                participantFilter,
                solutionFilter,
                substringFilter,
                atLeastOneAcFilter
        );

        updateComparisonsListView(predicate);
    }

    private void updateComparisonsListView(Predicate<Comparison> predicate) {
        var filteredComparisons = Services.comparisons().filtered(predicate);

        participantsToComparisons.clear();
        filteredComparisons.forEach(curComparison -> {
            var participants = curComparison.toParticipantIds();
            var participantsComparisons = participantsToComparisons.computeIfAbsent(
                    participants,
                    (ids) -> FXCollections.observableArrayList()
            );

            participantsComparisons.add(curComparison);
        });

        participantPairsListView.setItems(
                FXCollections.observableArrayList(participantsToComparisons.keySet())
        );
        participantPairsListView.getSelectionModel().selectFirst();

        ipToComparisons.clear();
        filteredComparisons.forEach(curComparison -> {
            if (curComparison.left.ip.equals(curComparison.right.ip)) {
                var ip = curComparison.left.ip;
                var curIpComparisons = ipToComparisons.computeIfAbsent(
                        ip,
                        (ipKey) -> FXCollections.observableArrayList()
                );

                curIpComparisons.add(curComparison);
            }
        });

        ipFiltersListView.setItems(
                FXCollections.observableArrayList(ipToComparisons.keySet())
        );

        var clusters = Services.verification();

        clusterToComparisons.clear();
        filteredComparisons.forEach(curComparison ->
            clusters.getCluster(curComparison).ifPresent(cluster -> {
                var curClusterComparisons = clusterToComparisons.computeIfAbsent(
                    cluster,
                    (clusterKey) -> FXCollections.observableArrayList()
                );

                curClusterComparisons.add(curComparison);
            })
        );

        clusterFiltersListView.setItems(
                FXCollections.observableArrayList(clusterToComparisons.keySet())
        );
    }

    public void fullSelectComparison(Optional<Comparison> selectedComparison) {
        var items = comparisonsListView.getItems();
        if (items.isEmpty()) return;

        comparisonsListView.getSelectionModel().select(
                selectedComparison.orElse(items.get(0))
        );
    }

    private void selectParticipantPairs(int participantPairsIndex) {
        if (participantPairsIndex < 0 || participantPairsListView.getItems().size() <= participantPairsIndex) return;

        var participantsPair = participantPairsListView.getItems().get(participantPairsIndex);

        this.participantsPairIndex = participantPairsIndex;
        comparisonsListView.setItems(
                participantsToComparisons.get(participantsPair)
        );

        if (comparisonsListView.getItems().size() > 0) {
            selectComparison(0);
        }
    }

    private void selectIp(int ipIndex) {
        if (ipIndex < 0 || ipFiltersListView.getItems().size() <= ipIndex) return;

        var ip = ipFiltersListView.getItems().get(ipIndex);

        comparisonsListView.setItems(
                ipToComparisons.get(ip)
        );

        if (ipFiltersListView.getItems().size() > 0) {
            selectComparison(0);
        }
    }

    private void selectCluster(int clusterIndex) {
        if (clusterIndex < 0 || clusterFiltersListView.getItems().size() <= clusterIndex) return;

        var cluster = clusterFiltersListView.getItems().get(clusterIndex);

        comparisonsListView.setItems(
                clusterToComparisons.get(cluster)
        );

        if (clusterFiltersListView.getItems().size() > 0) {
            selectComparison(0);
        }
    }

    private void selectComparison(int comparisonIndex) {
        if (comparisonIndex < 0 || comparisonsListView.getItems().size() <= comparisonIndex) return;

        this.comparisonIndex = comparisonIndex;
        this.comparison = comparisonsListView.getItems().get(comparisonIndex);
        comparisonsListView.getSelectionModel().select(comparison);

        showSelectedComparison();
    }

    private void showSelectedComparison() {
        if (null == comparison) return;

        var clusterOptional = Services.verification().getCluster(comparison);
        goToClusterButton.setDisable(clusterOptional.isEmpty());

        clusterOptional.ifPresent(cluster -> clusterCommentsTextArea.setText(
                cluster.commentsToText(false))
        );

        comparisonInfoLabel.setText(comparison.getProblemName());

        var verification = Services.verification();

        Status actualStatus = verification.getStatus(comparison);
        Status expectedStatus = verification.getExpectedStatus(comparison);

        String expectedStatusText = (Status.NOT_SEEN == expectedStatus)
                ? "Непонятно"
                : expectedStatus.text;

        comparisonStatusLabel.setText(
                String.format("%s%n(%s)", actualStatus.text, expectedStatusText)
        );

        showParticipantSolution(
                comparison.left,
                leftCodeInfoLabel,
                leftParticipantInfoTextArea,
                leftCodeTextArea
        );
        showParticipantSolution(
                comparison.right,
                rightCodeInfoLabel,
                rightParticipantInfoTextArea,
                rightCodeTextArea
        );
    }

    private void showParticipantSolution(Solution solution,
                                         Label codeInfoLabel,
                                         TextArea participantInfoTextArea,
                                         TextArea codeTextArea) {
        var participant = solution.getParticipant();

        participantInfoTextArea.setText(
                Services.contest().getInfo(participant) + "\n"
                + participant.toText() + "\n" + "\n"
                + solution.getFullLink()
        );

        String codeInfoText = solution.verdict + "\t" + solution.score;

        if (!blindModeCheckBox.selectedProperty().getValue()) {
            codeInfoText += "\t" + solution.getDateTimeString();
        }

        codeInfoLabel.setText(codeInfoText);

        codeTextArea.setText(solution.getProgram().code);
    }

    private void initializeComparisonInfoLabel() {
        comparisonInfoLabel.setText("Данные загружаются");
    }

    private void initializeDiffPart() {
        initializeParticipantPart();
        initializeCodePart();
        initializeVerificationPart();
    }

    private void initializeParticipantPart() {
        leftParticipantInfoTextArea.setEditable(false);
        rightParticipantInfoTextArea.setEditable(false);

        double participantAreaHeight = FxmlUtils.getScreenSize().getHeight() / 10.0;

        leftParticipantInfoTextArea.setPrefHeight(participantAreaHeight);
        rightParticipantInfoTextArea.setPrefHeight(participantAreaHeight);
    }

    private void initializeCodePart() {
        double codeAreaHeight = FxmlUtils.getScreenSize().getHeight() * 2.0 / 3.0;

        leftCodeTextArea.setPrefHeight(codeAreaHeight);
        rightCodeTextArea.setPrefHeight(codeAreaHeight);

        leftCodeTextArea.scrollTopProperty().bindBidirectional(rightCodeTextArea.scrollTopProperty());

        leftCodeTextArea.setEditable(false);
        rightCodeTextArea.setEditable(false);
    }

    private void initializeVerificationPart() {
        initializeVerdictButtons();
        initializeStatusPart();
        initializeClusterPart();
        initializeAllForAuthorsPart();
    }

    private void initializeAllForAuthorsPart() {
        showAllComparisonsForAuthorsButton.setOnAction(event -> {
            if (null == comparison) return;

            Predicate<Comparison> predicate = Services.comparisons().withAuthorsOf(comparison);

            updateComparisonsListView(predicate);
        });
    }

    private void initializeStatusPart() {

    }

    private void initializeClusterPart() {
        goToClusterButton.setOnAction(this::goToClusterAction);
    }

    private void initializeVerdictButtons() {
        plagiatButton.setOnAction(this::plagiatAction);
        unknownButton.setOnAction(this::unknownAction);
        ignoreButton.setOnAction(this::ignoreAction);
    }

    private void updateStatus(Status status) {
        Services.verification().setStatus(comparison, status);
        comparisonStatusLabel.setText(status.text);
        selectComparison(comparisonIndex);
    }

    public void plagiatAction(ActionEvent event) {
        updateStatus(Status.PLAGIAT);
        moveNext();
    }

    public void unknownAction(ActionEvent event) {
        updateStatus(Status.UNKNOWN);
        moveNext();
    }

    public void ignoreAction(ActionEvent event) {
        updateStatus(Status.IGNORED);
        moveNext();
    }

    public void goToClusterAction(ActionEvent event) {
        if (null == comparison) return;

        Services.verification().getCluster(comparison)
                .ifPresent(mainController::goTo);
    }

    public void showFromCluster(Cluster cluster) {
        var clusterFiltersSelectionModel = clusterFiltersListView.getSelectionModel();

        clusterFiltersSelectionModel.clearSelection();
        clusterFiltersSelectionModel.select(cluster);

        problemToFilter.values().forEach(filter -> filter.setSelected(false));
        problemToFilter.get(cluster.getProblemName()).setSelected(true);

        for (var statusFilter : statusToFilter.values()) {
            statusFilter.setSelected(true);
        }

        updateComparisonsListView();
    }
}
