package com.slamur.plagiarism.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class RequestUtils {

    public static final String GET = "GET", POST = "POST";
    public static final String DOMAIN = "http://contest.samsu.ru";

    public static String get(String url, Map<String, String> parameters, Map<String, String> cookies) throws IOException {
        return request(GET, url, parameters, cookies);
    }

    private static final String USER_AGENT = "Mozilla/5.0";

    private static String request(String type, String url, Map<String, String> parameters, Map<String, String> cookies) throws IOException {
        URL obj = new URL(DOMAIN + url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add request header
        con.setRequestMethod(type);

        if (cookies != null) {
            //Cookie: sessionID=login%09password
            StringBuilder cookieBuilder = new StringBuilder();
            for (var cookieEntry : cookies.entrySet()) {
                if (cookieBuilder.length() > 0) cookieBuilder.append("&");
                cookieBuilder.append(cookieEntry.getKey()).append("=").append(cookieEntry.getValue());
            }

            con.setRequestProperty("Cookie", cookieBuilder.toString());
        }

        StringBuilder urlParameters = null;
        if (parameters != null) {
            urlParameters = new StringBuilder();
            for (var parameterEntry : parameters.entrySet()) {
                if (urlParameters.length() > 0) urlParameters.append("&");
                urlParameters.append(parameterEntry.getKey()).append("=").append(parameterEntry.getValue());
            }

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters.toString());
            wr.flush();
            wr.close();
        }

        BufferedReader in = new BufferedReader(
                IOUtils.createReader(con.getInputStream())
        );

        String inputLine;

        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append('\n');
        }
        in.close();

        //print result
        return response.toString();
    }
}
