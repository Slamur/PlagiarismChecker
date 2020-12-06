package com.slamur.plagiarism.utils;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomUtils {

    private static final Random random;

    static {
        random = new Random();
    }

    public static void setSeed(long seed) {
        random.setSeed(seed);
    }

    public static void shuffle(List<?> values, long seed) {
        setSeed(seed);
        Collections.shuffle(values, random);
    }
}
