# WaalletPad - Expense Tracker

A Java Swing expense tracker application with SQLite persistence, category-based budgeting, and monthly analytics.

## Features

- Add and manage expense categories
- Track daily spending in multiple categories
- View monthly totals, daily averages, and largest expense
- Visual category breakdown in analytics reports
- Local SQLite database for persistent storage

## Requirements

- Java 17
- SQLite JDBC (included in `lib/`)

## Run locally

Open PowerShell in the project root and run:

```powershell
cd /d "d:\Java\Expense Tracker"
& "C:\java17\bin\javac.exe" -cp "lib\*" -d bin src\com\expensetracker\Main.java
& "C:\java17\bin\java.exe" -cp "bin;lib\*" com.expensetracker.Main
```

If Java is on your PATH, use:

```powershell
cd /d "d:\Java\Expense Tracker"
javac -cp "lib\*" -d bin src\com\expensetracker\Main.java
java -cp "bin;lib\*" com.expensetracker.Main
```

## Notes

- Do not commit the generated `bin/` directory or `expenses.db` file.
- The app uses FlatLaf for theming and SQLite for local data storage.
