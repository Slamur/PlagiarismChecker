package com.slamur.plagiarism.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.model.parsing.Solution;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.FxmlUtils;
import com.slamur.plagiarism.utils.StreamUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;

public class DiffController implements Controller {

    @FXML public ListView<Comparison> comparisonsListView;

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

    private Comparison comparison;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeComparisonInfoPart();
        initializeDiffPart();

        this.comparison = null;
    }

    private void initializeComparisonInfoPart() {
        initializeComparisonsListView();
        initializeComparisonInfoLabel();
    }

    private void initializeComparisonsListView() {
        Services.comparisons().afterInitialization(() -> {
                Platform.runLater(() -> comparisonInfoLabel.setText("Данные загружены"));
                updateComparisonsListView();
            }
        );

        var selectionModel = comparisonsListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener(
                (observableValue, oldComparison, newComparison) -> selectComparison(newComparison)
        );
    }

    private void updateComparisonsListView() {
        comparisonsListView.setItems(
                Services.comparisons().getComparisons(
                        StreamUtils.and(
                                Services.comparisons().moreThan(0.9),
                                Services.verification().isNot(Status.IGNORED)
                        )
                )
        );
    }

    public void fullSelectComparison(Comparison comparison) {
        comparisonsListView.getSelectionModel().select(comparison);
    }

    private void selectComparison(Comparison comparison) {
        if (null == comparison) return;
        this.comparison = comparison;

        showSelectedComparison();
    }

    private void showSelectedComparison() {
        var cluster = Services.verification().getCluster(comparison);
        goToClusterButton.setDisable(cluster.isEmpty());

        comparisonInfoLabel.setText(comparison.getProblemName());
        comparisonStatusLabel.setText(Services.verification().getStatus(comparison).text);

        showParticipantSolution(comparison.left, comparison.problemId, leftCodeInfoLabel, leftParticipantInfoTextArea, leftCodeTextArea);
        showParticipantSolution(comparison.right, comparison.problemId, rightCodeInfoLabel, rightParticipantInfoTextArea, rightCodeTextArea);
    }

    private static void showParticipantSolution(Participant participant,
                                                int problemId,
                                                Label codeInfoLabel,
                                                TextArea participantInfoTextArea,
                                                TextArea codeTextArea) {
        var solution = participant.solutions[problemId];

        participantInfoTextArea.setText(
                Services.contest().getInfo(participant) + "\n"
                + participant.toString() + "\n" + "\n"
                + solution.getFullLink()
        );

        codeInfoLabel.setText(
                solution.verdict + "\t"
                + solution.score + "\t"
                + solution.dateTime.format(Solution.DATE_TIME_FORMATTER));

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
    }

    private void initializeCodePart() {
        double codeAreaHeight = FxmlUtils.getScreenSize().height / 2.0;

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
    }

    public void plagiatAction(ActionEvent event) {
        updateStatus(Status.PLAGIAT);
    }

    public void unknownAction(ActionEvent event) {
        updateStatus(Status.UNKNOWN);
    }

    public void ignoreAction(ActionEvent event) {
        updateStatus(Status.IGNORED);
    }

    public void goToClusterAction(ActionEvent event) {
        if (null == comparison) return;

        Services.verification().getCluster(comparison)
                .ifPresent(mainController::goToCluster);
    }
}
