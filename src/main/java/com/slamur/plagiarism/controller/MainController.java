package com.slamur.plagiarism.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.service.FxmlStageService;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController implements Controller {

    @FXML public TabPane tabPane;

    @FXML public Tab diffTab;

    @FXML public DiffController diffController;

    @FXML public Tab clusterTab;

    @FXML public ClusterController clusterController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        diffController.setMainController(this);
        clusterController.setMainController(this);

        diffTab.setText(FxmlStageService.DIFF_TITLE);
        clusterTab.setText(FxmlStageService.CLUSTER_TITLE);

        selectTab(diffTab);

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    private void selectTab(Tab tab) {
        tabPane.getSelectionModel().select(tab);
    }

    public void goToComparison(Comparison comparison) {
        selectTab(diffTab);
        diffController.fullSelectComparison(comparison);
    }

    public void goToCluster(Cluster cluster) {
        selectTab(clusterTab);
        clusterController.fullSelectCluster(cluster);
    }
}
