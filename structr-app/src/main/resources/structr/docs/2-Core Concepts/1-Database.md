# Database

Structr is a full-stack, end-to-end platform for application development and runtime environment. It has its own database, application server, middleware, and back-end and front-end components – everything you need to develop and run software applications in a single system that was designed and built as a whole from the outset.

This chapter focuses on the database layer of Structr, how data is stored and retrieved through the backend APIs.

## Labeled Property Graph Model

The most important core concept of Structr is that any structured data is modeled as [Graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)) in the sense of discrete mathematics or graph theory. Every object containing structured data is a node (or vertex), and nodes are connected by relationships (or vertices).

<svg width="100%" viewBox="0 0 150 20" xmlns="http://www.w3.org/2000/svg"><circle cx="10" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/><line x1="15" y1="10" x2="35" y2="10" stroke="currentColor" stroke-width="0.5" /><circle cx="40" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/><line x1="45" y1="10" x2="65" y2="10" stroke="currentColor" stroke-width="0.5" /><circle cx="70" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/></svg>

Even more precise, the underlying data model is a directed graph in which relationships/edges have an orientation. You can think of directed edges as arrows, pointing from the source node to the target node.

<svg width="100%" viewBox="0 0 150 20" xmlns="http://www.w3.org/2000/svg"><circle cx="10" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/><line x1="15" y1="10" x2="35" y2="10" stroke="currentColor" stroke-width="0.5" /><circle cx="40" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/><line x1="45" y1="10" x2="65" y2="10" stroke="currentColor" stroke-width="0.5" /><circle cx="70" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/><polyline points="33,9 34.75,10 33,11" stroke="currentColor" stroke-width="0.5" fill="none"/><polyline points="47,9 45.25,10 47,11" stroke="currentColor" stroke-width="0.5" fill="none"/></svg>

Nodes and relationships can have one or more labels, here marked as different colors.

<svg width="100%" viewBox="0 0 150 20" xmlns="http://www.w3.org/2000/svg"><polyline points="33,9 34.75,10 33,11" stroke="#d0d" stroke-width="0.5" fill="none"/><polyline points="47,9 45.25,10 47,11" stroke="#dd0" stroke-width="0.5" fill="none"/><line x1="15" y1="10" x2="35" y2="10" stroke="#d0d" stroke-width="0.5" /><line x1="45" y1="10" x2="65" y2="10" stroke="#dd0" stroke-width="0.5" /><circle cx="40" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="#90d01e"/><circle cx="10" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="var(--color-sky-500)"/><circle cx="70" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="var(--color-sky-500)"/></svg>

Properties, or attributes, are used to store data that are typically unique to a specific node or relationship.


## Graph Database

Consequentially, the ideal database implementation for storing graph data is a [Graph database](https://en.wikipedia.org/wiki/Graph_database), storing data in the form of nodes and relationships down to the physical layer.

Basically, Graph databases are an evolution of Object databases where data records are stored as objects. In Graph databases, the objects are called Nodes and Relationships, and the important addition is that the logical relations between node objects are modeled as relationship objects which are treated as "first-class citizens" which means that they are stored as pointers in the physical layer, allowing direct (indexed) lookup of the vicinity of a node.

That way, retrieving aggregated data from a graph database is much cheaper than in other datbases because it is achieved by traversing the graph, starting at one or multiple nodes, typically found by an index lookup over nodes attributes, and just running along the relationships, collecting data from each node and relationship objects touched on the path through the graph.

## Benefits of a Graph Database

Graph databases have some serious advantages over classic databases.

### Performance

One of the key advantages of a graph database is that complex database queries are much faster than in other databases. The reason for that is that the [Time complexity](https://en.wikipedia.org/wiki/Time_complexity) of graph operations is typically O(N+R) where N is the number of nodes and R the number of relationships involved.

The larger the set of data objects involved is, the higher is the reduction in query time compared to typical other database models. In relational databases for example, the Time complexity is typically O(N²) where N is the number of tables involved, because data aggregation requires Join operations which have exponential costs.

### Nested object structures

Another and probably the most important advantage of the Structr Backend is the ability to transform nested JSON objects into graph structures, and vice-versa. The transformation is based on contextual properties in the data schema which encapsulate the relationships between nodes in the graph.

You can think of the data model as a blueprint oder template of the structures that are created in the database. If you specify a relationship between two types, each of them gets a contextual property that manages the association.

In this example, we use Person and Project, with the following schema which reads "a person can work in multiple projects, a project has multiple persons working in it".

<svg width="100%" viewBox="0 0 150 20" xmlns="http://www.w3.org/2000/svg"><circle cx="10" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/><text style="font-family:sans-serif;font-size:2.5px;font-weight:600" x="5.5" y="18">Person</text><text style="font-family:sans-serif;font-size:2.5px;font-weight:600" x="28" y="9">WORKS_IN</text><line x1="15" y1="10" x2="55" y2="10" stroke="currentColor" stroke-width="0.5" /><text style="font-family:sans-serif;font-size:4px;font-weight:600" x="15.5" y="8.5">\*</text><text style="font-family:sans-serif;font-size:4px;font-weight:600" x="52" y="8.5">\*</text><circle cx="60" cy="10" r="5" stroke-width="0.5" stroke="currentColor" fill="none"/><text style="font-family:sans-serif;font-size:2.5px;font-weight:600" x="57" y="18">Project</text><polyline points="53,9 54.75,10 53,11" stroke="currentColor" stroke-width="0.5" fill="none"/></svg>

### Uniqueness

An important concept of the graph model is that objects which are unique in reality are represented as one single unique node in the graph. This concept of graph uniqueness differentiates a true graph database from pseudo graph databases where nested objects are represented by objects with different IDs.

![True Graph Database](true-graph-database.svg)

In the above example of a true graph database, two tasks of the same project are stored as two `Person` nodes connected to a single `Project` node. This way, you can query the graph by looking up the project by its name `X` and get a single `Project` object with two sub objects of the type `Person`: 

    GET Project?name=X

    {
        type: "Person",
        id: "1",
        name: "X",
        projects: [
            {
                type: "Project",
                id: "3",
                name: "A"
            },
            {
                type: "Project",
                id: "4",
                name: "B"
            }
        ]
    }
            

![Pseudo Graph Database](pseudo-graph-database.svg)

Whereas in the pseudo graph database, there are two Person nodes of the same name but with different IDs. Semantically, they represent the same person, but technically they are not the same record.

The same query would result in a different result:

    GET Project?name=X

     {
        type: "Project",
        id: "1",
        name: "X",
        projects: [
            {
                type: "Project",
                id: "3",
                name: "A"
            }
        ]
    },
    {
        type: "Project",
        id: "2",
        name: "X",
        projects: [
            {
                type: "Project",
                id: "4",
                name: "B"
            }
        ]
    }

>**Note:** The difference might be subtle but a pseudo graph database is just a relational database with its typical persistence pattern with all its disadvantages.

## Query Language

To get data out of a graph database, a client is required to formulate queries to fetch a set of data records.

Typical query languages like the widely used SQL are descriptive languages which means that they use commands like SELECT, INSERT, UPDATE and DELETE together with statements like WHERE to extract, filter and manipulate records from a database.

The graph data model allows for much complexer data structures so it's not surprising that a different type of query language has developed, a so-called pattern matching query language.

The following query languages are the most relevant for querying property graph databases:

### Cypher

[Cypher](https://neo4j.com/docs/cypher-manual/current/introduction/) is the original query language for labeled property graph databases developed by market leader Neo4j.

The following Cypher example query matches the pattern of project and task nodes, connected by relationships of any type and direction. The RETURN statement yields the resulting nodes as collections named `p` and `t`:

    MATCH (p:Project)--(t:Task) RETURN p,t

>**Note:** The above query doesn't return project or task nodes that are not connected to each other.

A more precise query would take the type and the direction of possible relationships between project and tasks nodes into account and would assign the resulting relationship objects to the variable `r`: 

    MATCH (p:Project)-[r:HAS_TASK]->(t:Task) RETURN p,r,t

>**Note:** If the schema had been modeled as `(Project)<--(Task)` or used a different relationship type, the above query would not yield any results.


### openCypher

[openCypher](http://opencypher.org/) is the open source specification of Cypher and has evolved towards GQL, nowadays an [ISO/IEC Standard](https://www.iso.org/standard/76120.html). This [GitHub Repo](https://github.com/opencypher/openCypher) contains the full specification.

### GQL

>**Note:** GQL stands for Graph Query Language and should not be confused with GraphQL, an API query language developed by Facebook.

[ISO/IEC 39075 GQL](https://www.iso.org/standard/76120.html) is the result of a standardization process by the ISO committee for Data Management and Interchange, [JTC 1/SC 32](https://jtc1info.org/sd-2-history/jtc1-subcommittees/sc-32/). The GQL standard "defines data structures and basic operations on property graphs. It provides capabilities for creating, accessing, querying, maintaining, and controlling property graphs and the data they comprise".

GQL is vendor-neutral and contains the most language features of Cypher/openCypher and can be seen as successor. Vendor-specific language features will be represented as GQL extensions.

## Supported Graph Databases

The following databases are supported by Structr:

- [Neo4j](https://neo4j.com)
- [Memgraph](https://memgraph.com/) (experimental)
- [Amazon Neptune](https://aws.amazon.com/de/neptune/) (experimental)
- Structr In-Memory Database (for testing only)

## Schema Enforcement

A fundamental feature of Structr's database layer is **schema enforcement**. Based on the schema definition—itself stored as a graph structure in Neo4j—Structr validates all input before it is written to the database. This ensures that structural and value-based schema constraints are never violated, guaranteeing perfect consistency and compliance with the rules defined in your schema.

### How Schema Enforcement Works

Schema enforcement operates at multiple levels:

- **Object and property type validation** - Ensures data types match the schema definition
- **Required properties** – Prevents creation or modification of objects if mandatory fields are missing
- **Cardinality constraints** – Enforces relationship multiplicities defined in the schema
- **Custom validators** – Through validation expressions and format constraints in property definitions

All validations are checked during a transaction before any data is persisted. Invalid schema definitions never become active because the entire transaction is rolled back in case of any inconsistency or error.

### Automatic Reletionship Management

Structr automatically manages relationships based on the type definitions and their defined cardinality, requiring no manual code:

#### One-to-One Relationships
When a one-to-one schema relationship is defined and you change one end of the relationship, Structr automatically:
1. Deletes the existing relationship
2. Creates the new relationship

This ensures that the cardinality constraint is never violated—no object can be connected to more than one target.

#### One-to-Many Relationships
With one-to-many schema relationships, existing relationships are preserved until the corresponding object is:
- Explicitly removed from the collection, or
- Deleted entirely

This allows multiple objects to be connected on one side while maintaining a single connection on the other.

#### Many-to-Many Relationships
Many-to-many relationships provide maximum flexibility, allowing multiple connections on both ends:
- Any number of objects can be connected on both sides of the relationship
- Relationships persist until explicitly removed from either collection
- No automatic deletion occurs when adding new connections
- Each object maintains its full collection of related objects independently

This pattern is ideal for scenarios like tags, categories, group memberships, or any situation where objects need flexible, bidirectional associations.

## Real-Time Schema Evolution

A unique characteristic of Structr's schema system is that **schema changes can be made in real-time**, taking only a few milliseconds to propagate. Despite this flexibility:

- **The schema remains always valid** – Invalid schema states are prevented at the definition level
- **No downtime required** – Schema modifications happen while the system is running
- **Immediate enforcement** – New constraints are applied instantly to all subsequent operations

The schema itself is stored as nodes and relationships in the graph database, making it:
- Queryable and modifiable programmatically
- Part of the database rather than external configuration files
- Versionable and trackable like any other data
