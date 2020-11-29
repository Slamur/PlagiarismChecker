package com.slamur.plagiarism.utils;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FxmlUtils {

    public static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    private static String toSceneFileName(String sceneName) {
        return String.format("/scene/%s_scene.fxml", sceneName);
    }

    public static FXMLLoader createLoader(String sceneName) {
        return new FXMLLoader(
                ResourseUtils.loadResource(
                        toSceneFileName(sceneName)
                )
        );
    }

    public static Parent createParent(String sceneName) {
        try {
            return createLoader(sceneName).load();
        } catch (IOException e) {
            AlertUtils.error("Ошибка при загрузке FXML", e);
            throw new IllegalArgumentException(e);
        }
    }

    public static Scene createScene(String sceneName) {
        Dimension screenSize = getScreenSize();
        return createScene(sceneName, screenSize.width, screenSize.height);
    }

    private static Scene createScene(String sceneName, int width, int height) {
        Parent root = createParent(sceneName);
        return new Scene(root, width, height);
    }

    public static Stage showStage(Scene scene, String title) {
        return showStage(new Stage(), scene, title);
    }

    public static Stage showStage(Stage stage, Scene scene, String title) {
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
        return stage;
    }

}
