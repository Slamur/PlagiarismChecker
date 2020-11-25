package com.slamur.plagiarism.model.parsing;

public class ParticipantInfo {

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
}
