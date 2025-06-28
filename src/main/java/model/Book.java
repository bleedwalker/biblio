package model;

import java.math.BigDecimal;

public class Book {
    private int isbn; // Изменено на int
    private String title;
    private String author;
    private String genre;
    private BigDecimal depositCost;
    private BigDecimal rentalCostPerDay;

    public Book(int isbn, String title, String author, String genre,
                BigDecimal depositCost, BigDecimal rentalCostPerDay) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.depositCost = depositCost;
        this.rentalCostPerDay = rentalCostPerDay;
    }

    // Геттеры и сеттеры
    public int getIsbn() { return isbn; }

    public String getTitle() { return title; }

    public String getAuthor() { return author; }

    public String getGenre() { return genre; }

    public BigDecimal getDepositCost() { return depositCost; }

    public BigDecimal getRentalCostPerDay() { return rentalCostPerDay; }

    @Override
    public String toString() {
        return title + " (" + author + ")";
    }
}