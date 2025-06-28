package controller;

import model.User;
import util.SessionManager;

public class AuthController extends BaseController<User> {
    public AuthController() {
        super(User.class);
    }

    public User login(String username, String password) {
        return executeQuery(
                "SELECT * FROM users WHERE username = ? AND password = ?",
                rs -> {
                    if (rs.next()) {
                        User user = new User(
                                rs.getInt("user_id"),
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("role"),
                                rs.getObject("customer_id", Integer.class)
                        );
                        SessionManager.setCurrentUser(user);
                        return user;
                    }
                    return null;
                },
                username, password
        );
    }
}