# Prefetching

Structr's Neo4j Bolt driver includes an automatic prefetching system that optimizes graph traversals by reducing the number of individual database queries. When your application accesses relationships on many nodes of the same type, prefetching detects this pattern and loads the relevant subgraph in a single bulk query, caching the results in memory for the duration of the transaction.

This is similar in concept to the N+1 query problem in relational databases, where iterating over a list of objects and accessing a related object on each one results in one additional query per item. Prefetching solves this by recognizing repetitive query patterns at runtime and replacing them with a single, broader query.

## How It Works

Prefetching operates at three levels: automatic detection and learning, manual triggering via scripting, and self-optimization through query combining and evaluation.

### Automatic Prefetching

During a transaction, Structr monitors every relationship query that passes through the Bolt driver. Each query is recorded in a histogram that tracks the Cypher statement pattern, the source node type, the relationship type, the direction, and a counter of how often it has been executed.

When the counter for a given pattern exceeds a configurable threshold (default: 25 executions within a single transaction), the system checks whether activating prefetching for that pattern is safe. It runs a `count()` query to estimate the result size. If the result count is below the maximum allowed (default: 50,000), the pattern is registered for prefetching. If it exceeds the limit, the pattern is blacklisted instead, preventing it from being considered again.

The learned patterns are associated with a "prefetch hint" - an identifier set by higher-level application code that describes the context of the current operation, such as rendering a specific page or processing a REST request. On subsequent transactions with the same hint, all learned patterns are executed immediately at the start, populating the cache before application code begins accessing relationships.

Patterns that take longer than the maximum allowed duration (default: 1,000 ms) in several transactions in a row are deactivated as well, see Evaluation below.

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

A pattern that has been deactivated is never part of a combined pattern, and when a combined pattern is deactivated, the patterns it was made of stay deactivated as well.

### Evaluation

A learned pattern loads the relationships of all nodes of a type, not only those of the nodes the request uses, so its cost grows with the database while its benefit depends on the request. When a transaction with a prefetch closes, Structr compares the number of entities (nodes and relationships) the prefetch loaded with the number of lookups it answered. Each node counts once per relationship key, because a repeated lookup of the same node in the same transaction is answered by the relationship cache of the node, with or without the prefetch. If the prefetch loaded more entities per answered lookup than `database.prefetching.costratio` allows, or took longer than `database.prefetching.maxduration`, the evaluation fails.

The evaluation also compares the time the prefetch took with the time the answered lookups would have taken. Structr measures how long a relationship lookup query takes and keeps a moving average, which follows the latency and the load of your database. If the prefetch took longer than the answered lookups times that average, the evaluation fails. This makes the decision depend on where your database runs: with a database on another host, every lookup costs a network round trip, and a prefetch pays off with far fewer answered lookups than with a local database. The cost ratio still applies on top of that, as a limit for the memory a prefetch takes. You can turn the comparison off with `database.prefetching.compareduration`, for example if you need the same decisions on machines with different performance.

A hint names a kind of request, but the requests of one kind differ. A list can be asked for a page after the last one, a detail request can address an object without relationships, and such a request uses little or nothing of what a pattern loads. That is why a single failed evaluation does not deactivate a pattern. Structr deactivates it for the hint when it fails in `database.prefetching.maxfailures` transactions in a row (default: 3), and every successful evaluation starts the count again. A pattern that pays off for most requests of a kind therefore stays active, while one that does not pay off for any of them is deactivated after a few requests.

Deactivated patterns stay deactivated until Structr restarts or the caches are flushed with the `flushCaches` maintenance command. This also means that a pattern that was deactivated when a database was small is not activated again when the database grows.

## Caching Mechanism

When a learned pattern executes, Structr runs one query against Neo4j that returns one row per node at one end of the pattern, with its relationships and the nodes at the other end collected. That way a node with many relationships, like a project with its tasks, is transferred once instead of once per relationship. Structr groups by the end with fewer distinct nodes, which it counts each time the pattern runs. The driver iterates over the rows and populates two layers of cache:

The transaction-level cache stores `NodeWrapper` and `RelationshipWrapper` objects keyed by their Neo4j internal IDs. Any subsequent access to these entities within the same transaction returns the cached wrapper without hitting the database.

Each `NodeWrapper` maintains its own relationship cache organized as a tree structure. Prefetched relationships are inserted into this cache, and the node records which relationship keys have been prefetched. When application code later calls `getRelationships()` on a node, the system checks this cache first. If the key is marked as prefetched, the cached relationships are returned directly, or an empty result if there are none. This is an important optimization, because it avoids a database round-trip to confirm that no relationships exist. A key that is not marked is always looked up in the database, even if the cache holds some of its relationships.

A learned pattern like `(n:Project)-[r:HAS]->(m)` marks the key only for the nodes `n` it starts from. The nodes at the other end receive no relationships from it, because the pattern only loads their relationships to `Project` nodes. If your schema uses the relationship type `HAS` for `Milestone-[:HAS]->Task` as well, a task's incoming `HAS` relationships are incomplete in the prefetch, so they are looked up in the database instead.

Any write operation (creating or deleting a node or relationship) within the transaction invalidates the prefetch caches, forcing subsequent reads to go to the database again. This ensures data consistency within the transaction.

## Configuration

Prefetching behavior is controlled by seven settings in the "Prefetching" section of the database configuration.

| Setting | Key | Default | Description |
|---------|-----|---------|-------------|
| Enabled | `database.prefetching.enabled` | true | Turns automatic prefetching on or off, that is, the learning and the execution of patterns. The `prefetch()` and `prefetch2()` functions are not affected. Without automatic prefetching, every relationship lookup that is not covered by a manual prefetch queries the database, which gives the same results. |
| Prefetching Threshold | `database.prefetching.threshold` | 25 | The number of times an identical query pattern must execute within a single transaction before automatic prefetching is activated for that pattern. |
| Max Duration | `database.prefetching.maxduration` | 1000 | Maximum time in milliseconds that a prefetch query may take. A pattern that takes longer fails its evaluation. |
| Max Count | `database.prefetching.maxcount` | 50,000 | Maximum number of results a prefetch pattern may return. Patterns exceeding this count are blacklisted to avoid loading excessively large subgraphs into memory. |
| Cost Ratio | `database.prefetching.costratio` | 100 | How many entities (nodes and relationships) a prefetch query may load per lookup that it saves in the transaction. A prefetch that loads more than that fails its evaluation, because loading the graph costs more than the lookups it replaces. |
| Compare Duration | `database.prefetching.compareduration` | true | Compares the time a prefetch query takes with the time of the lookups it saves in the transaction. A prefetch that takes longer fails its evaluation. |
| Max Failures | `database.prefetching.maxfailures` | 3 | In how many transactions in a row a pattern may fail its evaluation (cost ratio, max duration or the comparison of durations) before it is deactivated for the hint it was learned for. With 1, the first failed evaluation deactivates a pattern. |

These settings can be adjusted in the Configuration Interface or directly in `structr.conf`.

## Measuring the Effect

Whether automatic prefetching pays off depends on your data and your requests, so Structr counts what it costs and what it saves. The `prefetchStatistics` maintenance command returns these figures, grouped by prefetch hint.

```
POST /structr/rest/maintenance/prefetchStatistics
{ "reset": false }
```

For each hint, `withPrefetching` and `withoutPrefetching` show the number of transactions and their average duration, number of queries and number of relationship lookup queries. A transaction is counted under the state `database.prefetching.enabled` had when it started. To find out what prefetching saves for each kind of request in your application, collect statistics for a while, turn the setting off for a comparable period, and compare the two columns.

`patterns` lists the patterns learned for each hint. You see how often a pattern ran, how many entities it loaded and how long that took on average, how many lookups it answered per execution, how often its evaluation failed, and its status. If a pattern was deactivated, the status says why. `averageSavedMillis` estimates what the answered lookups would have taken, from `averageLookupMillis`, the average time of a relationship lookup query over all transactions. Compared with `averageMillis`, the time the prefetch takes, it shows whether a pattern saves time with the latency of your database. The evaluation makes the same comparison, but with the moving average of the lookup time, so it can come to a different result when the latency or the load of your database changed since the statistics started. Manual prefetching with `prefetch()` and `prefetch2()` does not appear in the statistics.

The statistics are kept in memory, they start over when Structr restarts. Pass `"reset": true` to start over after reading them.

## Debugging

To monitor prefetching activity, enable Cypher debug logging by setting `log.cypher.debug` to `true` in your configuration. When enabled, the system logs:

- When a pattern is activated for prefetching, including the pattern and the threshold that triggered it
- When a pattern fails its evaluation because it takes too long, takes longer than the lookups it saves, or loads more entities per saved lookup than the cost ratio allows
- When a pattern is deactivated because it returns too many results or failed its evaluation too often
- The number of entities prefetched and the time taken for each prefetch operation
- A query count histogram at the end of each transaction, showing which patterns ran and how often

This information helps you understand whether prefetching is working effectively for your application's access patterns and whether any of the thresholds need adjustment.

## When Prefetching Helps

Prefetching is most effective in scenarios where your application iterates over a collection of nodes and accesses the same type of relationship on each one. Typical examples include rendering a list view where each item displays related data, processing a batch of records that each reference linked entities, or traversing a tree structure where every node has children of the same type.

If your application's access patterns are highly varied -- accessing different relationship types on different node types in an unpredictable order -- the automatic detection may not activate because no single pattern reaches the threshold. In these cases, manual prefetching with the `prefetch()` function gives you explicit control.

## Prefetching and Database Latency

Without prefetching, every relationship lookup is a query, and every query costs at least one network round trip between Structr and the database. With Neo4j on the same host, a round trip takes a fraction of a millisecond, and a request with a few hundred lookups spends little time waiting for the database. With the database on another host or in another data center, every round trip adds the latency of the network, typically between half a millisecond and several milliseconds. A request that reads a list with its related objects can make thousands of lookups, so the latency of the database quickly dominates its response time.

Prefetching replaces these lookups with one query per learned pattern, which is why it saves the most where the latency is high. In measurements with a sample application, a nested list of projects with their tasks, comments and assignees took about 600 ms without prefetching and 180 ms with it on a local database. With 3 ms of added round-trip time, the same request took about 20 seconds without prefetching and 340 ms with it.

The comparison of durations described under Evaluation adapts the decision to the latency of your database. With a local database, a pattern has to answer many lookups before it is faster than they would have been, while with a remote database, a pattern that answers a few dozen lookups already pays off. The default threshold of 25 is low enough to learn patterns for requests that address a single object with its related objects, and the evaluation deactivates the patterns that do not pay off.

If your database runs on another host, use the `prefetchStatistics` maintenance command to check whether the learned patterns save time: for a pattern that pays off, `averageSavedMillis` is larger than `averageMillis`. If requests of a kind make fewer identical lookups than the threshold and still spend much of their time on them, you can lower `database.prefetching.threshold` further. Structr then learns patterns for more kinds of requests, and each pattern costs one `count()` query when it is learned.

Independent of prefetching, every transaction costs round trips of its own: one to begin it and two to commit it, because the Neo4j driver resets the connection before it returns it to its pool. With a remote database, many small transactions are therefore noticeably slower than one transaction that does the same work.

The Bolt driver also keeps the number of queries for writes low, because each of them costs a round trip as well. Setting a property to a new value writes the value together with the time and the user of the modification in one query, and setting a value that is equal to the stored one, including an array with the same elements, runs no query at all. Deleting a relationship runs one query: the relationship is removed from the cached relationships of both end nodes, so the remaining relationships of these nodes stay cached and the next lookup in the same transaction needs no query. Replacing the elements of a collection property therefore costs one query per removed element.

Large results are transferred in batches of 10000 records, and each batch costs a round trip. A result of 10000 records takes one round trip instead of the ten that the default batch size of the Neo4j driver would take. The batch size is independent of the `database.result.fetchsize` setting, which controls the page size that Structr uses to page through large results.

## Related Topics

- Configuration - General configuration settings
- Best Practices - Application development guidelines
- Logging & Debugging - Monitoring and troubleshooting
