package com.expensetracker.ui;

import com.expensetracker.database.DatabaseManager;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ExpenseDialog extends JDialog {
    private final DatabaseManager dbManager;
    private final Runnable onExpenseSaved;
    private final Expense expenseToEdit; // Null if adding a new expense

    private JTextField amountField;
    private JComboBox<Category> categoryComboBox;
    private JTextField descriptionField;
    private JTextField dateField;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public ExpenseDialog(Frame owner, DatabaseManager dbManager, Runnable onExpenseSaved) {
        this(owner, dbManager, null, onExpenseSaved);
    }

    public ExpenseDialog(Frame owner, DatabaseManager dbManager, Expense expenseToEdit, Runnable onExpenseSaved) {
        super(owner, expenseToEdit == null ? "Add Expense" : "Edit Expense", true);
        this.dbManager = dbManager;
        this.onExpenseSaved = onExpenseSaved;
        this.expenseToEdit = expenseToEdit;

        setupUI();
        if (expenseToEdit != null) {
            loadExpenseData();
        }
    }

    private void setupUI() {
        setSize(380, 420);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. Amount Field
        JPanel amountPanel = new JPanel(new BorderLayout(5, 5));
        amountPanel.setMaximumSize(new Dimension(340, 60));
        amountPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel amountLabel = new JLabel("Amount (LKR)");
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        amountPanel.add(amountLabel, BorderLayout.NORTH);

        amountField = new JTextField();
        amountField.setPreferredSize(new Dimension(340, 32));
        amountField.putClientProperty("JTextField.placeholderText", "0.00");
        amountPanel.add(amountField, BorderLayout.CENTER);
        formPanel.add(amountPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 2. Category Dropdown
        JPanel categoryPanel = new JPanel(new BorderLayout(5, 5));
        categoryPanel.setMaximumSize(new Dimension(340, 60));
        categoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel categoryLabel = new JLabel("Category");
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        categoryPanel.add(categoryLabel, BorderLayout.NORTH);

        categoryComboBox = new JComboBox<>();
        categoryComboBox.setPreferredSize(new Dimension(340, 32));
        loadCategories();
        categoryPanel.add(categoryComboBox, BorderLayout.CENTER);
        formPanel.add(categoryPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 3. Description Field
        JPanel descPanel = new JPanel(new BorderLayout(5, 5));
        descPanel.setMaximumSize(new Dimension(340, 60));
        descPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descLabel = new JLabel("Description");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        descPanel.add(descLabel, BorderLayout.NORTH);

        descriptionField = new JTextField();
        descriptionField.setPreferredSize(new Dimension(340, 32));
        descriptionField.putClientProperty("JTextField.placeholderText", "e.g., Starbucks coffee");
        descPanel.add(descriptionField, BorderLayout.CENTER);
        formPanel.add(descPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 4. Date Field
        JPanel datePanel = new JPanel(new BorderLayout(5, 5));
        datePanel.setMaximumSize(new Dimension(340, 60));
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel dateLabel = new JLabel("Date (YYYY-MM-DD)");
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        datePanel.add(dateLabel, BorderLayout.NORTH);

        dateField = new JTextField();
        dateField.setPreferredSize(new Dimension(340, 32));
        dateField.setText(LocalDate.now().format(DATE_FORMATTER)); // Preset to today
        datePanel.add(dateField, BorderLayout.CENTER);
        formPanel.add(datePanel);

        add(formPanel, BorderLayout.CENTER);

        // 5. Actions Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.decode("#4F252E")));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        actionPanel.add(cancelBtn);

        JButton saveBtn = new JButton(expenseToEdit == null ? "Add Expense" : "Save Changes");
        saveBtn.putClientProperty("JButton.buttonType", "filled");
        saveBtn.setBackground(Color.decode("#F4AE52"));
        saveBtn.setForeground(Color.decode("#4F252E"));
        saveBtn.addActionListener(this::handleSave);
        actionPanel.add(saveBtn);

        add(actionPanel, BorderLayout.SOUTH);
    }

    private void loadCategories() {
        List<Category> categories = dbManager.getAllCategories();
        categoryComboBox.removeAllItems();
        for (Category cat : categories) {
            categoryComboBox.addItem(cat);
        }
    }

    private void loadExpenseData() {
        amountField.setText(String.format("%.2f", expenseToEdit.getAmount()));
        descriptionField.setText(expenseToEdit.getDescription());
        dateField.setText(expenseToEdit.getDate().format(DATE_FORMATTER));
        
        // Match category in dropdown
        for (int i = 0; i < categoryComboBox.getItemCount(); i++) {
            Category cat = categoryComboBox.getItemAt(i);
            if (cat.getId() == expenseToEdit.getCategory().getId()) {
                categoryComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private void handleSave(ActionEvent e) {
        // Validation
        String amountText = amountField.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive decimal amount.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Category selectedCategory = (Category) categoryComboBox.getSelectedItem();
        if (selectedCategory == null) {
            JOptionPane.showMessageDialog(this, "Please create and select a category first.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a description.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String dateText = dateField.getText().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(dateText, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a date in YYYY-MM-DD format.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (expenseToEdit == null) {
            // New Expense
            Expense newExpense = new Expense(amount, selectedCategory, description, date);
            dbManager.addExpense(newExpense);
        } else {
            // Update Existing Expense
            expenseToEdit.setAmount(amount);
            expenseToEdit.setCategory(selectedCategory);
            expenseToEdit.setDescription(description);
            expenseToEdit.setDate(date);
            dbManager.updateExpense(expenseToEdit);
        }

        if (onExpenseSaved != null) {
            onExpenseSaved.run();
        }
        dispose();
    }
}
