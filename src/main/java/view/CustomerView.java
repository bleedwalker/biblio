package view;

import controller.CustomerController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Customer;
import java.sql.SQLException;
import java.util.Optional;

public class CustomerView {
    private final CustomerController customerController = new CustomerController();
    private final TableView<Customer> table = new TableView<>();
    private final TextField idField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField phoneField = new TextField();

    public VBox getView() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        initializeTable();
        loadTableData();
        loadInitialData();

        VBox form = new VBox(5);
        form.setPadding(new Insets(10));
        form.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px;");

        idField.setDisable(true);
        form.getChildren().add(new Label("ID:"));
        form.getChildren().add(idField);

        form.getChildren().add(new Label("ФИО:"));
        form.getChildren().add(nameField);

        form.getChildren().add(new Label("Адрес:"));
        form.getChildren().add(addressField);

        form.getChildren().add(new Label("Телефон:"));
        form.getChildren().add(phoneField);

        HBox buttonBox = new HBox(10);
        Button addButton = new Button("Добавить");
        Button deleteButton = new Button("Удалить");

        addButton.setOnAction(e -> addCustomer());
        deleteButton.setOnAction(e -> deleteCustomer());

        buttonBox.getChildren().addAll(addButton, deleteButton);
        vbox.getChildren().addAll(table, form, buttonBox);

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillFormWithCustomer(newSelection);
                    }
                });

        return vbox;
    }

    private void initializeTable() {
        TableColumn<Customer, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));

        TableColumn<Customer, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<Customer, String> addressCol = new TableColumn<>("Адрес");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Телефон");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));

        table.getColumns().addAll(idCol, nameCol, addressCol, phoneCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadTableData() {
        table.setItems(customerController.getModel().getObservableData());
    }

    private void loadInitialData() {
        try {
            customerController.getModel().refreshImmediately();
        } catch (SQLException e) {
            showAlert("Ошибка загрузки", "Не удалось загрузить клиентов: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addCustomer() {
        try {
            Customer customer = createCustomerFromForm();
            int newId = customerController.addCustomer(customer);
            if (newId != -1) {
                clearForm();
            }
        } catch (Exception e) {
            showAlert("Ошибка ввода", "Некорректные данные: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteCustomer() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка выбора", "Выберите клиента для удаления", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление клиента");
        alert.setContentText("Вы уверены, что хотите удалить: " + selected.getFullName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (customerController.deleteCustomer(selected.getCustomerId())) {
                clearForm();
            }
        }
    }

    private void clearForm() {
        idField.clear();
        nameField.clear();
        addressField.clear();
        phoneField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void fillFormWithCustomer(Customer customer) {
        idField.setText(String.valueOf(customer.getCustomerId()));
        nameField.setText(customer.getFullName());
        addressField.setText(customer.getAddress());
        phoneField.setText(customer.getPhoneNumber());
    }

    private Customer createCustomerFromForm() {
        return new Customer(
                0,
                addressField.getText(),
                nameField.getText(),
                phoneField.getText()
        );
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}