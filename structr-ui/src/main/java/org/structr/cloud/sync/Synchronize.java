package org.structr.cloud.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.message.Finish;
import org.structr.cloud.message.Message;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;

/**
 * Synchronization message that causes the slave to examine its database
 * contents, reporting everything to the master.
 *
 * @author Christian Morgner
 */
public class Synchronize extends Message {

	@Override
	public void onRequest(final CloudConnection serverConnection) throws IOException, FrameworkException {

		final GraphDatabaseService graphDb   = StructrApp.getInstance().getGraphDatabaseService();
		final GlobalGraphOperations ggo      = GlobalGraphOperations.at(graphDb);
		final String uuidPropertyName        = GraphObject.id.dbName();
		final Set<Long> visitedObjectIDs     = new HashSet<>();

		for (final Node node : ggo.getAllNodes()) {

			if (!visitedObjectIDs.contains(node.getId())) {

				final String hash = contentHashCode(node, visitedObjectIDs);
				final Object uuid = node.getProperty(uuidPropertyName, null);

				if (uuid != null && uuid instanceof String) {

					serverConnection.send(new Diff(uuid.toString(), hash));
				}
			}
		}

		// clear set of visited objects because node and relationship IDs are offsets and can overlap.
		visitedObjectIDs.clear();

		for (final Relationship relationship : ggo.getAllRelationships()) {

			if (!visitedObjectIDs.contains(relationship.getId())) {

				final String hash = contentHashCode(relationship, visitedObjectIDs);
				final Object uuid = relationship.getProperty(uuidPropertyName, null);

				if (uuid != null && uuid instanceof String) {

					serverConnection.send(new Diff(uuid.toString(), hash));
				}
			}
		}

		serverConnection.send(new Finish());
	}

	@Override
	public void onResponse(final CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(final CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(final DataInputStream inputStream) throws IOException {
	}

	@Override
	protected void serializeTo(final DataOutputStream outputStream) throws IOException {
	}
}
