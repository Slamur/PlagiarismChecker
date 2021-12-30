package com.slamur.plagiarism.utils;

import java.io.IOException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class AlertUtils {

    public static void error(String message, Exception e) {
        alert(Alert.AlertType.ERROR, message, e);
    }

    public static void error(String message) {
        alert(Alert.AlertType.ERROR, message);
    }

    public static void warning(String message, Exception e) {
        alert(Alert.AlertType.WARNING, message, e);
    }

    public static void warning(String message) {
        alert(Alert.AlertType.WARNING, message);
    }

    public static void information(String message) {
        alert(Alert.AlertType.INFORMATION, message);
    }

    public static void alert(Alert.AlertType alertType, String message, Exception e) {
        alert(alertType, message + ": " + e.getMessage());

        e.printStackTrace();
    }

    public static void alert(Alert.AlertType alertType, String message) {
        Platform.runLater(() -> new Alert(
                alertType,
                message,
                ButtonType.OK
        ).show());
    }
}
