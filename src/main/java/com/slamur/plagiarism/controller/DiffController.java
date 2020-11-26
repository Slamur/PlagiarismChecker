package com.slamur.plagiarism.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.FxmlUtils;
import com.slamur.plagiarism.utils.StreamUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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

    @FXML public CheckBox blindModeCheckBox;

    @FXML public Button prevComparisonButton;

    @FXML public Button nextComparisonButton;

    private int comparisonIndex;
    private Comparison comparison;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

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
        comparisonsListView.setVisible(!isBlindMode);
        leftParticipantInfoTextArea.setVisible(!isBlindMode);
        rightParticipantInfoTextArea.setVisible(!isBlindMode);
    }

    private void initializeComparisonInfoPart() {
        initializeComparisonsListView();
        initializeComparisonInfoLabel();
    }

    private void initializeComparisonsListView() {
        Services.comparisons().afterInitialization(() -> {
                Platform.runLater(() -> {
                    comparisonInfoLabel.setText("Данные загружены");

//                    AlertUtils.information("Данные о сравнениях восстановлены");

                    updateComparisonsListView();

                    selectComparison(0);
                });
            }
        );

        var selectionModel = comparisonsListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedIndexProperty().addListener(
                (observableValue, oldComparisonIndex, newComparisonIndex)
                        -> selectComparison(newComparisonIndex.intValue())
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

    private void selectComparison(int comparisonIndex) {
        if (-1 == comparisonIndex) return;
        if (comparisonsListView.getItems().isEmpty()) return;

        this.comparisonIndex = comparisonIndex;
        this.comparison = comparisonsListView.getItems().get(comparisonIndex);

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

        double participantAreaHeight = FxmlUtils.getScreenSize().height / 10.0;

        leftParticipantInfoTextArea.setPrefHeight(participantAreaHeight);
        rightParticipantInfoTextArea.setPrefHeight(participantAreaHeight);
    }

    private void initializeCodePart() {
        double codeAreaHeight = FxmlUtils.getScreenSize().height * 2.0 / 3.0;

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
