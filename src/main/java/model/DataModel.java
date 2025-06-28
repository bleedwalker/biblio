package model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import util.DatabaseConnection;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class DataModel<T> {
    private final Class<T> type;
    private final List<T> dataCache = Collections.synchronizedList(new ArrayList<>());
    private final ObservableList<T> observableData = FXCollections.observableArrayList();
    private long lastCacheUpdate;
    private static final long CACHE_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    private static final Map<Integer, Book> bookCache = new ConcurrentHashMap<>();
    private static final Map<String, Discount> discountCache = new ConcurrentHashMap<>();
    private static final Map<String, Penalty> penaltyCache = new ConcurrentHashMap<>();

    private final Map<String, Function<ResultSet, T>> mappers = new HashMap<>();

    public DataModel(Class<T> type) {
        this.type = type;
        this.lastCacheUpdate = 0;
        initializeMappers();
    }

    private void initializeMappers() {
        mappers.put("Book", rs -> {
            try {
                Book book = new Book(
                        rs.getInt("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("genre"),
                        rs.getBigDecimal("deposit_cost"),
                        rs.getBigDecimal("rental_cost_per_day")
                );
                bookCache.put(book.getIsbn(), book);
                return type.cast(book);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        mappers.put("Customer", rs -> {
            try {
                return type.cast(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("address"),
                        rs.getString("full_name"),
                        rs.getString("phone_number")
                ));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        mappers.put("Discount", rs -> {
            try {
                Discount discount = new Discount(
                        rs.getString("discount_name"),
                        rs.getBigDecimal("discount_amount")
                );
                discountCache.put(discount.getDiscountName(), discount);
                return type.cast(discount);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        mappers.put("Order", rs -> {
            try {
                Order order = new Order(
                        rs.getInt("order_id"),
                        rs.getInt("customer_id"),
                        rs.getInt("isbn"),
                        rs.getDate("issue_date"),
                        rs.getDate("return_date")
                );
                loadOrderAssociations(order);

                // Устанавливаем книгу напрямую из кэша
                Book book = bookCache.get(order.getIsbn());
                if (book != null) {
                    order.setBook(book);
                }
                return type.cast(order);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        mappers.put("Penalty", rs -> {
            try {
                Penalty penalty = new Penalty(
                        rs.getString("penalty_name"),
                        rs.getBigDecimal("penalty_amount")
                );
                penaltyCache.put(penalty.getPenaltyName(), penalty);
                return type.cast(penalty);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        mappers.put("User", rs -> {
            try {
                return type.cast(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getObject("customer_id") != null ? rs.getInt("customer_id") : null
                ));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<T> getAll() throws SQLException {
        synchronized (this) {
            if (shouldRefreshCache()) {
                refreshCache();
            }
            return Collections.unmodifiableList(dataCache);
        }
    }

    public ObservableList<T> getObservableData() {
        return observableData;
    }

    private boolean shouldRefreshCache() {
        return System.currentTimeMillis() - lastCacheUpdate > CACHE_TIMEOUT || dataCache.isEmpty();
    }

    private void refreshCache() throws SQLException {
        String tableName = getTableName();
        String query = "SELECT * FROM " + tableName;

        List<T> newData = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                T item = mapResultSetToObject(rs);

                // Для заказов дополнительно загружаем ассоциации
                if (item instanceof Order) {
                    loadOrderAssociations((Order) item);
                }

                newData.add(item);
            }
        }

        refreshDiscountAndPenaltyCache();

        synchronized (this) {
            dataCache.clear();
            dataCache.addAll(newData);
            lastCacheUpdate = System.currentTimeMillis();

            Platform.runLater(() -> observableData.setAll(newData));
        }
    }

    private String getTableName() {
        Map<String, String> mappings = Map.of(
                "Book", "books",
                "Customer", "customers",
                "Discount", "discounts",
                "Order", "orders",
                "Penalty", "penalties",
                "User", "users"
        );

        String tableName = mappings.get(type.getSimpleName());
        if (tableName == null) {
            throw new IllegalArgumentException("No table mapping for: " + type.getSimpleName());
        }
        return tableName;
    }

    private void refreshDiscountAndPenaltyCache() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM discounts")) {

            while (rs.next()) {
                Discount discount = new Discount(
                        rs.getString("discount_name"),
                        rs.getBigDecimal("discount_amount")
                );
                discountCache.put(discount.getDiscountName(), discount);
            }
        } catch (SQLException e) {
            System.err.println("Discount refresh failed: " + e.getMessage());
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM penalties")) {

            while (rs.next()) {
                Penalty penalty = new Penalty(
                        rs.getString("penalty_name"),
                        rs.getBigDecimal("penalty_amount")
                );
                penaltyCache.put(penalty.getPenaltyName(), penalty);
            }
        } catch (SQLException e) {
            System.err.println("Penalty refresh failed: " + e.getMessage());
        }
    }

    private T mapResultSetToObject(ResultSet rs) throws SQLException {
        String className = type.getSimpleName();
        Function<ResultSet, T> mapper = mappers.get(className);
        if (mapper != null) {
            return mapper.apply(rs);
        }
        throw new IllegalArgumentException("Unsupported type: " + className);
    }

    public static void loadOrderAssociations(Order order) {

        String discountQuery = "SELECT d.* FROM orderdiscounts od " +
                "JOIN discounts d ON od.discount_name = d.discount_name " +
                "WHERE od.order_id = ?";

        String penaltyQuery = "SELECT p.* FROM orderpenalties op " +
                "JOIN penalties p ON op.penalty_name = p.penalty_name " +
                "WHERE op.order_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(discountQuery)) {

            pstmt.setInt(1, order.getOrderId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Discount discount = new Discount(
                            rs.getString("discount_name"),
                            rs.getBigDecimal("discount_amount")
                    );
                    Discount cached = discountCache.get(discount.getDiscountName());
                    if (cached != null) discount = cached;

                    order.addDiscount(discount);
                }
            }
        } catch (SQLException e) {
            System.err.println("Discount load failed: " + e.getMessage());
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(penaltyQuery)) {

            pstmt.setInt(1, order.getOrderId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Penalty penalty = new Penalty(
                            rs.getString("penalty_name"),
                            rs.getBigDecimal("penalty_amount")
                    );
                    Penalty cached = penaltyCache.get(penalty.getPenaltyName());
                    if (cached != null) penalty = cached;

                    order.addPenalty(penalty);
                }
            }
        } catch (SQLException e) {
            System.err.println("Penalty load failed: " + e.getMessage());
        }
        Book book = bookCache.get(order.getIsbn());
        if (book != null) {
            order.setBook(book);
        }
        order.calculateTotal();
    }

    public void refreshImmediately() throws SQLException {
        refreshCache();
    }

    // Статические методы доступа к кэшам
    public static Book getBookFromCache(int isbn) {
        return bookCache.get(isbn);
    }

    public static Discount getDiscountFromCache(String name) {
        return discountCache.get(name);
    }

    public static Penalty getPenaltyFromCache(String name) {
        return penaltyCache.get(name);
    }

    public static void addDiscountToCache(Discount discount) {
        discountCache.put(discount.getDiscountName(), discount);
    }

    public static void addPenaltyToCache(Penalty penalty) {
        penaltyCache.put(penalty.getPenaltyName(), penalty);
    }

    public static Map<String, Discount> getDiscountCache() {
        return Collections.unmodifiableMap(discountCache);
    }

    public static Map<String, Penalty> getPenaltyCache() {
        return Collections.unmodifiableMap(penaltyCache);
    }
}