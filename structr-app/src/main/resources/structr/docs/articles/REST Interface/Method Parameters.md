All schema methods and user-defined functions in Structr can be called via the REST API. This article explains how to pass parameters to these methods and how to access them from within your code.

The parameter handling described here applies to all method types:

| Method Type | Endpoint | `this` inside the method |
| --- | --- | --- |
| User-defined function | `/structr/rest/myFunction` | not available |
| Static method | `/structr/rest/MyType/myMethod` | not available |
| Instance method | `/structr/rest/MyType/<uuid>/myMethod` | the object identified by `<uuid>` |
| Method on current user | `/structr/rest/me/myMethod` | the authenticated user |

The way you pass and access parameters is identical across all of these. Only `this` differs.

## Endpoints

The REST endpoint depends on the method type:

    /structr/rest/<functionName>                      (user-defined function)
    /structr/rest/<TypeName>/<methodName>             (static method)
    /structr/rest/<TypeName>/<uuid>/<methodName>      (instance method)
    /structr/rest/me/<methodName>                     (method on current user)

For example, a user-defined function called `processOrder` is available at `/structr/rest/processOrder`, while a static method `findOverdue` on the type `Invoice` is available at `/structr/rest/Invoice/findOverdue`.

Each method is configured to accept a specific HTTP verb (POST, GET, PUT, PATCH, or DELETE). Requests with a different verb will return a `405 Method Not Allowed` error.

## Passing Parameters

How you pass parameters depends on the HTTP verb.

### POST, PUT, and PATCH

For these verbs, parameters are sent as a JSON object in the request body. Each top-level key in the JSON object becomes a named parameter inside the function.

#### Request

    $ curl -s -HX-User:admin -HX-Password:admin \
        -H"Content-Type: application/json" \
        -XPOST http://localhost:8082/structr/rest/processOrder \
        -d '{ "orderId": "ORD-2026-001", "priority": 3, "express": true }'

#### Using fetch()

When calling from JavaScript in the browser, use the `body` property (not `data`) and set the `Content-Type` header explicitly:

```javascript
fetch('/structr/rest/processOrder', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ orderId: 'ORD-2026-001', priority: 3, express: true })
});
```

> **Common mistake:** The `fetch()` API uses `body` for the request payload. Using `data` (which is a jQuery convention) sends an empty body, and your function will receive no parameters.

### GET and DELETE

For GET and DELETE requests, there is no request body. Instead, values can be passed as additional path segments:

    /structr/rest/myTestFunction/value1/value2

**Path segments require declared parameters.** Structr maps each segment to the corresponding declared parameter by position. Without declared parameters (or with fewer declared parameters than path segments), the request fails with a `422 Unprocessable Entity` error:

    {
      "code": 422,
      "message": "Tried to call user-defined function 'myTestFunction‛ with illegal arguments. To fix this error, you can either specify method parameters, or call the method with a single argument of type object, e.g. { \"name\": \"example\" }."
    }

For a function with declared parameters `foo` and `bar`, a request to `/structr/rest/myTestFunction/x/y` gives you:

```javascript
{
    let foo = $.args.foo;           // "x"
    let bar = $.args.bar;           // "y"
    let bar2 = $.retrieve('bar');   // also "y"
}
```

To pass values to a GET/DELETE method without declaring parameters, use query string parameters and read them via `$.request` inside the function (see Accessing Query String Parameters below).

{{"URL Compliance",h2,shortDescription,children}}

## Accessing Parameters in Code

### JavaScript

In JavaScript, use the `$.args` object (or its aliases `$.arguments` and `$.methodParameters`):

```javascript
{
    let orderId  = $.args.orderId;
    let priority = $.args.priority;
    let express  = $.args.express;

    $.log('Processing order ' + orderId + ' with priority ' + priority);
}
```

All three names refer to the same object:

| Accessor | Description |
| --- | --- |
| `$.args` | Short form (recommended) |
| `$.arguments` | Alias for `$.args` |
| `$.methodParameters` | Alias for `$.args` |

You can also use `$.retrieve()`:

```javascript
{
    let orderId = $.retrieve('orderId');
}
```

### StructrScript

In StructrScript, use the `retrieve()` function:

    ${retrieve('orderId')}

### Accessor Reference

The following table clarifies which accessors work for method parameters and which do not:

| Accessor | Works for... | Notes |
| --- | --- | --- |
| `$.args` | Enumeration or inspection of all parameters | Returns a map-like object, e.g. `{foo: "x", bar: "y"}` |
| `$.args.paramName` | Named parameters | Direct property access |
| `$.retrieve('paramName')` | Named parameters | Falls back to values stored with `$.store()` if no such parameter exists |
| `$.retrieve` (without call) | Not for inspection | Returns the function reference itself, not the parameters map |
| `Object.keys($.args)` | Listing parameter names | Useful for inspecting what was passed |
| `$.get('paramName')` | Not for parameters | Resolves a property on the current entity (`this`) |
| `$.requestStoreGet('paramName')` | Not for parameters | Only reads values previously stored with `$.store()` or `$.requestStore` |

To inspect all current parameters, use `$.args` directly -- it returns a readable map of all parameter names and values. `$.retrieve` is a function and only produces useful output when called with a key argument.

For clean named access, declare parameters on the function. This is the recommended approach for any method that takes arguments.

## Accessing Query String Parameters

If you need to read query string parameters from a GET request (e.g. `/structr/rest/search?q=test&limit=10`), use the `$.request` object:

```javascript
{
    let query = $.request.q;
    let limit = $.request.limit;
}
```

The `$.request` object exposes query string parameters as properties, reading directly from the underlying HTTP servlet request. This works regardless of the HTTP verb. If a parameter appears multiple times in the query string, `$.request` returns the single value for one occurrence and an array for multiple occurrences.

## Declared Parameters

You can declare typed parameters on a user-defined function in the Code area of the Admin UI. Declared parameters serve two purposes: they enable positional argument mapping for GET/DELETE requests, and they provide automatic type conversion.

### Positional Mapping

When a function has declared parameters, positional path arguments are mapped to parameter names in order. For example, if you declare two parameters `customerId` (String) and `year` (Integer), a GET request to:

    /structr/rest/getCustomerReport/C-100/2026

maps `"C-100"` to `customerId` and `"2026"` to `year` (converted to an Integer). Inside the function, you access them like any other parameter:

```javascript
{
    let id   = $.args.customerId;  // "C-100"
    let year = $.args.year;        // 2026 (Integer)
}
```

### Automatic Type Conversion

If you declare a parameter type, Structr converts incoming values automatically. Supported conversions:

| Declared Type | Input | Conversion |
| --- | --- | --- |
| `Integer`, `int` | Number or numeric String | Converted to Integer |
| `Long`, `long` | Number or numeric String | Converted to Long |
| `Double`, `double` | Number or numeric String | Converted to Double |
| `Float`, `float` | Number or numeric String | Converted to Float |
| `Date` | ISO 8601 String | Parsed to Date |

If a value cannot be converted (e.g. passing `"abc"` for an Integer parameter), the request fails with a `422 Unprocessable Entity` error.

Without declared parameters, all values pass through as-is. JSON numbers arrive as numbers, strings as strings, objects as maps.

## store() vs. Method Parameters

The `$.store()` / `$.retrieve()` mechanism and method parameters are separate. When you call `$.retrieve('key')`, Structr first checks whether `key` is a method parameter. Only if no parameter with that name exists does it fall back to values stored with `$.store()`.

This means that if you call `$.store('name', ...)` where `name` is also a method parameter, the stored value becomes inaccessible through `$.retrieve()` -- the parameter always takes precedence. Structr logs a warning when this happens. To avoid the conflict, use `$.requestStore` to read and write stored values directly:

```javascript
{
    // Parameter "name" is passed via REST
    let paramName = $.retrieve('name');     // returns the parameter value

    $.requestStore.name = 'other value';    // writes to the request store
    let stored = $.requestStore.name;       // reads from the request store
}
```

## Complete Example

A user-defined function `createTask` configured for POST:

```javascript
{
    let projectId = $.args.projectId;
    let taskName  = $.args.name;
    let assignee  = $.args.assigneeEmail;

    $.assert(!$.empty(projectId), 422, 'projectId is required');
    $.assert(!$.empty(taskName),  422, 'name is required');

    let project = $.find('Project', projectId);
    $.assert(project != null, 404, 'Project not found');

    let user = null;
    if (!$.empty(assignee)) {
        user = $.first($.find('User', { eMail: assignee }));
    }

    let task = $.create('Task', {
        name: taskName,
        project: project,
        owner: user
    });

    return task;
}
```

#### Request

    $ curl -s -HX-User:admin -HX-Password:admin \
        -H"Content-Type: application/json" \
        -XPOST http://localhost:8082/structr/rest/createTask \
        -d '{ "projectId": "a01e2889c250...", "name": "Design review", "assigneeEmail": "alice@example.com" }'

#### Response

    {
        "result": {
            "id": "f7a3b921...",
            "type": "Task",
            "name": "Design review"
        },
        "serialization_time": "0.001234"
    }
