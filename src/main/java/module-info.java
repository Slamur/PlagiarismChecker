module com.slamur.plagiarism {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires org.jsoup;
    requires java.desktop;
    requires javafx.lib;
    exports com.slamur.plagiarism.controller;
    exports com.slamur.plagiarism.model.parsing;
    exports com.slamur.plagiarism.model.verification;
    opens com.slamur.plagiarism ;
}