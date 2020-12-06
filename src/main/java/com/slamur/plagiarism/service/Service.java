package com.slamur.plagiarism.service;

public interface Service {

    void initialize();

    void afterInitialization(Runnable runnable);
}
