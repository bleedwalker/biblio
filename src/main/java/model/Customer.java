package model;

public class Customer {
    private int customerId;
    private String address;
    private String fullName;
    private String phoneNumber;

    public Customer(int customerId, String address, String fullName, String phoneNumber) {
        this.customerId = customerId;
        this.address = address;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    // Геттеры и сеттеры
    public int getCustomerId() { return customerId; }

    public String getAddress() { return address; }

    public String getFullName() { return fullName; }

    public String getPhoneNumber() { return phoneNumber; }


    @Override
    public String toString() {
        return fullName + " (" + phoneNumber + ")";
    }
}