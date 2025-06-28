package controller;

import model.Book;
import java.sql.SQLException;

public class BookController extends BaseController<Book> {
    public BookController() {
        super(Book.class);
    }

    public boolean addBook(Book book) {
        boolean result = executeUpdate(
                "INSERT INTO books (title, author, genre, deposit_cost, rental_cost_per_day) VALUES (?, ?, ?, ?, ?)",
                book.getTitle(), book.getAuthor(), book.getGenre(),
                book.getDepositCost(), book.getRentalCostPerDay()
        );

        if (result) {
            try {
                getModel().refreshImmediately();
                notifyRefreshListeners();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    public boolean deleteBook(int isbn) {
        boolean result = executeUpdate(
                "DELETE FROM books WHERE isbn = ?",
                isbn
        );

        if (result) {
            try {
                getModel().refreshImmediately();
                notifyRefreshListeners();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}