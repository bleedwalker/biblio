package view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import util.SessionManager;
import javafx.geometry.Pos;

public class MainUserView {
    private final Stage primaryStage;

    public MainUserView(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Scene getScene() {
        primaryStage.setTitle("Библиотечная система - Пользователь");

        BorderPane borderPane = new BorderPane();

        HBox header = new HBox(20);
        header.setPadding(new Insets(15, 12, 15, 12));
        header.setStyle("-fx-background-color: #336699;");

        Label titleLabel = new Label("Система управления библиотекой");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: white;");

        Label userLabel = new Label("Пользователь: " + SessionManager.getCurrentUsername());
        userLabel.setStyle("-fx-text-fill: white;");

        Button logoutButton = new Button("Выйти");
        logoutButton.setOnAction(e -> {
            SessionManager.clearSession();
            primaryStage.setScene(new LoginView(primaryStage).getScene());
        });

        header.getChildren().addAll(titleLabel, userLabel, logoutButton);
        header.setAlignment(Pos.CENTER_LEFT);
        borderPane.setTop(header);

        TabPane tabPane = new TabPane();

        Tab booksTab = new Tab("Книги");
        booksTab.setClosable(false);
        booksTab.setContent(new BookView(true).getView());

        Tab ordersTab = new Tab("Мои заказы");
        ordersTab.setClosable(false);
        ordersTab.setContent(new OrderView(true).getView());

        tabPane.getTabs().addAll(booksTab, ordersTab);
        borderPane.setCenter(tabPane);

        return new Scene(borderPane, 900, 600);
    }
}