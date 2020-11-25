package com.slamur.plagiarism.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.slamur.plagiarism.model.parsing.Credentials;
import com.slamur.plagiarism.utils.AlertUtils;

public class CredentialsService {

    private static final Credentials credentials;

    private static final String loginProperty = "login", passwordProperty = "password";

    static {
        var appProperties = PropertiesService.appProperties;

        try {
            String login = appProperties.getProperty(loginProperty, "");
            if (login.isEmpty()) {
                appProperties.setProperty(loginProperty, "");
                throw new IOException("Логин не найден");
            }

            String password = appProperties.getProperty(passwordProperty, "");
            if (password.isEmpty()) {
                appProperties.setProperty(passwordProperty, "");
                throw new IOException("Пароль не найден");
            }
        } catch (IOException e) {
            AlertUtils.warning("Логин-пароль не подгрузились, работа только оффлайн", e);
        }

        credentials = new Credentials(
                appProperties.getProperty("login"),
                appProperties.getProperty("password")
        );

    }

    public static Map<String, String> getCredentialCookies() {
        Map<String, String> loginCookies = new HashMap<>();
        loginCookies.put("sessionID", credentials.login + "%09" + credentials.password);

        return loginCookies;
    }
}
