package io.github.nmaksimka.mypetutil;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class PetApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("myPetUtil");
        stage.setScene(new Scene(new Label("myPetUtil"), 400, 300));
        stage.show();
    }
}
