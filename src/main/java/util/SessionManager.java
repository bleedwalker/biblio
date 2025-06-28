package util;

import model.User;

public class SessionManager {
    private static User currentUser;

    private SessionManager() {
        // Приватный конструктор для предотвращения инстанцирования
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : "Guest";
    }

    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.getRole());
    }

    public static void clearSession() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}