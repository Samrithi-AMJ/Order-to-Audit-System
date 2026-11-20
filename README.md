# Order-to-Audit Management System

## Project overview

The Order-to-Audit Management System is a Java backend for managing the operational and accounting flow of a food manufacturing business. It connects purchasing and production to stock movements, sales orders, dispatch, GST invoices, payments, and reports.

The project is intended to demonstrate an auditable workflow: inventory changes are recorded as stock movements, orders move through approval and dispatch, and invoice and customer balances are updated as payments are applied.

## Technology stack

- Java 21
- Spring Boot 4.1.2
- Maven and the Maven Wrapper
- Spring MVC, Spring Data JPA, Spring Security, and Jakarta Bean Validation
- MySQL Connector/J for the application database
- JWT authentication with JJWT 0.12.6
- H2 for automated tests
- OpenPDF for invoice PDFs
- Apache POI for Excel report exports; CSV report exports are also implemented
- JUnit 5 and Mockito/Spring Boot test support

## Main backend modules

- **Authentication and authorization:** registration, login, JWT authentication, and role-based access.
- **Master data:** customers, suppliers, products, raw materials, and packaging materials.
- **BOM and production:** product recipes, production runs, material consumption, yield and consumption variance, and production costing.
- **Purchasing and inventory:** purchase receipts, stock balances, opening stock, stock movements, stock summaries, and ledger views.
- **Sales and dispatch:** sales orders, approval or rejection, finished-goods dispatch, and stock checks.
- **Invoices and payments:** GST invoice creation, PDF invoices, payment recording, and outstanding balance updates.
- **Reports:** dashboard, GST summary, production variance, stock ledger exports, and sales register exports.
- **Benchmark scenario:** a deterministic workflow used to exercise purchases, production, sales, tax, payments, stock shortage handling, costing, and verification checks in an isolated database.

## Architecture and folder structure

The application follows a layered Spring architecture:

1. Controllers expose REST endpoints and apply request-level security.
2. Services implement business rules and transaction boundaries.
3. Repositories provide persistence through Spring Data JPA.
4. Entities map the domain model to database tables; DTOs define API request and response shapes.

```text
OrdertoAudit/
├── README.md
└── ordertoaudit/
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/
        ├── main/
        │   ├── java/com/nittrichy/ordertoaudit/
        │   │   ├── config/       # Security and database initialization
        │   │   ├── controller/   # REST API endpoints
        │   │   ├── dto/          # API request and response models
        │   │   ├── entity/       # JPA entities
        │   │   ├── enums/        # Domain statuses and categories
        │   │   ├── exception/    # Business exceptions and API error handling
        │   │   ├── repository/   # Spring Data JPA repositories
        │   │   ├── security/     # JWT and user principal support
        │   │   ├── service/      # Business workflows and transactions
        │   │   └── util/         # PDF, Excel, and CSV generation
        │   └── resources/
        │       └── application.properties
        └── test/
            ├── java/com/nittrichy/ordertoaudit/
            └── resources/application-test.properties
```

## MySQL database setup

Install and start MySQL, then create a database and a dedicated application user. For example, run the following as a MySQL administrator and replace the password with a unique value:

```sql
CREATE DATABASE ordertoaudit;
CREATE USER 'ordertoaudit_app'@'localhost' IDENTIFIED BY 'use-a-unique-password-here';
GRANT ALL PRIVILEGES ON ordertoaudit.* TO 'ordertoaudit_app'@'localhost';
FLUSH PRIVILEGES;
```

The default JDBC URL points to `localhost:3306/ordertoaudit` and includes `createDatabaseIfNotExist=true`. The application uses Hibernate `ddl-auto=update` to update the schema from the JPA entities. For a shared or production database, use managed schema migrations and backups rather than relying on automatic schema changes.

## Required environment variables

The application configuration requires database credentials and a JWT secret. Set these in the IntelliJ run configuration or the shell that launches the application:

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_USERNAME` | Yes | MySQL application username |
| `DB_PASSWORD` | Yes | MySQL application password |
| `JWT_SECRET` | Yes | Base64-encoded signing key; use a high-entropy value with at least 32 decoded bytes |
| `DB_URL` | No | JDBC URL; defaults to the local `ordertoaudit` database |
| `BENCHMARK_ENABLED` | No | Defaults to `false`; must be `true` to enable the benchmark endpoint |
| `BOOTSTRAP_ADMIN_USERNAME` | No | Username for an optional initial administrator account |
| `BOOTSTRAP_ADMIN_PASSWORD` | No | Optional initial administrator password; must be at least 12 characters and is only used when a username is supplied |

No built-in administrator account is created when bootstrap credentials are absent. Self-registration assigns the `SALESMAN` role. The JWT secret must be configured separately from the test-only secret in `application-test.properties`.

## Run in IntelliJ IDEA

1. Open `ordertoaudit/pom.xml` as a Maven project (or open the `ordertoaudit` directory) and allow IntelliJ to import dependencies.
2. Set the project SDK and Maven runner JDK to Java 21.
3. Configure MySQL and set `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in **Run | Edit Configurations...**.
4. Run `OrdertoauditApplication` from `src/main/java/com/nittrichy/ordertoaudit/OrdertoauditApplication.java`.
5. The API listens on port `8080` by default.

For local runs, `DB_URL` may be set to another JDBC URL, for example `jdbc:mysql://localhost:3306/ordertoaudit?serverTimezone=Asia/Kolkata`.

## Run Maven tests

From the `ordertoaudit` directory, run:

```powershell
.\mvnw.cmd test
```

Or, if Maven is installed:

```powershell
mvn test
```

Tests use an in-memory H2 database configured in `src/test/resources/application-test.properties`; they do not require the MySQL application database.

## API endpoint categories

All endpoint paths below are rooted at the application host, for example `http://localhost:8080`. Most API routes require a JWT bearer token; `/api/auth/login` and `/api/auth/register` are the authentication entry points.

| Category | Paths currently implemented |
| --- | --- |
| Authentication | `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/me` |
| Master data | `/api/products`, `/api/customers`, `/api/suppliers`, `/api/raw-materials`, `/api/packaging-materials` (implemented list/detail/create/update operations vary by resource) |
| BOMs | `GET /api/boms`, `GET /api/boms/{id}`, `GET /api/boms/product/{productId}`, `POST /api/boms`, `PUT /api/boms/{id}` |
| Purchases | `GET /api/purchases`, `GET /api/purchases/{id}`, `POST /api/purchases` |
| Production | `GET /api/production`, `GET /api/production/{id}`, `POST /api/production` |
| Inventory | `GET /api/inventory/summary`, `GET /api/inventory/ledger` |
| Sales orders | `GET /api/orders`, `GET /api/orders/{id}`, `GET /api/orders/customer/{customerId}`, `POST /api/orders`, `POST /api/orders/{id}/approval` |
| Dispatch | `POST /api/dispatch/{orderId}` |
| Invoices | `GET /api/invoices`, `GET /api/invoices/{id}`, `GET /api/invoices/order/{orderId}`, `GET /api/invoices/{id}/pdf` |
| Payments | `GET /api/payments`, `GET /api/payments/customer/{customerId}`, `POST /api/payments` |
| Reports | `GET /api/reports/dashboard`, `GET /api/reports/gst-summary`, `GET /api/reports/production-variance`, plus stock-ledger and sales-register CSV/Excel exports |
| Benchmark | `POST /api/benchmark/run` (restricted; see below) |

The controller also maps `GET /api/benchmark/status`, but benchmark results are not persisted and this route currently returns **501 Not Implemented**. It does not run the benchmark.

## Benchmark scenario

The benchmark service models a sweets manufacturing flow: it receives flour, consumes raw and packaging materials in a production run, records yield and oil-consumption variances, creates an intra-state retailer sale and GST invoice, records a partial payment, and checks that a distributor dispatch is blocked when finished-goods stock is short. It also reports production cost and verifies expected stock and audit values.

Benchmark execution is destructive by nature and therefore has additional safeguards. It requires `BENCHMARK_ENABLED=true`, an active `test` or `demo` profile, an H2 JDBC datasource, and empty business tables. It refuses to run against MySQL or when business records already exist. The automated integration test enables it for its isolated in-memory H2 database; do not point a demo benchmark at business data.
