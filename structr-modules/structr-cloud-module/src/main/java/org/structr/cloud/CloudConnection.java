/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.cloud.message.DataContainer;
import org.structr.cloud.message.FileNodeChunk;
import org.structr.cloud.message.FileNodeDataContainer;
import org.structr.cloud.message.FileNodeEndChunk;
import org.structr.cloud.message.Message;
import org.structr.cloud.message.NodeDataContainer;
import org.structr.cloud.message.RelationshipDataContainer;
import org.structr.cloud.message.SyncableInfo;
import org.structr.cloud.sync.Ping;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.TransactionSource;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.schema.ConfigurationProvider;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;

/**
 *
 *
 */
public class CloudConnection<T> extends Thread implements TransactionSource {

	// the logger
	private static final Logger logger = LoggerFactory.getLogger(CloudConnection.class.getName());

	// containers
	private final Map<String, FileNodeDataContainer> fileMap = new LinkedHashMap<>();
	private final Map<String, String> idMap = new LinkedHashMap<>();
	private final Map<String, Object> data = new LinkedHashMap<>();

	// private fields
	private final ConfigurationProvider config = Services.getInstance().getConfigurationProvider();
	private App app                            = null;
	private CloudListener listener             = null;
	private long transmissionAbortTime         = 0L;
	private boolean authenticated              = false;
	private String errorMessage                = null;
	private String remoteAddress               = null;
	private int errorCode                      = 0;
	private String password                    = null;
	private Cipher encrypter                   = null;
	private Cipher decrypter                   = null;
	private Receiver receiver                  = null;
	private Sender sender                      = null;
	private Socket socket                      = null;
 	private T payload                          = null;
 	private Tx tx                              = null;
	private int count                          = 0;
	private int total                          = 0;

	public CloudConnection(final SecurityContext securityContext, final Socket socket, final CloudListener listener) {

		super("CloudConnection(" + socket.getRemoteSocketAddress() + ")");

		this.app           = StructrApp.getInstance(securityContext);
		this.remoteAddress = socket.getInetAddress().getHostAddress();
		this.listener      = listener;
		this.socket        = socket;

		this.setDaemon(true);

		logger.info("New connection from {}", socket.getRemoteSocketAddress());
	}

	@Override
	public void start() {

		// setup read and write threads for the connection
		if (socket.isConnected() && !socket.isClosed()) {

			try {

				decrypter = Cipher.getInstance(CloudService.STREAM_CIPHER);
				encrypter = Cipher.getInstance(CloudService.STREAM_CIPHER);

				// this key is only used for the first two packets
				// of a transmission, it is replaced by the users
				// password hash afterwards.
				setEncryptionKey("StructrInitialEncryptionKey", 128);

				sender = new Sender(this, new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new CipherOutputStream(socket.getOutputStream(), encrypter), 32768, true))));
				receiver = new Receiver(this, new DataInputStream(new BufferedInputStream(new GZIPInputStream(new CipherInputStream(socket.getInputStream(), decrypter), 32768))));

				receiver.start();
				sender.start();

				// start actual thread
				super.start();

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}
	}

	@Override
	public void run() {

		while (isConnected()) {

			try {

				final Message request = receiver.receive();
				if (request != null) {

					logDebug("RECEIVED ", request);

					// refresh transmission timeout
					refreshTransmissionTimeout();

					if (request.wasSentFromHere()) {

						request.onResponse(this);

					} else {

						request.onRequest(this);
					}
				}

				if (count >= 100) {

					final String message = "Committing batch..";

					sender.send(new Ping(message));

					if (listener != null) {
						listener.transmissionProgress(message);
					}

					// intermediate commit
					this.commitTransaction();
					this.endTransaction();
					this.beginTransaction();

					count = 0;

				}

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

		shutdown();

		logger.info("Transmission finished");

	}

	public void send(final Message message) throws IOException, FrameworkException {

		logDebug("SEND", message);
		sender.send(message);
	}

	/**
	 * This method is private to prevent calling it from a different thread.
	 */
	private void shutdown() {

		close();
		endTransaction();
	}

	public void close() {

		try {

			socket.close();

		} catch (Throwable t) {
			logger.warn("", t);
		}
	}

	public void waitForAuthentication() throws FrameworkException {

		final long abortTime = System.currentTimeMillis() + CloudService.AUTH_TIMEOUT;

		while (!authenticated) {

			if (errorMessage != null) {
				throw new FrameworkException(errorCode, errorMessage);
			}

			if (System.currentTimeMillis() > abortTime) {

				throw new FrameworkException(401, "Authentication failed.");
			}

			try {

				Thread.sleep(10);

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}
	}

	public void refreshTransmissionTimeout() {
		transmissionAbortTime = System.currentTimeMillis() + CloudService.DEFAULT_TIMEOUT;
	}

	public void waitForTransmission() throws FrameworkException {

		transmissionAbortTime = System.currentTimeMillis() + CloudService.DEFAULT_TIMEOUT;

		while (isConnected()) {

			if (errorMessage != null) {
				throw new FrameworkException(errorCode, errorMessage);
			}

			if (System.currentTimeMillis() > transmissionAbortTime) {

				throw new FrameworkException(504, "Timeout while waiting for response.");
			}

			try {

				Thread.sleep(10);

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}
	}

	public void waitForClose(int timeout) throws FrameworkException {

		final long abortTime = System.currentTimeMillis() + CloudService.DEFAULT_TIMEOUT;

		while (isConnected() && System.currentTimeMillis() < abortTime) {

			try {

				Thread.sleep(10);

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

	}

	public void setEncryptionKey(final String key, final int keyLength) throws InvalidKeyException {

		try {

			SecretKeySpec skeySpec = new SecretKeySpec(CloudService.trimToSize(DigestUtils.sha256(key), keyLength), CloudService.STREAM_CIPHER);

			decrypter.init(Cipher.DECRYPT_MODE, skeySpec);
			encrypter.init(Cipher.ENCRYPT_MODE, skeySpec);

		} catch (Throwable t) {
			logger.warn("", t);
		}
	}

	public boolean isConnected() {
		return socket.isConnected() && !socket.isClosed();
	}

	public void setAuthenticated() {
		authenticated = true;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public App getApplicationContext() {
		return app;
	}

	public NodeInterface storeNode(final DataContainer receivedData) throws FrameworkException {

		final SecurityContext securityContext    = SecurityContext.getSuperUserInstance();
		final NodeDataContainer receivedNodeData = (NodeDataContainer) receivedData;
		final String typeName                    = receivedNodeData.getType();
		final Class nodeType                     = config.getNodeEntityClass(typeName);

		if (nodeType == null) {

			logger.error("Unknown entity type {}", typeName);
			return null;
		}

		// skip builtin schema node types
		if(Boolean.TRUE.equals(receivedNodeData.getProperties().get(SchemaNode.isBuiltinType.dbName()))) {
			return null;
		}


		final String uuid              = receivedNodeData.getSourceNodeId();
		GraphObject newOrExistingNode  = app.get(nodeType, uuid);


		if (newOrExistingNode != null) {

			// merge properties
			newOrExistingNode.setProperties(securityContext, PropertyMap.databaseTypeToJavaType(securityContext, nodeType, receivedNodeData.getProperties()));

		} else {

			final PropertyMap properties         = PropertyMap.databaseTypeToJavaType(securityContext, nodeType, receivedNodeData.getProperties());
			final List<DOMNode> existingChildren = new LinkedList<>();

			// special handling for ShadowDocument (all others must be deleted)
			if (ShadowDocument.class.getSimpleName().equals(typeName)) {

				// delete shadow document
				for (ShadowDocument existingDoc : app.nodeQuery(ShadowDocument.class).includeDeletedAndHidden().getAsList()) {

					existingChildren.addAll(existingDoc.getProperty(Page.elements));
					app.delete(existingDoc);
				}

				// add existing children to new shadow document
				properties.put(Page.elements, existingChildren);
			}

			// create node
			newOrExistingNode = app.create(nodeType, properties);
		}

		idMap.put(receivedNodeData.getSourceNodeId(), newOrExistingNode.getUuid());

		count++;
		total++;

		return (NodeInterface)newOrExistingNode;
	}

	public RelationshipInterface storeRelationship(final DataContainer receivedData) throws FrameworkException {

		final RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer) receivedData;
		final String sourceStartNodeId = receivedRelationshipData.getSourceStartNodeId();
		final String sourceEndNodeId = receivedRelationshipData.getSourceEndNodeId();
		final String uuid = receivedRelationshipData.getRelationshipId();

		// if end node ID was not found in the ID map,
		// assume it already exists in the database
		// (i.e. it was created earlier)
		String targetStartNodeId = idMap.get(sourceStartNodeId);
		if (targetStartNodeId == null) {
			targetStartNodeId = sourceStartNodeId;
		}

		// if end node ID was not found in the ID map,
		// assume it already exists in the database
		// (i.e. it was created earlier)
		String targetEndNodeId = idMap.get(sourceEndNodeId);
		if (targetEndNodeId == null) {
			targetEndNodeId = sourceEndNodeId;
		}

		if (targetStartNodeId != null && targetEndNodeId != null) {

			// Get new start and end node
			final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
			final NodeInterface targetStartNode   = app.getNodeById(targetStartNodeId);
			final NodeInterface targetEndNode     = app.getNodeById(targetEndNodeId);
			final String typeName                 = receivedRelationshipData.getType();
			final Class relType                   = config.getRelationshipEntityClass(typeName);

			if (targetStartNode != null && targetEndNode != null) {

				final RelationshipInterface existingCandidate = app.relationshipQuery().and(GraphObject.id, uuid).includeDeletedAndHidden().getFirst();

				count++;
				total++;

				if (existingCandidate != null) {

					// merge properties?
					existingCandidate.setProperties(securityContext, PropertyMap.databaseTypeToJavaType(securityContext, relType, receivedRelationshipData.getProperties()));

					return existingCandidate;

				} else {

					final PropertyMap properties = PropertyMap.databaseTypeToJavaType(securityContext, relType, receivedRelationshipData.getProperties());
					return app.create(targetStartNode, targetEndNode, relType, properties);
				}

			} else {

				logger.warn("Could not store relationship {} -> {}", new Object[]{ targetStartNode, targetEndNode });

			}

		}

		logger.warn("Could not store relationship {} -> {}", new Object[]{sourceStartNodeId, sourceEndNodeId});

		return null;
	}

	public void delete(final String uuid) throws FrameworkException {

		final GraphObject obj = app.get(uuid);
		if (obj != null) {

			if (obj instanceof NodeInterface) {

				app.delete((NodeInterface)obj);

			} else {

				app.delete((RelationshipInterface)obj);
			}

			count++;
			total++;
		}
	}

	public void deleteRelationship(final String uuid) throws FrameworkException {
		app.delete((RelationshipInterface)app.getRelationshipById(uuid));
	}

	public void beginTransaction() {

		tx = app.tx();
		tx.setSource(this);

		logDebug("######################## OPENING TRANSACTION " + tx + " in thread " + Thread.currentThread(), null);
	}

	public void commitTransaction() {

		if (tx != null) {

			try {

				logDebug("######################## COMMITING TRANSACTION " + tx + " in thread " + Thread.currentThread(), null);

				tx.success();

			} catch (Throwable t) {

				// do not catch specific exception only, we need to be able to shut
				// down the connection gracefully, so we must make sure not to be
				// interrupted here
				logger.warn("", t);
			}
		} else {

			System.out.println("NO TRANSACTION!");
		}
	}

	public void endTransaction() {

		if (tx != null) {

			logDebug("######################## CLOSING TRANSACTION " + tx + " in thread " + Thread.currentThread(), null);

			try {

				tx.close();

			} catch (Throwable t) {

				// do not catch specific exception only, we need to be able to shut
				// down the connection gracefully, so we must make sure not to be
				// interrupted here
				logger.warn("", t);
			}

			tx = null;
		}

		data.clear();
	}

	public Principal getUser(String userName) {

		try {

			return app.nodeQuery(User.class).andName(userName).getFirst();

		} catch (Throwable t) {
			logger.warn("", t);
		}

		return null;
	}

	public void impersonateUser(final Principal principal) throws FrameworkException {
		app = StructrApp.getInstance(SecurityContext.getInstance(principal, AccessMode.Backend));
	}

	public void beginFile(final FileNodeDataContainer container) {

		fileMap.put(container.getSourceNodeId(), container);

		count++;
		total++;
	}

	public void finishFile(final FileNodeEndChunk endChunk) throws FrameworkException {

		final FileNodeDataContainer container = fileMap.get(endChunk.getContainerId());
		if (container == null) {

			logger.warn("Received file end chunk for ID {} without file, this should not happen!", endChunk.getContainerId());

		} else {

			container.flushAndCloseTemporaryFile();

			final NodeInterface newNode = storeNode(container);
			final String filesPath = StructrApp.getConfigurationValue(Services.FILES_PATH);
			final String relativePath = newNode.getProperty(File.relativeFilePath);
			String newPath = null;

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
				logger.warn("", t);
			}

			count++;
			total++;
		}
	}

	public void fileChunk(final FileNodeChunk chunk) {

		final FileNodeDataContainer container = fileMap.get(chunk.getContainerId());

		if (container == null) {

			logger.warn("Received file chunk for ID {} without file, this should not happen!", chunk.getContainerId());

		} else {

			container.addChunk(chunk);

			count++;
			total++;
		}
	}

	public List<SyncableInfo> listSyncables(final Set<Class<? extends GraphObject>> types) throws FrameworkException {

		final List<SyncableInfo> syncables = new LinkedList<>();

		if (types == null || types.isEmpty()) {

			for (final Page page : app.nodeQuery(Page.class).includeDeletedAndHidden().getAsList()) {
				syncables.add(new SyncableInfo(page));
			}

			for (final File file : app.nodeQuery(File.class).getAsList()) {
				syncables.add(new SyncableInfo(file));
			}

			for (final Folder folder : app.nodeQuery(Folder.class).getAsList()) {
				syncables.add(new SyncableInfo(folder));
			}

			for (final SchemaNode schemaNode : app.nodeQuery(SchemaNode.class).getAsList()) {
				syncables.add(new SyncableInfo(schemaNode));
			}

			for (final SchemaRelationshipNode schemaRelationship : app.nodeQuery(SchemaRelationshipNode.class).getAsList()) {
				syncables.add(new SyncableInfo(schemaRelationship));
			}
		}

		for (final Class type : types) {

			if (NodeInterface.class.isAssignableFrom(type)) {

				for (final NodeInterface syncable : (Iterable<NodeInterface>) app.nodeQuery(type).includeDeletedAndHidden().getAsList()) {
					syncables.add(new SyncableInfo(syncable));
				}

			} else if (RelationshipInterface.class.isAssignableFrom(type)) {

				for (final RelationshipInterface syncable : (Iterable<RelationshipInterface>) app.relationshipQuery(type).getAsList()) {
					syncables.add(new SyncableInfo(syncable));
				}
			}
		}

		return syncables;
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

	public void setPayload(final T payload) {
		this.payload = payload;
	}

	public T getPayload() {
		return payload;
	}

	public void setError(final int errorCode, final String errorMessage) {

		this.errorMessage = errorMessage;
		this.errorCode = errorCode;

		close();
	}

	public void logDebug(final String prefix, final Message request) {

		if (CloudService.DEBUG) {
			System.out.println(Thread.currentThread().getId() + ": " + System.currentTimeMillis() + "        " + prefix + " " + (request != null ? request : "") + ", count: " + count);
		}
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isRemote() {
		return true;
	}

	@Override
	public String getOriginAddress() {
		return remoteAddress;
	}

	public CloudListener getListener() {
		return listener;
	}

	public int getCount() {
		return count;
	}

	public int getTotal() {
		return total;
	}
}
