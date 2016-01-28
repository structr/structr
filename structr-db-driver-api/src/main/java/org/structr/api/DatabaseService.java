package org.structr.api;

import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.GraphProperties;
import java.util.Map;
import java.util.Properties;
import org.structr.api.index.IndexManager;

/**
 *
 */
public interface DatabaseService {

	// ----- lifecycle -----
	void initialize(final Properties configuration);
	void shutdown();

	<T> T forName(final Class<T> type, final String name);

	Transaction beginTx();

	Node createNode();

	Node getNodeById(final long id);
	Relationship getRelationshipById(final long id);

	Iterable<Node> getAllNodes();
	Iterable<Relationship> getAllRelationships();

	GraphProperties getGlobalProperties();


	// ----- index -----
	IndexManager<Node> nodeIndexer();
	IndexManager<Relationship> relationshipIndexer();



	NativeResult execute(final String nativeQuery, final Map<String, Object> parameters);
	NativeResult execute(final String nativeQuery);

	void invalidateCache();
}
