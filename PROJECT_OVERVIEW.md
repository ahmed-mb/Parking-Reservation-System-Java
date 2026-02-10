# Project Overview

## What This Project Is

This is a **Parking Reservation System** originally built with ASP.NET Web Forms, migrated to **Java Spring Boot** (backend) + **React** (frontend). The goal was an exact functional clone -- same business logic, same table designs, same UI layout and styling.

## Original ASP.NET Application

**Location**: `C:\Users\Ahmed\Projects\Parking-Reservation-System\Parking Reservation System\`

The original system used:
- ASP.NET Web Forms with code-behind (.aspx / .aspx.cs)
- SQL Server LocalDB
- PBKDF2 password hashing (Rfc2898DeriveBytes, 100k iterations)
- Cookie-based session authentication
- reCAPTCHA v2 (checkbox)
- Bootstrap 4 + custom CSS
- GridView controls for tabular data

### Original ASP.NET Pages
| Page | Purpose |
|------|---------|
| Home.aspx | Public landing page with "Check Availability" |
| Login.aspx | Login with reCAPTCHA |
| Register.aspx | Registration form |
| C_userPage.aspx | Customer dashboard -- "Book Now" button |
| C_userBooking.aspx | Current active booking -- cancel / report spot taken |
| C_userHistory.aspx | Booking history table |
| C_userInfo.aspx | Profile -- GridView with inline editing |
| A_viewBooking.aspx | Admin -- view all bookings |
| A_viewCustomer.aspx | Admin -- manage customers (edit credit, delete) |
| A_viewParking.aspx | Admin -- view parking spot status |
| 404.aspx | Custom 404 page |

## Java Clone

**Location**: `C:\Users\Ahmed\Projects\Parking-Reservation-System-Java\`

### Feature Comparison

| Feature | ASP.NET | Java + React | Notes |
|---------|---------|-------------|-------|
| User registration | Yes | Yes | Same fields, same validation |
| Login with lockout | 5 attempts / 15 min | 5 attempts / 15 min | Identical logic |
| reCAPTCHA | v2 (checkbox) | v3 (invisible) | Upgraded -- better UX |
| Password hashing | PBKDF2 | BCrypt | Industry standard equivalent |
| Book parking ($6) | Yes | Yes | Same cost, same deduction |
| Cancel booking (refund $6) | Yes | Yes | Same refund logic |
| Report spot taken | Yes | Yes | Same reassignment logic |
| Booking history | Yes | Yes | Same column order |
| Profile inline edit | GridView | Table + input fields | Same layout |
| Admin edit customer credit | Yes | Yes | Same behavior |
| Admin delete booking (refund) | Yes | Yes | Handles "Unknown" bookings |
| Daily reset (1 AM EST) | Yes | Yes | Same schedule |
| Custom 404 page | Yes | Yes | Same concept |
| Modern alert modals | Custom JS/CSS | React context + CSS | Same themed styling |
| Swagger API docs | No | Yes | Added in migration |
| Health monitoring | No | Actuator | Added in migration |

### Terminology Alignment

The Java clone uses the same terminology as the ASP.NET original:
- Parking status: `"available"`, `"booked"`, `"unknown"` (not "occupied")
- Booking status: `"Active"`, `"Cancelled"`, `"Completed"`
- User roles: `"Customer"`, `"Admin"`
- Table name: `logintable` (preserves original naming)

### UI Parity

Every customer and admin page matches the ASP.NET layout:
- **Dashboard**: Single "Book Now" card (matches C_userPage.aspx)
- **Current Booking**: Table with ID, User ID, Username, Contact, Credit, Car Plate, Parking Spot, Date, Actions (matches C_userBooking.aspx)
- **Booking History**: Columns ending in `...Parking Spot, Date, Status` (matches C_userHistory.aspx column order)
- **User Profile**: Table with inline editing via icon buttons (matches C_userInfo.aspx GridView)
- **Admin Panel**: Tabbed view for Bookings, Customers, Parking (matches A_viewBooking/A_viewCustomer/A_viewParking)
- **Modern Alerts**: Themed modals (success/green, error/red, warning/yellow, confirm, info/teal, cancel/orange) matching ASP.NET's modern-alerts.css

### What Changed in the Migration

| Aspect | ASP.NET | Java + React |
|--------|---------|-------------|
| Architecture | Server-rendered pages | REST API + SPA |
| State | ViewState + sessions | React state + JWT |
| Navigation | Full page reloads | Client-side routing |
| Data access | Manual SQL (DatabaseHelper) | Spring Data JPA |
| Transactions | Manual | @Transactional |
| Error handling | Try-catch per method | @RestControllerAdvice |
| Build | MSBuild | Maven + Vite |
| Alerts | Browser alert()/confirm() | Themed React modal system |
