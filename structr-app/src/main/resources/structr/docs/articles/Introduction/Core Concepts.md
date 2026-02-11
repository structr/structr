
Structr is built on a graph database foundation. Understanding this architecture helps you make better decisions when modeling data and building applications.

## The Graph Data Model

All data in Structr is stored as a graph in the mathematical sense: objects are nodes, and connections between objects are relationships. Both nodes and relationships can have properties that store data, and both can have labels that indicate their type.

This differs fundamentally from relational databases, where data lives in tables and connections are established through foreign keys and join operations. In a graph database, relationships are first-class citizens stored as direct pointers between nodes, making traversal from one object to its related objects extremely efficient.

### Nodes, Relationships, and Properties

Nodes represent things – users, projects, documents, products, or any other entity in your domain. Each node has a type (like `User` or `Project`) and can have any number of properties (like `name`, `email`, or `createdDate`).

Relationships connect nodes and represent how things relate to each other. A relationship always has a direction (from source to target), a type (like `WORKS_ON` or `BELONGS_TO`), and can also have properties. For example, a `WORKS_ON` relationship between a User and a Project might have a `role` property indicating whether the user is a developer, manager, or reviewer.

Properties store actual data values. Structr supports common types like strings, numbers, booleans, and dates, as well as arrays and encrypted strings.

### Why Graphs?

Graph databases excel at handling connected data. When you need to find all projects a user works on, or all users who work on a specific project, or the shortest path between two entities, a graph database answers these questions by traversing relationships directly rather than performing expensive join operations.

The performance difference becomes significant as data grows. In relational databases, query time typically increases exponentially with the number of tables involved because joins have multiplicative cost. In graph databases, query time grows linearly with the number of nodes and relationships actually traversed – unrelated data doesn't slow things down.

### Graph Uniqueness

An important concept in graph modeling is that objects which are unique in reality should be represented by a single node in the graph. If the same person works on multiple projects, there should be one Person node connected to multiple Project nodes – not separate Person records duplicated for each project.

This differs from document databases where nested objects are often duplicated. In Structr, you model the relationship once, and the graph structure naturally reflects the real-world connections between entities.

## Supported Databases

Structr supports several graph database backends:

- **Neo4j** – The primary supported database, recommended for production use
- **Memgraph** – Experimental support
- **Amazon Neptune** – Experimental support
- **In-Memory Database** – For testing only

## Schema Enforcement

Structr validates all data against your schema before writing to the database. This ensures that structural and value-based constraints are never violated.

### How It Works

Schema enforcement operates at multiple levels:

- **Type validation** – Ensures data types match the schema definition
- **Required properties** – Prevents creation or modification of objects if mandatory fields are missing
- **Cardinality constraints** – Enforces relationship multiplicities (one-to-one, one-to-many, many-to-many)
- **Custom validators** – Through validation expressions and format constraints

All validations run during the transaction before data is persisted. If any validation fails, the entire transaction is rolled back and no changes are saved.

### Automatic Relationship Management

Structr manages relationships automatically based on the cardinality you define in the schema.

#### One-to-One

When you change one end of the relationship, Structr automatically removes the existing relationship and creates the new one. This ensures an object is never connected to more than one target.

#### One-to-Many

Existing relationships persist until you explicitly remove them or delete the related object. Multiple objects can connect on the "many" side while maintaining a single connection on the "one" side.

#### Many-to-Many

Maximum flexibility – any number of objects can connect on both sides. Relationships persist until explicitly removed.

## Real-Time Schema Evolution

Unlike traditional databases that require migrations and downtime for schema changes, Structr applies schema modifications instantly while the system is running. Changes take only milliseconds to propagate.

This works because the schema itself is stored as nodes and relationships in the graph database. When you modify the schema, you're updating data like any other operation – and the new constraints apply immediately to all subsequent operations.

### Schema and Data Are Loosely Coupled
However, the schema is loosely coupled to your data. When you rename a property, change a type, or restructure relationships, existing data is not automatically migrated. The old data remains as it was – a renamed property simply means existing nodes still have the old property name while new nodes get the new one. You need to migrate existing data manually, either through a script that updates all affected nodes or by handling both old and new structures in your application logic until the migration is complete.

### Incremental Development
This enables a development workflow where you can model your domain incrementally: start with a basic structure, build features against it, then extend the schema as requirements evolve. There's no migration step and no deployment process for schema changes. But keep in mind that while the schema changes instantly, bringing your existing data in line with the new schema is your responsibility.


## Accessing Data

Structr provides several ways to work with data, depending on the context.

In pages and business logic, you use built-in functions like `$.find()`, `$.create()`, and `$.delete()`. These functions work in both StructrScript (a simple expression language for template expressions) and JavaScript (for more complex logic). They handle security checks, transactions, and type conversion automatically.

For complex graph traversals, Structr supports Cypher – the query language developed by Neo4j for pattern matching in graphs:

```
MATCH (p:Project)-[:HAS_TASK]->(t:Task) 
WHERE p.status = 'active' 
RETURN p, t
```

This query finds all active projects and their tasks by matching the pattern of Project nodes connected to Task nodes via `HAS_TASK` relationships.

External systems access data through the REST API, which provides standard CRUD operations with filtering, sorting, and pagination.

## Next Steps

With these concepts in mind, you're ready to start building. The typical workflow is:

1. Design your data model in the Schema area
2. Create sample data to test your model
3. Build pages that display and manipulate your data
4. Add business logic to enforce rules and automate processes

Each of these topics is covered in detail in the Building Applications section.
