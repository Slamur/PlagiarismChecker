package com.slamur.plagiarism.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.slamur.plagiarism.service.Service;

abstract class ServiceBase implements Service {

    protected List<Runnable> afterInitializationRunnables;
    protected volatile boolean initialized;

    protected ServiceBase() {
        this.afterInitializationRunnables = Collections.synchronizedList(
                new ArrayList<>()
        );

        this.initialized = false;
    }

    @Override
    public void afterInitialization(Runnable runnable) {
        if (!initialized) {
            afterInitializationRunnables.add(runnable);
        } else {
            runnable.run();
        }
    }

    protected abstract void initializeOnly();

    @Override
    public void initialize() {
        new Thread(() -> {
            initializeOnly();

            initialized = true;

            for (var runnable : afterInitializationRunnables) {
                runnable.run();
            }
        }).start();
    }
}