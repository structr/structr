/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.cloud;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.Listener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class ConnectionListener extends Listener implements CloudTransmission {

	// the logger
	private static final Logger logger = Logger.getLogger(ConnectionListener.class.getName());

	// containers
	private final List<PullNodeRequestContainer> pullRequests = new LinkedList<PullNodeRequestContainer>();
	private final Map<Long, FileNodeDataContainer> fileMap = new HashMap<Long, FileNodeDataContainer>();
	private final Map<Long, Long> idMap = new HashMap<Long, Long>();

	// the root node
	// FIXME: superuser security context
	final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
	private AbstractNode rootNode = (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(0L);

	// private fields
	private boolean transactionFinished = false;
	private boolean linkNode = false;
	private String remoteHost = null;
	private int remoteTcpPort = 0;
	private int remoteUdpPort = 0;
	private int estimatedSize = 0;

	// authentication
	private User targetUser = null;
	
	// commands
	private final Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);
	private final Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
	private final Command findNode = Services.command(securityContext, FindNodeCommand.class);

	public ConnectionListener(Connection connection) {

		remoteHost = connection.getRemoteAddressTCP().getAddress().getHostAddress();
		remoteTcpPort = connection.getRemoteAddressTCP().getPort();
		remoteUdpPort = connection.getRemoteAddressUDP().getPort();
	}

	/**
	 * Returns the root node of this connection.
	 *
	 * @return the root node
	 */
	public AbstractNode getRootNode() {
		return (rootNode);
	}

	@Override
	public void received(Connection connection, Object object) {

		logger.log(Level.FINEST, "Received {0} ({1})", new Object[] { object, object.getClass().getName() } );

		// close connection on first keepalive package when transaction is done
		if(transactionFinished && object instanceof KeepAlive) {

			logger.log(Level.FINE, "Received first KeepAlive after transaction finished, closing connection.");
			connection.close();

		} else if(object instanceof Integer) {
			// Control signal received
			Integer controlSignal = (Integer)object;

			if(CloudService.BEGIN_TRANSACTION.equals(controlSignal)) {
				linkNode = true;

			} else if(CloudService.END_TRANSACTION.equals(controlSignal)) {
				logger.log(Level.INFO, "Received END_TRANSACTION signal, waiting for KeepAlive to close connection.");

				// idMap must be cleared AFTER the last nodes are created
				idMap.clear();

				transactionFinished = true;

			} else if(CloudService.CLOSE_TRANSACTION.equals(controlSignal)) {
				logger.log(Level.INFO, "Received CLOSE_TRANSACTION signal, closing connection..");

				// close the connection
				connection.close();

				// handle pull requests
				handlePullRequests();
			}

			connection.sendTCP(CloudService.ACK_DATA);

		} else if(object instanceof AuthenticationContainer) {
			
			AuthenticationContainer auth = (AuthenticationContainer)object;
			
			// try to find target user
			targetUser = (User)Services.command(securityContext, FindUserCommand.class).execute(auth.getUserName());
			if(targetUser == null) {
				
				logger.log(Level.WARNING, "User not found, disconnecting");
			
				connection.sendTCP("Authentication failed");
				connection.close();

			} else {

				connection.sendTCP(auth);

				EncryptionContext.setPassword(connection.getID(), targetUser.getEncryptedPassword());
			}
			
		} else if(object instanceof PullNodeRequestContainer) {

			pullRequests.add((PullNodeRequestContainer)object);

		} else if(object instanceof PushNodeRequestContainer) {

			PushNodeRequestContainer request = (PushNodeRequestContainer)object;

			// set desired root node for push request
			rootNode = (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(request.getTargetNodeId());

			connection.sendTCP(CloudService.ACK_DATA);

		} else if(object instanceof FileNodeDataContainer) {

			final FileNodeDataContainer container = (FileNodeDataContainer)object;
			fileMap.put(container.sourceNodeId, container);

			// FIXME
			estimatedSize += container.getFileSize();

			connection.sendTCP(CloudService.ACK_DATA);

		} else if(object instanceof FileNodeEndChunk) {
			final FileNodeEndChunk endChunk = (FileNodeEndChunk)object;
			final FileNodeDataContainer container = fileMap.get(endChunk.getContainerId());

			if(container == null) {
				logger.log(Level.WARNING, "Received file end chunk for ID {0} without file, this should not happen!", endChunk.getContainerId());

			} else {
				container.flushAndCloseTemporaryFile();

				// commit database node
				Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
				transactionCommand.execute(new StructrTransaction() {

					@Override
					public Object execute() throws Throwable {
						storeNode(container, linkNode);

						return null;
					}
				});

			}

			connection.sendTCP(CloudService.ACK_DATA);

		} else if(object instanceof FileNodeChunk) {
			final FileNodeChunk chunk = (FileNodeChunk)object;
			FileNodeDataContainer container = fileMap.get(chunk.getContainerId());

			if(container == null) {
				logger.log(Level.WARNING, "Received file chunk for ID {0} without file, this should not happen!", chunk.getContainerId());
			} else {
				container.addChunk(chunk);
			}

			// confirm reception of chunk with sequence number
			connection.sendTCP(chunk.getSequenceNumber());

		} else if(object instanceof NodeDataContainer) {
			final NodeDataContainer receivedNodeData = (NodeDataContainer)object;

			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {
					storeNode(receivedNodeData, linkNode);

					return null;
				}
			});

			connection.sendTCP(CloudService.ACK_DATA);

		} else if(object instanceof RelationshipDataContainer) {

			final RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer)object;

			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					storeRelationship(receivedRelationshipData);

					return null;
				}
			});

			connection.sendTCP(CloudService.ACK_DATA);
		}
	}

	private AbstractNode storeNode(final DataContainer receivedData, final boolean linkToRootNode) {
		NodeDataContainer receivedNodeData = (NodeDataContainer)receivedData;
		AbstractNode newNode = (AbstractNode)nodeFactory.execute(receivedNodeData);

		// Connect first node with root node
		if(linkToRootNode) {
			createRel.execute(rootNode, newNode, RelType.HAS_CHILD);

			// Reset link node flag, has to be explictly set to true!
			linkNode = false;
		}

		idMap.put(receivedNodeData.getSourceNodeId(), newNode.getId());

		return (newNode);
	}

	private StructrRelationship storeRelationship(final DataContainer receivedData) {
		RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer)receivedData;
		StructrRelationship newRelationship = null;

		long sourceStartNodeId = receivedRelationshipData.getSourceStartNodeId();
		long sourceEndNodeId = receivedRelationshipData.getSourceEndNodeId();

		Long targetStartNodeIdValue = idMap.get(sourceStartNodeId);
		Long targetEndNodeIdValue = idMap.get(sourceEndNodeId);

		if(targetStartNodeIdValue != null && targetEndNodeIdValue != null) {
			long targetStartNodeId = targetStartNodeIdValue.longValue();
			long targetEndNodeId = targetEndNodeIdValue.longValue();

			// Get new start and end node
			AbstractNode targetStartNode = (AbstractNode)findNode.execute(targetStartNodeId);
			AbstractNode targetEndNode = (AbstractNode)findNode.execute(targetEndNodeId);
			String name = receivedRelationshipData.getName();

			if(targetStartNode != null && targetEndNode != null && StringUtils.isNotEmpty(name)) {
				newRelationship = (StructrRelationship)createRel.execute(targetStartNode, targetEndNode, name);

				// add properties to newly created relationship
				for(Entry<String, Object> entry : receivedRelationshipData.getProperties().entrySet()) {
					newRelationship.setProperty(entry.getKey(), entry.getValue());
				}
			}
		} else {
			logger.log(Level.WARNING, "Could not store relationship {0} -> {1}", new Object[]{sourceStartNodeId, sourceEndNodeId});
		}

		return newRelationship;
	}

	private void handlePullRequests()
	{
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(2000);

					synchronized(pullRequests)
					{
						for(Iterator<PullNodeRequestContainer> it = pullRequests.iterator(); it.hasNext();)
						{
							PullNodeRequestContainer request = it.next();

							// swap source and target nodes since we're dealing with a request from the remote's point of view!
							AbstractNode sourceNode = (AbstractNode)findNode.execute(request.getSourceNodeId());
							boolean recursive = request.isRecursive();

							Command pushNodes = Services.command(securityContext, PushNodes.class);

							User remoteUser = request.getRemoteUser();
							long remoteTargetNodeId = request.getTargetNodeId();
							String remoteHostValue = request.getRemoteHost();
							Integer remoteTcpPort = request.getRemoteTcpPort();
							Integer remoteUdpPort = request.getRemoteUdpPort();

							pushNodes.execute(remoteUser, sourceNode, remoteTargetNodeId, remoteHostValue, remoteTcpPort, remoteUdpPort, recursive);

							it.remove();
						}
					}

				} catch(Throwable t)
				{
					logger.log(Level.WARNING, "Error while handling pull requests: {0}", t);
				}
			}
		};

		new Thread(r, "PullRequestThread").start();
	}

	// ----- interface CloudTransmission -----
	@Override
	public TransmissionType getTransmissionType() {
		return (TransmissionType.Incoming);
	}

	@Override
	public int getTransmittedObjectCount() {
		return (idMap.size());
	}

	@Override
	public int getEstimatedSize() {
		return (estimatedSize);
	}

	@Override
	public String getRemoteHost() {
		return (remoteHost);
	}

	@Override
	public int getRemoteTcpPort() {
		return (remoteTcpPort);
	}

	@Override
	public int getRemoteUdpPort() {
		return (remoteUdpPort);
	}
}
