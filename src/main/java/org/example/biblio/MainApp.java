package org.example.biblio;

import javafx.application.Application;
import javafx.stage.Stage;
import util.DatabaseConnection;
import view.LoginView;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        LoginView loginView = new LoginView(primaryStage);
        primaryStage.setScene(loginView.getScene());
        primaryStage.setTitle("Библиотека");
        primaryStage.setMinWidth(2000);
        primaryStage.setMinHeight(1000);
        primaryStage.show();
    }

    @Override
    public void stop() {

        DatabaseConnection.closeAll();
        System.out.println("Закрыто");
    }

    public static void main(String[] args) {
        launch(args);
    }
}