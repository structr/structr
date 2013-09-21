# Structr Backend

The Structr backend is the basis of any Structr system. It is a Java wrapper around a Neo4j graph database where you can define a schema, and which provides additional features listed below.

## Features

### Schema

- Polymorphic schema definitions
- Support of
    - type inheritance,
    - interfaces,
    - mix-ins
- Type-safe property system
- Constraints/validation
    - Non-null/non-empty fields
    - Regular expressions
    - Complex logic
- Filters
- Transformations
- Cascading delete
- Forced delete (to enforce compliance with constraints)
- Auto-creation of related entities
- Synchronous/async. counters (f.e. for a view count)
- Semantic error messages (error tree)
- Event handlers
    - Before commit: onCreation/onModification/onDeletion
    - After commit: afterCreation/afterModification/afterDeletion: 
- Dynamic entity definitions (experimental)
    - Configure new types at run-time via JSON/REST
    - Add/remove properties
    
### URL Path Mapping
    
- Run-time URL path evaluation
- Dynamic URL mapping
    - REST endpoint to graph entites
    - URL appendices for scopes, filters

### JSON/REST Interface

- Real-time JSON documents creation out of graph objects
    - Complex selection criteria
    - Arbitrary depth
    - Neo4j core API for best performance
    - Traversal API for convenience
    - Cypher queries for maximum flexibility
- Scoped views
- Streaming JSON serialization
- Lazy evaluation (down to Neo4j Iterables)
- Access control (users, groups)
- Sorting
- Paging (incl. offset + negative paging)
- Triggers with cascading events

### Search

- Search/filtering by complex criteria
- Type-safe property search
- Supports 'null' value (search for empty fields)
- Complex search queries with boolean combinations, grouping
- Range queries (strings, date, numerical, open interval)
- Geocoding and spatial search
    - Configurable geo data providers
    - Predefined map providers: Google, Bing

### Tools

- Push notifications (HTTP/e-mail)
- Cron jobs
- Reporting
- Asynchronous background agents
- Maintenance tools (rebuild index, import/export, change properties, change type, ..)

## Infrastructure

- Embedded Neo4j (1.9, 2.0 in development)
- Embedded Jetty servlet container
- Integrated UrlRewrite engine for flexible URL mapping
- Google GSON (de-/serialization)
- Server configurable with builder pattern
- Typical response times: 1-5 ms
- Full ACID-compliant transactions
- Thread safe (even with hundreds of parallel requests)
- Required Maven projects:
    - structr-core
    - structr-rest
    - structr-server
- Maven archetypes

