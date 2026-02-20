# Prefetching

Structr's Neo4j Bolt driver includes an automatic prefetching system that optimizes graph traversals by reducing the number of individual database queries. When your application accesses relationships on many nodes of the same type, prefetching detects this pattern and loads the relevant subgraph in a single bulk query, caching the results in memory for the duration of the transaction.

This is similar in concept to the N+1 query problem in relational databases, where iterating over a list of objects and accessing a related object on each one results in one additional query per item. Prefetching solves this by recognizing repetitive query patterns at runtime and replacing them with a single, broader query.

## How It Works

Prefetching operates at three levels: automatic detection and learning, manual triggering via scripting, and self-optimization through query combining.

### Automatic Prefetching

During a transaction, Structr monitors every relationship query that passes through the Bolt driver. Each query is recorded in a histogram that tracks the Cypher statement pattern, the source node type, the relationship type, the direction, and a counter of how often it has been executed.

When the counter for a given pattern exceeds a configurable threshold (default: 100 executions within a single transaction), the system checks whether activating prefetching for that pattern is safe. It runs a `count()` query to estimate the result size. If the result count is below the maximum allowed (default: 50,000), the pattern is registered for prefetching. If it exceeds the limit, the pattern is blacklisted instead, preventing it from being considered again.

The learned patterns are associated with a "prefetch hint" - an identifier set by higher-level application code that describes the context of the current operation, such as rendering a specific page or processing a REST request. On subsequent transactions with the same hint, all learned patterns are executed immediately at the start, populating the cache before application code begins accessing relationships.

Patterns that take longer than the maximum allowed duration (default: 1,000 ms) are also blacklisted automatically.

### Manual Prefetching

You can trigger prefetching explicitly from StructrScript or JavaScript using the `prefetch()` function. This is useful when you know in advance which part of the graph your code will traverse.

**StructrScript:**

```
${prefetch('(:Customer)-[]->(:Task)', merge('HAS_TASK', 'ASSIGNED_TO'))}
```

**JavaScript:**

```javascript
$.prefetch('(:Customer)-[]->(:Task)', ['HAS_TASK', 'ASSIGNED_TO']);
```

The first argument is a Cypher path pattern describing the subgraph to load. The second argument is a list of relationship type keys that should be considered prefetched after the query completes. This tells the caching layer that it does not need to make additional database queries for these relationship types on the affected nodes.

### Manual Prefetching with prefetch2()

The `prefetch2()` function provides more control over prefetching by allowing you to write a custom Cypher query that returns explicit node and relationship collections, and by separating outgoing and incoming relationship keys.

**JavaScript:**

```javascript
$.prefetch2(
    'MATCH (n:Customer)-[r:HAS_TASK]->(m:Task) RETURN collect(n) + collect(m) AS nodes, collect(r) AS rels',
    ['Customer/all/OUTGOING/HAS_TASK'],
    ['Task/all/INCOMING/HAS_TASK']
);
```

**StructrScript:**

```
${prefetch2(
    'MATCH (n:Customer)-[r:HAS_TASK]->(m:Task) RETURN collect(n) + collect(m) AS nodes, collect(r) AS rels',
    merge('Customer/all/OUTGOING/HAS_TASK'),
    merge('Task/all/INCOMING/HAS_TASK')
)}
```

The arguments are:

1. A Cypher query that returns two collections: `nodes` (all nodes in the subgraph) and `rels` (all relationships). Unlike `prefetch()`, which expects a path pattern, this gives you full control over the query structure.
2. A list of outgoing relationship keys that should be marked as prefetched on the source nodes.
3. A list of incoming relationship keys that should be marked as prefetched on the target nodes.
4. An optional ID parameter that is passed to the Cypher query as `$id`. This allows you to scope the prefetch to a specific starting node.

The key format for relationship keys follows the pattern `Type/all/DIRECTION/RELATIONSHIP_TYPE`, for example `Customer/all/OUTGOING/HAS_TASK`.

The difference between the two functions is that `prefetch()` works with path patterns and treats all relationship keys symmetrically (both outgoing and incoming get the same keys), while `prefetch2()` lets you write arbitrary Cypher queries and assign different keys to outgoing and incoming sides. Use `prefetch2()` when you need fine-grained control over what gets cached, or when your subgraph structure does not fit a simple path pattern.

### Query Optimization

When a transaction closes, Structr analyzes the collected prefetching patterns and attempts to combine them to reduce the number of queries on future transactions.

**Combining by type and direction.** If multiple patterns share the same source node type and direction but differ in relationship type, they are merged into a single pattern using Neo4j's pipe syntax. For example, two separate patterns like `(n:Task)-[r:HAS_SUBTASK]->` and `(n:Task)-[r:HAS_ASSIGNEE]->` would be merged into `(n:Task)-[r:HAS_SUBTASK|HAS_ASSIGNEE]->`.

**Combining by inheritance.** If two patterns differ only in their source node type and those types share a common base type in the schema, the patterns are merged using the common ancestor. This further reduces the number of prefetch queries needed.

## Caching Mechanism

When a prefetch query executes, it runs a `MATCH p = (pattern) RETURN p` query against Neo4j that returns complete paths. The driver iterates over each path segment and populates two layers of cache:

The transaction-level cache stores `NodeWrapper` and `RelationshipWrapper` objects keyed by their Neo4j internal IDs. Any subsequent access to these entities within the same transaction returns the cached wrapper without hitting the database.

Each `NodeWrapper` maintains its own relationship cache organized as a tree structure. Prefetched relationships are inserted into this cache, and the node records which relationship keys have been prefetched. When application code later calls `getRelationships()` on a node, the system checks this cache first. If data is present, it is returned directly. If a key is marked as prefetched but no relationships exist for it, an empty result is returned immediately. This is an important optimization, because it avoids a database round-trip to confirm that no relationships exist.

Any write operation (creating or deleting a node or relationship) within the transaction invalidates the prefetch caches, forcing subsequent reads to go to the database again. This ensures data consistency within the transaction.

## Configuration

Prefetching behavior is controlled by three settings in the "Prefetching" section of the database configuration.

| Setting | Key | Default | Description |
|---------|-----|---------|-------------|
| Prefetching Threshold | `database.prefetching.threshold` | 100 | The number of times an identical query pattern must execute within a single transaction before automatic prefetching is activated for that pattern. |
| Max Duration | `database.prefetching.maxduration` | 1000 | Maximum time in milliseconds that a prefetch query may take. Patterns that exceed this duration are blacklisted and will not be prefetched again. |
| Max Count | `database.prefetching.maxcount` | 50,000 | Maximum number of results a prefetch pattern may return. Patterns exceeding this count are blacklisted to avoid loading excessively large subgraphs into memory. |

These settings can be adjusted in the Configuration Interface or directly in `structr.conf`.

## Debugging

To monitor prefetching activity, enable Cypher debug logging by setting `log.cypher.debug` to `true` in your configuration. When enabled, the system logs:

- When a pattern is activated for prefetching, including the pattern and the threshold that triggered it
- When a pattern is blacklisted because it returns too many results or takes too long
- The number of entities prefetched and the time taken for each prefetch operation
- A query count histogram at the end of each transaction, showing which patterns ran and how often

This information helps you understand whether prefetching is working effectively for your application's access patterns and whether any of the thresholds need adjustment.

## When Prefetching Helps

Prefetching is most effective in scenarios where your application iterates over a collection of nodes and accesses the same type of relationship on each one. Typical examples include rendering a list view where each item displays related data, processing a batch of records that each reference linked entities, or traversing a tree structure where every node has children of the same type.

If your application's access patterns are highly varied -- accessing different relationship types on different node types in an unpredictable order -- the automatic detection may not activate because no single pattern reaches the threshold. In these cases, manual prefetching with the `prefetch()` function gives you explicit control.

## Related Topics

- Configuration - General configuration settings
- Best Practices - Application development guidelines
- Logging & Debugging - Monitoring and troubleshooting
