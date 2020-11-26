package com.slamur.plagiarism.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface Service {

    void initialize();

    void afterInitialization(Runnable runnable);

    abstract class ServiceImpl implements Service {

        protected List<Runnable> afterInitializationRunnables;
        protected volatile boolean initialized;

        protected ServiceImpl() {
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
}
