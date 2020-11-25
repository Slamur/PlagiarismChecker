package com.slamur.plagiarism.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class IOUtils {
    public static final String RUSSIAN_ENCODING = "cp1251";

    public static InputStreamReader createReader(InputStream in) throws UnsupportedEncodingException {
        return new InputStreamReader(in, RUSSIAN_ENCODING);
    }
}
