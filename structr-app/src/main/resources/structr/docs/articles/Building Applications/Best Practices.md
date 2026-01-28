
Structr gives you a lot of freedom in how you build applications. The schema is optional, the data model can change at any time, and there are often multiple ways to achieve the same result. This flexibility is powerful, but it also means that Structr doesn't force you into patterns that other frameworks impose by default.

This chapter collects practices that have proven useful in real projects. None of them are strict rules – Structr will happily let you do things differently. But if you're unsure how to approach something, these recommendations are a good starting point.

## Security

### Use the LoginServlet for Authentication

Configure your login form to POST directly to `/structr/rest/login` instead of implementing authentication in JavaScript. This handles session management automatically. The trade-off is that two-factor authentication requires a custom implementation.

### Automate SSL Certificate Renewal

If you use Let's Encrypt, schedule a user-defined function to call `$.renewCertificates()` daily.

### Use Group-Based Permissions for Type Access

You can grant an entire group access to all instances of a type directly in the schema. This is often simpler than managing individual object permissions.

### Set Visibility Flags Consistently

Login pages should be `visibleToPublicUsers` but not `visibleToAuthenticatedUsers`. Protected pages should be `visibleToAuthenticatedUsers` only. This prevents authenticated users from seeing the login page.

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