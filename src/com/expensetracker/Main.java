package com.expensetracker;

import com.formdev.flatlaf.FlatDarkLaf;
import com.expensetracker.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Configure FlatLaf modern UI theme customizations
        configureLafTheme();

        // Bootstrapping the application in Swing's Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow frame = new MainWindow();
                frame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Critical error launching WalletPad application: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static void configureLafTheme() {
        try {
            // Set modern UI element attributes before initializing FlatLaf
            UIManager.put("Button.arc", 8);                 // Semi-rounded modern buttons
            UIManager.put("Component.arc", 8);              // Rounded textfields, comboboxes, panels
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 999);        // Smooth, pill-shaped scrollbar thumbs
            UIManager.put("ScrollBar.width", 10);            // Sleek slim scrollbar widths
            // Set default theme backgrounds to 4F252E (Burgundy) instead of default dark gray
            UIManager.put("Panel.background", Color.decode("#4F252E"));
            UIManager.put("Dialog.background", Color.decode("#4F252E"));
            UIManager.put("Frame.background", Color.decode("#4F252E"));
            UIManager.put("ScrollPane.background", Color.decode("#4F252E"));
            UIManager.put("Viewport.background", Color.decode("#4F252E"));
            UIManager.put("Table.background", Color.decode("#4F252E"));
            UIManager.put("TableHeader.background", Color.decode("#4F252E"));
            UIManager.put("TableHeader.foreground", Color.decode("#C1EBE9"));
            
            // Text inputs and lists
            UIManager.put("TextField.background", Color.decode("#4F252E"));
            UIManager.put("TextField.foreground", Color.decode("#C1EBE9"));
            UIManager.put("ComboBox.background", Color.decode("#4F252E"));
            UIManager.put("ComboBox.foreground", Color.decode("#C1EBE9"));
            UIManager.put("List.background", Color.decode("#4F252E"));
            UIManager.put("List.foreground", Color.decode("#C1EBE9"));
            
            // Popup context menus
            UIManager.put("PopupMenu.background", Color.decode("#4F252E"));
            UIManager.put("PopupMenu.foreground", Color.decode("#C1EBE9"));
            UIManager.put("MenuItem.background", Color.decode("#4F252E"));
            UIManager.put("MenuItem.foreground", Color.decode("#C1EBE9"));
            UIManager.put("MenuItem.selectionBackground", Color.decode("#F4AE52"));
            UIManager.put("MenuItem.selectionForeground", Color.decode("#4F252E"));

            UIManager.put("Table.alternateRowColor", Color.decode("#4F252E"));
            UIManager.put("Table.selectionBackground", Color.decode("#F4AE52"));
            UIManager.put("Table.selectionForeground", Color.decode("#4F252E"));
            UIManager.put("TableHeader.bottomSeparatorColor", Color.decode("#4F252E"));

            // Initialize the flat dark look and feel
            FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("FlatLaf setup failed, falling back to System Look & Feel: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
