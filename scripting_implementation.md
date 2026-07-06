# Pre-request & Test Scripts — Implementation Summary

## Overview
Full Postman-compatible JavaScript scripting engine for Japi, powered by **Mozilla Rhino**. Users can write pre-request scripts that run before HTTP execution and test scripts that run after, with assertions displayed in a dedicated Test Results tab.

---

## Files Created

| File | Purpose |
|------|---------|
| [ScriptExecutor.java](file:///d:/project%20slpro/japi/src/main/java/in/slpro/japi/http/ScriptExecutor.java) | Core Rhino-based script engine with `pm.*` API |
| [ScriptResult.java](file:///d:/project%20slpro/japi/src/main/java/in/slpro/japi/model/ScriptResult.java) | Model holding test assertions, console logs, errors |

## Files Modified

| File | Changes |
|------|---------|
| [HttpClientWrapper.java](file:///d:/project%20slpro/japi/src/main/java/in/slpro/japi/http/HttpClientWrapper.java) | Added `executeWithScripts()` → returns `ExecutionResult` (response + script results). Old `execute()` delegates to it for backward compat. |
| [ResponsePanel.java](file:///d:/project%20slpro/japi/src/main/java/in/slpro/japi/ui/ResponsePanel.java) | Added **Test Results** tab with pass/fail table, summary badges, and console output area |
| [RequestPanel.java](file:///d:/project%20slpro/japi/src/main/java/in/slpro/japi/ui/RequestPanel.java) | Updated `sendRequest()` to use `executeWithScripts()`. Added snippet sidebar to script tabs. |

---

## `pm.*` API Reference

### Available in Both Pre-request & Test Scripts

| API | Description |
|-----|-------------|
| `pm.environment.get("key")` | Get environment variable |
| `pm.environment.set("key", "value")` | Set/create environment variable |
| `pm.environment.unset("key")` | Remove environment variable |
| `pm.environment.toObject()` | Get all variables as JS object |
| `pm.variables.get("key")` | Get request-scoped transient variable |
| `pm.variables.set("key", "value")` | Set request-scoped transient variable |
| `pm.request.url` | Current request URL |
| `pm.request.method` | HTTP method |
| `pm.request.headers` | Request headers as object |
| `pm.request.body` | Request body content |
| `console.log()` / `.info()` / `.warn()` / `.error()` | Log to console output |

### Test Scripts Only

| API | Description |
|-----|-------------|
| `pm.response.code` | HTTP status code (number) |
| `pm.response.status` | Status text ("OK", "Not Found") |
| `pm.response.responseTime` | Response time in ms |
| `pm.response.responseSize` | Response size in bytes |
| `pm.response.text()` | Response body as string |
| `pm.response.json()` | Parse body as JSON object |
| `pm.response.headers.get("name")` | Get response header value |
| `pm.test("name", fn)` | Define a test assertion |
| `pm.expect(value)` | Chainable assertion (see below) |

### Chainable Assertions

```javascript
pm.expect(value).to.equal(expected)
pm.expect(value).to.eql(expected)
pm.expect(value).to.include("substring")
pm.expect(value).to.be.a("string")
pm.expect(value).to.be.above(n)
pm.expect(value).to.be.below(n)
pm.expect(obj).to.have.property("key")
pm.expect(code).to.have.status(200)
pm.expect(arr).to.have.lengthOf(n)
pm.expect(value).to.not.equal(unexpected)  // negation
```

---

## UI Features

### Script Editor Tabs
- **Pre-request Script** tab with JavaScript syntax highlighting
- **Tests** tab with JavaScript syntax highlighting
- **Snippet Sidebar** — clickable reference buttons that insert example code at cursor position

### Test Results Tab (Response Panel)
- **Summary Bar** — color-coded pass/fail count (green ✓ all passed, red ✗ failures)
- **Assertion Table** — each test with PASS/FAIL badge, name, and failure details
- **Console Output** — all `console.log()` messages from both pre-request and test scripts
- **Tab Badge** — Test Results tab title shows pass/fail count
- **Auto-focus** — automatically switches to Test Results tab when failures occur

---

## Example Usage

### Pre-request Script
```javascript
// Generate a timestamp and set it as env variable
var ts = new Date().getTime();
pm.environment.set("timestamp", "" + ts);
console.log("Generated timestamp: " + ts);
```

### Test Script
```javascript
pm.test("Status code is 200", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("Response time is acceptable", function() {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

pm.test("Response has user data", function() {
    var data = pm.response.json();
    pm.expect(data).to.have.property("id");
    pm.expect(data.name).to.be.a("string");
});

pm.test("Save token for next request", function() {
    var data = pm.response.json();
    pm.environment.set("auth_token", data.token);
    console.log("Token saved: " + data.token);
});
```
