module org.erpklassup.erpklassup {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
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
    exports org.erpklassup.erpklassup;
    opens org.erpklassup.erpklassup.controllers to javafx.fxml;
    opens org.erpklassup.erpklassup to javafx.fxml;
    opens org.erpklassup.erpklassup.controllers.actionPage to javafx.fxml;
    opens org.erpklassup.erpklassup.controllers.renduView to javafx.fxml;
    exports org.erpklassup.erpklassup.util;
    opens org.erpklassup.erpklassup.util to javafx.fxml;

}