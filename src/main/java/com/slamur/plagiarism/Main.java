package com.slamur.plagiarism;

import com.slamur.plagiarism.service.Services;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Services.fxml().showMainStage();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
