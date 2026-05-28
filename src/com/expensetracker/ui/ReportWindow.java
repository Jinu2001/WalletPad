package com.expensetracker.ui;

import com.expensetracker.database.DatabaseManager;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.util.CollectionsEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportWindow extends JDialog {
    private final DatabaseManager dbManager;
    private YearMonth selectedMonth;

    private JLabel monthLabel;
    private JLabel totalSpentLabel;
    private JLabel dailyAvgLabel;
    private JLabel largestExpenseLabel;
    private JPanel breakdownPanel;
    private JScrollPane scrollPane;

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    public ReportWindow(Frame owner, DatabaseManager dbManager) {
        super(owner, "Monthly Reports & Analytics", true);
        this.dbManager = dbManager;
        this.selectedMonth = YearMonth.now();

        setupUI();
        loadReportData();
    }

    private void setupUI() {
        setSize(480, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        // 1. Navigation Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.decode("#4F252E"));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JButton prevBtn = new JButton("◀");
        prevBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        prevBtn.setFocusPainted(false);
        prevBtn.addActionListener(e -> {
            selectedMonth = selectedMonth.minusMonths(1);
            loadReportData();
        });
        headerPanel.add(prevBtn, BorderLayout.WEST);

        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        monthLabel.setForeground(Color.decode("#C1EBE9"));
        headerPanel.add(monthLabel, BorderLayout.CENTER);

        JButton nextBtn = new JButton("▶");
        nextBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nextBtn.setFocusPainted(false);
        nextBtn.addActionListener(e -> {
            selectedMonth = selectedMonth.plusMonths(1);
            loadReportData();
        });
        headerPanel.add(nextBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // 2. Stats Dashboard
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel statsGrid = new JPanel(new GridLayout(1, 3, 10, 0));
        statsGrid.setMaximumSize(new Dimension(440, 90));
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        statsGrid.add(createStatCard("Total Spending", totalSpentLabel = new JLabel("LKR 0.00"), "#FFF7C5"));
        statsGrid.add(createStatCard("Daily Average", dailyAvgLabel = new JLabel("LKR 0.00"), "#C1EBE9"));
        statsGrid.add(createStatCard("Largest Single", largestExpenseLabel = new JLabel("LKR 0.00"), "#F4AE52"));

        centerPanel.add(statsGrid);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        // 3. Category Breakdown Header
        JLabel breakdownTitle = new JLabel("Category Spending Breakdown");
        breakdownTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        breakdownTitle.setForeground(Color.decode("#C1EBE9"));
        breakdownTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(breakdownTitle);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 4. Breakdown List (Scrollable)
        breakdownPanel = new JPanel();
        breakdownPanel.setLayout(new BoxLayout(breakdownPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(breakdownPanel);
        scrollPane.setBorder(null);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        
        centerPanel.add(scrollPane);

        add(centerPanel, BorderLayout.CENTER);

        // Footer Action Panel
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.decode("#4F252E")));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String hexColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#3A1921"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Draw a small left indicator stripe
                g2.setColor(Color.decode(hexColor));
                g2.fillRoundRect(0, 0, 5, getHeight(), 6, 6);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 15, 12, 12));
        card.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLabel.setForeground(Color.decode("#FFF7C5"));
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valueLabel.setForeground(Color.decode("#C1EBE9"));

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(valueLabel);

        return card;
    }

    private void loadReportData() {
        monthLabel.setText(selectedMonth.format(MONTH_YEAR_FORMATTER));

        List<Expense> allExpenses = dbManager.getAllExpenses();
        List<Expense> monthlyExpenses = CollectionsEngine.filterByMonth(allExpenses, selectedMonth);

        double totalSpent = CollectionsEngine.getTotalSpending(monthlyExpenses);
        double dailyAvg = CollectionsEngine.getAverageDailySpending(allExpenses, selectedMonth);
        Expense largestExp = CollectionsEngine.getLargestExpense(monthlyExpenses);

        totalSpentLabel.setText(String.format("LKR %.2f", totalSpent));
        dailyAvgLabel.setText(String.format("LKR %.2f", dailyAvg));
        largestExpenseLabel.setText(largestExp != null ? String.format("LKR %.2f", largestExp.getAmount()) : "LKR 0.00");

        // Clear and rebuild breakdownPanel
        breakdownPanel.removeAll();

        Map<Category, Double> breakdown = CollectionsEngine.getCategoryBreakdown(allExpenses, selectedMonth);

        if (breakdown.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            JLabel emptyLabel = new JLabel("No expenses logged for this month.");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            emptyLabel.setForeground(Color.decode("#FFF7C5"));
            emptyPanel.add(emptyLabel);
            emptyPanel.setOpaque(false);
            breakdownPanel.add(emptyPanel);
        } else {
            for (Map.Entry<Category, Double> entry : breakdown.entrySet()) {
                Category cat = entry.getKey();
                double amount = entry.getValue();
                double percent = totalSpent > 0 ? (amount / totalSpent) * 100 : 0.0;

                breakdownPanel.add(createBreakdownRow(cat, amount, percent));
                breakdownPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        breakdownPanel.revalidate();
        breakdownPanel.repaint();
    }

    private JPanel createBreakdownRow(Category cat, double amount, double percent) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(420, 50));

        // Row Label Header (Category Name on Left, Amount + Percentage on Right)
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);

        String iconText = cat.getIcon();
        if (iconText == null || iconText.isBlank() || iconText.contains("?") || iconText.contains("�")) {
            iconText = switch (cat.getName()) {
                case "Food" -> "🍔";
                case "Shopping" -> "🛍️";
                case "Travel" -> "✈️";
                case "Bills" -> "🧾";
                case "Entertainment" -> "🎬";
                default -> "▫";
            };
        }

        JLabel catLabel = new JLabel(iconText + "  " + cat.getName());
        catLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        catLabel.setForeground(Color.decode("#C1EBE9"));
        textPanel.add(catLabel, BorderLayout.WEST);

        Color categoryColor = Color.decode(cat.getColorHex());
        Color amountTextColor = isColorDark(categoryColor) ? Color.decode("#C1EBE9") : categoryColor;
        Color progressFillColor = categoryColor.equals(Color.decode("#4F252E")) ? Color.decode("#F4AE52") : categoryColor;

        JLabel amountLabel = new JLabel(String.format("LKR %.2f (%.1f%%)", amount, percent));
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        amountLabel.setForeground(amountTextColor);
        textPanel.add(amountLabel, BorderLayout.EAST);

        row.add(textPanel);
        row.add(Box.createRigidArea(new Dimension(0, 5)));

        // Custom painted Progress Bar
        JPanel progressBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw background track
                g2.setColor(Color.decode("#4F252E"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Draw filled progress portion
                int fillWidth = (int) ((percent / 100.0) * getWidth());
                if (fillWidth > 0) {
                    g2.setColor(progressFillColor);
                    g2.fillRoundRect(0, 0, fillWidth, getHeight(), 8, 8);
                }
                
                g2.dispose();
            }
        };
        progressBar.setPreferredSize(new Dimension(420, 10));
        progressBar.setMaximumSize(new Dimension(420, 10));
        progressBar.setOpaque(false);

        row.add(progressBar);

        return row;
    }

    private boolean isColorDark(Color color) {
        double brightness = Math.sqrt(
                color.getRed() * color.getRed() * 0.241 +
                color.getGreen() * color.getGreen() * 0.691 +
                color.getBlue() * color.getBlue() * 0.068
        );
        return brightness < 130;
    }
}
