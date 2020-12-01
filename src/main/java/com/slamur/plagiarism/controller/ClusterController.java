package com.slamur.plagiarism.controller;

import java.net.URL;
import java.util.EnumMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.model.verification.Status;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.FxmlUtils;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

public class ClusterController implements Controller {

    @FXML public ListView<Cluster> clustersListView;

    @FXML public TextArea clusterInfoTextArea;

    @FXML public TextArea commentTextArea;

    @FXML public Button saveCommentButton;

    @FXML public TableView<Participant> clusterComparisonsTableView;

    private Cluster cluster;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeClusterPart();
        initializeCommentPart();
        initializeClusterComparisonsPart();

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

        updateClusterComparisonsTableView();
    }

    private void initializeCommentPart() {
        saveCommentButton.setOnAction(this::saveCommentAction);
    }

    private void saveCommentAction(ActionEvent event) {
        if (null == cluster) return;

        String comment = commentTextArea.getText();
        Services.verification().setJuryComment(
                cluster, comment
        );

        clusterInfoTextArea.setText(cluster.toText());
    }

    private void initializeClusterComparisonsPart() {

    }

    private void updateClusterComparisonsTableView() {
        var participants = cluster.getParticipants();

        clusterComparisonsTableView.setItems(
                FXCollections.observableList(participants)
        );

        var columns = clusterComparisonsTableView.getColumns();
        columns.clear();

//        for (var columnParticipant : participants) {
//            var column = new ButtonColumn<Participant>("" + columnParticipant.id);
//
//            column.setCellFactory(tableColumn -> new ComparisonButtonCell(columnParticipant, rowParticipant -> {
//                goToComparison(rowParticipant, columnParticipant);
//            }));
//
//            columns.add(column);
//        }
    }

    private static final EnumMap<Status, Color> statusColors;

    static {
        statusColors = new EnumMap<>(Status.class);

        statusColors.put(Status.NOT_SEEN, Color.GRAY);
        statusColors.put(Status.IGNORED, Color.GREEN);
        statusColors.put(Status.UNKNOWN, Color.YELLOW);
        statusColors.put(Status.PLAGIAT, Color.RED);
    }

//    private class ComparisonButtonCell extends ButtonCell<Participant> {
//
//        private final Participant columnParticipant;
//
//        public ComparisonButtonCell(Participant columnParticipant, Consumer<Participant> consumer) {
//            super("?", consumer);
//            this.columnParticipant = columnParticipant;
//        }
//
//        @Override
//        protected void updateItem(Participant rowParticipant, boolean empty) {
//            super.updateItem(rowParticipant, empty);
//
//            getComparison(rowParticipant, columnParticipant)
//                    .ifPresentOrElse(
//                            comparison -> {
//                                Status expectedStatus = Services.verification().getExpectedStatus(comparison);
//                                Status actualStatus = Services.verification().getStatus(comparison);
//
////                                buttonColumnCell.setText("" + expectedStatus.name().charAt(0));
//
//                                Color actualColor = statusColors.get(actualStatus);
//                                setBackground(new Background(new BackgroundFill(actualColor, CornerRadii.EMPTY, null)));
//                            },
//                            () -> {
//                                setText("");
//                                setStyle("-fx-background-color: black ;");
//                            }
//                    );
//        }
//    }

    public Optional<Comparison> getComparison(Participant left, Participant right) {
        if (null == cluster) return Optional.empty();
        if (left == right) return Optional.empty();

        return Services.verification().getComparison(
                cluster, left, right
        );
    }

    public void goToComparison(Participant rowParticipant, Participant columnParticipant) {
        getComparison(rowParticipant, columnParticipant)
                .ifPresent(mainController::goToComparison);
    }

}
