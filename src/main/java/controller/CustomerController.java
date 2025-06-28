package controller;

import model.Customer;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomerController extends BaseController<Customer> {
    public CustomerController() {
        super(Customer.class);
    }

    public int addCustomer(Customer customer) {
        return executeQuery(
                "INSERT INTO customers (address, full_name, phone_number) VALUES (?, ?, ?)",
                rs -> {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        try {
                            getModel().refreshImmediately();
                            notifyRefreshListeners();
                            return newId;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return -1;
                        }
                    }
                    return -1;
                },
                Statement.RETURN_GENERATED_KEYS,
                new Object[]{customer.getAddress(), customer.getFullName(), customer.getPhoneNumber()}
        );
    }

    public boolean deleteCustomer(int customerId) {
        boolean result = executeUpdate(
                "DELETE FROM customers WHERE customer_id = ?",
                customerId
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