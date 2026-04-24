# Smart Campus RESTful API

> **Author:** Hasanjaya Perera | 20231509 (w2120668)

---

## Table of Contents

- [API Overview](#api-overview)
- [How to Build and Run](#how-to-build-and-run)
- [API Endpoints](#api-endpoints)
- [Sample cURL Commands](#sample-curl-commands)
- [Report: Answers to Coursework Questions](#report-answers-to-coursework-questions)

---

## API Overview

The Smart Campus API is a robust, highly available RESTful web service designed to manage campus infrastructure. Built exclusively with **Java and JAX-RS (Jersey)**, it provides a seamless interface for facilities managers and automated building systems to track Rooms, Sensors, and Historical Sensor Readings.

**Key architectural features include:**

- **Deep Resource Nesting:** Implements the Sub-Resource Locator pattern for logical hierarchical routing (e.g., `/sensors/{id}/readings`).
- **Resilient Error Handling:** Features a leak-proof API design with custom Exception Mappers guaranteeing structured JSON responses for `409 Conflict`, `422 Unprocessable Entity`, `403 Forbidden`, and `500 Internal Server Errors`.
- **Complete Observability:** Global JAX-RS filters intercept and log all inbound URIs and outbound HTTP status codes.

**Base URL:** `http://localhost:8080/smart-campus-api/api/v1`

---

## How to Build and Run

### Prerequisites
- Apache NetBeans
- Apache Tomcat (or any configured JAX-RS compatible web server)
- Maven

### Steps

**1. Clone the Repository**

```bash
git clone https://github.com/Hasanjaya123/CSA-Smart-Campus-Sensor-Room-Management-API-.git
```

**2. Open the Project**

Launch Apache NetBeans. Select **File > Open Project**, navigate to the cloned directory, and select the `smart-campus-api` folder.

**3. Clean and Build**

In the Projects window, right-click the `smart-campus-api` project node and select **Clean and Build**. This will resolve all Maven dependencies and compile the JAX-RS classes. Wait for the `BUILD SUCCESS` message in the output console.

**4. Deploy and Run the Server**

Right-click the project node again and select **Run**. NetBeans will automatically deploy the compiled application to your configured web server and launch it.

**5. Verify Deployment**

Open a web browser or Postman and navigate to the Discovery endpoint:

```
http://localhost:8080/smart-campus-api/api/v1
```

---

## API Endpoints

### System Discovery

| Method | Endpoint | Description | Success Response |
|--------|----------|-------------|-----------------|
| `GET` | `/` | Returns API metadata and HATEOAS navigational links. | `200 OK` |

---

### Room Management

| Method | Endpoint | Description | Success Response | Error Responses |
|--------|----------|-------------|-----------------|-----------------|
| `GET` | `/rooms` | Retrieves a list of all registered rooms. | `200 OK` | — |
| `POST` | `/rooms` | Creates a new room. | `201 Created` | `400 Bad Request` |
| `GET` | `/rooms/{roomId}` | Retrieves details for a specific room. | `200 OK` | `404 Not Found` |
| `DELETE` | `/rooms/{roomId}` | Deletes a room. | `204 No Content` | `409 Conflict` (if sensors exist) |

---

### Sensor Operations

| Method | Endpoint | Description | Success Response | Error Responses |
|--------|----------|-------------|-----------------|-----------------|
| `GET` | `/sensors` | Retrieves all sensors. | `200 OK` | — |
| `GET` | `/sensors?type={type}` | Retrieves sensors filtered by type (e.g., `CO2`). | `200 OK` | — |
| `POST` | `/sensors` | Registers a new sensor to a specific room. | `201 Created` | `422 Unprocessable Entity` (invalid room) |

---

### Sensor Readings (Sub-Resources)

| Method | Endpoint | Description | Success Response | Error Responses |
|--------|----------|-------------|-----------------|-----------------|
| `GET` | `/sensors/{id}/readings` | Fetches the historical log of readings for a sensor. | `200 OK` | `404 Not Found` |
| `POST` | `/sensors/{id}/readings` | Appends a new reading and updates the parent sensor. | `201 Created` | `403 Forbidden` (if `MAINTENANCE`) |

---

## Sample cURL Commands

### 1. System Discovery

```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1 -H "Accept: application/json"
```

### 2. Create a New Room

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\":\"LIB-301\", \"name\":\"Library Quiet Study\", \"capacity\":50}"
```

### 3. Retrieve All Rooms

```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms -H "Accept: application/json"
```

### 4. Register a New Sensor

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\":\"TEMP-001\", \"type\":\"Temperature\", \"status\":\"ACTIVE\", \"roomId\":\"LIB-301\"}"
```

### 5. Filter Sensors by Type

```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=Temperature" -H "Accept: application/json"
```

### 6. Add a Sensor Reading

```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -d "{\"value\": 24.5}"
```

---

## Report: Answers to Coursework Questions

### Part 1: Service Architecture & Setup

#### 1. Default Lifecycle of a JAX-RS Resource Class & Data Synchronization

By default, the JAX-RS runtime uses a Per-Request lifecycle — a brand-new instance of a Resource class (such as RoomResource) is created for every incoming HTTP request and destroyed once the response is sent.

This has significant implications for in-memory data management. A high-traffic API processes multiple requests simultaneously, meaning multiple resource class instances will attempt to read and write to backend data structures concurrently. Using standard collections like HashMap or ArrayList would create race conditions, leading to thread-safety failures, overwritten data, or ConcurrentModificationException crashes.

To prevent data loss, the mock database relies exclusively on ConcurrentHashMap and properly synchronized block mechanisms.

#### 2. The Benefits of Hypermedia (HATEOAS) in Advanced RESTful Design

HATEOAS (Hypermedia as the Engine of Application State) transforms a static API into a dynamic, self-documenting state machine. By embedding navigable links in JSON responses (such as in the Discovery endpoint), the server explicitly tells the client what state transitions and resources are currently available.

This completely decouples the client from the server's URI routing structure. Client developers program against the "rooms" link provided by the Discovery endpoint rather than hardcoding /api/v1/rooms. If backend routing ever changes, client code remains unbroken — vastly improving the maintainability and resilience of the distributed system.

---

### Part 2: Room Management

#### 1. Implications of Returning Only IDs vs. Full Room Objects

| Approach | Pros | Cons |
|----------|------|------|
| IDs only | Minimal payload, conserves bandwidth, low serialization overhead | Forces the "N+1 query pattern" — one request for IDs, then N more for details; introduces severe network latency |
| Full objects | Resolves N+1 in a single round-trip; all UI data delivered upfront | Larger initial payload |

Returning full objects is generally preferred. Modern networks handle reasonably sized JSON arrays efficiently, and the elimination of subsequent requests greatly optimizes client-side rendering speed.

#### 2. Idempotency of the DELETE Operation

The DELETE operation in RoomResource is strictly idempotent. An operation is idempotent if executing it multiple times produces the same server state as executing it once.

- **First call:** If no active sensors exist, the room is removed from the ConcurrentHashMap and the server returns 204 No Content.
- **Subsequent calls:** The server finds the room is already absent, safely bypasses the removal logic, and still returns 204 No Content.

The server state remains unchanged across repeated calls — satisfying the strict definition of REST idempotency.

---

### Part 3: Sensor Operations & Linking

#### 1. Technical Consequences of a MediaType Mismatch

The @Consumes(APPLICATION_JSON) annotation enforces the API contract by defining the data format the server accepts.

If a client sends a payload as text/plain or application/xml, the JAX-RS runtime intercepts the request before it reaches any Java method logic. It inspects the Content-Type header, finds a mismatch, and immediately returns HTTP 415 Unsupported Media Type. This automatic validation prevents incompatible data from crashing the internal JSON parser and ensures the backend only processes correctly formatted structures.

#### 2. Query Parameters vs. Path Parameters for Filtering

| Parameter Type | Intended Use |
|----------------|-------------|
| Path parameters | Identify a unique or hierarchical resource (e.g., a specific sensor by ID) |
| Query parameters | Filter or modify the view of a collection without changing its core identity |

The primary architectural advantage of query parameters is composability. Multiple optional filters can be combined cleanly (e.g., ?type=CO2&status=ACTIVE). Using path parameters for filtering causes URL namespace pollution and forces complex, brittle routing logic for every possible filter combination.

---

### Part 4: Deep Nesting with Sub-Resources

#### 1. Architectural Benefits of the Sub-Resource Locator Pattern

Defining every nested path within a single controller class causes it to deteriorate into a bloated, tightly coupled "God Object" — thousands of lines long, difficult to maintain, and prone to version control conflicts.

The Sub-Resource Locator pattern enforces strict Separation of Concerns through delegation. Instead of handling readings logic itself, SensorResource acts as a router: upon receiving /sensors/{sensorId}/readings, it extracts the ID context and delegates to a new instance of SensorReadingResource.

Key benefits for large-scale APIs:

- **Maintainability:** Classes remain lightweight and focused on exactly one domain entity.
- **Reusability:** SensorReadingResource is fully decoupled from its parent's routing annotations and can be reused in other contexts.
- **Complexity Management:** Delegating down a chain of specialized classes allows developers to navigate and update deep hierarchies without risking breaking changes to the broader application.

---

### Part 5: Advanced Error Handling, Exception Mapping & Logging

#### 1. Semantic Accuracy: 422 Unprocessable Entity vs. 404 Not Found

Returning 404 implies the target URI endpoint could not be located. However, when a client sends a POST to a valid endpoint with a roomId foreign key that doesn't exist in the database, a 404 is misleading.

422 Unprocessable Entity is semantically correct for this scenario — it communicates that the server understood the content type, the JSON syntax was valid, and the endpoint was reached, but the request could not be processed due to a semantic business logic error (the missing dependency).

#### 2. Cybersecurity Risks of Exposing Java Stack Traces

Allowing unhandled exceptions like NullPointerException to return raw Java stack traces represents a severe Information Disclosure vulnerability. A stack trace acts as a reconnaissance map of the backend, explicitly leaking:

- Internal package naming conventions (e.g., com.example.dao)
- File and line numbers where logic flaws occur
- Names and versions of underlying frameworks (e.g., Jersey, Jackson)

Malicious actors use this footprint to search vulnerability databases and craft targeted exploits. A catch-all ExceptionMapper mitigates this by intercepting crashes and sanitizing the response into a safe, generic 500 Internal Server Error.

#### 3. Advantages of JAX-RS Filters for Cross-Cutting Concerns

Logging is a cross-cutting concern — distinct from core business logic but affecting the entire application. Manually inserting Logger.info() statements inside every resource method creates massive code duplication and tightly couples business logic to infrastructure.

JAX-RS filters (ContainerRequestFilter and ContainerResponseFilter) solve this by acting as centralized interceptors. Logging logic is defined in exactly one place, and the framework automatically applies it to every incoming request and outgoing response across the entire API — ensuring consistent formatting while keeping resource classes clean and focused solely on processing data.
