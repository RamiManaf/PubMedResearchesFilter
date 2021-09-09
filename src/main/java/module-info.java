module org.rami.prf {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires org.controlsfx.controls;

    opens org.rami.prf to javafx.fxml;
    exports org.rami.prf;
}
