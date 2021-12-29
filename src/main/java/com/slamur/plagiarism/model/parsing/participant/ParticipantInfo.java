package com.slamur.plagiarism.model.parsing.participant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.slamur.plagiarism.utils.IOUtils;

public class ParticipantInfo {

    public static final String NOT_AVAILABLE = "not available";

    public static final ParticipantInfo INFO_NOT_AVAILABLE = new ParticipantInfo(
            NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE
    );

    public final String name;
    public final String school;
    public final String city;

    public ParticipantInfo(String name, String school, String city) {
        this.name = name;
        this.school = school;
        this.city = city;
    }

    @Override
    public String toString() {
        return String.format(
                "%s(%s; %s)",
                name, school, city
        );
    }

    public void saveToFile(File infoFile)
            throws FileNotFoundException, UnsupportedEncodingException {
        IOUtils.saveToFile(infoFile, (out) -> {
            out.println(name);
            out.println(school);
            out.println(city);
        });
    }

    public static ParticipantInfo loadFromFile(File infoFile) throws IOException {
        return IOUtils.loadFromFile(infoFile, (in) -> {
            String name = in.readLine();
            String school = in.readLine();
            String city = in.readLine();

            return new ParticipantInfo(name, school, city);
        });
    }
}
