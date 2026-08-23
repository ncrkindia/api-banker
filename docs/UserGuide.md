# 📄 ApiBanker Comprehensive User Guide

<div style="background-color: #f8f9fa; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px 20px; margin: 15px 0;">
  <b style="font-size: 15px; color: #1a73e8;">Table of Contents</b>
  <ul style="margin-top: 10px; padding-left: 20px; line-height: 1.8;">
    <li><a href="#sec-requests">1. Request Configuration & All Workspace Tabs</a></li>
    <li><a href="#sec-tree">2. Collection Tree, Organization & Import/Export</a></li>
    <li><a href="#sec-env-manager">3. Environment Manager & Profile Control</a></li>
    <li><a href="#sec-global-vars">4. Global Variable Manager & Scope Hierarchy</a></li>
    <li><a href="#sec-settings">5. Application Settings (⚙) & Configuration</a></li>
    <li><a href="#sec-auth">6. Comprehensive Authentication Strategies</a></li>
    <li><a href="#sec-scripting">7. Rhino JS Scripting & Advanced Examples (Pre & Test Scripts)</a></li>
    <li><a href="#sec-mock-server">8. Mock Server Panel & Endpoint Simulation</a></li>
    <li><a href="#sec-console-logs">9. Console & Logging Infrastructure</a></li>
    <li><a href="#sec-tools-suite">10. Developer Tools (JWT, Data Tools, Comparator, Cookies)</a></li>
    <li><a href="#sec-openapi">11. OpenAPI / Swagger Spec Import Engine</a></li>
    <li><a href="#sec-runner">12. Collection Runner & Load Simulation</a></li>
    <li><a href="#sec-shortcuts">13. Keyboard Shortcuts Reference</a></li>
    <li><a href="#sec-safety">14. UI Scaling, Stricter Safety Mode & SSL Diagnostics</a></li>
  </ul>
</div>

---

<a name="sec-requests"></a>
## 1. 🚀 Request Configuration & All Workspace Tabs

The **Request Panel** is the core workspace component for building, sending, and inspecting HTTP requests.

### 1.1 HTTP Method & Endpoint Bar
- **Method Selector**: Choose from standard HTTP verbs (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, `HEAD`).
- **URL Field**: Input target URI with dynamic variable interpolation (`{{baseUrl}}/v1/users`). Hovering over variables displays a popover with the resolved value and variable scope origin.
- **Send Button**: Press **Send** or `Ctrl + R` to execute. When a request is active, the Send button transforms into a **Cancel** button with single-click cancellation.

### 1.2 Request Tabs Detail
- **Params Tab**:
  - Key-value grid for query parameters automatically appended to the URL string.
  - Features auto-managing table rows (automatically appends an empty row when the last row is filled, and prunes empty rows on focus lost).
  - Individual row checkboxes allow instant enabling/disabling of parameters without deletion.
- **Authorization Tab**:
  - Select authentication strategy (`Inherit`, `Bearer Token`, `Basic Auth`, `API Key`, `OAuth 2.0`). See [Section 6](#sec-auth) for details.
- **Headers Tab**:
  - Custom HTTP headers grid with auto-completion for common headers (e.g. `Content-Type`, `Accept`, `Authorization`).
  - Supports dynamic variable substitution (e.g. `X-Request-ID: {{uuid}}`).
- **Body Tab**:
  - `none`: Sends request without a payload body.
  - `raw`: Syntax-highlighted code editor supporting `JSON`, `XML`, `HTML`, and `Plain Text`. Features auto-formatting and validation.
  - `form-data`: Key-value multipart form data. Supports text values and **Local File Uploads** (`Type = file`). Click the `...` button to select local files from your filesystem.
  - `x-www-form-urlencoded`: Standard web form URL-encoded parameter grid.
  - `GraphQL`: Dual-pane editor for GraphQL `Query` and `Variables` JSON with schema introspection.
- **Pre-request Script Tab**:
  - JavaScript editor for executing code prior to sending the HTTP request.
- **Tests Tab**:
  - JavaScript assertion editor for validating responses post-execution.
- **Settings Tab**:
  - **SSL Certificate Verification**: Override global SSL settings (`Inherit`, `Verify`, `No Verify`).
  - **Connection Timeout**: Override request timeout bounds in seconds.
  - **Follow Redirects**: Toggle automatic HTTP 3xx redirect handling.

### 1.3 Response Viewer
- **Status Badge**: Color-coded HTTP status code (2xx Green, 3xx Blue, 4xx Yellow, 5xx Red) with latency timing (ms) and payload size (KB).
- **Body Tab**: Syntax-highlighted response content (JSON/XML/HTML/Text) with search, prettify, and one-click copy.
- **Headers Tab**: Complete HTTP response header key-value table.
- **Cookies Tab**: Extracted response cookies automatically synchronized with the Cookie Jar.
- **Test Results Tab**: Summary table of passing and failing JavaScript assertion tests.

---

<a name="sec-tree"></a>
## 2. 📂 Collection Tree, Organization & Import/Export

The **Collection Sidebar** provides a hierarchical view of your API workspace.

### 2.1 Tree Organization & Actions
- **Structure**: Organize endpoints into **Collections**, nested **Folders**, and **Requests**.
- **Context Menu & Shortcuts**:
  - **F2**: Inline rename selected node.
  - **Delete**: Remove selected request, folder, or collection.
  - **Ctrl + C / Ctrl + V**: Copy and paste request nodes across collections and folders.
  - **Ctrl + D**: Instantly duplicate selected node.
- **Drag-and-Drop**: Drag requests and folders to reorder or move them across parent folders.

### 2.2 Collection README Documentation
Click any Collection node to open its overview tab. Write rich Markdown documentation describing API endpoints, setup instructions, and authentication details. Toggle seamlessly between **Edit** mode (editor with syntax highlighting) and **Read** mode (rendered HTML preview).

### 2.3 Import & Export Capabilities
- **Export Formats**: Export collections to standard **Postman v2.1 JSON** or Apache **JMeter `.jmx`** load test files.
- **Import Formats**: Import Postman v2.1 collections or OpenAPI / Swagger spec files.

---

<a name="sec-env-manager"></a>
## 3. 🌐 Environment Manager & Profile Control

Manage environment profiles to easily switch target hosts and configuration states (e.g. *Development*, *Staging*, *Production*).

### 3.1 Access & Controls
- **Access**: Click the environment dropdown in the top-right toolbar or press `Ctrl + E`.
- **Managing Profiles**: Create new environment profiles, duplicate existing ones, or delete unused profiles.
- **Key-Value Variables**: Define key-value pairs per environment. Supports enabled/disabled row toggles.
- **Active Environment Selection**: Select the active profile from the top-right dropdown. All requests immediately resolve `{{variableName}}` placeholders against the active environment.
- **Import / Export**: Save environment profiles as `.json` files or import existing environment files.

---

<a name="sec-global-vars"></a>
## 4. 🌐 Global Variable Manager & Scope Hierarchy

Global variables provide workspace-wide variables accessible across all collections and environments.

### 4.1 Access & Scope Precedence
- **Access**: Open via **Tools → Global Variables** from the main menu.
- **Scope Precedence (Highest to Lowest)**:
  1. **Environment Scope**: Overrides Collection and Global variables when an active environment is selected.
  2. **Collection Scope**: Overrides Global variables for requests within that specific collection.
  3. **Global Scope**: Default fallback accessible across the entire application workspace.

### 4.2 Recursive Variable Resolution
Variables resolve recursively across scopes. For example:
- Global Variable: `host` = `api.example.com`
- Collection Variable: `baseUrl` = `https://{{host}}/v1`
- Request URL: `{{baseUrl}}/users` → Resolves dynamically to `https://api.example.com/v1/users`.

---

<a name="sec-settings"></a>
## 5. ⚙️ Application Settings & Configuration

Access the primary configuration panel via **File → Settings** (or the gear icon ⚙).

### Key Settings Options
- **Look & Feel (Theme)**: Switch between modern FlatLaf themes (**Dark** / **Light**).
- **Stricter Editing Mode**: Enable explicit confirmation dialogs for destructive actions (deleting nodes, clearing lists, or undoing operations).
- **Default Request Timeout**: Set global connection and read timeout values (in seconds) for HTTP execution.
- **Global SSL Verification**: Enable or disable SSL certificate verification globally for all requests.
- **Logging Directory**: Configure custom file path for storing application action audit logs and mock server logs.
- **Enable File Logging**: Toggle daily rotating audit logs (`action_audit_<YYYY-MM-DD>.log`).

---

<a name="sec-auth"></a>
## 6. 🔐 Comprehensive Authentication Strategies

Authentication can be configured at the Collection level (with automatic child inheritance) or overridden per request.

| Auth Strategy | Configuration Details |
| :--- | :--- |
| **Inherit Auth** | Child requests automatically inherit authentication rules from their parent folder or collection. |
| **Bearer Token** | Enter Bearer token. Includes a **Decode JWT** button to open the token in the built-in JWT Decoder tool. |
| **Basic Auth** | Enter Username and Password. Automatically generates standard `Authorization: Basic Base64(...)` header. |
| **API Key** | Specify Key Name, Key Value, and Location (`Header` or `Query Param`). |
| **OAuth 2.0** | Supports **Authorization Code**, **Implicit**, **Password Credentials**, and **Client Credentials** grant types. Features built-in browser callback listener, client authentication modes (`Header` vs `Body`), and one-click access token refresh. |

---

<a name="sec-scripting"></a>
## 7. ⚡ Rhino JS Scripting & Advanced Examples

ApiBanker embeds the Mozilla Rhino JavaScript engine, empowering full request pre-processing and post-response assertion testing.

### 7.1 Apibanker JS Object Reference
```javascript
// Accessing Variables
apibanker.environment.get("varName");
apibanker.environment.set("varName", "value");
apibanker.collectionVariables.get("varName");
apibanker.collectionVariables.set("varName", "value");
apibanker.globals.get("varName");

// Accessing Request Details
apibanker.request.url;
apibanker.request.method;
apibanker.request.headers;
apibanker.request.body;

// Accessing Response Details (in Test scripts)
apibanker.response.status;      // e.g. 200
apibanker.response.time;        // Latency in ms
apibanker.response.body;        // Raw response text
apibanker.response.headers;     // Response headers object

// Assertion Helper
apibanker.test("Test Title", function() {
    return true; // Return boolean condition
});
```

---

### 7.2 Pre-request Script Examples

#### Scenario A: Dynamic Timestamp & UUID Generation
```javascript
// Generate current ISO timestamp & random UUID
var timestamp = new Date().toISOString();
var uuid = java.util.UUID.randomUUID().toString();

apibanker.environment.set("requestTimestamp", timestamp);
apibanker.environment.set("requestGuid", uuid);
```

#### Scenario B: Compute HMAC-SHA256 Signature
```javascript
// Calculate HMAC-SHA256 signature for API Security
var secret = "my_api_secret_key";
var payload = apibanker.request.method + ":" + apibanker.request.url;

var sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
var secret_key = new javax.crypto.spec.SecretKeySpec(
    new java.lang.String(secret).getBytes("UTF-8"), "HmacSHA256"
);
sha256_HMAC.init(secret_key);

var hash = javax.xml.bind.DatatypeConverter.printHexBinary(
    sha256_HMAC.doFinal(new java.lang.String(payload).getBytes("UTF-8"))
).toLowerCase();

apibanker.environment.set("apiSignature", hash);
```

#### Scenario C: Dynamic URL Query Parameter Construction
```javascript
// Append dynamic date range parameters
var today = new Date();
var startDate = new Date(today.getTime() - (7 * 24 * 60 * 60 * 1000)).toISOString().split('T')[0];
var endDate = today.toISOString().split('T')[0];

apibanker.environment.set("startDate", startDate);
apibanker.environment.set("endDate", endDate);
```

---

### 7.3 Test Script (Post-request) Examples

#### Scenario A: Assert HTTP Status & Response Time Threshold
```javascript
// Validate 200 OK status code
apibanker.test("Status code is 200 OK", function() {
    return apibanker.response.status === 200;
});

// Assert latency is under 500ms
apibanker.test("Response time is under 500ms", function() {
    return apibanker.response.time < 500;
});
```

#### Scenario B: Extract Bearer Token for Request Chaining
```javascript
// Parse JSON response and extract token to Environment
apibanker.test("Response contains access token", function() {
    var data = JSON.parse(apibanker.response.body);
    if (data && data.token) {
        apibanker.environment.set("authToken", data.token);
        return true;
    }
    return false;
});
```

#### Scenario C: Validate JSON Response Schema & Fields
```javascript
apibanker.test("User object has valid schema", function() {
    var res = JSON.parse(apibanker.response.body);
    return res.hasOwnProperty("id") && 
           typeof res.id === "number" && 
           res.hasOwnProperty("email") &&
           res.email.indexOf("@") !== -1;
});
```

#### Scenario D: Assert Response Headers
```javascript
apibanker.test("Content-Type is application/json", function() {
    var contentType = apibanker.response.headers["Content-Type"];
    return contentType && contentType.indexOf("application/json") !== -1;
});
```

#### Scenario E: Assert Array Items & Length
```javascript
apibanker.test("Users list is non-empty array", function() {
    var list = JSON.parse(apibanker.response.body);
    return Array.isArray(list) && list.length > 0;
});
```

---

<a name="sec-mock-server"></a>
## 8. 🛠️ Mock Server Panel & Endpoint Simulation

Run local HTTP mock servers to test client applications against simulated API endpoints.

### Key Features & Controls
- **Endpoint Rules**: Define HTTP method (`GET`, `POST`, etc.), path (`/api/v1/users`), and latency delay (ms).
- **Multi-Response Criteria**: Add multiple response variations based on matching query params or request header values.
- **Custom Status & Body**: Specify HTTP status code (e.g. `200`, `404`, `500`), content type headers, and mock JSON/XML response bodies.
- **Code Snippet Generator**: Instantly generate code snippets for client integration in `curl`, `Python`, `JavaScript (fetch)`, or `Java (HttpClient)`.
- **Mock Log Traffic & Download**: View real-time request traffic in the mock log area and download traffic logs to disk.

---

<a name="sec-console-logs"></a>
## 9. 🖥️ Console & Logging Infrastructure

ApiBanker includes an advanced, multi-layered logging system designed for full operational visibility.

### 9.1 Standalone Persistent Console Window
- **Independent Top-Level Window**: Open via **Tools → Live Console** or by pressing `Ctrl+Alt+C`. Operates as an independent window that can be minimized, maximized, and placed on separate displays.
- **Single-Instance Focus**: Only one Console window exists at a time. Re-opening brings the active console window to focus.
- **Live Level Filtering**: Filter log entries by level (`INFO`, `WARN`, `ERROR`, `DEBUG`) or search string.
- **Dynamic Zoom Sync**: The console bidirectionally synchronizes its zoom level (using `Ctrl+`, `Ctrl-`) with the main workspace.
- **Timestamped Downloads**: Export filtered logs to file with automated timestamp naming: `apibanker-live-log-yyyy-MM-dd-HH-mm-ss.log`.

### 9.2 Daily Action Audit Logs
- Automatically logs user actions (imports, exports, collection mutations, settings updates) to daily rotating log files (`action_audit_<YYYY-MM-DD>.log` capped at 10MB) in your configured log directory.

---

<a name="sec-tools-suite"></a>
## 10. ⚡ Developer Tools Suite

Access built-in developer productivity utilities directly from the sidebar or Tools menu:

### 10.1 JWT Decoder
- Paste any JWT token to instantly decode Header claims, Payload data, and Signature verification status. Inspect token expiration (`exp`), issuer (`iss`), and user claims with one-click copy features.

### 10.2 Data Tools (JSON Prettify & Mock Generator)
- **JSON Formatter**: Prettify messy JSON strings or minify JSON payloads for network efficiency.
- **JSON Schema Validator**: Validate JSON data against custom JSON Schema definitions.
- **Mock Data Generator**: Bulk-generate dynamic mock test data including UUIDs, realistic names, emails, addresses, timestamps, and numbers.

### 10.3 Data Comparator
- Side-by-side visual diff tool for JSON and XML payloads. Highlights additions, deletions, and value modifications with line-by-line visual indicators.

### 10.4 Cookie Jar
- Inspect, add, edit, and clear domain-specific HTTP cookies automatically managed during API executions.

---

<a name="sec-openapi"></a>
## 11. 📄 OpenAPI / Swagger Spec Import Engine

Import REST API specifications in JSON or YAML formats and convert them into organized test collections.

### Workflow Steps
1. Navigate to **File → Import OpenAPI / Swagger Spec**.
2. Select your Swagger 2.0 or OpenAPI 3.0 / 3.1 / 3.2 spec file.
3. Click **Analyze Spec** to parse all paths, parameters, and security definitions.
4. Review the endpoint selection matrix and check/uncheck endpoints to import.
5. Select **Base URL Strategy** (Direct URLs vs `{{baseUrl}}` Collection Variable).
6. Click **Import Selected**. Auto-generates a structured collection complete with an endpoint README document.

---

<a name="sec-runner"></a>
## 12. 📊 Collection Runner & Load Simulation

Run automated test suites and simulate concurrent server load across collections.

### Execution & Load Modes
- **Fixed Iterations**: Execute the collection sequence a specific number of times.
- **Fixed Duration**: Run continuous test cycles for a set duration (Seconds, Minutes, Hours, Days).
- **Virtual Users (VUsers) & Ramp-up**: Adjust concurrent threads and ramp-up timing to simulate real-world user traffic.

### Reporting & JMeter Export
- Monitor real-time execution progress with live scatter plots and pass/fail metrics.
- Export results to formatted **PDF** or **CSV** summary reports.
- Export native **Apache JMeter `.jmx`** test plans for large-scale performance testing.

---

<a name="sec-shortcuts"></a>
## 13. ⌨️ Keyboard Shortcuts Reference

| Key Combination | Action Command |
| :--- | :--- |
| **Ctrl + R** | Send / Execute active HTTP Request |
| **Ctrl + S** | Save active request or collection tab |
| **Ctrl + F** | Open Document Search Bar (User Guide) |
| **Ctrl + O** | Open saved Collection file |
| **Ctrl + E** | Open Environment Manager |
| **Ctrl + D** | Duplicate selected tree item (request / folder) |
| **Ctrl + C / Ctrl + V** | Copy / Paste request node |
| **F2** | Inline rename selected tree item |
| **Delete** | Delete selected request, folder, or collection |
| **Ctrl + `=` / `-`** | Responsive UI font zoom in / out |
| **Ctrl + Mouse Wheel** | Real-time global UI zoom adjustment |

---

<a name="sec-safety"></a>
## 14. 🎨 UI Scaling, Stricter Safety Mode & SSL Diagnostics

### Responsive High-DPI UI Scaling
ApiBanker supports real-time global font and component scaling via `Ctrl + Scroll` or `Ctrl + = / -`. UI components adjust layout padding and button widths dynamically to guarantee crisp, responsive rendering.

### Stricter Editing Mode
Enable **Stricter Editing Mode** in **Settings (⚙)** to safeguard your workspace. Destructive operations (deleting nodes, clearing lists, or undoing operations) trigger explicit confirmation dialogs to prevent accidental data loss.

### SSL Certificate Diagnostics
Configure SSL verification globally, per collection, or per request (`Inherit`, `Verify`, `No Verify`). Hover over the SSL validation indicator in the response status bar to view detailed, color-coded certificate trust reports and expiration metrics.

### Contextual Search (User Guide)
The User Guide features a robust, embeddable search bar activated via **Ctrl + F**. 
- **Live Highlight**: Instantly highlights search occurrences throughout the document.
- **Fuzzy Search & Exact Match**: Toggle strict case-sensitive matching or enable regex-backed fuzzy search to find content dynamically.
- **Quick Navigation**: Cycle through results using `Prev`/`Next` buttons and press `Esc` to quickly dismiss the search bar and return focus to reading.
