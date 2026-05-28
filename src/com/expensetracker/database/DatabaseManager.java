package com.expensetracker.database;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:expenses.db";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    static {
        try {
            // Explicitly load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver: " + e.getMessage());
        }
    }

    public DatabaseManager() {
        initializeDatabase();
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Re-enable FK support on every new connection — SQLite resets PRAGMAs per connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Enable foreign key support in SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Create Categories Table
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL UNIQUE," +
                    "color_hex TEXT NOT NULL," +
                    "icon TEXT" +
                    ");");

            // Create Expenses Table
            stmt.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "amount REAL NOT NULL," +
                    "category_id INTEGER NOT NULL," +
                    "description TEXT," +
                    "expense_date TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE" +
                    ");");

            // Check if categories table is empty, and seed sample categories
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories;");
            if (rs.next() && rs.getInt(1) == 0) {
                seedDefaultCategories(conn);
                seedDefaultExpenses(conn);
            }

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedDefaultCategories(Connection conn) throws SQLException {
        String sql = "INSERT INTO categories (name, color_hex, icon) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Object[][] categories = {
                {"Food", "#C1EBE9", "🍔"},
                {"Shopping", "#FFF7C5", "🛍️"},
                {"Travel", "#F4AE52", "✈️"},
                {"Bills", "#4F252E", "🧾"},
                {"Entertainment", "#C1EBE9", "🎬"}
            };

            for (Object[] cat : categories) {
                pstmt.setString(1, (String) cat[0]);
                pstmt.setString(2, (String) cat[1]);
                pstmt.setString(3, (String) cat[2]);
                pstmt.executeUpdate();
            }
        }
    }

    private void seedDefaultExpenses(Connection conn) throws SQLException {
        // Find seeded category IDs
        Map<String, Integer> catMap = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM categories;")) {
            while (rs.next()) {
                catMap.put(rs.getString("name"), rs.getInt("id"));
            }
        }

        String sql = "INSERT INTO expenses (amount, category_id, description, expense_date, created_at) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();

            Object[][] expenses = {
                {45.50, catMap.get("Food"), "Dinner at Pizzeria", today.minusDays(1).format(DATE_FORMATTER), now.minusDays(1).format(DATE_TIME_FORMATTER)},
                {120.00, catMap.get("Bills"), "Electricity Bill Payment", today.minusDays(3).format(DATE_FORMATTER), now.minusDays(3).format(DATE_TIME_FORMATTER)},
                {15.00, catMap.get("Travel"), "Train Ticket to City Center", today.format(DATE_FORMATTER), now.format(DATE_TIME_FORMATTER)},
                {85.00, catMap.get("Shopping"), "Purchased winter jacket", today.minusDays(5).format(DATE_FORMATTER), now.minusDays(5).format(DATE_TIME_FORMATTER)},
                {22.00, catMap.get("Entertainment"), "Cinema ticket and popcorn", today.minusDays(2).format(DATE_FORMATTER), now.minusDays(2).format(DATE_TIME_FORMATTER)},
                {35.20, catMap.get("Food"), "Groceries from Supermarket", today.format(DATE_FORMATTER), now.format(DATE_TIME_FORMATTER)}
            };

            for (Object[] exp : expenses) {
                pstmt.setDouble(1, (Double) exp[0]);
                pstmt.setInt(2, (Integer) exp[1]);
                pstmt.setString(3, (String) exp[2]);
                pstmt.setString(4, (String) exp[3]);
                pstmt.setString(5, (String) exp[4]);
                pstmt.executeUpdate();
            }
        }
    }

    // --- CATEGORY CRUD ---

    public void addCategory(Category category) {
        String sql = "INSERT INTO categories (name, color_hex, icon) VALUES (?, ?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getColorHex());
            pstmt.setString(3, category.getIcon());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    category.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding category: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, color_hex, icon FROM categories ORDER BY name ASC;";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Category category = new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("color_hex"),
                        rs.getString("icon")
                );
                categories.add(category);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all categories: " + e.getMessage());
            e.printStackTrace();
        }
        return categories;
    }

    // --- EXPENSE CRUD ---

    public void addExpense(Expense expense) {
        String sql = "INSERT INTO expenses (amount, category_id, description, expense_date, created_at) VALUES (?, ?, ?, ?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setDouble(1, expense.getAmount());
            pstmt.setInt(2, expense.getCategory().getId());
            pstmt.setString(3, expense.getDescription());
            pstmt.setString(4, expense.getDate().format(DATE_FORMATTER));
            pstmt.setString(5, expense.getCreatedAt().format(DATE_TIME_FORMATTER));
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    expense.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding expense: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateExpense(Expense expense) {
        String sql = "UPDATE expenses SET amount = ?, category_id = ?, description = ?, expense_date = ? WHERE id = ?;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, expense.getAmount());
            pstmt.setInt(2, expense.getCategory().getId());
            pstmt.setString(3, expense.getDescription());
            pstmt.setString(4, expense.getDate().format(DATE_FORMATTER));
            pstmt.setInt(5, expense.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating expense: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteExpense(int id) {
        String sql = "DELETE FROM expenses WHERE id = ?;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error deleting expense: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Expense> getAllExpenses() {
        List<Expense> expenses = new ArrayList<>();
        
        // Cache categories by ID for quick lookups
        Map<Integer, Category> categoryCache = new HashMap<>();
        for (Category cat : getAllCategories()) {
            categoryCache.put(cat.getId(), cat);
        }

        String sql = "SELECT id, amount, category_id, description, expense_date, created_at FROM expenses;";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int categoryId = rs.getInt("category_id");
                Category category = categoryCache.get(categoryId);
                
                // Fallback in case category is missing
                if (category == null) {
                    category = new Category(categoryId, "Uncategorized", "#808080", "❓");
                }

                LocalDate date = LocalDate.parse(rs.getString("expense_date"), DATE_FORMATTER);
                LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"), DATE_TIME_FORMATTER);

                Expense expense = new Expense(
                        rs.getInt("id"),
                        rs.getDouble("amount"),
                        category,
                        rs.getString("description"),
                        date,
                        createdAt
                );
                expenses.add(expense);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all expenses: " + e.getMessage());
            e.printStackTrace();
        }
        return expenses;
    }
}
