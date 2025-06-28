package controller;

import model.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import java.math.BigDecimal;
import java.sql.*;
import java.util.concurrent.TimeUnit;

public class OrderController extends BaseController<Order> {
    public OrderController() {
        super(Order.class);
    }

    public boolean deleteOrder(int orderId) {
        boolean result = executeUpdate("DELETE FROM orders WHERE order_id = ?", orderId);
        if (result) {
            try {
                getModel().refreshImmediately();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public int createOrder(int customerId, int isbn, Date issueDate, Date returnDate) {
        return executeQuery(
                "INSERT INTO orders (customer_id, isbn, issue_date, return_date) VALUES (?, ?, ?, ?)",
                rs -> {
                    if (rs.next()) {
                        int newId = rs.getInt(1);

                        // Создаем новый объект заказа
                        Order newOrder = new Order(
                                newId,
                                customerId,
                                isbn,
                                issueDate,
                                returnDate
                        );

                        // Загружаем ассоциации
                        DataModel.loadOrderAssociations(newOrder);

                        // Добавляем в наблюдаемый список
                        Platform.runLater(() -> {
                            getModel().getObservableData().add(newOrder);
                        });

                        return newId;
                    }
                    return -1;
                },
                Statement.RETURN_GENERATED_KEYS,
                new Object[]{customerId, isbn, issueDate, returnDate}
        );
    }

    public boolean addDiscountToOrder(int orderId, Discount discount) {
        boolean success = ensureDiscountExists(discount) &&
                executeUpdate("INSERT INTO orderdiscounts (order_id, discount_name) VALUES (?, ?)",
                        orderId, discount.getDiscountName());

        if (success) {
            refreshSingleOrder(orderId);
        }
        return success;
    }

    public boolean addPenaltyToOrder(int orderId, Penalty penalty) {
        boolean success = ensurePenaltyExists(penalty) &&
                executeUpdate("INSERT INTO orderpenalties (order_id, penalty_name) VALUES (?, ?)",
                        orderId, penalty.getPenaltyName());

        if (success) {
            refreshSingleOrder(orderId);
        }
        return success;
    }

    private boolean ensureDiscountExists(Discount discount) {
        return executeUpdate(
                "INSERT INTO discounts (discount_name, discount_amount) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE discount_amount = VALUES(discount_amount)",
                discount.getDiscountName(), discount.getDiscountAmount()
        );
    }

    private boolean ensurePenaltyExists(Penalty penalty) {
        return executeUpdate(
                "INSERT INTO penalties (penalty_name, penalty_amount) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE penalty_amount = VALUES(penalty_amount)",
                penalty.getPenaltyName(), penalty.getPenaltyAmount()
        );
    }

    // Метод для обновления одного заказа
    private void refreshSingleOrder(int orderId) {
        Order updatedOrder = getOrderById(orderId);
        if (updatedOrder != null) {
            Platform.runLater(() -> {
                ObservableList<Order> orders = getModel().getObservableData();
                for (int i = 0; i < orders.size(); i++) {
                    if (orders.get(i).getOrderId() == orderId) {
                        orders.set(i, updatedOrder);
                        break;
                    }
                }
            });
        }
    }

    // Метод для получения заказа по ID
    public Order getOrderById(int orderId) {
        return executeQuery(
                "SELECT * FROM orders WHERE order_id = ?",
                rs -> {
                    if (rs.next()) {
                        Order order = new Order(
                                rs.getInt("order_id"),
                                rs.getInt("customer_id"),
                                rs.getInt("isbn"),
                                rs.getDate("issue_date"),
                                rs.getDate("return_date")
                        );
                        DataModel.loadOrderAssociations(order);
                        return order;
                    }
                    return null;
                },
                orderId
        );
    }

    // Упрощенный метод расчета стоимости аренды
    public BigDecimal calculateRentalCost(Order order) {
        if (order.getIssueDate() == null || order.getReturnDate() == null) {
            return BigDecimal.ZERO;
        }

        long diff = order.getReturnDate().getTime() - order.getIssueDate().getTime();
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        days = Math.max(days, 1);

        Book book = order.getBook();
        if (book != null) {
            BigDecimal costPerDay = book.getRentalCostPerDay();
            return costPerDay.multiply(BigDecimal.valueOf(days));
        }
        return BigDecimal.ZERO;
    }
}