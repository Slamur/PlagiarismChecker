package com.slamur.plagiarism.controller.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.ResourceBundle;

import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.FxmlUtils;
import com.slamur.plagiarism.utils.ModelUtils;
import com.slamur.plagiarism.utils.StreamUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DiffController extends TabController {

    @FXML public VBox comparisonsVBox;

    @FXML public ListView<Comparison> comparisonsListView;

    @FXML public HBox problemFiltersHBox;

    @FXML public HBox statusFiltersHBox;

    @FXML public Spinner<Double> minSimilaritySpinner;

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

    private int comparisonIndex;
    private Comparison comparison;

    private CheckBox[] problemFilters;
    private EnumMap<Status, CheckBox> statusFilters;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeComparisonInfoPart();
        initializeDiffPart();
        initializeBlindMode();
        initializeMovePart();

        this.comparison = null;
        this.comparisonIndex = -1;
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

        comparisonIndex = (comparisonIndex + comparisons.size() + shift) % comparisons.size();
        selectComparison(comparisonIndex);
    }

    private void initializeBlindMode() {
        blindModeCheckBox.selectedProperty().addListener(
                (observableValue, wasBlind, nowBlind) -> updateViewForMode(nowBlind)
        );

        blindModeCheckBox.selectedProperty().setValue(true);
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

        this.problemFilters = new CheckBox[contest.getProblemsCount()];
        for (int problem = 0; problem < problemFilters.length; ++problem) {
            var problemFilter = new CheckBox(ModelUtils.getProblemName(problem));
            problemFilters[problem] = problemFilter;

            problemFiltersHBox.getChildren().add(problemFilter);
        }

        this.statusFilters = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            var statusFilter = new CheckBox(status.text);
            statusFilters.put(status, statusFilter);

            statusFiltersHBox.getChildren().add(statusFilter);
        }

        minSimilaritySpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        0, 1, 0.9, 0.05
                )
        );

        filterComparisonsButton.setOnAction(actionEvent -> updateComparisonsListView());

        for (CheckBox problemFilter : problemFilters) {
            problemFilter.setSelected(true);
        }

        for (Status status : new Status[] { Status.NOT_SEEN }) {
            statusFilters.get(status).setSelected(true);
        }
    }

    private void initializeComparisonsListView() {
        Services.comparisons().afterInitialization(() -> Platform.runLater(() -> {
                comparisonInfoLabel.setText("Данные загружены");

                updateComparisonsListView();

                selectComparison(0);
            })
        );

        var selectionModel = comparisonsListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedIndexProperty().addListener(
                (observableValue, oldComparisonIndex, newComparisonIndex)
                        -> selectComparison(newComparisonIndex.intValue())
        );
    }

    private void updateComparisonsListView() {
        double minSimilarity = minSimilaritySpinner.getValue();

        List<Integer> expectedProblems = new ArrayList<>();
        for (int problem = 0; problem < problemFilters.length; ++problem) {
            if (problemFilters[problem].isSelected()) {
                expectedProblems.add(problem);
            }
        }

        List<Status> expectedStatuses = new ArrayList<>();
        for (var statusEntry : statusFilters.entrySet()) {
            if (statusEntry.getValue().isSelected()) {
                expectedStatuses.add(statusEntry.getKey());
            }
        }

        var comparisons = Services.comparisons();

        comparisonsListView.setItems(
                comparisons.filtered(
                        StreamUtils.and(
                                comparisons.moreThan(minSimilarity),
                                comparisons.forProblem(expectedProblems),
                                Services.verification().withStatus(expectedStatuses)
                        )
                )
        );
    }

    public void fullSelectComparison(Comparison comparison) {
        comparisonsListView.getSelectionModel().select(comparison);
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

        var cluster = Services.verification().getCluster(comparison);
        goToClusterButton.setDisable(cluster.isEmpty());

        comparisonInfoLabel.setText(comparison.getProblemName());
        comparisonStatusLabel.setText(Services.verification().getStatus(comparison).text);

        showParticipantSolution(comparison.left, comparison.problemId, leftCodeInfoLabel, leftParticipantInfoTextArea, leftCodeTextArea);
        showParticipantSolution(comparison.right, comparison.problemId, rightCodeInfoLabel, rightParticipantInfoTextArea, rightCodeTextArea);
    }

    private void showParticipantSolution(Participant participant,
                                         int problemId,
                                         Label codeInfoLabel,
                                         TextArea participantInfoTextArea,
                                         TextArea codeTextArea) {
        var solution = participant.solutions[problemId];

        participantInfoTextArea.setText(
                Services.contest().getInfo(participant) + "\n"
                + participant.toText() + "\n" + "\n"
                + solution.getFullLink()
        );

        String codeInfoText = solution.verdict + "\t"
                + solution.score;

        if (!blindModeCheckBox.selectedProperty().getValue()) {
            codeInfoText += "\t" + solution.getDateTimeString();
        }

        codeInfoLabel.setText(codeInfoText);

        codeTextArea.setText(solution.code);
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
        updateComparisonsListView();
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
        move(0);
    }

    public void goToClusterAction(ActionEvent event) {
        if (null == comparison) return;

        Services.verification().getCluster(comparison)
                .ifPresent(mainController::goToCluster);
    }
}
