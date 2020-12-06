package com.slamur.plagiarism.service.impl;

import static com.slamur.plagiarism.utils.FxmlUtils.createScene;
import static com.slamur.plagiarism.utils.FxmlUtils.showStage;

public class FxmlStageService extends ServiceBase {

    public static final String MAIN_SCENE_NAME = "main", MAIN_TITLE = "Поиск списавших";

    public FxmlStageService() {

    }

    @Override
    protected void initializeOnly() {

    }

    public void showMainStage() {
        var scene = createScene(MAIN_SCENE_NAME);
        var stage = showStage(scene, MAIN_TITLE);
        stage.setMaximized(true);
    }
}
