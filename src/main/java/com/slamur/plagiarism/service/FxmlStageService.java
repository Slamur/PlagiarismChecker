package com.slamur.plagiarism.service;

import javafx.scene.Scene;
import static com.slamur.plagiarism.utils.FxmlUtils.*;

public class FxmlStageService {

    public static final String MAIN_SCENE_NAME = "main", MAIN_TITLE = "Поиск списавших";
    public static final String DIFF_SCENE_NAME = "diff", DIFF_TITLE = "Сверка решений";
    public static final String CLUSTER_SCENE_NAME = "diff", CLUSTER_TITLE = "Кластеры списавших";

//    public static void showDiffStage() {
//        Scene scene = createScene(DIFF_SCENE_NAME);
//        showStage(scene, DIFF_TITLE);
//    }
//
//    public static void showClusterStage() {
//        Scene scene = createScene(CLUSTER_SCENE_NAME);
//        showStage(scene, CLUSTER_TITLE);
//    }

    public static void showMainStage() {
        Scene scene = createScene(MAIN_SCENE_NAME);
        showStage(scene, MAIN_TITLE);
    }
}
