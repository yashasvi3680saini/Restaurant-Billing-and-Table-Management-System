# Restaurant Billing and Table Management System

A working Java + HTML/CSS/JavaScript project for your evaluation. It uses Core Java's built-in `HttpServer` and in-memory collections (no MySQL/JDBC yet), as requested for Evaluation 1. Data resets when the server restarts. JDBC/MySQL can be added for Evaluation 2.

## Requirements
- JDK 17 or newer (JDK 21 recommended)
- VS Code Java Extension Pack

## Run
1. Extract this folder.
2. Open the extracted `Restaurant_Billing_Table_Management` folder in VS Code.
3. Open `src/Main.java`.
4. Run `Main.java` (Run button above `main`).
5. Wait for `Restaurant system running at http://localhost:8080`.
6. Open **http://localhost:8080** in Chrome.

Do NOT use Live Server for this project. Java serves the frontend and API together.
If port 8080 is already occupied, stop the old Java run with Ctrl+C and run again.

## Features
- Table status and table allocation
- Menu listing and adding menu items
- Create an order for an available table
- Add item quantities and remove order items
- Subtotal, 5% tax, discount, final bill
- Checkout and mark payment method
- Orders and bills shown on dashboard
- Java OOP classes, collections, discount polymorphism, and an edit-history stack

All data is stored in memory for this evaluation; restarting Java resets demo data.
