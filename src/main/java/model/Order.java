package model;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Order {
    private final IntegerProperty orderId = new SimpleIntegerProperty();
    private final IntegerProperty customerId = new SimpleIntegerProperty();
    private final ObjectProperty<Book> book = new SimpleObjectProperty<>();
    private final ObjectProperty<Date> issueDate = new SimpleObjectProperty<>();
    private final ObjectProperty<Date> returnDate = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> rentalCost = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> totalAmount = new SimpleObjectProperty<>();
    private final List<Discount> discounts = new ArrayList<>();
    private final List<Penalty> penalties = new ArrayList<>();
    private final int isbn;

    public Order(int orderId, int customerId, int isbn, Date issueDate, Date returnDate) {
        this.orderId.set(orderId);
        this.customerId.set(customerId);
        this.isbn = isbn;
        this.issueDate.set(issueDate);
        this.returnDate.set(returnDate);
        this.rentalCost.addListener((obs, oldVal, newVal) -> calculateTotal());
    }

    public int getOrderId() { return orderId.get(); }


    public int getCustomerId() { return customerId.get(); }


    public Book getBook() { return book.get(); }
    public void setBook(Book book) {
        this.book.set(book);
        calculateRentalCost();
    }


    public int getIsbn() {
        return isbn;
    }

    public Date getIssueDate() { return issueDate.get(); }

    public ObjectProperty<Date> issueDateProperty() { return issueDate; }

    public Date getReturnDate() { return returnDate.get(); }

    public ObjectProperty<Date> returnDateProperty() { return returnDate; }

    public BigDecimal getRentalCost() { return rentalCost.get(); }

    public ObjectProperty<BigDecimal> rentalCostProperty() { return rentalCost; }

    public BigDecimal getTotalAmount() { return totalAmount.get(); }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount.set(totalAmount); }
    public ObjectProperty<BigDecimal> totalAmountProperty() { return totalAmount; }

    public void addDiscount(Discount discount) {
        this.discounts.add(discount);
        calculateTotal();
    }


    public void addPenalty(Penalty penalty) {
        this.penalties.add(penalty);
        calculateTotal();
    }

    public String getDiscountsString() {
        if (discounts.isEmpty()) return "None";
        return discounts.stream()
                .map(d -> d.getDiscountName() + " (-" + d.getDiscountAmount() + ")")
                .collect(Collectors.joining(", "));
    }

    public String getPenaltiesString() {
        if (penalties.isEmpty()) return "None";
        return penalties.stream()
                .map(p -> p.getPenaltyName() + " (+" + p.getPenaltyAmount() + ")")
                .collect(Collectors.joining(", "));
    }

    public BigDecimal getDepositAmount() {
        return (book.get() != null) ?
                book.get().getDepositCost() :
                BigDecimal.ZERO;
    }

    public void calculateRentalCost() {
        if (getIssueDate() == null || getReturnDate() == null) {
            rentalCost.set(BigDecimal.ZERO);
            return;
        }

        long diff = getReturnDate().getTime() - getIssueDate().getTime();
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        days = Math.max(days, 1);

        if (book.get() != null) {
            BigDecimal cost = book.get().getRentalCostPerDay().multiply(BigDecimal.valueOf(days));
            rentalCost.set(cost);
        } else {
            rentalCost.set(BigDecimal.ZERO);
        }
        calculateTotal();
    }

    public void calculateTotal() {
        BigDecimal rental = getRentalCost() != null ? getRentalCost() : BigDecimal.ZERO;
        BigDecimal total = rental;

        for (Discount discount : discounts) {
            total = total.subtract(discount.getDiscountAmount());
        }

        for (Penalty penalty : penalties) {
            total = total.add(penalty.getPenaltyAmount());
        }

        total = total.max(BigDecimal.ZERO);
        setTotalAmount(total);
    }
}