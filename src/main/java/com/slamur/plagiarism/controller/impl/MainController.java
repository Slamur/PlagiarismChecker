package com.slamur.plagiarism.controller.impl;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import com.slamur.plagiarism.controller.Controller;
import com.slamur.plagiarism.model.verification.Cluster;
import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;

public class MainController implements Controller {

    public static final String DIFF_TITLE = "Сверка решений";
    public static final String CLUSTER_TITLE = "Кластеры списавших";
    public static final String PROPERTIES_TITLE = "Настройки";

    @FXML public TabPane tabPane;

    @FXML public Tab propertiesTab;

    @FXML public PropertiesController propertiesController;

    @FXML public Tab diffTab;

    @FXML public DiffController diffController;

    @FXML public Tab clusterTab;

    @FXML public ClusterController clusterController;

    @FXML public MenuItem saveRawDataMenuItem;

    @FXML public MenuItem loadRawDataMenuItem;

    @FXML public MenuItem saveReportMenuItem;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTabPane();
        initalizeControllers();
        initializeMenu();

        selectTab(diffTab);
    }

    private void initializeTabPane() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    private void initalizeControllers() {
        initializeTabController(propertiesController, propertiesTab, PROPERTIES_TITLE);
        initializeTabController(diffController, diffTab, DIFF_TITLE);
        initializeTabController(clusterController, clusterTab, CLUSTER_TITLE);
    }

    private void initializeTabController(TabController tabController, Tab tab, String title) {
        tabController.setMainController(this);
        tab.setText(title);
    }

    private void selectTab(Tab tab) {
        tabPane.getSelectionModel().select(tab);
    }

    public void showComparisonsFrom(Cluster cluster) {
        diffController.showFromCluster(cluster);
    }

    public void goTo(Optional<Comparison> comparison) {
        diffController.fullSelectComparison(comparison);
        selectTab(diffTab);
    }

    public void goTo(Cluster cluster) {
        clusterController.fullSelectCluster(cluster);
        selectTab(clusterTab);
    }

    private void initializeMenu() {
        saveRawDataMenuItem.setOnAction(this::saveRawDataAction);
        loadRawDataMenuItem.setOnAction(this::loadRawDataAction);
        saveReportMenuItem.setOnAction(this::saveReportAction);
    }

    private void saveRawDataAction(ActionEvent event) {
        try {
            Services.verification().saveDataToFile();
            AlertUtils.information(
                    "Данные сохранены"
            );
        } catch (Exception e) {
            AlertUtils.error(
                    "Ошибка при сохранении данных", e
            );
        }
    }

    private void loadRawDataAction(ActionEvent event) {
        var patchFile = new FileChooser()
                .showOpenDialog(tabPane.getScene().getWindow());

        if (null != patchFile) {
            try {
                Services.verification().loadPatchFrom(patchFile);
                AlertUtils.information(
                        "Патч данных загружен"
                );
            } catch (Exception e) {
                AlertUtils.error(
                        "Ошибка при загрузке патча данных", e
                );
            }
        }
    }

    private void saveReportAction(ActionEvent event) {
        try {
            Services.verification().saveReportToFile();
            AlertUtils.information(
                    "Проткоол сохранён"
            );
        } catch (Exception e) {
            AlertUtils.error(
                    "Ошибка при сохранении протокола", e
            );
        }
    }
}
