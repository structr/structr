
Structr gives you a lot of freedom in how you build applications. The schema is optional, the data model can change at any time, and there are often multiple ways to achieve the same result. This flexibility is powerful, but it also means that Structr doesn't force you into patterns that other frameworks impose by default.

This chapter collects practices that have proven useful in real projects. None of them are strict rules – Structr will happily let you do things differently. But if you're unsure how to approach something, these recommendations are a good starting point.

## Security

Security requires attention at multiple levels. A system is only as strong as its weakest link.

### Enable HTTPS

All production deployments should use HTTPS. Structr integrates with Let's Encrypt for free SSL certificates:

1. Configure `letsencrypt.domains` in `structr.conf` with your domain
2. Call the `/maintenance/letsencrypt` endpoint or use the `letsencrypt` maintenance command
3. Enable HTTPS: `application.https.enabled = true`
4. Configure ports: `application.http.port = 80` and `application.https.port = 443`
5. Force HTTPS: `httpservice.force.https = true`

### Automate Certificate Renewal

Let's Encrypt certificates expire after 90 days. Schedule a user-defined function to call `$.renewCertificates()` daily or weekly to keep certificates current.

### Enable Password Security Rules

Configure password complexity requirements in `structr.conf`:

```
security.passwordpolicy.minlength = 8
security.passwordpolicy.complexity.enforce = true
security.passwordpolicy.complexity.requiredigits = true
security.passwordpolicy.complexity.requirelowercase = true
security.passwordpolicy.complexity.requireuppercase = true
security.passwordpolicy.complexity.requirenonalphanumeric = true
security.passwordpolicy.maxfailedattempts = 4
```

### Use the LoginServlet for Authentication

Configure your login form to POST directly to `/structr/rest/login` instead of implementing authentication in JavaScript. This handles session management automatically.

### Secure File Permissions

On the server filesystem, protect sensitive files:

- `structr.conf` should be readable only by the Structr process (mode 600)
- Follow Neo4j's file permission recommendations for database files

### Use Encrypted String Properties

For sensitive data like API keys or personal information, use the `EncryptedString` property type. Data is encrypted using AES with a key configured in `structr.conf` or set via `$.set_encryption_key()`.

### Use Parameterized Cypher Queries

Always use parameters instead of string concatenation when building Cypher queries. This protects against injection attacks and improves readability.

#### Recommended

```javascript
$.cypher('MATCH (n) WHERE n.name CONTAINS $searchTerm RETURN n', { searchTerm: 'Admin' })
```

#### Not recommended

```javascript
$.cypher('MATCH (n) WHERE n.name CONTAINS "' + searchTerm + '" RETURN n')
```

The parameterized version passes values safely to the database regardless of special characters or malicious input.

### Use Group-Based Permissions for Type Access

Grant groups access to all instances of a type directly in the schema. This is simpler than managing individual object permissions.

### Set Visibility Flags Consistently

Login pages should be `visibleToPublicUsers` but not `visibleToAuthenticatedUsers`. Protected pages should be `visibleToAuthenticatedUsers` only.

### Test With Non-Admin Users Early

Admin users bypass all permission checks. If you only test as admin, permission problems won't surface until a regular user tries the application.

## Data Modeling

### Use Unique Relationship Types

Don't use generic names like `HAS` for all relationships. Specific names like `PROJECT_HAS_TASK` allow the database to query relationships directly without filtering in code.

### Index Properties You Query Frequently

Especially properties with uniqueness constraints – without an index, uniqueness validation slows down object creation significantly.

### Use Traits for Shared Functionality

If multiple types need the same properties or methods, define them in a trait and inherit from it. Structr supports multiple inheritance through traits, so a type can combine functionality from several sources.

### Use Self-Referencing Relationships for Tree Structures

A type can have a relationship to itself – for example, a `Folder` type with a `parent` relationship pointing to another `Folder`. This is the natural way to model hierarchies in a graph database.

## Business Logic

### Use "After" Methods for Side Effects

Email notifications, external API calls, and other side effects belong in `afterCreate` or `afterSave`, not in `onCreate` or `onSave`. The "after" methods run in a separate transaction after data is safely persisted.

### Use Service Classes for Cross-Type Logic

Logic that doesn't belong to a specific type – like report generation or external system integration – should live in a service class.

### Pass UUIDs Into Privileged Contexts

When using `$.doPrivileged()` or `$.doAs()`, pass the object's UUID and retrieve it inside the new context. Object references from the outer context carry the wrong security context.

## Pages and Templates

### Start With a Page Import

Instead of building pages from scratch, import an existing HTML template or page. Structr parses the HTML structure and creates the corresponding DOM elements, which you can then make dynamic with repeaters and data bindings.

### Use Shared Components for Repeated Elements

Headers, footers, and navigation menus should be Shared Components. Changes propagate automatically to all pages that use them.

### Use Template Elements for Complex Markup Blocks

Template elements contain larger blocks of HTML and can include logic that pre-processes data. Use them when you need more control than simple DOM elements provide – for example, when building a page layout with multiple insertion points.

### Call render(children) in Templates

Templates don't render their children automatically. If content disappears when you move it into a template, you probably forgot this.

### Use Pagination for Lists

Structr can render thousands of objects, but users can't navigate thousands of table rows. Always limit result sets with `page()` or a reasonable maximum.

### Create Widgets for Repeated Patterns

Widgets are reusable page fragments that can be dragged into any page. If you find yourself building the same UI pattern multiple times, turn it into a widget.

## Performance

### Create Views for Your API Consumers

The default `public` view contains only `id`, `type`, and `name`. Create dedicated views with exactly the properties each consumer needs – this reduces data transfer and improves response times.

### Use Cypher for Complex Graph Traversals

For queries that traverse multiple relationship levels, `$.cypher()` is often faster than nested `$.find()` calls. Results are automatically instantiated as Structr entities.

## What You Don't Need to Do

Structr handles many things automatically that other platforms require you to implement manually:

- Transactions – Every request runs in a transaction automatically
- Session management – The LoginServlet handles this
- REST endpoints – Created automatically for all schema types
- Relationship management – Handled based on schema cardinality
- Input validation – Schema constraints are enforced automatically

If you find yourself implementing any of these manually, there's probably a simpler way.
