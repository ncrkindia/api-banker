# Japi: Project Status

This document tracks the goals,  components, and  roadmap items for the **Japi-API Testing** desktop application.
Tech Java based desktop app. should have same look and feel of Postman .
---

## 1. Project Goal
Build a high-fidelity, premium, and **completely offline** desktop API client (similar to Postman) using Java 21 and Swing, using Postman like theme, font and text .

### Key Design Pillars:
* **Offline-First Security**: Zero cloud dependencies or registration. All data is saved on the local disk.
* **Readable filesystem workspace**: All collections, requests, history logs, and environment variables are saved in human-readable JSON files. The user can switch data directories inside the UI to easily version-control directories under Git.
* **Self-Signed SSL Support**: Seamless testing of local servers (e.g. `localhost` running with self-signed SSL certificates) by default.
* **Premium Usability**: Tabbed request editing, auto-generating query parameter/header grids, automatic JSON response formatting, and Postman Collection imports.

---

## 2.  Features

### Core Infrastructure
* [] **Maven Build Configuration**: Configured `pom.xml` with dependencies for FlatLaf (UI L&F), RSyntaxTextArea (syntax editor), and Gson (JSON binding).
* [] **JSON Storage Layer**: `StorageManager` reads and writes `collections.json`, `environments.json`, and `history.json` under `~/.japi/data` or a user-selected path.
* [] **Asynchronous Client Engine**: `HttpClientWrapper` wraps Java 21's `HttpClient` inside a SwingWorker background thread to avoid freezing the GUI during network execution.
* [] **Variable Resolution**: Supports injecting environment values into URLs, headers, and request bodies via `{{variable_name}}` syntax.
* [] **Self-Signed Certificates**: Configured default trust managers allowing connections to unsafe/local SSL endpoints.

### UI & UX Elements
* [] **Modern L&F**: Customized FlatLaf Dark theme with rounded components (`arc=8`), customized tab bars, and glowing branding.
* [] **Dynamic Key-Value Tables**: KeyValue tables for params, headers, and environment variables that automatically insert new rows upon editing.
* [] **Tabbed Request Workspace**: Supports opening multiple requests side-by-side with close buttons and method color badges (e.g. green for `GET`, orange for `POST`).
* [] **Sidebar Tree & Lists**:
  - Search filter text field for searching collections and history.
  - Interactive collections tree with right-click options to add/delete/rename requests. Grouping ofn  Add options in Add (Like Add request, Add Runner, Add JWT, Add Cmparator, Add JSON formattor)
  - Double-click loading of items.
  - History viewer sorted chronologically.
* [] **Importer & Exporter Utility**: Parses and loads Postman Collections and Environments, and exports JAPI collections/environments to Postman-compatible JSON formats.
* [] **Request Editor**: Authorization inputs (Bearer Token, Basic Auth credentials) and request body syntax editor.
* [] **Response Panel**: Shows colored status tags (2xx green, 3xx blue, 4xx orange, 5xx red), latency durations, response size formatting, pretty-printed JSON/XML highlight viewers, and response header grids.
* [] **Environment Dialog**: Full manager interface to add, delete, and modify variables.

---


### Protocol Enhancements
* [ ] **Multipart/Form-Data Support**: Add a file/text form-data selector inside the Request Body tab to upload binary files.
* [ ] **Cookie Jar Manager**: Capture response cookies and attach them to outgoing requests of matching domains.
* [ ] **WebSocket Client**: A dedicated tab style to establish connection, send frame payloads, and stream real-time events.
* [ ] **GraphQL Support**: An editor pane to write GraphQL queries, variables, and fetch introspection schemas.

### Automation & Scripting
* [ ] **Pre-request & Test Scripts**: Embed a JavaScript engine (e.g., Nashorn or GraalVM JavaScript) to run pre-request setups and validation assertions (similar to Postman's `pm.test` API).
* [] **Collection Runner**: Run all requests inside a collection in a sequence with statistics reporting (success/failure summaries) with Comatible to JMeter imort and Export option(jms). Show Visualtion and result with APi Name, Methods, Status code, Response Time(avg,min,max,p99,p95,p90,p75), Size , success  and failure(4xx,5xx) count and percentage, Response format(JSON, XML, Text). Also same at total.
 [] **Collection Runner**: with Export to CSV, Excel and PDF with all config and metedata needed. 

### Developer Quality-of-Life
* [] **UI Scaling**: Zoom in or out of the application interface and editors using `Ctrl + =/+` and `Ctrl + -`.
* [ ] **Response Search**: Add a search/filter bar inside the Response panel to find text substrings within JSON/HTML payloads.
* [ ] **Response Exporter**: "Save Response to File" button to download response payloads directly.
* [ ] **Code Snippet Generator**: Automatically generate fetch code snippets from the request in other languages (curl, JavaScript fetch, Python requests, Java HttpClient).


### Additional Feature
*  **JWT Decoder**: Add a dedicated tab for decoding JWT tokens, showing header, payload, and signature in a readable format.
*  **Response Comparator**: Add a dedicated tab for comparing two JSON or XML responses, showing the differences in a readable format.
*  **Response Formatter**: Add a dedicated tab for formatting JSON or XML responses in a readable format.
*  **Schema Validator**: Add a dedicated tab for validating JSON or XML responses against a schema.
*  **Mock Server**: Create a mock server that can be used to test API responses.
*  **Data Masker**: Add a dedicated tab for masking data in JSON or XML responses.
*  **Data Anonymizer**: Add a dedicated tab for anonymizing data in JSON or XML responses.
*  **Data Generator**: Add a dedicated tab for generating data in JSON or XML format.
*  **Data Validator**: Add a dedicated tab for validating data in JSON or XML format.
*  **Data Transformer**: Add a dedicated tab for transforming data in JSON or XML format.
*  **Data Comparotor**: Add a dedicated tab for comparing two JSON or XML data, showing the differences in a readable format.



### Additional log feature
*  **Log Console**: Add a dedicated tab for logging all the requests and responses with filtering options.
*  **Log Exporter**: Add a dedicated tab for exporting logs to CSV, Excel and PDF format.
*  **Log Filter**: Add a filter to filter logs based on request method, status code, response time, and response size.
*  **Log Search**: Add a search bar to search logs based on request method, status code, response time, and response size.
*  **Log Viewer**: Add a viewer to view logs in a readable format.
*  **Log for RUnner** : Save runner log in differenet file format as per selected format and metedata needed. and also provide runner history.. Name as yyyy-mm-dd--<collectionname>-<runnumber>-<incre>.log and also provide runner history.. Name as yyyy-mm-dd--<collectionname>-<runnumber>-<incre>.log



