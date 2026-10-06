# Maybank Java Backend Assessment

A Spring Boot REST API for customer accounts. It stores data in MSSQL (`TESTDB`), logs every request and response to a file, has a paginated listing API, and has an API that calls a third-party exchange rate service from inside our own endpoint.

## Requirement checklist

| # | Requirement | Where it's done |
|---|---|---|
| 1 | Java Spring Boot application | Spring Boot 4.1.1, Java 21 ([pom.xml](pom.xml)) |
| 2 | Maintainable project structure | Layered packages: controller → service → repository, plus dto, mapper, client, exception, logging, config (see [Project structure](#project-structure)) |
| 3 | APIs for a client plus a Postman collection | [postman/Maybank-Assessment.postman_collection.json](postman/Maybank-Assessment.postman_collection.json) (16 requests with test assertions) |
| 4 | Log REQUEST and RESPONSE of every API to a log file | [RequestResponseLoggingFilter](src/main/java/com/maybank/assessment/logging/RequestResponseLoggingFilter.java) and [OutboundRequestLoggingInterceptor](src/main/java/com/maybank/assessment/logging/OutboundRequestLoggingInterceptor.java) write to `logs/api-request-response.log` ([logback-spring.xml](src/main/resources/logback-spring.xml)) |
| 5 | Connect to local MSSQL, DB `TESTDB` | [application.yml](src/main/resources/application.yml), Flyway migrations in [db/migration](src/main/resources/db/migration), local SQL Server via [docker-compose.yml](docker-compose.yml) |
| 5 | `@Transactional` for INSERT, UPDATE, GET | [CustomerServiceImpl](src/main/java/com/maybank/assessment/service/impl/CustomerServiceImpl.java): `@Transactional` on create and update, `@Transactional(readOnly = true)` on the GET methods |
| 6 | GET API with pagination, 10 records per page | `GET /api/v1/customers?page=0`. Page size is fixed at `CustomerService.PAGE_SIZE = 10` |
| 7 | API that calls a third-party API | `GET /api/v1/customers/{id}/balance/convert?to=USD`: Postman calls our API, our API calls https://open.er-api.com |

## Tech stack

Spring Boot 4.1.1 (Web MVC, Data JPA, Validation), Microsoft SQL Server 2022, Flyway, Spring `RestClient`, SLF4J/Logback, Lombok, springdoc-openapi (Swagger UI), JUnit 5 and Mockito.

## Prerequisites

- JDK 21 or newer (`JAVA_HOME` set)
- Docker Desktop, for the bundled SQL Server. You can use your own local SQL Server instead (see below).
- You don't need to install Maven; the Maven wrapper (`mvnw` / `mvnw.cmd`) is included.

## Getting started

### 1. Start MSSQL and create `TESTDB`

```bash
docker compose up -d
```

This starts SQL Server 2022 on `localhost:1433` (user `sa`, password `Maybank@12345`). A one-off init container then creates the `TESTDB` database.

**Using your own SQL Server instead:** run `CREATE DATABASE TESTDB;` and set these environment variables to override the defaults:

```bash
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=TESTDB;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=<your password>
```

### 2. Run the application

```bash
# Windows
mvnw.cmd spring-boot:run
# macOS / Linux
./mvnw spring-boot:run
```

On startup, Flyway creates the `customer` table and seeds 25 customers, which gives 3 pages of results. Hibernate then checks that the entity mappings match the schema (`ddl-auto: validate`).

- API base URL: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html

### 3. Test with Postman

Import both files from [postman/](postman/):

- `Maybank-Assessment.postman_collection.json`
- `Maybank-Assessment.local.postman_environment.json`

Then run the collection with the **Collection Runner**. It creates a customer, reads it, updates it, pages through the list, calls the third-party API, and finally deletes the customer, so you can run it again. Each request has test assertions.

To run the collection from the command line instead:

```bash
npx newman run postman/Maybank-Assessment.postman_collection.json -e postman/Maybank-Assessment.local.postman_environment.json
```

### 4. Run the unit tests

```bash
mvnw.cmd test
```

The tests use mocks and don't need a running database.

## API reference

| Method | Endpoint | Description | Transaction |
|---|---|---|---|
| POST | `/api/v1/customers` | Create a customer (INSERT) | `@Transactional` |
| PUT | `/api/v1/customers/{id}` | Update a customer (UPDATE) | `@Transactional` |
| GET | `/api/v1/customers/{id}` | Get a customer by ID | `@Transactional(readOnly = true)` |
| GET | `/api/v1/customers?page=0&status=ACTIVE&sortBy=balance&direction=desc` | Paginated list, **10 per page** | `@Transactional(readOnly = true)` |
| DELETE | `/api/v1/customers/{id}` | Delete a customer | `@Transactional` |
| GET | `/api/v1/customers/{id}/balance/convert?to=USD` | Converts the balance using live rates from the **third-party API** | DB read uses `readOnly = true` |
| GET | `/api/v1/exchange-rates?base=MYR&symbols=USD,SGD` | Latest exchange rates, passed through from the **third-party API** | none |

Pagination query parameters for `GET /api/v1/customers`:

- `page`: zero-based, default `0`
- `status`: optional; `ACTIVE`, `INACTIVE` or `CLOSED`
- `sortBy`: default `id`; one of `id`, `fullName`, `email`, `accountNo`, `balance`, `status`, `createdAt`, `updatedAt`
- `direction`: `asc` or `desc`

The client can't change the page size; it is always 10.

### Sample request and response

```http
POST /api/v1/customers
Content-Type: application/json

{ "fullName": "Muhammad Ali bin Abu", "email": "ali@example.com", "phoneNo": "+60123456789", "initialDeposit": 2500.75, "currency": "MYR" }
```

```json
{
  "success": true,
  "message": "Customer created successfully",
  "data": { "id": 26, "accountNo": "514029974368", "balance": 2500.75, "currency": "MYR", "status": "ACTIVE", "...": "..." },
  "timestamp": "2026-10-06T09:13:05.6455987+08:00"
}
```

Errors always come back in the same shape, with a correlation ID and per-field messages for validation failures:

```json
{
  "success": false, "status": 400, "error": "Bad Request", "message": "Validation failed",
  "path": "/api/v1/customers", "correlationId": "ae07e810-...",
  "fieldErrors": { "email": "email must be a valid email address" }
}
```

| HTTP status | Returned when |
|---|---|
| 400 | Validation failed, or a parameter or currency is invalid |
| 404 | The customer or endpoint doesn't exist |
| 409 | The email already exists, or the record was changed by another request (optimistic lock) |
| 502 | The third-party API is down or returned an unexpected response |

## Logging

Log files are written to the `logs/` folder and roll over daily or at 10 MB. Archives are gzipped and kept for 30 days.

| File | Contents |
|---|---|
| `logs/api-request-response.log` | REQUEST and RESPONSE of every inbound API call, plus every outbound third-party call |
| `logs/app.log` | All application logs (includes the lines above) |

For each call, the log records:

- method, URI and query string, client IP, and headers (sensitive headers such as `Authorization` are masked)
- request body, response status, response headers, response body, and duration

Every line carries a correlation ID. If the client sends a well-formed `X-Correlation-Id` header, that value is used; otherwise one is generated. The ID is returned in the `X-Correlation-Id` response header, so one Postman call can be traced through the inbound request, the third-party call, and the response:

```
... [cid:f980d13e-...] RequestResponseLoggingFilter - >>> REQUEST  | GET /api/v1/customers/1/balance/convert?to=USD | ...
... [cid:f980d13e-...] OutboundRequestLoggingInterceptor - >>> 3RD-PARTY REQUEST  | [ExchangeRateAPI] GET https://open.er-api.com/v6/latest/MYR | ...
... [cid:f980d13e-...] OutboundRequestLoggingInterceptor - <<< 3RD-PARTY RESPONSE | [ExchangeRateAPI] ... | status=200 | duration=334ms | ...
... [cid:f980d13e-...] RequestResponseLoggingFilter - <<< RESPONSE | GET /api/v1/customers/1/balance/convert?to=USD | status=200 | duration=390ms | body={...}
```

## Project structure

```
src/main/java/com/maybank/assessment/
├── MaybankAssessmentApplication.java
├── config/        RestClientConfig, ExchangeRateProperties, JpaAuditingConfig, OpenApiConfig
├── controller/    CustomerController, ExchangeRateController   (HTTP layer only)
├── service/       CustomerService, ExchangeRateService          (interfaces)
│   └── impl/      CustomerServiceImpl, ExchangeRateServiceImpl  (business logic + @Transactional)
├── repository/    CustomerRepository                            (Spring Data JPA)
├── entity/        Customer, BaseEntity (audit + @Version), CustomerStatus
├── dto/request/   CreateCustomerRequest, UpdateCustomerRequest  (validated input)
├── dto/response/  ApiResponse, ErrorResponse, PageResponse, CustomerResponse, CurrencyConversionResponse
├── mapper/        CustomerMapper                                (entity <-> DTO)
├── client/        ExchangeRateClient + dto/                     (third-party API integration)
├── exception/     GlobalExceptionHandler + custom exceptions
├── logging/       RequestResponseLoggingFilter, OutboundRequestLoggingInterceptor
└── util/          AccountNumberGenerator
src/main/resources/
├── application.yml
├── logback-spring.xml
└── db/migration/  V1__create_customer_table.sql, V2__seed_customer_data.sql
postman/           Postman collection + environment
docker-compose.yml Local SQL Server 2022 + TESTDB creation
```

## Design notes

- **Transactions belong to the service layer, not the controllers.**
  - Write methods use `@Transactional(rollbackFor = Exception.class)`.
  - Read methods use `readOnly = true`, which skips dirty checking and flushing.
- **No database connection is held during the third-party call.** `ExchangeRateServiceImpl` is deliberately not transactional. It reads the customer through `CustomerService`, which runs its own read-only transaction, and only then calls the external API. A slow third-party response therefore can't hold a connection from the database pool.
- **Optimistic locking.** `@Version` on `BaseEntity` stops concurrent updates from silently overwriting each other; the API returns 409 instead.
- **Flyway owns the schema.** Hibernate only validates the mappings (`ddl-auto: validate`), so the schema is versioned and the same in every environment.
- **Entities are never exposed through the API.** Requests and responses use record DTOs, with Bean Validation on the input.
- **The third-party client is hardened.**
  - Connect and read timeouts are set in `application.yml`.
  - Non-2xx responses and network errors become a `502` response.
  - An unsupported currency code becomes a `400` response.
- **Account numbers** are 12 digits: the branch prefix `5140` followed by 8 digits from `SecureRandom`. A clash is retried, and a unique constraint in the database is the final guard.
