package com.slamur.plagiarism.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class RequestUtils {

    public static final String GET = "GET", POST = "POST";

    public static String request(String type, 
                                 String url, 
                                 String domain, 
                                 Map<String, String> parameters, 
                                 Map<String, String> cookies) throws IOException {
        if (!url.startsWith("/")) url = "/" + url;
        if (!url.startsWith(domain)) url = domain + url;
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        //add request header
        connection.setRequestMethod(type);

        setCookies(cookies, connection);

        sendParameters(parameters, connection);

        return IOUtils.readFrom(connection.getInputStream(), (in) -> {
            StringBuilder response = new StringBuilder();

            for (String inputLine; (inputLine = in.readLine()) != null; ) {
                response.append(inputLine).append('\n');
            }

            //print result
            return response.toString();
        });
    }

    private static void sendParameters(Map<String, String> parameters, HttpURLConnection connection) throws IOException {
        if (!parameters.isEmpty()) {
            StringBuilder urlParameters = new StringBuilder();
            for (var parameterEntry : parameters.entrySet()) {
                if (urlParameters.length() > 0) urlParameters.append("&");
                urlParameters.append(parameterEntry.getKey()).append("=").append(parameterEntry.getValue());
            }

            // Send post request
            connection.setDoOutput(true);
            try (var wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(urlParameters.toString());
            };
        }
    }

    private static void setCookies(Map<String, String> cookies, HttpURLConnection connection) {
        if (!cookies.isEmpty()) {
            StringBuilder cookieBuilder = new StringBuilder();
            for (var cookieEntry : cookies.entrySet()) {
                if (cookieBuilder.length() > 0) cookieBuilder.append("&");
                cookieBuilder.append(cookieEntry.getKey()).append("=").append(cookieEntry.getValue());
            }

            connection.setRequestProperty("Cookie", cookieBuilder.toString());
        }
    }
}
