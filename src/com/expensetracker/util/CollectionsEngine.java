package com.expensetracker.util;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionsEngine {

    /**
     * Filters expenses by category. If category is null, returns all expenses.
     */
    public static List<Expense> filterByCategory(List<Expense> expenses, Category category) {
        if (category == null) {
            return new ArrayList<>(expenses);
        }
        return expenses.stream()
                .filter(e -> e.getCategory() != null && e.getCategory().getId() == category.getId())
                .collect(Collectors.toList());
    }

    /**
     * Filters expenses by search query (checks description and category name, case-insensitive).
     */
    public static List<Expense> filterBySearch(List<Expense> expenses, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(expenses);
        }
        String lowerQuery = query.toLowerCase().trim();
        return expenses.stream()
                .filter(e -> (e.getDescription() != null && e.getDescription().toLowerCase().contains(lowerQuery)) ||
                        (e.getCategory() != null && e.getCategory().getName().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
    }

    /**
     * Filters expenses that belong to a specific YearMonth.
     */
    public static List<Expense> filterByMonth(List<Expense> expenses, YearMonth yearMonth) {
        if (yearMonth == null) {
            return new ArrayList<>(expenses);
        }
        return expenses.stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(yearMonth))
                .collect(Collectors.toList());
    }

    /**
     * Sorts expenses by date in descending order (newest first), then by amount descending.
     */
    public static List<Expense> sortExpenses(List<Expense> expenses) {
        return expenses.stream()
                .sorted(Comparator.comparing(Expense::getDate, Comparator.reverseOrder())
                        .thenComparing(Expense::getAmount, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Calculates the sum of amounts for a list of expenses.
     */
    public static double getTotalSpending(List<Expense> expenses) {
        return expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    /**
     * Computes the total spending grouped by Category for a given month and year.
     * The results are returned as a Map, sorted by spending in descending order.
     */
    public static Map<Category, Double> getCategoryBreakdown(List<Expense> expenses, YearMonth yearMonth) {
        List<Expense> monthlyExpenses = filterByMonth(expenses, yearMonth);

        // Group by category and sum up amounts
        Map<Category, Double> breakdown = monthlyExpenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        // Sort the map by value descending (LinkedHashMap preserves insertion order)
        return breakdown.entrySet().stream()
                .sorted(Map.Entry.<Category, Double>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Gets the largest single expense in a list of expenses.
     * Returns null if list is empty.
     */
    public static Expense getLargestExpense(List<Expense> expenses) {
        return expenses.stream()
                .max(Comparator.comparingDouble(Expense::getAmount))
                .orElse(null);
    }

    /**
     * Calculates the average daily spending for a given month.
     * Divides total spending in that month by the number of days in the month
     * (or the number of elapsed days if it is the current month).
     */
    public static double getAverageDailySpending(List<Expense> expenses, YearMonth yearMonth) {
        List<Expense> monthlyExpenses = filterByMonth(expenses, yearMonth);
        if (monthlyExpenses.isEmpty()) {
            return 0.0;
        }

        double total = getTotalSpending(monthlyExpenses);
        
        int days;
        YearMonth currentMonth = YearMonth.now();
        if (yearMonth.equals(currentMonth)) {
            // For the current month, divide by current day of month to get realistic average
            days = LocalDate.now().getDayOfMonth();
        } else {
            days = yearMonth.lengthOfMonth();
        }

        return total / days;
    }
}
