package view;

import controller.AuthController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import util.SessionManager;

public class LoginView {
    private final Stage primaryStage;
    private final AuthController authController;

    public LoginView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.authController = new AuthController();
    }

    public Scene getScene() {
        primaryStage.setTitle("Библиотечная система - Вход");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text scenetitle = new Text("Добро пожаловать");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(scenetitle, 0, 0, 2, 1);

        Label userName = new Label("Логин:");
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 1);

        Label pw = new Label("Пароль:");
        grid.add(pw, 0, 2);

        PasswordField pwBox = new PasswordField();
        grid.add(pwBox, 1, 2);

        Button btn = new Button("Войти");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        final Text actiontarget = new Text();
        grid.add(actiontarget, 1, 6);

        btn.setOnAction(e -> {
            String username = userTextField.getText();
            String password = pwBox.getText();

            if (authController.login(username, password) != null) {
                if (SessionManager.isAdmin()) {
                    primaryStage.setScene(new MainAdminView(primaryStage).getScene());
                } else {
                    primaryStage.setScene(new MainUserView(primaryStage).getScene());
                }
            } else {
                showAlert("Ошибка входа", "Неверный логин или пароль", Alert.AlertType.ERROR);
            }
        });


        return new Scene(grid, 400, 300);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}