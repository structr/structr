
Structr automatically generates OpenAPI 3.0.2 documentation for your REST API. This documentation describes your types, methods, and endpoints in a standardized format that other developers and tools can use to understand and interact with your API.

## When You Need OpenAPI

OpenAPI documentation becomes valuable when your API moves beyond internal use:

- **External developers** need to integrate with your system without access to your codebase
- **Frontend teams** want to generate TypeScript types or API clients automatically
- **Partners or customers** require formal API documentation as part of a contract
- **API testing tools** like Postman can import OpenAPI specs to create test collections
- **Code generators** can create client SDKs in various languages from your spec

If your Structr application is only used through its own pages and you control all the code, you may not need OpenAPI at all. But as soon as others consume your API, OpenAPI saves time and prevents misunderstandings.

## How It Works

Structr generates and serves the OpenAPI specification directly from your schema. There is no separate documentation file to maintain - when you request the OpenAPI endpoint, Structr reads your current schema and builds the specification on the fly. Add a property to a type or change a method signature, and the next request to the OpenAPI endpoint reflects that change.

You control what appears in the documentation: types and methods must be explicitly enabled for OpenAPI output, and you can add descriptions, summaries, and parameter documentation to make the spec useful for consumers.

## Accessing the Documentation

### Swagger UI

Structr includes Swagger UI, an interactive documentation interface where you can explore your API, view endpoint details, and test requests directly in the browser.

Access Swagger UI in the Admin UI:

1. Open the Code area
2. Click "OpenAPI" in the navigation tree on the left

Swagger UI displays all documented endpoints grouped by tag. You can expand any endpoint to see its parameters, request body schema, and response format. The "Try it out" feature lets you execute requests and see real responses.

### JSON Endpoints

The raw OpenAPI specification is available at:

```
/structr/openapi
```

This returns the complete OpenAPI document as JSON. You can use this URL with any OpenAPI-compatible tool - code generators, API testing tools, or documentation platforms.

When you organize your API with tags, each tag also gets its own endpoint:

```
/structr/openapi/<tag>.json
```

For example, if you tag your project management types with "projects", the documentation is available at `/structr/openapi/projects.json`. This is useful when you want to share only a subset of your API with specific consumers.

## Configuring Types for OpenAPI

By default, types are not included in the OpenAPI output. To document a type and its endpoints, you must explicitly enable it and assign a tag. Methods on that type must also be enabled separately - enabling a type does not automatically include all its methods.

> **Note:** OpenAPI visibility requires explicit opt-in at two levels: first enable the type, then enable each method you want to document. This gives you fine-grained control over what appears in your API documentation.

### Enabling OpenAPI Output for Types

In the Schema area or Code area:

1. Select the type you want to document
2. Open the type settings
3. Enable "Include in OpenAPI output"
4. Enter a tag name (e.g., "projects", "users", "public-api")

All types with the same tag are grouped together in the documentation. The tag also determines the URL for the tag-specific endpoint (`/structr/openapi/<tag>.json`).

### Type Documentation Fields

Each type has fields for OpenAPI documentation:

| Field | Purpose |
|-------|---------|
| Summary | A short one-line description shown in endpoint lists |
| Description | A detailed explanation shown when the endpoint is expanded |

Write the summary for scanning - developers should understand what the type represents at a glance. Use the description for details: what the type is used for, important relationships, or usage notes.

## Documenting Methods

Schema methods must also be explicitly enabled for OpenAPI output - just enabling the type is not enough. Each method you want to document needs its own OpenAPI configuration.

### Enabling OpenAPI Output for Methods

In the Schema area or Code area:

1. Select the method you want to document
2. Open the API tab
3. Enable OpenAPI output for this method
4. Add summary, description, and parameter documentation

Methods marked as "Not callable via HTTP" cannot be included in OpenAPI documentation since they are not accessible via the REST API.

### Method Documentation Fields

| Field | Purpose |
|-------|---------|
| Summary | A short description of what the method does |
| Description | Detailed explanation, including side effects or prerequisites |

### Parameter Documentation

In the API tab, you can define typed parameters for your method. Each parameter has:

| Field | Purpose |
|-------|---------|
| Name | The parameter name as it appears in requests |
| Type | The expected data type (String, Integer, Boolean, etc.) |
| Description | What the parameter is used for |
| Required | Whether the parameter must be provided |

Structr validates incoming requests against these definitions before your code runs. This provides automatic input validation and generates accurate parameter documentation.

### Example: Documenting a Search Method

For a method `searchProjects` that searches projects by keyword:

| Setting | Value |
|---------|-------|
| Summary | Search projects by keyword |
| Description | Returns all projects where the name or description contains the search term. Results are sorted by relevance. |

Parameters:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| query | String | Yes | The search term to match against project names and descriptions |
| limit | Integer | No | Maximum number of results (default: 20) |
| offset | Integer | No | Number of results to skip for pagination |

## Documenting User-Defined Functions

User-defined functions (global schema methods) can also be documented for OpenAPI. The same fields are available: summary, description, and typed parameters.

This is useful when you create utility endpoints that don't belong to a specific type - for example, a global search across multiple types or a health check endpoint.

## Global Settings

Configure global OpenAPI settings in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `openapiservlet.server.title` | Structr REST Server | The title shown at the top of the documentation |
| `openapiservlet.server.version` | 1.0.1 | The API version number |

Set these to match your application:

```properties
openapiservlet.server.title = Project Management API
openapiservlet.server.version = 2.1.0
```

The title appears prominently in Swagger UI and helps consumers identify which API they are viewing. The version number should follow semantic versioning and be updated when you make changes to your API.

## Standard Endpoints

Structr automatically documents the standard endpoints for authentication and system operations:

- `/structr/rest/login` - Session-based login
- `/structr/rest/logout` - End the current session
- `/structr/rest/token` - JWT token creation and refresh
- `/structr/rest/me` - Current user information

These endpoints appear in the documentation without additional configuration.

## Organizing Your API

### Choosing Tags

Tags group related endpoints in the documentation. Choose tags based on how API consumers think about your domain:

| Approach | Example Tags |
|----------|--------------|
| By domain area | `projects`, `tasks`, `users`, `reports` |
| By access level | `public`, `internal`, `admin` |
| By consumer | `mobile-app`, `web-frontend`, `integrations` |

You can use multiple tag strategies by giving some types domain tags and others access-level tags. A type can only have one tag, so choose the most useful grouping for your consumers.

### What to Include

Not every type needs to be in the OpenAPI documentation. Consider including:

- Types that external consumers interact with directly
- Types that represent your domain model
- Utility methods that provide specific functionality

Consider excluding:

- Internal types used only by your application logic
- Types that are implementation details
- Methods that should not be called by external consumers (mark these as "Not callable via HTTP")

## Best Practices

### Write for Your Consumers

Documentation is for people who don't know your codebase. Avoid jargon, explain abbreviations, and provide context. A good description answers: What is this? When would I use it? What should I know before using it?

### Keep Summaries Short

Summaries appear in lists and should be scannable. Aim for under 60 characters. Save details for the description field.

### Document Side Effects

If a method sends emails, creates related objects, or has other side effects, document them. Consumers need to know what happens when they call your API.

### Version Your API

Update `openapiservlet.server.version` when you make breaking changes. This helps consumers know when they need to update their integrations.

### Review the Output

Periodically open Swagger UI and review your documentation as a consumer would. Look for missing descriptions, unclear summaries, or undocumented parameters.

## Related Topics

- REST Interface - How the REST API works and how to access it
- Data Model - Configuring types and their OpenAPI settings
- Business Logic - Creating methods and configuring their API exposure
- Code Area (Admin UI) - Using Swagger UI and the method editor
