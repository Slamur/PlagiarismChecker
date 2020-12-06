package com.slamur.plagiarism.controller.impl;

import java.net.URL;
import java.util.ResourceBundle;

import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class PropertiesController extends TabController {

    @FXML public TextField loginTextField;
    @FXML public TextField passwordTextField;
    @FXML public TextField juryTextField;
    @FXML public Button savePropertiesButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initCredentialsPart();
        initCommentsPart();
        initSavePart();
    }

    private void initCredentialsPart() {
        Services.properties().afterInitialization(() -> Platform.runLater(
                () -> {
                    loginTextField.setText(
                            Services.properties().getLogin()
                    );

                    passwordTextField.setText(
                            Services.properties().getPassword()
                    );
                }
            )
        );
    }

    private void initCommentsPart() {
        Services.properties().afterInitialization(() -> Platform.runLater(
                () -> juryTextField.setText(
                    Services.properties().getJury()
                )
            )
        );
    }

    private void initSavePart() {
        savePropertiesButton.setOnAction(this::savePropertiesAction);
    }

    private void savePropertiesAction(ActionEvent actionEvent) {
        var properties = Services.properties();

        properties.setLogin(loginTextField.getText());
        properties.setPassword(passwordTextField.getText());
        properties.setJury(juryTextField.getText());

        AlertUtils.information("Настройки изменены");

        properties.saveToFile();
    }
}
