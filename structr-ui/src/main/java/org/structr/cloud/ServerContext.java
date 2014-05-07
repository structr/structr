package org.structr.cloud;

import java.security.InvalidKeyException;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
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

	public void setEncryptionKey(final String key) throws InvalidKeyException;

	public void ack(final String message, final int sequenceNumber);

	public Principal getUser(final String userName);
	public void impersonateUser(final Principal principal) throws FrameworkException;

	public void beginFile(final FileNodeDataContainer container);
	public void fileChunk(final FileNodeChunk chunk);
	public void finishFile(final FileNodeEndChunk endChunk);

	public NodeInterface storeNode(final DataContainer node);
	public RelationshipInterface storeRelationship(final DataContainer relationship);

	public List<String> listPages() throws FrameworkException;
}
