package com.slamur.plagiarism.service.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import com.slamur.plagiarism.utils.AlertUtils;

public class PropertiesService extends ServiceBase  {

    private static final String propertiesFileName = "app.properties";

    private static final String loginProperty = "login", passwordProperty = "password";
    private static final String juryProperty = "jury";

    private final Properties appProperties;

    public PropertiesService() {
        this.appProperties = new Properties();
    }

    @Override
    protected void initializeOnly() {
        var appPropertiesFile = new File(propertiesFileName);
        if (!appPropertiesFile.exists()) {
            try {
                if (!appPropertiesFile.createNewFile()) {
                    throw new IOException("Файл настроек не создан, но исключение не выпало");
                }
            } catch (IOException e) {
                AlertUtils.error("Проблема при создании файла настроек", e);
            }
        }

        try (var in = new FileReader(appPropertiesFile)){
            appProperties.load(in);
        } catch (IOException ignored) {
        }
    }

    public String getLogin() {
        return appProperties.getProperty(loginProperty, "");
    }

    public String getPassword() {
        return appProperties.getProperty(passwordProperty, "");
    }

    public String getJury() {
        return appProperties.getProperty(juryProperty, "Jury" + this.hashCode());
    }

    public void saveProperties() {
        try (var out = new PrintWriter(propertiesFileName)) {
            appProperties.store(out, "Properties of jury " + getJury());
        } catch (IOException e) {
            AlertUtils.error("Проблема при сохранении настроек", e);
        }
    }

}
