# DineFlow POS — Restaurant Billing & Table Management System

A restaurant billing, table reservation, menu management, and point-of-sale (POS) application built around a Java domain model (`Table`, `MenuItem`, `Order`, `Bill`, `DiscountPolicy`, and `RestaurantService`) paired with a browser-based interface (`public/`).

## Features

* **Dashboard Overview**: Track live revenue, active orders, table occupancy, and reservations alongside a mini floor plan and recent settled bills.
* **Tables & Reservations**: Manage dining tables (`T1`–`T10`), add new tables, schedule reservations with guest details, times, and notes, seat guests, and monitor active table totals.
* **Order Taking & POS Cart**: Filter dishes by category or dietary preference (Veg/Non-Veg), assign orders to a dining table or Takeaway counter, add kitchen notes, and send Kitchen Order Tickets (KOTs) for multi-round dining.
* **Billing, Discounts & Settlement**: Apply loyalty tiers or custom percentage/flat discounts, configure applicable tax slabs and service charges, calculate cash change due, and generate itemized invoices.
* **Menu Management & Billing History**: Add, edit, toggle stock availability, or delete dishes, and search or reprint past bills.

## Project Structure

```text
Restaurant_billing/
├── README.md
├── public/
│   ├── index.html
│   ├── app.js
│   └── style.css
└── src/
    ├── Bill.java
    ├── DiscountPolicy.java
    ├── Main.java
    ├── MenuItem.java
    ├── Order.java
    ├── RestaurantService.java
    └── Table.java
