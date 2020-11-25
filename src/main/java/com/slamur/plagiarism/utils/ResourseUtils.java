package com.slamur.plagiarism.utils;

import java.net.URL;

public class ResourseUtils {

    public static Class<?> getBaseClass() {
        return ResourseUtils.class;
    }

    public static URL loadResource(String resourceName) {
        return getBaseClass().getResource(resourceName);
    }
}
