package model;

import java.math.BigDecimal;

public class Discount {
    private String discountName;
    private BigDecimal discountAmount; // Абсолютное значение

    public Discount(String discountName, BigDecimal discountAmount) {
        this.discountName = discountName;
        this.discountAmount = discountAmount;
    }


    public String getDiscountName() { return discountName; }

    public BigDecimal getDiscountAmount() { return discountAmount; }


    @Override
    public String toString() {
        return discountName + " -" + discountAmount;
    }
}