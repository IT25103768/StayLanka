# StayLanka

StayLanka is a server-rendered hotel reservation management system for customers, hotel staff, and administrators. It is a Java 21 modular monolith built with Spring Boot 4.1, Spring MVC, Spring Data JPA, Spring Security, Thymeleaf, Flyway, Bootstrap 5, and MySQL.

## Features

- Public room availability search by dates, capacity, type, and price
- Customer registration, profile management, reservation history, reviews, and guest requests
- Collision-safe reservation references, price snapshots, promotions, explicit status transitions, and overlap protection
- Staff reservation processing, room operations, transactional check-in/check-out, and additional charges
- Admin staff accounts, room types, promotions, review moderation, and operational dashboards
- Ownership checks, role authorization, BCrypt passwords, CSRF protection, validation, safe deactivation, and friendly error pages
- Responsive, accessible Thymeleaf interface with pagination, status badges, empty states, and confirmation prompts

## Requirements

- JDK 21
- MySQL 8.0 or newer
- Bash, PowerShell, or Command Prompt for the included Maven Wrapper

Maven does not need to be installed. The first wrapper run downloads Maven 3.9.16.

## Database setup

Run the following as a MySQL administrator. Replace the sample password before use.

```sql
CREATE DATABASE IF NOT EXISTS staylanka
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'staylanka'@'localhost'
  IDENTIFIED BY 'replace-with-a-local-password';
GRANT ALL PRIVILEGES ON staylanka.* TO 'staylanka'@'localhost';
FLUSH PRIVILEGES;
```

Flyway owns the schema. At startup it applies `src/main/resources/db/migration/V1__initial_schema.sql`; Hibernate then validates the mapped entities against it.

## Configuration

Set environment variables in the terminal or IDE. `.env.example` lists every supported value; it is an example only and is not loaded automatically.

Linux/macOS:

```bash
export DB_URL='jdbc:mysql://localhost:3306/staylanka?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Colombo'
export DB_USERNAME='staylanka'
export DB_PASSWORD='replace-with-a-local-password'
export UPLOAD_DIR='./uploads'
```

PowerShell:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/staylanka?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Colombo'
$env:DB_USERNAME = 'staylanka'
$env:DB_PASSWORD = 'replace-with-a-local-password'
$env:UPLOAD_DIR = './uploads'
```

The default profile never inserts users or sample data. The `dev` profile inserts sample room types, rooms, and a promotion. It creates administrator and staff accounts only when the corresponding passwords are supplied:

```bash
export DEV_ADMIN_EMAIL='admin@staylanka.lk'
export DEV_ADMIN_PASSWORD='choose-a-strong-development-password'
export DEV_STAFF_EMAIL='staff@staylanka.lk'
export DEV_STAFF_PASSWORD='choose-a-different-development-password'
```

Passwords are BCrypt-hashed before persistence and are never logged. Do not commit real credentials or a populated `.env` file.

## Run

From the project directory:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Windows Command Prompt:

```bat
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Open <http://localhost:8080>. Omit the `dev` profile for an empty production-style database.

## Build and test

```bash
./mvnw clean verify
```

The test profile uses an isolated H2 database in MySQL compatibility mode, runs the real Flyway migration, and keeps development seeding disabled. The integration suite covers:

- security redirects and CUSTOMER/STAFF/ADMIN route boundaries
- public Thymeleaf rendering
- reservation dates, capacity, price snapshots, cancellation, and overlapping-booking prevention
- percentage and fixed promotion strategies, eligibility, and the zero price floor
- reservation confirmation through check-in, charges, check-out, and one-review-per-stay
- guest request ownership, assignment, response, resolution, closure, and complete history

To build an executable archive without rerunning tests:

```bash
./mvnw package -DskipTests
java -jar target/staylanka-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## Architecture

The code follows `Controller → Service → Repository → database`, organized by feature under `com.staylanka`. Controllers bind validated form DTOs; services own business rules and transaction boundaries; Spring Data repositories perform persistence.

| Area | Important implementation |
|---|---|
| Authentication and authorization | `SecurityConfig`, `RegistrationService`, `CurrentUserService` |
| Room availability | `RoomService`, pessimistic room locking, reservation overlap query |
| Reservation lifecycle | `ReservationService`, explicit transition map, `ReservationStatusChangedEvent` |
| Check-in/out | `StayService`, single transactions for stay/reservation/room changes |
| Promotions | `DiscountStrategy`, `PercentageDiscountStrategy`, `FixedAmountDiscountStrategy` |
| Reviews | `ReviewService`, completed-stay ownership and uniqueness checks |
| Guest requests | `GuestRequestService`, `RequestStatusChangedEvent`, `RequestHistoryListener` |

Room uploads accept JPEG, PNG, or WebP files up to 2 MiB and are written beneath `UPLOAD_DIR`. Uploaded files, logs, local configuration, IDE metadata, and build output are ignored by Git.

## Operational notes

- Reservation availability uses half-open date overlap: `existing.checkIn < requestedCheckOut AND existing.checkOut > requestedCheckIn`.
- Cancellation, rejection, and no-show statuses release future availability.
- A final overlap check is performed while holding a pessimistic lock on the room to prevent concurrent double booking.
- Future reservations do not change a room's operational status. Only check-in changes it to `OCCUPIED`; check-out restores `AVAILABLE`.
- Historical reservations, stays, reviews, and request history are preserved through status changes or safe deactivation.
