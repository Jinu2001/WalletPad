package com.expensetracker.ui;

import com.expensetracker.database.DatabaseManager;
import com.expensetracker.model.Category;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class CategoryDialog extends JDialog {
    private final DatabaseManager dbManager;
    private final Runnable onCategoryAdded;

    private JTextField nameField;
    private String selectedEmoji = "🍔";
    private String selectedColor = "#C1EBE9";
    
    private final String[] emojis = {
        "🍔", "🛍️", "✈️", "🧾", "🎬", "🏠", "🚗", "💊", 
        "🎓", "🎁", "💪", "🐾", "🔌", "💼", "🌐", "🛒"
    };

    private final String[] colors = {
        "#C1EBE9", "#FFF7C5", "#F4AE52", "#4F252E"
    };

    private final List<JButton> emojiButtons = new ArrayList<>();
    private final List<JButton> colorButtons = new ArrayList<>();

    public CategoryDialog(Frame owner, DatabaseManager dbManager, Runnable onCategoryAdded) {
        super(owner, "New Category", true);
        this.dbManager = dbManager;
        this.onCategoryAdded = onCategoryAdded;

        setupUI();
    }

    private void setupUI() {
        setSize(400, 480);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. Name Input
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.setMaximumSize(new Dimension(360, 60));
        namePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel nameLabel = new JLabel("Category Name");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        namePanel.add(nameLabel, BorderLayout.NORTH);

        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(360, 32));
        nameField.putClientProperty("JTextField.placeholderText", "e.g., Groceries");
        namePanel.add(nameField, BorderLayout.CENTER);
        contentPanel.add(namePanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 2. Emoji Selector Grid
        JLabel emojiLabel = new JLabel("Choose Icon");
        emojiLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emojiLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(emojiLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JPanel emojiGrid = new JPanel(new GridLayout(2, 8, 5, 5));
        emojiGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        emojiGrid.setMaximumSize(new Dimension(360, 80));

        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(40, 40));
            
            // Set initial selected state style
            if (emoji.equals(selectedEmoji)) {
                btn.setBackground(Color.decode("#C1EBE9"));
                btn.setBorder(BorderFactory.createLineBorder(Color.decode("#4F252E"), 2));
            } else {
                btn.setBackground(Color.decode("#FFF7C5"));
                btn.setBorder(BorderFactory.createLineBorder(Color.decode("#4F252E"), 1));
            }

            btn.addActionListener(e -> {
                selectedEmoji = emoji;
                updateEmojiSelection();
            });
            emojiButtons.add(btn);
            emojiGrid.add(btn);
        }
        contentPanel.add(emojiGrid);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 3. Color Selector
        JLabel colorLabel = new JLabel("Choose Tag Color");
        colorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        colorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(colorLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JPanel colorGrid = new JPanel(new GridLayout(1, 8, 8, 8));
        colorGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorGrid.setMaximumSize(new Dimension(360, 40));

        for (String hex : colors) {
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int size = Math.min(getWidth(), getHeight()) - 8;
                    int x = (getWidth() - size) / 2;
                    int y = (getHeight() - size) / 2;
                    
                    g2.setColor(Color.decode(hex));
                    g2.fillOval(x, y, size, size);
                    
                    // Draw outer ring if selected
                    if (hex.equalsIgnoreCase(selectedColor)) {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2));
                        g2.drawOval(x - 2, y - 2, size + 4, size + 4);
                    }
                    g2.dispose();
                }
            };
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setPreferredSize(new Dimension(36, 36));
            btn.addActionListener(e -> {
                selectedColor = hex;
                updateColorSelection();
            });
            colorButtons.add(btn);
            colorGrid.add(btn);
        }
        contentPanel.add(colorGrid);
        contentPanel.add(Box.createVerticalGlue());

        add(contentPanel, BorderLayout.CENTER);

        // 4. Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.decode("#4F252E")));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        actionPanel.add(cancelBtn);

        JButton saveBtn = new JButton("Save Category");
        saveBtn.putClientProperty("JButton.buttonType", "filled");
        saveBtn.setBackground(Color.decode(selectedColor));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(this::handleSave);
        actionPanel.add(saveBtn);

        add(actionPanel, BorderLayout.SOUTH);
    }

    private void updateEmojiSelection() {
        for (int i = 0; i < emojis.length; i++) {
            JButton btn = emojiButtons.get(i);
            String emoji = emojis[i];
            if (emoji.equals(selectedEmoji)) {
                btn.setBackground(Color.decode("#C1EBE9"));
                btn.setBorder(BorderFactory.createLineBorder(Color.decode("#4F252E"), 2));
            } else {
                btn.setBackground(Color.decode("#FFF7C5"));
                btn.setBorder(BorderFactory.createLineBorder(Color.decode("#4F252E"), 1));
            }
        }
    }

    private void updateColorSelection() {
        // Redraw color buttons to reflect chosen highlight ring
        for (JButton btn : colorButtons) {
            btn.repaint();
        }
        
        // Update save button background color to match chosen theme color
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                JPanel ap = (JPanel) comp;
                if (ap.getLayout() instanceof FlowLayout) {
                    for (Component c : ap.getComponents()) {
                        if (c instanceof JButton && "Save Category".equals(((JButton) c).getText())) {
                            c.setBackground(Color.decode(selectedColor));
                        }
                    }
                }
            }
        }
        
        // Refresh emoji border colors to match newly selected category color
        updateEmojiSelection();
    }

    private void handleSave(ActionEvent e) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a category name.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Add to database
        Category category = new Category(name, selectedColor, selectedEmoji);
        dbManager.addCategory(category);

        if (category.getId() != -1) {
            if (onCategoryAdded != null) {
                onCategoryAdded.run();
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Category name must be unique. Choose another name.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
