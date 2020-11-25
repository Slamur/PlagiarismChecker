package com.slamur.plagiarism.utils;

import java.util.Arrays;
import java.util.function.Predicate;

public class StreamUtils {

    @SafeVarargs
    public static <T> Predicate<T> and(Predicate<T>... predicates) {
        return Arrays.stream(predicates).reduce(Predicate::and).orElse((value) -> false);
    }
}
