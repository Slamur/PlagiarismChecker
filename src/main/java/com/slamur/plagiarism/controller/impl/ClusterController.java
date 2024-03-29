package com.slamur.plagiarism.controller.impl;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.FxmlUtils;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;

public class ClusterController extends TabController {

    @FXML public ListView<Cluster> clustersListView;

    @FXML public TextArea clusterInfoTextArea;

    @FXML public TextArea commentTextArea;

    @FXML public Button saveCommentButton;

    @FXML public ListView<Solution> leftSolutionListView;

    @FXML public ListView<Solution> rightSolutionListView;

    @FXML public Label comparisonStatusLabel;

    @FXML public Button goToComparisonsButton;

    private Cluster cluster;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeClusterPart();
        initializeCommentPart();
        initializeSolutionsPart();

        this.cluster = null;
    }

    private void initializeClusterPart() {
        initializeClustersListView();
        initializeClusterInfoTextArea();
    }

    private void initializeClustersListView() {
        clustersListView.setItems(
                Services.verification().getClusters()
        );

        var selectionModel = clustersListView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener(
                (observableValue, oldCluster, newCluster) -> selectCluster(newCluster)
        );
    }

    private void initializeClusterInfoTextArea() {
        clusterInfoTextArea.setPrefHeight(
                FxmlUtils.getScreenSize().getHeight()
        );
    }

    public void fullSelectCluster(Cluster cluster) {
        clustersListView.getSelectionModel().select(cluster);
    }

    private void selectCluster(Cluster cluster) {
        if (null == cluster) return;
        this.cluster = cluster;

        showSelectedCluster();
    }

    private void showSelectedCluster() {
        if (null == cluster) return;

        clusterInfoTextArea.setText(
                cluster.toText()
        );

        commentTextArea.setText(
                Services.verification().getJuryComment(cluster)
        );

        setSolutions(cluster, leftSolutionListView);
        setSolutions(cluster, rightSolutionListView);
    }

    private static void setSolutions(Cluster cluster, ListView<Solution> solutionListView) {
        solutionListView.setItems(
                FXCollections.observableList(
                        cluster.getSolutions()
                )
        );

        solutionListView.getSelectionModel().select(0);
    }

    private void initializeCommentPart() {
        saveCommentButton.setOnAction(this::saveCommentAction);
    }

    private void saveCommentAction(ActionEvent event) {
        if (null == cluster) return;

        String comment = commentTextArea.getText().replace("\n", "; ");
        Services.verification().setJuryComment(
                cluster, comment
        );

        clusterInfoTextArea.setText(cluster.toText());
    }

    private void initializeSolutionsPart() {
        initializeSolutionListView(leftSolutionListView);
        initializeSolutionListView(rightSolutionListView);

        goToComparisonsButton.setOnAction(this::goToComparisonAction);
    }

    private void initializeSolutionListView(ListView<Solution> solutionListView) {
        solutionListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        solutionListView.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldParticipant, newParticipant) -> updateComparisonStatusAction()
        );

        solutionListView.setCellFactory(listView -> new ListCell<>(){
            @Override
            protected void updateItem(Solution solution, boolean empty) {
                super.updateItem(solution, empty);

                if(empty || null == solution){
                    setText("");
                } else {
                    setText(solution.getParticipant().toString());
                }
            }
        });
    }

    private Optional<Comparison> getSelectedComparison() {
        var left = leftSolutionListView.getSelectionModel().getSelectedItem();
        var right = rightSolutionListView.getSelectionModel().getSelectedItem();

        return  Services.verification().getComparison(left, right);
    }

    private void updateComparisonStatusAction() {
        updateComparisonStatus(getSelectedComparison());
    }

    private void updateComparisonStatus(Optional<Comparison> comparisonOptional) {
        comparisonOptional.ifPresentOrElse(
                (comparison) -> {
                    var verification = Services.verification();

                    var actualStatus = verification.getStatus(comparison);
                    var expectedStatus = verification.getExpectedStatus(comparison);

                    comparisonStatusLabel.setText(
                            String.format("%s%n(%s)", actualStatus.text, expectedStatus.text)
                    );
                },
                () -> comparisonStatusLabel.setText("")
        );
    }

    public void goToComparisonAction(ActionEvent event) {
        if (null == cluster) return;
        if (cluster.size() < 2) return;

        mainController.showComparisonsFrom(cluster);
        mainController.goTo(getSelectedComparison());
    }

}
