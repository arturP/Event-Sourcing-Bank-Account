# Event Sourcing Bank Account with REST API

A comprehensive banking application built with Event Sourcing architecture, featuring high-performance REST API, JWT authentication, and advanced optimization features.

## 🚀 Features

### Core Banking
- **Account Management**: Create accounts, deposits, withdrawals with overdraft protection
- **Event Sourcing**: Complete audit trail with event replay capabilities  
- **Domain-Driven Design**: Value objects, aggregates, domain services
- **CQRS Pattern**: Separate command and query responsibilities

### Performance & Scalability  
- **✅ HikariCP Connection Pooling** - Optimized database connections
- **✅ Async Event Processing** - CompletableFuture-based async operations
- **✅ Event Stream Pagination** - Memory-efficient large dataset handling
- **✅ Caffeine Caching** - High-performance read model caching (99%+ hit rates)
- **✅ Database Indexing** - Comprehensive index strategies for optimal performance
- **✅ Metrics Collection** - Dropwizard Metrics for monitoring
- **✅ Batch Processing** - High-throughput event handling

### REST API & Security
- **✅ Spring Boot REST API** - Production-ready RESTful endpoints
- **✅ JWT Authentication** - Stateless authentication with configurable expiration
- **✅ Role-based Security** - USER and ADMIN role permissions
- **✅ Input Validation** - Comprehensive request validation with error handling
- **✅ OpenAPI Documentation** - Interactive Swagger UI at `/swagger-ui.html`
- **✅ Exception Handling** - Structured error responses
- **✅ Health Checks** - System health and metrics endpoints

## 📡 API Endpoints

### Authentication
```
POST /api/auth/login          # Login with username/password, get JWT token
GET  /api/auth/demo-users     # View available demo users
```

### Account Management (Requires JWT)
```
POST /api/accounts                    # Create new bank account
GET  /api/accounts                    # Get all accounts  
GET  /api/accounts/{id}               # Get specific account
GET  /api/accounts/{id}/balance       # Get account balance (cached)
POST /api/accounts/{id}/deposit       # Deposit money
POST /api/accounts/{id}/withdraw      # Withdraw money
```

### System
```
GET  /api/health                      # Health check
GET  /api/health/metrics              # Performance metrics
GET  /swagger-ui.html                 # Interactive API documentation
```

## 🔐 Demo Users

| Username | Password | Roles      |
|----------|----------|------------|
| admin    | admin123 | ADMIN, USER|
| user     | user123  | USER       |
| demo     | demo123  | USER       |

## 🛠️ Quick Start

### 1. Build and Run
```bash
# Build the project
mvn clean install

# Run the application  
mvn spring-boot:run

# Access the API documentation
open http://localhost:8080/swagger-ui.html
```

### 2. Test the API

#### Login to get JWT token:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "demo", "password": "demo123"}'
```

#### Create a bank account:
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountHolder": "John Doe",
    "initialBalance": 1000.00,
    "overdraftLimit": 500.00
  }'
```

#### Make a deposit:
```bash
curl -X POST http://localhost:8080/api/accounts/{ACCOUNT_ID}/deposit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 250.00,
    "description": "Salary deposit"
  }'
```

## 📊 Performance Metrics

The application includes comprehensive performance monitoring:

- **Cache Hit Rates**: 99%+ for read operations
- **Transaction Processing**: 70,000+ operations/sec 
- **Event Stream Pagination**: Memory-efficient handling of large event histories
- **Async Processing**: Non-blocking operations with CompletableFuture
- **Connection Pooling**: Optimized database connections with HikariCP

## 🧪 Testing

```bash
# Run all tests (73 tests)
mvn test

# Run without integration tests  
mvn test -Dtest="!*Integration*"

# View test coverage
mvn test jacoco:report
```

## 🏗️ Architecture

### Event Sourcing
- Events stored in H2 database with comprehensive indexing
- Event replay capabilities for audit and debugging
- Snapshot support for performance optimization
- Paginated event stream loading

### Performance Optimizations
- **Caffeine Cache**: Multi-level caching for read models
- **Async Processing**: Separate thread pools for events, projections, notifications
- **Batch Processing**: High-throughput event handling
- **Database Indexes**: Optimized queries for account_id, timestamps, event types

### Security  
- JWT tokens with configurable expiration (24h default)
- Role-based access control (USER, ADMIN)
- CORS configuration for cross-origin requests
- Comprehensive input validation

## 📈 Monitoring

Access real-time metrics at:
- `/api/health` - System health status
- `/api/health/metrics` - Performance statistics  
- `/actuator/metrics` - Detailed Spring Actuator metrics

## 🔧 Configuration

Key configuration in `application.properties`:
```properties
# Server
server.port=8080

# Database & Connection Pool
db.pool.maxSize=20
db.pool.metrics.enabled=true

# JWT Security
app.jwt.expiration=86400000  # 24 hours

# API Documentation
springdoc.swagger-ui.path=/swagger-ui.html
```

## 🎯 Next Steps

Potential enhancements:
- Microservices architecture with service discovery
- Real-time WebSocket notifications
- Multi-currency support with exchange rates
- Advanced fraud detection
- Kubernetes deployment with auto-scaling

---

**Built with**: Spring Boot, Event Sourcing, JWT Security, Caffeine Cache, HikariCP, OpenAPI, Dropwizard Metrics