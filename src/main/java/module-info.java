module org.erpklassup.erpklassup {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;

    // Modules Ikonli
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires org.kordamp.ikonli.fontawesome5;

    // Consommation des services Ikonli
    uses org.kordamp.ikonli.IkonHandler;
    uses org.kordamp.ikonli.IkonProvider;

    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires com.sun.jna.platform;
    requires com.sun.jna;
    requires atlantafx.base;
    requires java.prefs;
    requires HikariCP.java7;
    requires java.sql;
    requires bcrypt;
    requires okhttp3;
    requires googleauth;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires org.apache.commons.codec;
    requires javafx.swing;
    requires com.fasterxml.jackson.databind;
    requires quartz;
    requires net.bytebuddy;

    exports org.erpklassup.erpklassup;
    exports org.erpklassup.erpklassup.util;

    opens org.erpklassup.erpklassup to javafx.fxml;
    opens org.erpklassup.erpklassup.controllers to javafx.fxml;
    opens org.erpklassup.erpklassup.controllers.actionPage to javafx.fxml;
    opens org.erpklassup.erpklassup.controllers.renduView to javafx.fxml;
    opens org.erpklassup.erpklassup.util to javafx.fxml;

    exports org.erpklassup.erpklassup.controllers.renduView.module2;
    exports org.erpklassup.erpklassup.controllers.actionPage.module2;

    exports org.erpklassup.erpklassup.controllers.actionPage.GRH;


    exports org.erpklassup.erpklassup.controllers.renduView.RH;
    opens org.erpklassup.erpklassup.controllers.renduView.module2 to javafx.fxml;

    opens org.erpklassup.erpklassup.controllers.renduView.RH to javafx.fxml;


    opens  org.erpklassup.erpklassup.controllers.actionPage.module2 to javafx.fxml;
    opens  org.erpklassup.erpklassup.controllers.actionPage.GRH to javafx.fxml;

    exports org.erpklassup.erpklassup.util.grh;
    opens org.erpklassup.erpklassup.util.grh to javafx.fxml;
}