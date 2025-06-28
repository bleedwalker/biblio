package view;

import controller.BookController;
import controller.CustomerController;
import controller.OrderController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.*;
import util.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class OrderView {
    private final OrderController orderController = new OrderController();
    private final CustomerController customerController = new CustomerController();
    private final BookController bookController = new BookController();
    private final TableView<Order> table = new TableView<>();
    private final boolean readOnly;
    private final TextField idField = new TextField();
    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final ComboBox<Book> bookCombo = new ComboBox<>();
    private final DatePicker issueDatePicker = new DatePicker();
    private final DatePicker returnDatePicker = new DatePicker();
    private final TextField totalField = new TextField();
    private final ComboBox<Discount> discountCombo = new ComboBox<>();
    private final ComboBox<Penalty> penaltyCombo = new ComboBox<>();

    public OrderView() {
        this(false);
    }

    public OrderView(boolean readOnly) {
        this.readOnly = readOnly;
        initializeTable();
        loadTableData();
        loadCombos();
        loadInitialData();
        registerRefreshListeners();
    }

    public VBox getView() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        GridPane form = new GridPane();
        form.setPadding(new Insets(10));
        form.setVgap(5);
        form.setHgap(10);
        form.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px;");

        idField.setDisable(true);
        totalField.setDisable(true);

        form.add(new Label("ID заказа:"), 0, 0);
        form.add(idField, 1, 0);

        form.add(new Label("Клиент:"), 0, 1);
        form.add(customerCombo, 1, 1);

        form.add(new Label("Книга:"), 0, 2);
        form.add(bookCombo, 1, 2);

        form.add(new Label("Дата выдачи:"), 0, 3);
        form.add(issueDatePicker, 1, 3);

        form.add(new Label("Дата возврата:"), 0, 4);
        form.add(returnDatePicker, 1, 4);

        form.add(new Label("Итоговая сумма:"), 0, 5);
        form.add(totalField, 1, 5);

        form.add(new Label("Скидки:"), 0, 6);
        form.add(discountCombo, 1, 6);

        form.add(new Label("Штрафы:"), 0, 7);
        form.add(penaltyCombo, 1, 7);

        HBox buttonBox = new HBox(10);
        Button createButton = new Button("Создать заказ");
        Button addDiscountButton = new Button("Добавить скидку");
        Button addPenaltyButton = new Button("Добавить штраф");
        Button deleteButton = new Button("Удалить заказ");

        createButton.setOnAction(e -> createOrder());
        addDiscountButton.setOnAction(e -> addDiscount());
        addPenaltyButton.setOnAction(e -> addPenalty());

        deleteButton.setOnAction(e -> deleteOrder());

        buttonBox.getChildren().addAll(createButton, addDiscountButton,
                addPenaltyButton, deleteButton);

        if (readOnly) {
            form.setVisible(false);
            buttonBox.setVisible(false);
        }

        vbox.getChildren().addAll(table, form, buttonBox);

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillFormWithOrder(newSelection);
                    }
                });

        return vbox;
    }

    private void registerRefreshListeners() {
        bookController.addRefreshListener(this::refreshBookCombo);
        customerController.addRefreshListener(this::refreshCustomerCombo);
    }

    private void refreshBookCombo() {
        try {
            List<Book> books = bookController.getModel().getAll();
            Platform.runLater(() -> {
                Book selected = bookCombo.getValue();
                bookCombo.setItems(FXCollections.observableArrayList(books));
                bookCombo.setValue(selected);
            });
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось обновить список книг", Alert.AlertType.ERROR);
        }
    }

    private void refreshCustomerCombo() {
        try {
            List<Customer> customers = customerController.getModel().getAll();
            Platform.runLater(() -> {
                Customer selected = customerCombo.getValue();
                customerCombo.setItems(FXCollections.observableArrayList(customers));
                customerCombo.setValue(selected);
            });
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось обновить список клиентов", Alert.AlertType.ERROR);
        }
    }

    public void refreshCombos() {
        refreshBookCombo();
        refreshCustomerCombo();
        refreshDiscountsAndPenalties();
    }

    private void refreshDiscountsAndPenalties() {
        Platform.runLater(() -> {
            discountCombo.setItems(FXCollections.observableArrayList(
                    DataModel.getDiscountCache().values()
            ));
            penaltyCombo.setItems(FXCollections.observableArrayList(
                    DataModel.getPenaltyCache().values()
            ));
        });
    }

    private void initializeTable() {
        TableColumn<Order, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<Order, Integer> customerIdCol = new TableColumn<>("ID клиента");
        customerIdCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));

        TableColumn<Order, Integer> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));

        TableColumn<Order, Date> issueCol = new TableColumn<>("Дата выдачи");
        issueCol.setCellValueFactory(cellData -> cellData.getValue().issueDateProperty());

        TableColumn<Order, Date> returnCol = new TableColumn<>("Дата возврата");
        returnCol.setCellValueFactory(cellData -> cellData.getValue().returnDateProperty());

        TableColumn<Order, BigDecimal> rentalCostCol = new TableColumn<>("Стоимость аренды");
        rentalCostCol.setCellValueFactory(cellData -> cellData.getValue().rentalCostProperty());
        rentalCostCol.setCellFactory(col -> new FormattedTableCell());

        TableColumn<Order, BigDecimal> depositCol = new TableColumn<>("Залог");
        depositCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDepositAmount()));
        depositCol.setCellFactory(col -> new FormattedTableCell());

        TableColumn<Order, String> discountsCol = new TableColumn<>("Скидки");
        discountsCol.setCellValueFactory(new PropertyValueFactory<>("discountsString"));
        discountsCol.setPrefWidth(200);

        TableColumn<Order, String> penaltiesCol = new TableColumn<>("Штрафы");
        penaltiesCol.setCellValueFactory(new PropertyValueFactory<>("penaltiesString"));
        penaltiesCol.setPrefWidth(200);

        TableColumn<Order, BigDecimal> totalCol = new TableColumn<>("Итоговая сумма");
        totalCol.setCellValueFactory(cellData -> cellData.getValue().totalAmountProperty());
        totalCol.setCellFactory(col -> new FormattedTableCell());

        table.getColumns().addAll(idCol, customerIdCol, isbnCol, issueCol, returnCol,
                rentalCostCol, depositCol, discountsCol, penaltiesCol, totalCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadTableData() {
        table.setItems(orderController.getModel().getObservableData());
    }

    private void loadInitialData() {
        try {
            orderController.getModel().refreshImmediately();
        } catch (SQLException e) {
            showAlert("Ошибка загрузки", "Не удалось загрузить заказы: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private static class FormattedTableCell extends TableCell<Order, BigDecimal> {
        private final DecimalFormat format = new DecimalFormat("0.00");

        @Override
        protected void updateItem(BigDecimal item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(format.format(item));
            }
        }
    }

    private void loadCombos() {
        try {
            List<Customer> customers = customerController.getModel().getAll();
            customerCombo.setItems(FXCollections.observableArrayList(customers));

            List<Book> books = bookController.getModel().getAll();
            bookCombo.setItems(FXCollections.observableArrayList(books));

            discountCombo.setItems(FXCollections.observableArrayList(
                    DataModel.getDiscountCache().values()
            ));

            penaltyCombo.setItems(FXCollections.observableArrayList(
                    DataModel.getPenaltyCache().values()
            ));
        } catch (SQLException e) {
            showAlert("Ошибка БД", "Не удалось загрузить данные: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void createOrder() {
        Customer customer = customerCombo.getValue();
        Book book = bookCombo.getValue();
        LocalDate issueDate = issueDatePicker.getValue();
        LocalDate returnDate = returnDatePicker.getValue();

        if (customer == null || book == null || issueDate == null || returnDate == null) {
            showAlert("Ошибка ввода", "Выберите клиента, книгу, дату выдачи и дату возврата", Alert.AlertType.WARNING);
            return;
        }

        if (returnDate.isBefore(issueDate)) {
            showAlert("Ошибка ввода", "Дата возврата должна быть позже даты выдачи", Alert.AlertType.WARNING);
            return;
        }

        int newId = orderController.createOrder(
                customer.getCustomerId(),
                book.getIsbn(),
                Date.valueOf(issueDate),
                Date.valueOf(returnDate)
        );

        if (newId != -1) {
            clearForm();
            showAlert("Успех", "Заказ успешно создан", Alert.AlertType.INFORMATION);
        } else {
            showAlert("Ошибка", "Не удалось создать заказ", Alert.AlertType.ERROR);
        }
    }

    private void addDiscount() {
        Order selected = table.getSelectionModel().getSelectedItem();
        Discount discount = discountCombo.getValue();

        if (selected == null || discount == null) {
            showAlert("Ошибка выбора", "Выберите заказ и скидку", Alert.AlertType.WARNING);
            return;
        }

        if (orderController.addDiscountToOrder(selected.getOrderId(), discount)) {
            showAlert("Успех", "Скидка успешно добавлена", Alert.AlertType.INFORMATION);
        } else {
            showAlert("Ошибка", "Не удалось добавить скидку", Alert.AlertType.ERROR);
        }
    }

    private void addPenalty() {
        Order selected = table.getSelectionModel().getSelectedItem();
        Penalty penalty = penaltyCombo.getValue();

        if (selected == null || penalty == null) {
            showAlert("Ошибка выбора", "Выберите заказ и штраф", Alert.AlertType.WARNING);
            return;
        }

        if (orderController.addPenaltyToOrder(selected.getOrderId(), penalty)) {
            showAlert("Успех", "Штраф успешно добавлен", Alert.AlertType.INFORMATION);
        } else {
            showAlert("Ошибка", "Не удалось добавить штраф", Alert.AlertType.ERROR);
        }
    }

    private void calculateTotal() {
        Order selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка выбора", "Выберите заказ для расчета", Alert.AlertType.WARNING);
            return;
        }

        selected.calculateTotal();
        totalField.setText(selected.getTotalAmount().toString());
    }

    private void deleteOrder() {
        Order selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка выбора", "Выберите заказ для удаления", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление заказа");
        alert.setContentText("Вы уверены, что хотите удалить заказ #" + selected.getOrderId() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (orderController.deleteOrder(selected.getOrderId())) {
                clearForm();
                showAlert("Успех", "Заказ успешно удален", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Ошибка", "Не удалось удалить заказ", Alert.AlertType.ERROR);
            }
        }
    }

    private void clearForm() {
        idField.clear();
        customerCombo.getSelectionModel().clearSelection();
        bookCombo.getSelectionModel().clearSelection();
        issueDatePicker.setValue(null);
        returnDatePicker.setValue(null);
        totalField.clear();
        discountCombo.getSelectionModel().clearSelection();
        penaltyCombo.getSelectionModel().clearSelection();
        table.getSelectionModel().clearSelection();
    }

    private void fillFormWithOrder(Order order) {
        idField.setText(String.valueOf(order.getOrderId()));

        customerCombo.setValue(customerCombo.getItems().stream()
                .filter(c -> c.getCustomerId() == order.getCustomerId())
                .findFirst()
                .orElse(null));

        bookCombo.setValue(bookCombo.getItems().stream()
                .filter(b -> b.getIsbn() == order.getIsbn())
                .findFirst()
                .orElse(null));

        issueDatePicker.setValue(order.getIssueDate().toLocalDate());
        if (order.getReturnDate() != null) {
            returnDatePicker.setValue(order.getReturnDate().toLocalDate());
        }

        if (order.getTotalAmount() != null) {
            totalField.setText(order.getTotalAmount().toString());
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}