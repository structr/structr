package org.structr.cloud;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.cloud.message.DataContainer;
import org.structr.cloud.message.FileNodeChunk;
import org.structr.cloud.message.FileNodeDataContainer;
import org.structr.cloud.message.FileNodeEndChunk;
import org.structr.cloud.message.NodeDataContainer;
import org.structr.cloud.message.RelationshipDataContainer;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class CloudContext {

	private static final Logger logger = Logger.getLogger(CloudContext.class.getName());

	// containers
	private final Map<String, FileNodeDataContainer> fileMap = new LinkedHashMap<>();
	private final Map<String, String> idMap                  = new LinkedHashMap<>();
	private final Map<String, Object> data                   = new LinkedHashMap<>();

	// private fields
	private App app = StructrApp.getInstance();
	private Tx tx   = null;

	public App getApplicationContext() {
		return app;
	}

	public NodeInterface storeNode(final DataContainer receivedData) {

		try {
			final NodeDataContainer receivedNodeData = (NodeDataContainer)receivedData;
			final PropertyMap properties             = PropertyMap.databaseTypeToJavaType(SecurityContext.getSuperUserInstance(), receivedNodeData.getType(), receivedNodeData.getProperties());
			final String uuid                        = receivedNodeData.getSourceNodeId();
			NodeInterface newOrExistingNode          = null;

			final NodeInterface existingCandidate = app.nodeQuery().and(GraphObject.id, uuid).includeDeletedAndHidden().getFirst();
			if (existingCandidate != null && existingCandidate instanceof NodeInterface) {

				newOrExistingNode = (NodeInterface)existingCandidate;

				// merge properties
				((Syncable)newOrExistingNode).updateFromPropertyMap(properties);

			} else {

				// create
				newOrExistingNode = app.create(receivedNodeData.getType(), properties);
			}

			idMap.put(receivedNodeData.getSourceNodeId(), newOrExistingNode.getUuid());

			return newOrExistingNode;

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}

	public RelationshipInterface storeRelationship(final DataContainer receivedData) {

		try {

			final RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer)receivedData;
			final String sourceStartNodeId                           = receivedRelationshipData.getSourceStartNodeId();
			final String sourceEndNodeId                             = receivedRelationshipData.getSourceEndNodeId();
			final String uuid                                        = receivedRelationshipData.getRelationshipId();
			final String targetStartNodeId                           = idMap.get(sourceStartNodeId);
			final String targetEndNodeId                             = idMap.get(sourceEndNodeId);

			if (targetStartNodeId != null && targetEndNodeId != null) {

				// Get new start and end node
				final NodeInterface targetStartNode   = (NodeInterface)app.get(targetStartNodeId);
				final NodeInterface targetEndNode     = (NodeInterface)app.get(targetEndNodeId);
				final Class relType                   = receivedRelationshipData.getType();
				final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

				if (targetStartNode != null && targetEndNode != null) {

					final RelationshipInterface existingCandidate = app.relationshipQuery().and(GraphObject.id, uuid).includeDeletedAndHidden().getFirst();
					final PropertyMap properties                  = PropertyMap.databaseTypeToJavaType(securityContext, relType, receivedRelationshipData.getProperties());

					if (existingCandidate != null) {

						// merge properties?
						((Syncable)existingCandidate).updateFromPropertyMap(properties);

						return existingCandidate;

					} else {

						return app.create(targetStartNode, targetEndNode, relType, properties);
					}
				}

			}

			logger.log(Level.WARNING, "Could not store relationship {0} -> {1}", new Object[]{sourceStartNodeId, sourceEndNodeId});

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}

//	private void handlePullRequests() {
//
//		Runnable r = new Runnable() {
//
//			@Override
//			public void run() {
//
//				try {
//					Thread.sleep(2000);
//
//					synchronized (pullRequests) {
//
//						final App app = StructrApp.getInstance();
//
//						for (Iterator<PullNodeRequestContainer> it = pullRequests.iterator(); it.hasNext();) {
//							PullNodeRequestContainer request = it.next();
//
//							// swap source and target nodes since we're dealing with a request from the remote's point of view!
//							NodeInterface sourceNode = (NodeInterface)app.get(request.getSourceNodeId());
//							boolean recursive = request.isRecursive();
//
//							PushNodes pushNodes = app.command(PushNodes.class);
//
//							final User remoteUser           = request.getRemoteUser();
//							final String password           = null;
//							final String remoteTargetNodeId = request.getTargetNodeId();
//							final String remoteHostValue    = request.getRemoteHost();
//							final Integer remoteTcpPort     = request.getRemoteTcpPort();
//							final Integer remoteUdpPort     = request.getRemoteUdpPort();
//
//							pushNodes.pushNodes(remoteUser, password, sourceNode, remoteTargetNodeId, remoteHostValue, remoteTcpPort, remoteUdpPort, recursive);
//
//							it.remove();
//						}
//					}
//
//				} catch (Throwable t) {
//					logger.log(Level.WARNING, "Error while handling pull requests: {0}", t);
//				}
//			}
//		};
//
//		new Thread(r, "PullRequestThread").start();
//	}

	public void beginTransaction() {
		tx = app.tx();
	}

	public void commitTransaction() {

		if (tx != null) {

			try {

				tx.success();

			} catch (Throwable t) {

				// do not catch specific exception only, we need to be able to shut
				// down the connection gracefully, so we must make sure not to be
				// interrupted here

				t.printStackTrace();
			}
		}
	}

	public void endTransaction() {

		if (tx != null) {

			try {

				tx.close();

			} catch (Throwable t) {

				// do not catch specific exception only, we need to be able to shut
				// down the connection gracefully, so we must make sure not to be
				// interrupted here

				t.printStackTrace();
			}
		}

		data.clear();
	}

	public void ack(String message, int sequenceNumber) {
	}

	public Principal getUser(String userName) {

		try {

			return app.nodeQuery(User.class).andName(userName).getFirst();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	public void impersonateUser(final Principal principal) throws FrameworkException {
		app = StructrApp.getInstance(SecurityContext.getInstance(principal, AccessMode.Backend));
	}

	public void beginFile(final FileNodeDataContainer container) {
		fileMap.put(container.getSourceNodeId(), container);
	}

	public void finishFile(final FileNodeEndChunk endChunk) {

		final FileNodeDataContainer container = fileMap.get(endChunk.getContainerId());
		if (container == null) {

			logger.log(Level.WARNING, "Received file end chunk for ID {0} without file, this should not happen!", endChunk.getContainerId());

		} else {

			container.flushAndCloseTemporaryFile();

			final NodeInterface newNode = storeNode(container);
			final String filesPath      = StructrApp.getConfigurationValue(Services.FILES_PATH);
			final String relativePath   = newNode.getProperty(File.relativeFilePath);
			String newPath              = null;

			if (filesPath.endsWith("/")) {

				newPath = filesPath + relativePath;

			} else {

				newPath = filesPath + "/" + relativePath;
			}

			try {
				container.persistTemporaryFile(newPath);

			} catch (Throwable t) {

				// do not catch specific exception only, we need to be able to shut
				// down the connection gracefully, so we must make sure not to be
				// interrupted here
				t.printStackTrace();
			}
		}
	}

	public void fileChunk(final FileNodeChunk chunk) {

		final FileNodeDataContainer container = fileMap.get(chunk.getContainerId());

		if (container == null) {

			logger.log(Level.WARNING, "Received file chunk for ID {0} without file, this should not happen!", chunk.getContainerId());

		} else {

			container.addChunk(chunk);
		}
	}

	public List<String> listPages() throws FrameworkException {

		final List<String> pages = new LinkedList<>();

		for (final Page page : app.nodeQuery(Page.class).getAsList()) {

			pages.add(page.getName());
		}

		return pages;
	}

	public void storeValue(final String key, final Object value) {
		data.put(key, value);
	}

	public Object getValue(final String key) {
		return data.get(key);
	}

	public void removeValue(final String key) {
		data.remove(key);
	}
}
