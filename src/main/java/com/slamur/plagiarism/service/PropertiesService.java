package com.slamur.plagiarism.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.IOUtils;

public class PropertiesService {

    public static final Properties appProperties;

    static {
        appProperties = new Properties();

        try {
            appProperties.load(
                    IOUtils.createReader(new FileInputStream(new File("app.properties")))
            );
        } catch (IOException e) {
            AlertUtils.warning("Проблема при загрузке настроек", e);
        }
    }
}
