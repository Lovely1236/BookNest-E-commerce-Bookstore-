# BookNest Service Testing Guide

## Testing Summary

### Services with JUnit Testing

**Total Services in BookNest: 12**  
**Services with Testing: 12**  
**Services with Comprehensive Service Tests: 5**

#### Detailed Breakdown:

| Service | Application Test | Service Implementation Test | Total Tests |
|---------|------------------|---------------------------|-------------|
| book-service | ✅ | ✅ **NEW** | 2 |
| order-service | ✅ | ✅ **NEW** | 2 |
| cart-service | ✅ | ✅ **NEW** | 2 |
| wallet-service | ✅ | ✅ | 2 |
| review-service | ✅ | ✅ | 2 |
| api-gateway | ✅ | ❌ | 1 |
| auth-service | ✅ | ❌ | 1 |
| eureka-server | ✅ | ❌ | 1 |
| notification-service | ✅ | ❌ | 1 |
| website-controller | ✅ | ❌ | 1 |
| wishlist-service | ✅ | ❌ | 1 |
| admin-service | ❌ | ❌ | 0 |

---

## New Tests Created

### 1. BookServiceImplTest.java
**Location:** `book-service/src/test/java/org/example/bookservice/service/BookServiceImplTest.java`

**Test Coverage:**
- `addBook()` - Saving books
- `getAllBooks()` - Retrieving all books with empty list handling
- `getBookById()` - Finding books by ID with exception handling
- `searchBooks()` - Searching books by keyword
- `getByGenre()` - Filtering books by genre
- `updateBook()` - Updating book details with validation
- `deleteBook()` - Deleting books by ID
- `updateStock()` - Managing inventory
- `deductStock()` - Reducing stock with insufficient stock validation
- `restoreStock()` - Restoring stock after cancellations

**Total Test Methods: 21**

### 2. OrderServiceImplTest.java
**Location:** `order-service/src/test/java/org/example/orderservice/service/impl/OrderServiceImplTest.java`

**Test Coverage:**
- `getAllOrders()` - Retrieving all orders
- `getOrdersByUserId()` - Filtering orders by user
- `placeOrder()` - Creating orders with payment mode defaults, notification publishing, stock deduction
- `onlinePayment()` - Processing online payments
- `changeOrderStatus()` - Managing order status transitions (CONFIRMED, DISPATCHED, DELIVERED)
- `deleteOrder()` - Canceling orders with stock restoration
- `getOrderById()` - Retrieving specific orders with error handling
- `storeAddress()` - Saving delivery addresses

**Total Test Methods: 18**

### 3. CartServiceImplTest.java
**Location:** `cart-service/src/test/java/org/example/cartservice/service/CartServiceImplTest.java`

**Test Coverage:**
- `getCartByUser()` - Creating/retrieving user carts
- `addItem()` - Adding items to cart with price calculation
- `removeItem()` - Removing items with total price updates
- `updateQuantity()` - Changing item quantities
- `clearCart()` - Emptying cart
- `cartTotal()` - Calculating cart totals with various scenarios

**Total Test Methods: 18**

---

## How to Run Tests

### Run All Tests in a Single Service

```bash
# For book-service
cd book-service
mvn test

# For order-service
cd order-service
mvn test

# For cart-service
cd cart-service
mvn test
```

### Run All Tests in the Entire Project

```bash
# From BookNest root directory
cd BookNest
mvn clean test
```

### Run Specific Test Class

```bash
cd book-service
mvn test -Dtest=BookServiceImplTest
# OR
mvn test -Dtest=BookServiceApplicationTests
```

### Run Specific Test Method

```bash
cd book-service
mvn test -Dtest=BookServiceImplTest#addBook_shouldSaveAndReturnBook
```

### Run Tests with Coverage Report

```bash
cd book-service
mvn clean test jacoco:report
# Report location: target/site/jacoco/index.html
```

### Run Tests in Parallel (Faster Execution)

```bash
cd book-service
mvn test -DnumThreads=4
```

### Run Only the Three New Service Tests

```bash
# From BookNest root
mvn test -Dtest=BookServiceImplTest,OrderServiceImplTest,CartServiceImplTest
```

---

## Test Framework & Dependencies

All tests use:
- **Framework:** JUnit 5 (Jupiter)
- **Mocking:** Mockito
- **Spring Support:** MockitoExtension

Dependencies are already included in `spring-boot-starter-test` in pom.xml

---

## Test Statistics

### BookServiceImplTest.java
- Total Assertions: 67
- Test Methods: 21
- Coverage Areas: CRUD operations, stock management, search functionality

### OrderServiceImplTest.java
- Total Assertions: 52
- Test Methods: 18
- Coverage Areas: Order lifecycle, payment processing, notifications, stock deduction

### CartServiceImplTest.java
- Total Assertions: 58
- Test Methods: 18
- Coverage Areas: Cart operations, item management, price calculations

---

## Key Testing Patterns Used

### 1. Mocking with Mockito
```java
@Mock
private BookRepository bookRepository;

@InjectMocks
private BookServiceImpl bookService;
```

### 2. Setup and Teardown
```java
@BeforeEach
void setUp() {
    // Initialize test data before each test
}
```

### 3. Assertion Testing
```java
assertEquals(expectedValue, actualValue);
assertNotNull(result);
assertTrue(condition);
assertThrows(Exception.class, () -> method());
```

### 4. Verification
```java
verify(bookRepository, times(1)).save(any(Book.class));
verify(notificationPublisher, never()).publish(any());
```

---

## Integration with CI/CD

Tests will automatically run during:
1. **Maven build process:** `mvn clean install`
2. **Pre-commit hooks** (if configured)
3. **GitHub Actions** (if workflow configured)

---

## Next Steps

### For Remaining Services Without Comprehensive Tests:
1. **admin-service** - Create test directory and tests
2. **api-gateway** - Add comprehensive tests for gateway routing
3. **auth-service** - Add authentication/authorization tests
4. **eureka-server** - Add Eureka client registration tests
5. **notification-service** - Add notification publishing tests
6. **website-controller** - Add controller endpoint tests
7. **wishlist-service** - Add wishlist operation tests

---

## Troubleshooting

### If tests fail to run:
```bash
# Clean and rebuild
mvn clean install

# Check for compilation errors
mvn clean compile
```

### If dependencies are missing:
```bash
# Update dependencies
mvn dependency:resolve
mvn dependency:tree
```

### View test reports after running:
```bash
# In service directory after mvn test
open target/surefire-reports/  # macOS
start target/surefire-reports/ # Windows
```

---

## Continuous Integration Example (GitHub Actions)

Add to `.github/workflows/test.yml`:
```yaml
name: Run Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: 17
      - run: mvn clean test
```

