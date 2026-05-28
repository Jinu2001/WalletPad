package com.expensetracker.ui;

import com.expensetracker.database.DatabaseManager;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.util.CollectionsEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private final DatabaseManager dbManager;
    private List<Expense> allExpenses = new ArrayList<>();
    private List<Expense> displayedExpenses = new ArrayList<>();
    private Category activeCategoryFilter = null; // null represents "All Categories"
    private double monthlyBudget = 1000.0; // Baseline standard budget

    // UI Components
    private JTextField searchField;
    private JPanel categoryFilterListPanel;
    private JTable expenseTable;
    private DefaultTableModel tableModel;
    private JLabel totalSpentLabel;
    private JLabel dailyAvgLabel;
    private JLabel largestExpenseLabel;
    
    // Budget components
    private JLabel budgetStatusLabel;
    private JPanel budgetProgressBar;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public MainWindow() {
        this.dbManager = new DatabaseManager();
        
        setupFrame();
        initializeLayout();
        refreshData();
    }

    private void setupFrame() {
        setTitle("WalletPad");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 680);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        try {
            java.net.URL iconUrl = MainWindow.class.getResource("/img/wallet.png");
            if (iconUrl != null) {
                setIconImage(new ImageIcon(iconUrl).getImage());
            }
        } catch (Exception e) {
            System.err.println("Could not load app icon: " + e.getMessage());
        }
    }

    private void initializeLayout() {
        setLayout(new BorderLayout());

        // 1. Sidebar (Category filters, search, logo)
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(240, 680));
        sidebar.setBackground(Color.decode("#FFF7C5"));
        sidebar.setBorder(new EmptyBorder(25, 20, 25, 20));

        // Logo
        JLabel logo = new JLabel(" WalletPad");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(Color.decode("#4F252E"));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        try {
            java.net.URL iconUrl = MainWindow.class.getResource("/img/wallet.png");
            if (iconUrl != null) {
                ImageIcon originalIcon = new ImageIcon(iconUrl);
                Image scaledImg = originalIcon.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(scaledImg));
            }
        } catch (Exception e) {
            System.err.println("Could not load logo icon: " + e.getMessage());
        }
        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel subtitle = new JLabel("Personal WalletPad");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(Color.decode("#4F252E"));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(subtitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Search Panel
        JLabel searchTitle = new JLabel("Search transactions");
        searchTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchTitle.setForeground(Color.decode("#4F252E"));
        searchTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(searchTitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(200, 32));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.putClientProperty("JTextField.placeholderText", "Type to search...");
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterData(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterData(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterData(); }
        });
        sidebar.add(searchField);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Category Filter Header
        JPanel catHeader = new JPanel(new BorderLayout());
        catHeader.setOpaque(false);
        catHeader.setMaximumSize(new Dimension(200, 20));
        catHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel catTitle = new JLabel("Categories");
        catTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        catTitle.setForeground(Color.decode("#4F252E"));
        catHeader.add(catTitle, BorderLayout.WEST);

        JButton addCatBtn = new JButton("+ New");
        addCatBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        addCatBtn.setFocusPainted(false);
        addCatBtn.setContentAreaFilled(false);
        addCatBtn.setBorderPainted(false);
        addCatBtn.setForeground(Color.decode("#F4AE52"));
        addCatBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addCatBtn.addActionListener(e -> {
            CategoryDialog dialog = new CategoryDialog(this, dbManager, this::refreshData);
            dialog.setVisible(true);
        });
        catHeader.add(addCatBtn, BorderLayout.EAST);

        sidebar.add(catHeader);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        // Categories List Box
        categoryFilterListPanel = new JPanel();
        categoryFilterListPanel.setLayout(new BoxLayout(categoryFilterListPanel, BoxLayout.Y_AXIS));
        categoryFilterListPanel.setOpaque(false);
        categoryFilterListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane sidebarScroll = new JScrollPane(categoryFilterListPanel);
        sidebarScroll.setBorder(null);
        sidebarScroll.setOpaque(false);
        sidebarScroll.getViewport().setOpaque(false);
        sidebarScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        sidebar.add(sidebarScroll);

        add(sidebar, BorderLayout.WEST);

        // 2. Main Dashboard Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.decode("#C1EBE9"));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // 2a. Dashboard Header
        JPanel dashboardHeader = new JPanel(new BorderLayout(20, 20));
        dashboardHeader.setOpaque(false);

        // Header Title
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setOpaque(false);
        JLabel mainTitle = new JLabel("Dashboard Overview");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainTitle.setForeground(Color.decode("#4F252E"));
        titlePanel.add(mainTitle);

        JLabel todayLabel = new JLabel("Today is " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        todayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        todayLabel.setForeground(Color.decode("#4F252E"));
        titlePanel.add(todayLabel);
        
        dashboardHeader.add(titlePanel, BorderLayout.WEST);

        // Budget Widget Panel
        JPanel budgetWidget = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#FFF7C5"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        budgetWidget.setLayout(new BoxLayout(budgetWidget, BoxLayout.Y_AXIS));
        budgetWidget.setBorder(new EmptyBorder(12, 18, 12, 18));
        budgetWidget.setPreferredSize(new Dimension(280, 80));
        budgetWidget.setOpaque(false);

        JPanel budgetTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        budgetTextPanel.setOpaque(false);
        budgetTextPanel.setMaximumSize(new Dimension(260, 40));
        
        JLabel budgetTitle = new JLabel("Monthly Budget Status");
        budgetTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        budgetTitle.setForeground(Color.decode("#4F252E"));
        budgetTextPanel.add(budgetTitle);

        budgetStatusLabel = new JLabel("LKR 0.00 / LKR 1000.00");
        budgetStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        budgetStatusLabel.setForeground(Color.decode("#4F252E"));
        budgetTextPanel.add(budgetStatusLabel);

        budgetWidget.add(budgetTextPanel);
        budgetWidget.add(Box.createRigidArea(new Dimension(0, 8)));

        // Custom Budget Progress Bar
        budgetProgressBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Track
                g2.setColor(Color.decode("#C1EBE9"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Active Fill
                double totalMonthSpent = CollectionsEngine.getTotalSpending(
                        CollectionsEngine.filterByMonth(allExpenses, YearMonth.now())
                );
                double percent = Math.min((totalMonthSpent / monthlyBudget), 1.0);
                int fillWidth = (int) (percent * getWidth());

                if (fillWidth > 0) {
                    // Turn progress bar burgundy if budget exceeded
                    if (totalMonthSpent > monthlyBudget) {
                        g2.setColor(Color.decode("#4F252E"));
                    } else {
                        g2.setColor(Color.decode("#F4AE52"));
                    }
                    g2.fillRoundRect(0, 0, fillWidth, getHeight(), 8, 8);
                }
                
                g2.dispose();
            }
        };
        budgetProgressBar.setPreferredSize(new Dimension(244, 8));
        budgetProgressBar.setMaximumSize(new Dimension(280, 8));
        budgetProgressBar.setOpaque(false);
        budgetWidget.add(budgetProgressBar);

        dashboardHeader.add(budgetWidget, BorderLayout.EAST);

        // Stats Cards Grid Layout
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 15, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(700, 85));

        statsRow.add(createStatCard("Total Spent (This Month)", totalSpentLabel = new JLabel("LKR 0.00"), "#FFF7C5"));
        statsRow.add(createStatCard("Daily Avg Spending", dailyAvgLabel = new JLabel("LKR 0.00"), "#C1EBE9"));
        statsRow.add(createStatCard("Largest Purchase", largestExpenseLabel = new JLabel("LKR 0.00"), "#F4AE52"));

        JPanel headerContainer = new JPanel();
        headerContainer.setLayout(new BoxLayout(headerContainer, BoxLayout.Y_AXIS));
        headerContainer.setOpaque(false);
        headerContainer.add(dashboardHeader);
        headerContainer.add(Box.createRigidArea(new Dimension(0, 20)));
        headerContainer.add(statsRow);

        mainPanel.add(headerContainer, BorderLayout.NORTH);

        // 2b. Transactions Table (Center Panel)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        tableContainer.setBorder(new EmptyBorder(35, 0, 0, 0));

        // Subheader (Title + Action Buttons)
        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setOpaque(false);

        JLabel tableTitle = new JLabel("Recent Transactions");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tableTitle.setForeground(Color.BLACK);
        tableHeaderPanel.add(tableTitle, BorderLayout.WEST);

        // Buttons Action Panel
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtonPanel.setOpaque(false);

        JButton addExpenseBtn = new JButton("+ Add Expense");
        configureHeaderButton(addExpenseBtn, Color.decode("#4F252E"), Color.decode("#FFF7C5"));
        addExpenseBtn.addActionListener(e -> {
            ExpenseDialog dialog = new ExpenseDialog(this, dbManager, this::refreshData);
            dialog.setVisible(true);
        });
        actionButtonPanel.add(addExpenseBtn);

        JButton analyticsBtn = new JButton("📊 Analytics Report");
        configureHeaderButton(analyticsBtn, Color.decode("#F4AE52"), Color.decode("#4F252E"));
        analyticsBtn.addActionListener(e -> {
            ReportWindow dialog = new ReportWindow(this, dbManager);
            dialog.setVisible(true);
        });
        actionButtonPanel.add(analyticsBtn);

        tableHeaderPanel.add(actionButtonPanel, BorderLayout.EAST);

        JPanel tableHeaderWrapper = new JPanel(new BorderLayout());
        tableHeaderWrapper.setOpaque(false);
        tableHeaderWrapper.add(tableHeaderPanel, BorderLayout.NORTH);
        tableHeaderWrapper.add(Box.createRigidArea(new Dimension(0, 16)), BorderLayout.SOUTH);
        tableContainer.add(tableHeaderWrapper, BorderLayout.NORTH);

        // Modernized Table Setup
        String[] columnNames = {"Description", "Category", "Date", "Amount"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };

        expenseTable = new JTable(tableModel);
        expenseTable.setRowHeight(38);
        expenseTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        expenseTable.setShowGrid(false);
        expenseTable.setIntercellSpacing(new Dimension(0, 0));
        expenseTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        expenseTable.getTableHeader().setPreferredSize(new Dimension(0, 32));
        expenseTable.getTableHeader().setReorderingAllowed(false);
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expenseTable.setBackground(Color.decode("#FFF7C5"));
        expenseTable.setSelectionBackground(Color.decode("#4F252E"));
        expenseTable.setSelectionForeground(Color.WHITE);

        // Add Popup Context Menu for Edit & Delete
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("✏️ Edit Expense");
        editItem.addActionListener(e -> handleEditExpense());
        popupMenu.add(editItem);

        JMenuItem deleteItem = new JMenuItem("🗑️ Delete Expense");
        deleteItem.addActionListener(e -> handleDeleteExpense());
        popupMenu.add(deleteItem);

        expenseTable.setComponentPopupMenu(popupMenu);

        // Apply Custom Cells Rendering
        expenseTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                c.setOpaque(true);
                if (isSelected) {
                    c.setBackground(Color.decode("#4F252E"));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(Color.decode("#FFF7C5"));
                    c.setForeground(Color.decode("#4F252E"));
                }
                return c;
            }
        });

        expenseTable.getColumnModel().getColumn(1).setCellRenderer(new CategoryRenderer());

        expenseTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                c.setOpaque(true);
                if (value instanceof LocalDate) {
                    c.setText(((LocalDate) value).format(DATE_FORMATTER));
                }
                if (isSelected) {
                    c.setBackground(Color.decode("#4F252E"));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(Color.decode("#FFF7C5"));
                    c.setForeground(Color.decode("#4F252E"));
                }
                return c;
            }
        });

        expenseTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                c.setHorizontalAlignment(SwingConstants.RIGHT);
                c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                c.setOpaque(true);
                if (value instanceof Double) {
                    c.setText(String.format("LKR %.2f", (Double) value));
                }
                if (isSelected) {
                    c.setBackground(Color.decode("#4F252E"));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(Color.decode("#FFF7C5"));
                    c.setForeground(Color.decode("#8B4513")); // Darker amber for readability
                }
                return c;
            }
        });

        JScrollPane tableScroll = new JScrollPane(expenseTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.decode("#FFF7C5"));
        tableScroll.setOpaque(false);
        tableScroll.getViewport().setOpaque(false);

        JPanel tableScrollWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#FFF7C5"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        tableScrollWrapper.setOpaque(false);
        tableScrollWrapper.setBorder(new EmptyBorder(6, 6, 6, 6));
        tableScrollWrapper.add(tableScroll, BorderLayout.CENTER);
        tableContainer.add(tableScrollWrapper, BorderLayout.CENTER);

        mainPanel.add(tableContainer, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JButton createRoundedButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
                // Draw button text centered
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g3.setColor(fg);
                g3.setFont(getFont());
                FontMetrics fm = g3.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g3.drawString(getText(), textX, textY);
                g3.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(175, 38));
        btn.setMinimumSize(new Dimension(175, 38));
        return btn;
    }

    private void configureHeaderButton(JButton btn, Color background, Color foreground) {
        btn.putClientProperty("JButton.buttonType", "filled");
        btn.setBackground(background);
        btn.setForeground(foreground);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.setPreferredSize(new Dimension(170, 36));
        btn.setMinimumSize(new Dimension(170, 36));
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String hexColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#FFF7C5"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                // Side color stripe
                g2.setColor(Color.decode(hexColor));
                g2.fillRoundRect(0, 0, 6, getHeight(), 8, 8);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(15, 20, 15, 15));
        card.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(Color.decode("#4F252E"));
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(Color.decode("#4F252E"));

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(valueLabel);

        return card;
    }

    // Load items from database, apply active categories & search filters, and rebuild components
    private void refreshData() {
        allExpenses = dbManager.getAllExpenses();
        
        rebuildCategorySidebar();
        filterData();
    }

    private void rebuildCategorySidebar() {
        categoryFilterListPanel.removeAll();
        
        List<Category> categories = dbManager.getAllCategories();

        // 1. "All Categories" Toggle Button
        JButton allBtn = createSidebarFilterButton("🌐  All Expenses", activeCategoryFilter == null, "#4F252E");
        allBtn.addActionListener(e -> {
            activeCategoryFilter = null;
            refreshSidebarSelection();
            filterData();
        });
        categoryFilterListPanel.add(allBtn);
        categoryFilterListPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // 2. Individual Category Buttons
        for (Category cat : categories) {
            boolean isSelected = activeCategoryFilter != null && activeCategoryFilter.getId() == cat.getId();
            JButton btn = createSidebarFilterButton(cat.getIcon() + "  " + cat.getName(), isSelected, cat.getColorHex());
            btn.addActionListener(e -> {
                activeCategoryFilter = cat;
                refreshSidebarSelection();
                filterData();
            });
            categoryFilterListPanel.add(btn);
            categoryFilterListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        categoryFilterListPanel.revalidate();
        categoryFilterListPanel.repaint();
    }

    private JButton createSidebarFilterButton(String label, boolean isSelected, String hexColor) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI Emoji", isSelected ? Font.BOLD : Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setPreferredSize(new Dimension(200, 36));
        btn.setMaximumSize(new Dimension(200, 36));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 15, 0, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (isSelected) {
            btn.setBackground(Color.decode("#C1EBE9"));
            btn.setForeground(Color.decode("#4F252E"));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Color.decode(hexColor)),
                BorderFactory.createEmptyBorder(0, 12, 0, 15)
            ));
        } else {
            btn.setBackground(Color.decode("#FFF7C5"));
            btn.setForeground(Color.decode("#4F252E"));
            btn.setBorder(new EmptyBorder(0, 15, 0, 15));
        }
        
        return btn;
    }

    private void refreshSidebarSelection() {
        rebuildCategorySidebar();
    }

    private void filterData() {
        // Apply category filter in-memory using CollectionsEngine
        displayedExpenses = CollectionsEngine.filterByCategory(allExpenses, activeCategoryFilter);

        // Apply search query filter in-memory using CollectionsEngine
        String query = searchField.getText();
        displayedExpenses = CollectionsEngine.filterBySearch(displayedExpenses, query);

        // Sort by date (descending) using CollectionsEngine
        displayedExpenses = CollectionsEngine.sortExpenses(displayedExpenses);

        // Rebuild table rows
        tableModel.setRowCount(0);
        for (Expense exp : displayedExpenses) {
            tableModel.addRow(new Object[]{
                    exp.getDescription(),
                    exp.getCategory(),
                    exp.getDate(),
                    exp.getAmount()
            });
        }

        updateDashboardStats();
    }

    private void updateDashboardStats() {
        YearMonth thisMonth = YearMonth.now();

        // Standard stats for the active displayed set in this month
        List<Expense> monthlyExpenses = CollectionsEngine.filterByMonth(allExpenses, thisMonth);
        double totalMonthSpent = CollectionsEngine.getTotalSpending(monthlyExpenses);
        double dailyAvg = CollectionsEngine.getAverageDailySpending(allExpenses, thisMonth);
        Expense largest = CollectionsEngine.getLargestExpense(monthlyExpenses);

        totalSpentLabel.setText(String.format("LKR %.2f", totalMonthSpent));
        dailyAvgLabel.setText(String.format("LKR %.2f", dailyAvg));
        largestExpenseLabel.setText(largest != null ? String.format("LKR %.2f", largest.getAmount()) : "LKR 0.00");

        // Progress bar and budget string update
        budgetStatusLabel.setText(String.format("LKR %.2f / LKR %.2f", totalMonthSpent, monthlyBudget));
        budgetProgressBar.repaint();
    }

    // --- GRID/CONTEXT ACTIONS ---

    private void handleEditExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow == -1) return;

        Expense selectedExpense = displayedExpenses.get(selectedRow);
        ExpenseDialog dialog = new ExpenseDialog(this, dbManager, selectedExpense, this::refreshData);
        dialog.setVisible(true);
    }

    private void handleDeleteExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow == -1) return;

        Expense selectedExpense = displayedExpenses.get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this expense:\n\"" + selectedExpense.getDescription() + "\" (LKR " + selectedExpense.getAmount() + ")?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dbManager.deleteExpense(selectedExpense.getId());
            refreshData();
        }
    }

    // Inner Custom Renderer class for Category Tags in Table
    private static class CategoryRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Category) {
                Category cat = (Category) value;
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
                label.setText(" " + iconText + "  " + cat.getName() + " ");
                label.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
                
                Color catColor = Color.decode(cat.getColorHex());
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.LEFT);

                Color backgroundColor;
                Color foregroundColor;
                switch (cat.getName()) {
                    case "Food" -> {
                        backgroundColor = Color.decode("#D8F0E4");
                        foregroundColor = Color.decode("#1A5B43");
                    }
                    case "Shopping" -> {
                        backgroundColor = Color.decode("#FFE5C8");
                        foregroundColor = Color.decode("#7F4A20");
                    }
                    case "Travel" -> {
                        backgroundColor = Color.decode("#D8E9FF");
                        foregroundColor = Color.decode("#214B76");
                    }
                    case "Bills" -> {
                        backgroundColor = Color.decode("#F4E8D4");
                        foregroundColor = Color.decode("#5C3F23");
                    }
                    case "Entertainment" -> {
                        backgroundColor = Color.decode("#F2D4FF");
                        foregroundColor = Color.decode("#5A2E78");
                    }
                    default -> {
                        backgroundColor = new Color(catColor.getRed(), catColor.getGreen(), catColor.getBlue(), 140);
                        foregroundColor = catColor.darker();
                    }
                }

                if (isSelected) {
                    label.setBackground(Color.decode("#4F252E"));
                    label.setForeground(Color.WHITE);
                    label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 1, true),
                        BorderFactory.createEmptyBorder(4, 10, 4, 10)
                    ));
                } else {
                    Color borderColor = backgroundColor.darker();
                    label.setBackground(backgroundColor);
                    label.setForeground(foregroundColor);
                    label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor, 1, true),
                        BorderFactory.createEmptyBorder(4, 10, 4, 10)
                    ));
                }
            }
            return label;
        }
    }
}
