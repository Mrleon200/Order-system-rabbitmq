# Order System with RabbitMQ

A small **event-driven order processing system** built with **Spring Boot**, **RabbitMQ** and an in-memory **H2 database**.  
The goal of the project is to demonstrate how to offload heavy work (email, inventory update, logging…) from the HTTP request thread to background workers using a message queue.

The project was implemented as a coursework assignment and is intentionally simple, but follows a clean layered architecture and can be used as a learning reference.

---

## 1. Features

### Order side (client)

- Create new orders with:
  - Customer name  
  - Product ID  
  - Quantity  
  - Total price
- Asynchronous background processing via RabbitMQ:
  - Send confirmation email (simulated)
  - Update inventory
  - Write order log
- View all orders in a **dashboard table**:
  - Per-step status: Email / Inventory / Log
  - Overall status:  
    - **Completed** – all steps OK  
    - **Stock error** – email & log OK but inventory update failed  
    - **Processing** – still being handled in the background
- View **aggregated statistics**:
  - Total orders
  - Successfully processed orders
  - Pending orders
  - Orders with inventory errors
- Lookup order status by ID with a user-friendly explanation
- Cancel an order (with inventory rollback if it has already been decremented)

### Inventory side

- Dedicated **Inventory Management** page (`inventory.html`)
- Add or update stock for a product
- View the full inventory list in a table
- Edit or delete inventory items
- Visual **bar chart** of stock levels by product (color-coded: high / medium / low)

### Technology highlights

- Clear separation between **Controller – Service – Repository – Messaging – Config**
- Uses **RabbitMQ** as a real message broker (not just HTTP synchronous calls)
- Uses **H2 in-memory DB** so the application runs without any external database
- Provides **H2 Console** for debugging: you can inspect tables `ORDERS` and `INVENTORY`
- Simple, responsive HTML/CSS front-end (no framework) that works on both desktop and mobile

---

## 2. Architecture Overview

### Components

- **Spring Boot Application**
  - REST controllers for `/orders` and `/inventory`
  - Service layer with business logic: `OrderService`, `InventoryService`
  - JPA entities & repositories: `Order`, `Inventory`, `OrderRepository`, `InventoryRepository`
- **RabbitMQ**
  - Exchange, queue and routing key configured in `RabbitMQConfig`
  - Producer: `OrderProducer` sends `OrderCreatedMessage`
  - Consumer: `OrderConsumer` receives messages and performs background work
- **Database**
  - In-memory H2 database
  - Tables:
    - `ORDERS`: id, customerName, productId, quantity, totalPrice, createdAt, emailSent, stockUpdated, logWritten, cancelled  
    - `INVENTORY`: id, productId, quantity
- **Front-end**
  - `index.html`: order creation, status lookup, statistics, order list
  - `inventory.html`: inventory CRUD + bar chart

### Message flow (OrderCreated)

1. User creates an order via `POST /orders` or the form in `index.html`.
2. `OrderService.createOrder()` saves the order to H2 and publishes an `OrderCreatedMessage` to RabbitMQ.
3. `OrderConsumer` receives the message and, in the background:
   - “Sends email” (simulated)
   - Tries to decrease inventory for the product
   - Writes processing log
   - Calls `OrderService.updateOrderStatusProcessing()` to update flags on the `Order`.
4. The front-end periodically reloads or the user presses “Load orders” / “Load stats” to see the updated status.

If inventory is insufficient, only the inventory step fails; the system still records that email and log were written, and marks the order as a **stock error**.

---

## 3. Tech Stack

- **Backend**
  - Java 17+  
  - Spring Boot (Web, Data JPA, AMQP, H2)
  - RabbitMQ Java client via Spring AMQP
- **Database**
  - H2 (in-memory, auto-created schema via JPA)
- **Messaging**
  - RabbitMQ (default port 5672, management UI on 15672)
- **Build & Dependency Management**
  - Maven
- **Front-end**
  - Static HTML + CSS + vanilla JavaScript (Fetch API)

---

## 4. Getting Started

### 4.1 Prerequisites

- **Java JDK 17+**
- **Maven 3.8+**
- **RabbitMQ + Erlang** installed locally  
  (default user `guest` / `guest`, localhost:5672)
- Git (if cloning the repository)

### 4.2 Clone the project

```bash
git clone https://github.com/<your-account>/Order-system-rabbitmq.git
cd Order-system-rabbitmq
````

### 4.3 Start RabbitMQ

Make sure the RabbitMQ service is running:

* On Windows (Service): `RabbitMQ` → Start
* Or via command line:

  ```bash
  rabbitmq-service start
  ```
* Optional: enable management plugin (once):

  ```bash
  rabbitmq-plugins enable rabbitmq_management
  ```

  Then open [http://localhost:15672/](http://localhost:15672/) to see the management UI.

### 4.4 Run the Spring Boot application

Using Maven:

```bash
mvn spring-boot:run
```

or:

```bash
mvn clean package
java -jar target/order-system-*.jar
```

By default the application starts on **port 8080**.

### 4.5 Access the UI

* **Order system (main UI)**
  `http://localhost:8080/index.html`

* **Inventory management**
  `http://localhost:8080/inventory.html`

* **H2 Console**
  `http://localhost:8080/h2-console`

  Use:

  * JDBC URL: `jdbc:h2:mem:ordersdb`
  * User: `sa`
  * Password: *(leave empty unless changed in `application.properties`)*

---

## 5. Configuration

All configuration is under `src/main/resources/application.properties`, for example:

```properties
server.port=8080

spring.datasource.url=jdbc:h2:mem:ordersdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

app.rabbitmq.exchange=order.exchange
app.rabbitmq.queue=order.created.queue
app.rabbitmq.routing-key=order.created
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

You can change host, port, credentials, or the exchange/queue names here if needed.

---

## 6. REST API Overview

### Orders

* **Create order**

  `POST /orders`

  ```json
  {
    "customerName": "Hung",
    "productId": "P001",
    "quantity": 2,
    "totalPrice": 150000
  }
  ```

* **Get all orders**

  `GET /orders`

* **Get a single order**

  `GET /orders/{id}`

* **Get processing status of an order**

  `GET /orders/{id}/status`
  Returns flags like `emailSent`, `stockUpdated`, `logWritten` and `orderId`.

* **Cancel an order**

  `POST /orders/{id}/cancel`
  If the order was already processed and stock was deducted, this will try to **restore inventory**.

* **Get statistics**

  `GET /orders/stats`

  ```json
  {
    "totalOrders": 3,
    "processedOrders": 1,
    "pendingOrders": 0,
    "failedOrders": 2
  }
  ```

### Inventory

* **Increase stock for a product (simple demo endpoint)**

  `POST /inventory/increase?productId=P001&quantity=10`

* **List all inventory items**

  `GET /inventory`

* **Get inventory by product ID**

  `GET /inventory/{productId}`

* **Create inventory item**

  `POST /inventory`

  ```json
  { "productId": "P002", "quantity": 100 }
  ```

* **Update inventory item**

  `PUT /inventory/{id}`

* **Delete inventory item**

  `DELETE /inventory/{id}`

The HTML pages use these endpoints via JavaScript `fetch()` calls.

---

## 7. Concurrency & Reliability Notes

* Order creation and status update methods are annotated with `@Transactional` to ensure database consistency.
* RabbitMQ handles message delivery; if a consumer fails, messages can be re-delivered.
* Inventory checks ensure that stock is not allowed to go negative; when not enough stock is available, the order is marked as a **stock error** and can be inspected or cancelled.
* The system was designed to handle **multiple concurrent clients** placing orders without data races at the database level.

---

## 8. Why RabbitMQ instead of Redis (for this project)

Although Redis can be used as a simple queue (e.g. with `LPUSH`/`BRPOP`), RabbitMQ offers:

* Native concepts: **exchange, queue, routing key, bindings**
* Built-in support for **acknowledgements, retries, TTL, dead-letter queues**
* A very useful **management web UI** (monitoring queues, messages, consumers)
* Better alignment with an **event-driven architecture**, where more consumers (email, warehouse, analytics, etc.) can subscribe to the same `OrderCreated` event.

For these reasons, RabbitMQ was chosen as the messaging backbone, while Redis would remain more suitable as a cache in this scenario.

---

