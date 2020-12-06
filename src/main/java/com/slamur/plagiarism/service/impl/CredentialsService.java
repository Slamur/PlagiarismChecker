package com.slamur.plagiarism.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.slamur.plagiarism.model.parsing.Credentials;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;

public class CredentialsService extends ServiceBase {

    public CredentialsService() {

    }

    @Override
    protected void initializeOnly() {
        var properties = Services.properties();

        try {
            String login = properties.getLogin();
            if (login.isEmpty()) {
                throw new IOException("Логин не найден");
            }

            String password = properties.getPassword();
            if (password.isEmpty()) {
                throw new IOException("Пароль не найден");
            }
        } catch (IOException e) {
            AlertUtils.warning("Логин-пароль не подгрузились, работа только оффлайн", e);
        }
    }

    public Map<String, String> getCredentialCookies() {
        var properties = Services.properties();

        var credentials = new Credentials(
                properties.getLogin(), properties.getPassword()
        );

        Map<String, String> loginCookies = new HashMap<>();
        loginCookies.put("sessionID", credentials.login + "%09" + credentials.password);

        return loginCookies;
    }
}
