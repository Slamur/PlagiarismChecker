package com.slamur.plagiarism.controller.impl;

import com.slamur.plagiarism.controller.Controller;

abstract class TabController implements Controller {

    protected MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
