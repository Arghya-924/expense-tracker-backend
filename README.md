# Expense Tracker Backend

A high-performance, modular RESTful API built with **Spring Boot 4** and **Java 21** for tracking personal expenses. This application features robust security, intelligent data aggregation for performance optimization, and advanced caching strategies.

## 🚀 Key Features

* **Secure Authentication**: Stateless JWT (JSON Web Token) authentication with custom validation filters.
* **Optimized Performance**:
    * **Read-Optimized Architecture**: Uses a dedicated `AggregateExpense` entity to pre-calculate monthly totals during write operations, ensuring O(1) complexity for dashboard summaries.
    * **Advanced Caching**: Integrated **Caffeine Cache** with custom expiry policies for user details and JWTs.
* **Expense Management**: Full CRUD support for expenses with automatic category management.
* **Data Integrity**: Transactional updates ensure that summary tables (`AggregateExpense`) always stay in sync with individual expense records.
* **Interactive Documentation**: Integrated OpenAPI 3 (Swagger UI) for easy API exploration.

---

## 🛠️ Tech Stack

* **Framework**: Spring Boot 4.0.0
* **Language**: Java 21
* **Database**: H2 In-Memory Database (Pre-configured for Dev/Test)
* **Security**: Spring Security 6, JWT (JJWT 0.12.6), BCrypt Password Encoding
* **Caching**: Caffeine Cache
* **Build Tool**: Maven
* **Containerization**: Docker

---

## 🏗️ Architecture & Functionality

### 1. Security & Authentication
The application enforces a stateless security model configured in `ProjectSecurityConfig`.

* **JWT Filter Chain**: A custom `JwtValidatorFilter` intercepts requests before the standard authentication filter. It validates the token signature, expiration, and extracts user context.
* **Public Endpoints**:
    * `/public/login`: Generates a JWT signed with HMAC-SHA256.
    * `/public/register`: Registers new users with BCrypt encrypted passwords.
    * `/h2-console/**`, `/swagger-ui/**`: Developer tools.

### 2. Data Aggregation Strategy (Write-Heavy vs. Read-Heavy)
To avoid expensive summation queries on the `Expense` table every time a user views their dashboard, this project uses a **Write-Optimized** approach:

* **Entity**: `AggregateExpense` stores the total expense amount per user, per month.
* **Logic**:
    * **On Create/Update**: When an expense is saved, the service immediately calculates the delta and updates the corresponding `AggregateExpense` record.
    * **On Delete**: The deleted amount is subtracted from the aggregate table.
    * **On Read**: The "Total Monthly Expense" is a simple lookup from the `AggregateExpense` table, making it extremely fast even with millions of expense records.

### 3. Caching Mechanism
Caching is handled by **Caffeine** and configured in `CacheConfig.java`.

| Cache Name | Usage | Eviction Policy |
| :--- | :--- | :--- |
| `user_details_email` | Caches `User` objects during login. | **Size**: 10 items<br>**Time**: 30 mins after access |
| `user_details_id` | Caches `User` objects during expense operations. | **Size**: 50 items<br>**Time**: 30 mins after access |
| `jwt_cache` | Caches active JWTs to validate tokens without re-parsing. | **Custom Strategy**: Expires exactly when the JWT claim expires (Precision eviction). |

---

## ⚙️ Configuration

### Application Properties
The application runs on port **9090** by default. Key configurations in `application.yml`:

```yaml
server:
  port: 9090
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # In-memory DB
  h2:
    console:
      enabled: true          # Access at /h2-console
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION_SECONDS}
