# StarVoice Lanka (Spring Boot 3.3.4 + JPA + Microsoft SQL Server)

Web-based voting and reality singing competition platform.
SE2030 Software Engineering, group 2026-Y2-S1-KU-03.

This repository contains the complete Java / Spring Boot rewrite of the StarVoice Lanka platform, replacing the legacy Node.js/Express/MongoDB stack. It keeps the exact same six modules, business rules, and data model, expressed cleanly in Java with Spring Data JPA, Microsoft SQL Server (plus an in-memory H2 fallback profile for development and tests), Thymeleaf views, and Spring Security session-based authentication.

---

## Tech Stack

- **Java**: 17+ (tested with Java 17 & 24)
- **Framework**: Spring Boot 3.3.4, Maven
- **Persistence**: Spring Data JPA / Hibernate 6
- **Database**:
  - **Microsoft SQL Server** (`mssql-jdbc` driver) via the `mssql` Spring profile
  - **In-memory H2 Database** via the default `h2` profile (zero-dependency standalone dev & automated testing)
- **Security**: Spring Security (cookie-based session authentication with `svl_session`, BCrypt password hashing, method-level `@PreAuthorize` guards)
- **Frontend Views**: Thymeleaf 3 server-rendered HTML templates replacing legacy EJS views
- **PDF Generation**: OpenPDF 2.0.3 for payment receipts
- **Testing**: JUnit 5, Spring Boot Test, MockMvc, AssertJ

---

## Getting Started

### Prerequisites

- Java 17 or higher (`java -version`)
- Apache Maven 3.8+ (`mvn -version`)

### Quick Start (H2 In-Memory DB - Zero External Dependencies)

By default, the application runs on the `h2` profile with an in-memory database and automatic schema generation:

```bash
# Build and run all automated tests
mvn clean test

# Run the application locally on http://localhost:4000
mvn spring-boot:run
```

The database is automatically seeded upon boot with:
- 1 Admin user (`admin@starvoice.lk` / `admin12345`)
- 5 Verified Voters (`voter1@starvoice.lk` through `voter5@starvoice.lk` / `voter12345`)
- 1 Active Season ("StarVoice Lanka Season 3")
- 5 Contestants assigned to an open Quarter Final round with initial votes
- 3 Vote bundles (Starter, Fan, Super Fan)
- 1 Title Sponsor with impressions and banner delivery

H2 Web Console is available at: `http://localhost:4000/h2-console` (JDBC URL: `jdbc:h2:mem:starvoice`, User: `sa`, Password: *empty*).

### Running with Microsoft SQL Server

To use Microsoft SQL Server:
1. Ensure SQL Server is running and create the database `starvoice_lanka`.
2. Update connection credentials in `src/main/resources/application.yml` under profile `mssql` or supply via environment variables:
   - `spring.datasource.url`: `jdbc:sqlserver://localhost:1433;databaseName=starvoice_lanka;encrypt=false;trustServerCertificate=true`
   - `spring.datasource.username`: your SQL Server username
   - `spring.datasource.password`: your SQL Server password
3. Run with the `mssql` active profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mssql
```

---

## Project Structure

```
src/main/java/com/starvoicelanka/
  StarVoiceLankaApplication.java       // Application entrypoint (@SpringBootApplication, @EnableScheduling)
  common/                              // Shared base entity, API response wrappers, exception handling, helpers
    BaseEntity.java
    ApiResponse.java
    PagedResponse.java
    exception/                         // ApiError hierarchy (BadRequestException, NotFound, Conflict, etc.)
    helper/                            // WebHelpers for Thymeleaf formatting (tone, titleCase, money, etc.)
  config/                              // SecurityConfig, AppProperties, WebMvcConfig, DataInitializer
  web/                                 // Web controllers serving Thymeleaf templates
    SiteWebController.java             // Public landing page, contestants, results, bundles, partners
    AuthWebController.java             // Login, register, mobile verification, password reset
    AppWebController.java              // Dashboard, voting, my votes, wallet, receipts, profile, notifications
    AdminWebController.java            // Admin management screens
  user/                                // Module 1: UM01-UM06 User Management
    entity/                            // User, AuditLog
    repository/                        // UserRepository, AuditLogRepository
    service/                           // UserService, CustomUserDetailsService
    controller/                        // UserApiController
  contestant/                          // Module 2: CM01-CM06 Contestant & Round Management
    entity/                            // Season, Round, Contestant, RoundEntry
    repository/                        // SeasonRepository, RoundRepository, ContestantRepository, RoundEntryRepository
    service/                           // ContestantService, MediaStorageService
    controller/                        // ContestantApiController, RoundApiController
  voting/                              // Module 3: VM01-VM06 Voting Management
    entity/                            // Vote, VoteQuota
    repository/                        // VoteRepository, VoteQuotaRepository
    service/                           // VotingService (rate-limiter, fraud engine, quota deductions)
    controller/                        // VotingApiController
  notification/                        // Module 4: NM01-NM06 Notification Management
    entity/                            // Notification, NotificationPreference
    repository/                        // NotificationRepository, NotificationPreferenceRepository
    service/                           // NotificationService, NotificationScheduler, TemplateRenderer
  payment/                             // Module 5: PM01-PM06 Payment & Credit Account Management
    entity/                            // VoteBundle, Payment, CreditAccount
    repository/                        // VoteBundleRepository, PaymentRepository, CreditAccountRepository
    service/                           // PaymentService, ReceiptPdfService (OpenPDF)
    controller/                        // PaymentApiController
  sponsor/                             // Module 6: SM01-SM06 Sponsor & Campaign Management
    entity/                            // Sponsor, SponsorshipPackage, SponsorshipAgreement, Impression, Invoice
    repository/                        // SponsorRepository, AgreementRepository, ImpressionRepository, etc.
    service/                           // SponsorService, SponsorScheduler
    controller/                        // SponsorApiController

src/main/resources/
  application.yml                      // Multi-profile YAML (h2 & mssql profiles)
  static/assets/                       // CSS stylesheet (style.css), JS bundle (app.js)
  templates/                           // Thymeleaf templates replacing legacy EJS views
    layout/                            // head.html, foot.html
    partials/                          // board.html, sponsors.html
    site/                              // index, contestants, contestant, results, leaderboard, bundles, partners
    auth/                              // login, register, verify, forgot, reset
    app/                               // dashboard, vote, my-votes, wallet, payments, receipt, notifications, preferences, profile
    admin/                             // placeholder, agreements, audit, rounds, users

src/test/java/com/starvoicelanka/      // Automated test suite
  StarVoiceLankaApplicationTests.java  // Web integration tests (endpoints, Thymeleaf rendering, security)
  user/UserModuleTests.java            // User registration, verification, role guards
  voting/VotingModuleTests.java        // Vote eligibility, quota deduction, tallying
  payment/PaymentModuleTests.java      // Bundle purchases, webhooks, credit accounts
  sponsor/SponsorModuleTests.java      // Sponsorship packages, impressions, click tracking
```

---

## Architectural Principles & Integrity

1. **Strict Layering**: Controllers handle HTTP parameters, authentication principal resolution, and response mapping. Services handle all business logic, transactions, and security checks.
2. **One-Way Module Dependencies**: Voting calls Contestant, Payment, Notification, and Sponsor. No circular dependencies exist.
3. **Audit Trails & Soft Inactivation**: Users and sensitive resources are never hard-deleted; accounts are suspended and anonymized with complete audit logs (`AuditLog`).
4. **Resilient Scheduling**: Notification retries, daily reconciliation, and sponsor agreement expiry run via Spring `@Scheduled` tasks.
5. **Session & Security Integrity**: Protected web routes require authenticated sessions; admin desks enforce `@PreAuthorize("hasRole('ADMIN')")`.


## Seasons (admin)
Admin > Seasons lets you create, edit, make current and delete seasons. Rounds and contestants belong to a season; the Rounds page shows the current season. A season cannot be deleted while it is current or still has rounds/contestants, and switching season is blocked while a round is open.
