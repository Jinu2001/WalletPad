package com.expensetracker.model;

import java.util.Objects;

public class Category {
    private int id;
    private String name;
    private String colorHex;
    private String icon; // Emoji icon representing the category (e.g., "🍔", "🛍️")

    public Category(int id, String name, String colorHex, String icon) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
        this.icon = icon;
    }

    public Category(String name, String colorHex, String icon) {
        this(-1, name, colorHex, icon);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id && Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return (icon != null && !icon.isEmpty() ? icon + " " : "") + name;
    }
}
