package model;

import java.math.BigDecimal;

public class Penalty {
    private String penaltyName;
    private BigDecimal penaltyAmount; // Абсолютное значение

    public Penalty(String penaltyName, BigDecimal penaltyAmount) {
        this.penaltyName = penaltyName;
        this.penaltyAmount = penaltyAmount;
    }

    // Геттеры и сеттеры
    public String getPenaltyName() { return penaltyName; }
    public void setPenaltyName(String penaltyName) { this.penaltyName = penaltyName; }

    public BigDecimal getPenaltyAmount() { return penaltyAmount; }


    @Override
    public String toString() {
        return penaltyName + " +" + penaltyAmount;
    }
}