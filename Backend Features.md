### Structr Backend

The Structr backend is the foundation of any Structr system but can also be used stand-alone as high performance, domain-focused REST server for Neo4j.

It is realized as a wrapper around a Neo4j graph database, which supports defining and mapping schemas for your domain objects. Complex graph operations can be mapped to domain-oriented REST endpoints.
  
#### JSON/REST Interface
- Bidirectional Graph-to-JSON document transformation
    - Conversion of arbitrary subgraphs into JSON documents
    - Creation of subgraphs from JSON documents with automatic relationship creation
    - Support for arbitrary document views on a single object / subgraph
    - Relationship-dependent document views
- Java-based schema declaration
- Custom REST endpoints
- Access control (users, groups)
- Sorting
- Paging (incl. offset + negative paging)

#### URL Path Mapping
- Run-time URL path evaluation
- Dynamic URL mapping
    - REST endpoint to graph entites
    - URL appendices for scopes, filters

#### Search
- Search/filtering by complex criteria
- Type-safe property search
- Supports 'null' value (search for empty fields)
- Complex search queries with boolean combinations, grouping
- Range queries (strings, date, numerical, open interval)
- Geocoding and spatial search
    - Configurable geo data providers
    - Predefined map providers: Google, Bing

#### Schema
- Type inheritance
- Mix-ins (interfaces with properties, validation and functionality)
- Type-safety
- Constraints/validation
    - Non-null/non-empty fields
    - Regular expressions
    - Complex logic
- Semantic error messages (error tree)
- Filters
- Transformations
- Cascading delete
- Forced delete (to enforce compliance with constraints)
- Auto-creation of related entities
- Synchronous/async. counters (f.e. for a view count)
- Dynamic entity definitions (experimental)
    - Configure new types at run-time through JSON/REST
    - Add custom properties

#### Transactions
- Full Neo4j transaction support
- Automatic deadlock detection and resolution
- Creation, modification and deletion callbacks
    - Before commit: onCreation/onModification/onDeletion
    - After commit: afterCreation/afterModification/afterDeletion
    - Cascading events

#### Tools
- Push notifications (HTTP/e-mail)
- Cron-like task scheduler
- Reporting
- Asynchronous background agents
- Maintenance tools (rebuild index, import/export, change properties, change type, ..)

#### Infrastructure
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
