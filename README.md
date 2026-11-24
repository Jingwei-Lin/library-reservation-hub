# Library Reservation & Review Hub

[![CI](https://github.com/Jingwei-Lin/library-reservation-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/Jingwei-Lin/library-reservation-hub/actions/workflows/ci.yml)

A full-stack library web app where members can browse books, reserve copies, join a waiting list when every copy
is taken, and share reviews. Built by a team of five for ELEC5619 (Object-Oriented Application Frameworks) at
the University of Sydney, Aug – Nov 2025.

**Stack:** Java 17 · Spring Boot 3.5 · MyBatis-Plus · MySQL · Quartz · JWT · React 19 · Vite ·
JUnit 5 · Mockito · JaCoCo · Docker · GitHub Actions

| Browse the catalogue | Book details |
|---|---|
| ![Dashboard with search, genres and library statistics](docs/screenshots/dashboard.png) | ![Book detail page with availability and Reserve Now](docs/screenshots/book-detail.png) |
| **Reserve a copy** | **My reservations** |
| ![Reservation dialog with pickup and due dates](docs/screenshots/reserve.png) | ![Reservations table with edit and cancel actions](docs/screenshots/my-reservations.png) |

## Features

- **Reservations:** reserve and cancel books, change pickup times; admins manage reservation status.
- **Waiting list with notifications:** when every copy is out, users join a queue. A returned or cancelled copy is
  held for the waiting users and they are notified. A nightly job cancels stale waiting entries.
- **Stock-aware inventory:** each book tracks its total, publicly available and held quantities, so a copy that is
  being held for a waiting user cannot be reserved by someone else.
- **Books and search:** keyword, genre and availability filters, with pagination and sorting.
- **Reviews and ratings:** users write and edit reviews; average ratings are kept up to date by MySQL triggers.
- **Accounts:** registration, login and role-based admin access with JWT.
- **AI assistant:** a chat panel that streams book recommendations over Server-Sent Events (Spring AI).

## My contribution

I owned the reservation and waiting-list workflow, end to end:

- **Back end:** `ReservationServiceImpl` and `WaitingListServiceImpl`, and their controllers and mappers.
  - Reserving a copy checks availability and fulfils the user's waiting-list entry.
  - Cancelling a reservation either holds the copy for the next waiting user or releases it to the public.
  - The changes run in transactions, so stock counts and the queue stay consistent.
- **Front end:** the main book dashboard (search, genre browsing and top-rated books), the book detail page, and
  the reservation and waiting-list screens.
- **Testing:**
  - Wrote the JUnit and Mockito tests for the reservation service and controller.
  - Added JaCoCo coverage reporting to the Gradle build.
- **Database:** the schema and seed data for books, reservations and the waiting list.

Teammates built user management, reviews and ratings, the admin pages and the AI assistant.

## Architecture

```
React (Vite, :5173)  ──REST / SSE──▶  Spring Boot API (:8432/api)  ──MyBatis-Plus──▶  MySQL
                                        ├─ controllers → services → mappers
                                        ├─ Quartz / @Scheduled jobs (stale waiting-list cleanup)
                                        └─ Spring AI (DashScope) for the chat assistant
```

API documentation is served by Knife4j / springdoc at `http://localhost:8432/api/swagger-ui.html` when the
back end is running.

## Quick start with Docker

```bash
cp .env.example .env      # optional: add a DashScope key to try the AI assistant
docker compose up --build
```

Open http://localhost:8080 and sign in with a demo account (password `123456789`):
`jingwei.lin2@example.com`, or `admin@admin.com` at `/admin` for the admin panel.

Compose starts three containers:
- MySQL, with the schema and 101 sample books loaded;
- the Spring Boot API;
- the React app, served by nginx, which proxies `/api` to the API.

## Running locally without Docker

**Requirements:** JDK 17, MySQL 8, Node.js 20.19+

1. **Create the database.** Run `LibraryReservationReviewHub/Schema.sql` in MySQL.
2. **Set environment variables.** The back end reads them through `application.yml`:

   | Variable | Default | Purpose |
   |---|---|---|
   | `DB_URL` | `jdbc:mysql://localhost:3306/library_reservation` | JDBC URL |
   | `DB_USERNAME` | `root` | Database user |
   | `DB_PASSWORD` | *(empty)* | Database password |
   | `DASHSCOPE_API_KEY` | *(empty)* | Only needed for the AI assistant |

3. **Start the back end:**

   ```bash
   cd LibraryReservationReviewHub
   ./gradlew bootRun
   ```

4. **Start the front end:**

   ```bash
   cd frontend
   npm install
   npm run dev        # http://localhost:5173
   ```

## Tests and coverage

There are 174 JUnit tests: Mockito unit tests for the services and controllers, and Spring Boot integration tests
that run against MySQL. CI runs all of them on every push, with a MySQL service container, and uploads the
JaCoCo coverage report.

```bash
cd LibraryReservationReviewHub
./gradlew test jacocoTestReport   # HTML report: build/reports/jacoco/test/html/index.html
```

The integration tests need the database from step 1. `PersonalAgentTest` calls the real AI model, so it is
skipped unless `DASHSCOPE_API_KEY` holds a real key.

The full design notes are in [docs/DESIGN.md](docs/DESIGN.md).
