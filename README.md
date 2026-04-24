# Smart Campus RESTful API

**Author: Hasanjaya Perera | 20231509 (w2120668)**

---

## API Overview

The Smart Campus API is a robust, highly available RESTful web service designed to manage campus infrastructure. Built exclusively with **Java and JAX-RS (Jersey)**, it provides a seamless interface for facilities managers and automated building systems to track Rooms, Sensors, and Historical Sensor Readings.

Key architectural features include:

- **Deep Resource Nesting:** Implements the Sub-Resource Locator pattern for logical hierarchical routing (e.g., `/sensors/{id}/readings`).
- **Resilient Error Handling:** Features a leak-proof API design with custom Exception Mappers guaranteeing structured JSON responses for 409 Conflict, 422 Unprocessable Entity, 403 Forbidden, and 500 Internal Server Errors.
- **Complete Observability:** Global JAX-RS filters intercept and log all inbound URIs and outbound HTTP status codes.

**Base URL:** `http://localhost:8080/smart-campus-api/api/v1`

---

## How to Build and Run (Apache NetBeans)

**1. Clone the Repository:**

Open your terminal or command prompt and run:

```bash
git clone https://github.com/Hasanjaya123/CSA-Smart-Campus-Sensor-Room-Management-API-.git
```

**2. Open the Project:**

Launch Apache NetBeans. Select **File > Open Project**, navigate to the directory where you cloned the repository, and select the `smart-campus-api` folder.

**3. Clean and Build:**

In the Projects window on the left, right-click the `smart-campus-api` project node and select **Clean and Build**. This will resolve all Maven dependencies and compile the JAX-RS classes. Wait for the `BUILD SUCCESS` message in the output console.

**4. Deploy and Run the Server:**

Right-click the project node again and select **Run**. NetBeans will automatically deploy the compiled application to your configured web server (e.g., Apache Tomcat) and launch it.

**5. Verify Deployment:**

Open a web browser or Postman and navigate to the Discovery endpoint:

```
http://localhost:8080/smart-campus-api/api/v1
```

---

## API Endpoints

### System Discovery

| Method | Endpoint | Description | Success Response |
|--------|----------|-------------|-----------------|
| GET | `/` | Returns API metadata and HATEOAS navigational links. | 200 OK |

---

### Room Management

| Method | Endpoint | Description | Success Response | Error Responses |
|--------|----------|-------------|-----------------|-----------------|
| GET | `/rooms` | Retrieves a list of all registered rooms. | 200 OK | - |
| POST | `/rooms` | Creates a new room. | 201 Created | 400 Bad Request |
| GET | `/rooms/{roomId}` | Retrieves details for a specific room. | 200 OK | 404 Not Found |
| DELETE | `/rooms/{roomId}` | Deletes a room. | 204 No Content | 409 Conflict (If sensors exist) |

---

### Sensor Operations

| Method | Endpoint | Description | Success Response | Error Responses |
|--------|----------|-------------|-----------------|-----------------|
| GET | `/sensors` | Retrieves all sensors. | 200 OK | - |
| GET | `/sensors?type={type}` | Retrieves sensors filtered by type (e.g., CO2). | 200 OK | - |
| POST | `/sensors` | Registers a new sensor to a specific room. | 201 Created | 422 Unprocessable Entity (Invalid room) |

---

### Sensor Readings (Sub-Resources)

| Method | Endpoint | Description | Success Response | Error Responses |
|--------|----------|-------------|-----------------|-----------------|
| GET | `/sensors/{id}/readings` | Fetches the historical log of readings for a sensor. | 200 OK | 404 Not Found |
| POST | `/sensors/{id}/readings` | Appends a new reading and updates the parent sensor. | 201 Created | 403 Forbidden (If MAINTENANCE) |

---

## Sample CURL Commands

**1. System Discovery**

```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1 -H "Accept: application/json"
```

**2. Create a New Room**

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\":\"LIB-301\", \"name\":\"Library Quiet Study\", \"capacity\":50}"
```

**3. Retrieve All Rooms**

```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms -H "Accept: application/json"
```

**4. Register a New Sensor**

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\":\"TEMP-001\", \"type\":\"Temperature\", \"status\":\"ACTIVE\", \"roomId\":\"LIB-301\"}"
```

**5. Filter Sensors by Type**

```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=Temperature" -H "Accept: application/json"
```

**6. Add a Sensor Reading**

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -d "{\"value\": 24.5}"
```

---

## Report: Answers to Coursework Questions

### Part 1: Service Architecture & Setup

#### 1. Default Lifecycle of a JAX-RS Resource Class & Data Synchronization

By default, the JAX-RS runtime uses a **Per-Request lifecycle**. This means the server instantiates a brand-new instance of a Resource class (such as `RoomResource`) for every single incoming HTTP request and then destroys it once the response is sent.

This architectural design has massive implications for in-memory data management. Because a high-traffic API will process multiple requests simultaneously, multiple instances of the resource classes will attempt to read and write to the backend data structures at the exact same time. If standard collections like `HashMap` or `ArrayList` were used, these concurrent threads would create race conditions, leading to thread-safety failures, overwritten data, or `ConcurrentModificationException` crashes. To prevent data loss and ensure thread-safe synchronization across the per-request resource instances, the Mock database relies exclusively on `ConcurrentHashMap`s and properly synchronized block mechanisms.

#### 2. The Benefits of Hypermedia (HATEOAS) in Advanced RESTful Design

Hypermedia as the Engine of Application State (HATEOAS) is a hallmark of advanced RESTful design because it transforms a static API into a dynamic, self-documenting state machine. By providing navigable links within the JSON responses (such as in the Discovery endpoint), the server explicitly tells the client what state transitions and resources are currently available.

This provides a massive benefit to client developers compared to static documentation. It completely decouples the client application from the server's specific URI routing structure. Client developers no longer need to hardcode endpoints like `/api/v1/rooms` — instead, they program the client to follow the `"rooms"` link provided by the Discovery endpoint. If the backend routing ever changes in the future, the client code will not break, vastly improving the maintainability and resilience of the distributed system.

---

### Part 2: Room Management

#### 1. Implications of Returning Only IDs vs. Full Room Objects

When fetching a collection of resources, deciding between returning an array of raw IDs versus an array of fully populated objects involves a direct trade-off between network bandwidth and client-side processing.

- **Returning Only IDs:** This approach significantly minimizes the JSON payload size, conserving network bandwidth and reducing server-side serialization overhead. However, it forces the client to execute the **"N+1 query pattern."** To display a dashboard, the client must make one request to get the list of IDs, and then N subsequent HTTP requests to fetch the details for each specific room. This introduces severe network latency and complicates client-side state management.

- **Returning Full Objects:** This approach increases the initial payload size, but it is generally better for performance. It resolves the N+1 problem by providing the client with all necessary UI data in a single network round-trip. Given that modern networks handle reasonably sized JSON arrays efficiently, delivering full objects optimizes the client-side processing and rendering speed.

#### 2. Idempotency of the DELETE Operation

Yes, the DELETE operation implemented in the Room Resource class is strictly **idempotent**. In RESTful architecture, an operation is idempotent if executing it multiple times has the exact same effect on the server's state as executing it a single time.

In this implementation, if a client sends a valid DELETE request for a room that has no active sensors, the server successfully removes the room from the `ConcurrentHashMap` and returns HTTP `204 No Content`. If the client mistakenly sends the exact same DELETE request a second time, the server checks the map, finds that the room is already `null`, safely bypasses the removal logic, and still returns an HTTP `204 No Content` status. The server state remains unchanged, and no server-side crashes occur, satisfying the strict definition of REST idempotency.

---

### Part 3: Sensor Operations & Linking

#### 1. Technical Consequences of a MediaType Mismatch

In JAX-RS, the `APPLICATION_JSON` annotation strictly enforces the API contract by explicitly defining the data format the server is willing to accept for that specific endpoint.

If a client attempts to send a payload formatted as `text/plain` or `application/xml`, the JAX-RS runtime intercepts the request before it ever reaches the Java method logic. The framework inspects the incoming `Content-Type` HTTP header and realizes it does not match the `@Consumes` declaration. As a result, JAX-RS immediately rejects the payload and autonomously returns an HTTP **415 Unsupported Media Type** error to the client. This automatic validation is highly beneficial because it prevents incompatible data from crashing into the internal JSON parser and ensures the backend only processes correctly formatted data structures.

#### 2. Query Parameters vs. Path Parameters for Filtering

While both approaches can technically route a request, utilizing **Query Parameters** for filtering is vastly superior to embedding the filter within the URL path due to RESTful architectural semantics.

In REST design, **Path Parameters** should be used exclusively to identify a unique resource or a fixed hierarchical entity (such as a specific sensor ID). **Query Parameters** are designed to filter or modify the view of an underlying collection without changing the collection's core identity.

The primary architectural advantage of query parameters is **composability**. If the API needs to evolve to support multiple optional filters simultaneously, query parameters handle this cleanly. If you attempted to use path parameters for this, you would suffer from URL namespace pollution, forcing you to write complex and brittle routing logic to handle every possible combination of optional filters.

---

### Part 4: Deep Nesting with Sub-Resources

#### 1. Architectural Benefits of the Sub-Resource Locator Pattern and Delegation

In RESTful architecture, as APIs grow to encompass complex hierarchies — such as fetching the readings of a specific sensor inside a specific room — the routing logic can easily become tangled. If a developer defines every single nested path within a single controller class, that class rapidly deteriorates into a "God Object." It becomes thousands of lines long, tightly coupled, exceedingly difficult to maintain, and highly susceptible to version control merge conflicts in team environments.

The **Sub-Resource Locator pattern** elegantly solves this by enforcing strict **Separation of Concerns** through delegation. Instead of handling the business logic for sensor readings, the parent `SensorResource` simply acts as a router. When it receives a request for `/sensors/{sensorId}/readings`, it extracts the ID context and immediately hands off control by returning a new instance of the `SensorReadingResource` class.

This architectural mastery provides massive benefits for large-scale APIs:

- **Maintainability:** Classes remain incredibly lightweight and focused on exactly one domain entity.
- **Reusability:** The `SensorReadingResource` is fully decoupled from the routing annotations of its parent, meaning it could potentially be reused in other contexts.
- **Complexity Management:** By delegating requests down a chain of specialized classes, developers can easily navigate and update deep resource hierarchies without risking breaking changes to the broader application.

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging

#### 1. Semantic Accuracy: 422 Unprocessable Entity vs. 404 Not Found

Returning an HTTP `404` implies that the target URI endpoint itself (`/api/v1/sensors`) could not be located on the server. However, when a client sends a POST request to a valid endpoint, but includes a foreign key like `roomId` in the JSON payload that does not exist in the database, a `404` is highly misleading.

**HTTP 422 (Unprocessable Entity)** is semantically superior for this scenario. It correctly communicates to the client that the server understood the content type, the JSON syntax was perfectly valid, and the endpoint was reached — but the server was unable to process the contained instructions due to semantic business logic errors (the missing dependency).

#### 2. Cybersecurity Risks of Exposing Java Stack Traces

Allowing a global exception like `NullPointerException` to return a raw Java stack trace to the client represents a severe **Information Disclosure** vulnerability. From a cybersecurity standpoint, an unhandled stack trace acts as a comprehensive reconnaissance map of the backend architecture.

It explicitly leaks internal package naming conventions (e.g., `com.example.dao`), the specific file line numbers where logic flaws occur, and crucially, the exact names and versions of underlying frameworks and parsing libraries (such as Jersey or Jackson). Malicious actors utilize this precise footprint to search vulnerability databases and craft highly targeted exploits against those specific library versions or logic flaws. A catch-all Exception Mapper mitigates this by intercepting crashes and sanitizing the response into a safe, generic `500` status.

#### 3. Advantages of JAX-RS Filters for Cross-Cutting Concerns

Logging is a classic **cross-cutting concern** that affects the entire application but is completely distinct from the core business logic. If developers manually inserted `Logger.info()` statements inside every single resource method, it would result in massive code duplication, bloating the controllers, and tightly coupling the business logic to the infrastructure.

JAX-RS filters (`ContainerRequestFilter` and `ContainerResponseFilter`) solve this by acting as **centralized interceptors**. They allow you to define the logging logic in exactly one place. The framework then automatically applies this filter to wrap around every incoming request and outgoing response across the entire API. This ensures consistent formatting and keeps the core resource classes pristine and focused solely on processing data.
