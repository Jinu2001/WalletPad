package com.expensetracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Expense {
    private int id;
    private double amount;
    private Category category;
    private String description;
    private LocalDate date;
    private LocalDateTime createdAt;

    public Expense(int id, double amount, Category category, String description, LocalDate date, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.createdAt = createdAt;
    }

    public Expense(double amount, Category category, String description, LocalDate date) {
        this(-1, amount, category, description, date, LocalDateTime.now());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return id == expense.id &&
                Double.compare(expense.amount, amount) == 0 &&
                Objects.equals(category, expense.category) &&
                Objects.equals(description, expense.description) &&
                Objects.equals(date, expense.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount, category, description, date);
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", amount=" + amount +
                ", category=" + category +
                ", description='" + description + '\'' +
                ", date=" + date +
                '}';
    }
}
