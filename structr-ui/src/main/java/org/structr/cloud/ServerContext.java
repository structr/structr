package org.structr.cloud;

import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

/**
 *
 * @author Christian Morgner
 */
public interface ServerContext {

	public void beginTransaction();
	public void commitTransaction();
	public void endTransaction();

	public void closeConnection();

	public void ack(final String message, final int sequenceNumber);

	public boolean authenticateUser(final String userName);

	public void beginFile(final FileNodeDataContainer container);
	public void fileChunk(final FileNodeChunk chunk);
	public void finishFile(final FileNodeEndChunk endChunk);

	public NodeInterface storeNode(final DataContainer node);
	public RelationshipInterface storeRelationship(final DataContainer relationship);
}
