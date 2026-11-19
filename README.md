# 🏨 Hotel Management App: Secure JavaFX Application

> 💼 **Cybersecurity Internship at Network Associates (NA), Summer 2025**

A desktop hotel management platform for **clients** and **staff**, built with **Java 17, JavaFX and MySQL**. Clients create an account, browse available rooms and book stays with check-in/check-out dates. Staff and admins manage bookings, users, roles and access requests from dashboards.

> **My role:** during my internship I was responsible for the **security** of the application: analysing its vulnerabilities and implementing the protections below.

## 🔐 Security hardening (my contribution)

| # | Vulnerability | Before | What I implemented |
|---|---|---|---|
| 1 | **Malformed input** | No validation on user input | Validation of e-mail format, password strength and name format (letters only) |
| 2 | **SQL injection** | SQL built by string concatenation | `PreparedStatement` for every database query, plus an `InputSanitizer` class that detects suspicious patterns with regular expressions |
| 3 | **Privilege escalation** | Anyone could click "staff" at login and get admin rights | Hierarchical approval: a "staff" sign-up sends a request to existing staff, who accept or decline it |
| 4 | **No identity verification** | No user accounts at all | E-mail verification with a one-time 4-digit code that expires after 5 minutes; resend limited to once every 30 s; unverified accounts cannot log in |
| 5 | **Plain-text passwords** | Passwords stored in clear in the database | **BCrypt** hashing with automatic salt; login compares against the hash |
| 6 | **Brute-force attacks** | Unlimited login attempts | Account locked for **10 minutes after 3 failed attempts**, with the remaining time shown to the user |
| 7 | **Oversized input / buffer overflow** | No length limits on fields | Length limits on input fields (e.g. 25 characters for first/last name) enforced at the UI layer |

Each fix was analysed in terms of its impact on **confidentiality, integrity and availability** (CIA triad).

## Features

- Account creation with e-mail verification
- Room browsing and online booking (check-in / check-out)
- Role-based access: client vs staff/admin
- Admin dashboards: bookings, users, roles and pending privilege requests

## Tech stack

Java 17 · JavaFX (FXML, ControlsFX) · MySQL · JDBC · Maven · jBCrypt · Jakarta Mail (SMTP)

## Where to find the security code

| Protection | File |
|---|---|
| Input validation & sanitization | `utils/InputSanitizer.java`, `utils/PasswordValidator.java` |
| Password hashing (BCrypt) | `utils/PasswordHasher.java` |
| E-mail verification / 2FA tokens | `TokenService.java`, `EmailService.java`, `VerifyController.java` |
| Staff approval workflow | `VerifyController.java`, `StaffDashboardController.java` |
| SQL injection protection | `PreparedStatement` in every controller and `DatabaseUtil.java` |
| Brute-force lockout | `LoginController.java` |
| Password reset | `ForgotPasswordController.java` |

## Run locally

1. Install **JDK 17+** and **MySQL**, and create a database named `hoteldb`.
2. Set your credentials as environment variables (nothing secret is stored in the code):
   ```bash
   # macOS / Linux
   export DB_URL="jdbc:mysql://127.0.0.1:3306/hoteldb"
   export DB_USER="root"
   export DB_PASSWORD="your_password"
   export SMTP_USER="you@gmail.com"            # account that sends verification codes
   export SMTP_APP_PASSWORD="your_app_password" # Gmail app password
   ```
   ```powershell
   # Windows PowerShell
   $env:DB_PASSWORD="your_password"
   $env:SMTP_USER="you@gmail.com"
   $env:SMTP_APP_PASSWORD="your_app_password"
   ```
   Then run `src/main/resources/create_forgotten_password_token_table.sql` on the database.
3. Run the app:
   ```bash
   ./mvnw clean javafx:run
   ```

## Report

- 📄 [Internship report (INT101)](docs/Amine.BenNasrallah_INT%20101%20-%20Clean.pdf)

## Credits

Developed during a cybersecurity internship at **Network Associates (NA)**, Summer 2025. The base application started in [Gaston202/HospitalManagement](https://github.com/Gaston202/HospitalManagement); the security hardening above is my work.
