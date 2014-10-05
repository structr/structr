If you want to use Cypher over REST, you can use the Cypher Query Resource to obtain nodes and relationships from a single query. The Cypher resource in Structr is available at `/cypher` and can be used to execute arbitrary Cypher commands. You should be very aware of the fact that Cypher accesses the database on a lower level than Structr, so Structr has no control over modifications or read operations that happen using Cypher.

<p class="warning">The use of Cypher via REST bypasses Structr's security system and the use of it in your application poses a severe security risk.</p>

Having said that, Structr limits access to the Cypher query resource to the superuser and administrative users (users that have the `isAdmin` flag set).

You can access the Cypher query resource by issuing an HTTP POST request to the `/cypher` resource with the desired query in the request body. The following examples illustrate the use of the resource.

