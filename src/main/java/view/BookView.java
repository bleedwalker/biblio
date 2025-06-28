package view;

import controller.BookController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Book;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

public class BookView {
    private final BookController bookController = new BookController();
    private final TableView<Book> table = new TableView<>();
    private final boolean readOnly;
    private final TextField isbnField = new TextField();
    private final TextField titleField = new TextField();
    private final TextField authorField = new TextField();
    private final TextField genreField = new TextField();
    private final TextField depositField = new TextField();
    private final TextField rentalField = new TextField();

    public BookView() {
        this(false);
    }

    public BookView(boolean readOnly) {
        this.readOnly = readOnly;
        initializeTable();
        loadTableData();
        loadInitialData();
    }

    public VBox getView() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        VBox form = new VBox(5);
        form.setPadding(new Insets(10));
        form.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px;");

        isbnField.setDisable(true);
        form.getChildren().add(new Label("ISBN:"));
        form.getChildren().add(isbnField);

        form.getChildren().add(new Label("Название:"));
        form.getChildren().add(titleField);

        form.getChildren().add(new Label("Автор:"));
        form.getChildren().add(authorField);

        form.getChildren().add(new Label("Жанр:"));
        form.getChildren().add(genreField);

        form.getChildren().add(new Label("Залоговая стоимость:"));
        form.getChildren().add(depositField);

        form.getChildren().add(new Label("Стоимость аренды/день:"));
        form.getChildren().add(rentalField);

        HBox buttonBox = new HBox(10);
        Button addButton = new Button("Добавить");
        Button deleteButton = new Button("Удалить");

        addButton.setOnAction(e -> addBook());
        deleteButton.setOnAction(e -> deleteBook());

        buttonBox.getChildren().addAll(addButton, deleteButton);

        if (readOnly) {
            form.setVisible(false);
            buttonBox.setVisible(false);
        }

        vbox.getChildren().addAll(table, form, buttonBox);

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillFormWithBook(newSelection);
                    }
                });

        return vbox;
    }

    private void initializeTable() {
        TableColumn<Book, Integer> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));

        TableColumn<Book, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> authorCol = new TableColumn<>("Автор");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));

        TableColumn<Book, String> genreCol = new TableColumn<>("Жанр");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));

        TableColumn<Book, BigDecimal> depositCol = new TableColumn<>("Залог");
        depositCol.setCellValueFactory(new PropertyValueFactory<>("depositCost"));

        TableColumn<Book, BigDecimal> rentalCol = new TableColumn<>("Аренда/день");
        rentalCol.setCellValueFactory(new PropertyValueFactory<>("rentalCostPerDay"));

        table.getColumns().addAll(isbnCol, titleCol, authorCol, genreCol, depositCol, rentalCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadTableData() {
        table.setItems(bookController.getModel().getObservableData());
    }

    private void loadInitialData() {
        try {
            bookController.getModel().refreshImmediately();
        } catch (SQLException e) {
            showAlert("Ошибка загрузки", "Не удалось загрузить книги: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addBook() {
        try {
            Book book = createBookFromForm();
            if (book != null && bookController.addBook(book)) {
                clearForm();
            }
        } catch (Exception e) {
            showAlert("Ошибка ввода", "Некорректные данные: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteBook() {
        Book selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка выбора", "Выберите книгу для удаления", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление книги");
        alert.setContentText("Вы уверены, что хотите удалить: " + selected.getTitle() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (bookController.deleteBook(selected.getIsbn())) {
                clearForm();
            }
        }
    }

    private void clearForm() {
        isbnField.clear();
        titleField.clear();
        authorField.clear();
        genreField.clear();
        depositField.clear();
        rentalField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void fillFormWithBook(Book book) {
        isbnField.setText(String.valueOf(book.getIsbn()));
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        genreField.setText(book.getGenre());
        depositField.setText(book.getDepositCost().toString());
        rentalField.setText(book.getRentalCostPerDay().toString());
    }

    private Book createBookFromForm() {
        return new Book(
                0,
                titleField.getText(),
                authorField.getText(),
                genreField.getText(),
                new BigDecimal(depositField.getText()),
                new BigDecimal(rentalField.getText())
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