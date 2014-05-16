package org.structr.cloud;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.structr.cloud.message.DataContainer;
import org.structr.cloud.message.FileNodeChunk;
import org.structr.cloud.message.FileNodeDataContainer;
import org.structr.cloud.message.FileNodeEndChunk;
import org.structr.cloud.message.Message;
import org.structr.cloud.message.NodeDataContainer;
import org.structr.cloud.message.RelationshipDataContainer;
import org.structr.cloud.message.SyncableInfo;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class CloudConnection<T> extends Thread {

	// the logger
	private static final Logger logger = Logger.getLogger(CloudConnection.class.getName());

	// containers
	private final Map<String, FileNodeDataContainer> fileMap = new LinkedHashMap<>();
	private final Map<String, String> idMap = new LinkedHashMap<>();
	private final Map<String, Object> data = new LinkedHashMap<>();

	// private fields
	private final Set<String> localMessageIds = new LinkedHashSet<>();
	private final Set<String> remoteMessageIds = new LinkedHashSet<>();
	private App app = StructrApp.getInstance();
	private long transmissionAbortTime = 0L;
	private ExportContext context = null;
	private boolean authenticated = false;
	private String errorMessage = null;
	private int errorCode = 0;
	private String password = null;
	private Cipher encrypter = null;
	private Cipher decrypter = null;
	private Receiver receiver = null;
	private Sender sender = null;
	private Socket socket = null;
	private T payload = null;
	private Tx tx = null;

	public CloudConnection(final Socket socket, final ExportContext context) {

		super("CloudConnection(" + socket.getRemoteSocketAddress() + ")");

		this.socket = socket;
		this.context = context;

		this.setDaemon(true);

		logger.log(Level.INFO, "New connection from {0}", socket.getRemoteSocketAddress());
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

				sender = new Sender(this, new ObjectOutputStream(new GZIPOutputStream(new CipherOutputStream(new BufferedOutputStream(socket.getOutputStream()), encrypter), true)));
				receiver = new Receiver(this, new ObjectInputStream(new GZIPInputStream(new CipherInputStream(new BufferedInputStream(socket.getInputStream()), decrypter))));

				receiver.start();
				sender.start();

				// start actual thread
				super.start();

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	public void run() {

		while (isConnected()) {

			try {

				final Message request = receiver.receive();
				if (request != null) {

					// inform sender that a message has arrived
					sender.messageReceived();

					// refresh transmission timeout
					refreshTransmissionTimeout();

					// mark as "received"
					remoteMessageIds.add(request.getId());

					if (wasSentFromHere(request)) {

						request.onResponse(this, context);

					} else {

						request.onRequest(this, context);
					}

					if (CloudService.DEBUG) {
						System.out.println("        => " + request);
					}
				}

				Thread.yield();

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		shutdown();

		logger.log(Level.INFO, "Transmission finished");

	}

	private boolean wasSentFromHere(final Message message) {
		return localMessageIds.contains(message.getId());
	}

	public void send(final Message message) throws IOException, FrameworkException {

		sender.send(message);

		if (CloudService.DEBUG) {
			System.out.println(message);
		}

		localMessageIds.add(message.getId());
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
			t.printStackTrace();
		}
	}

	public void waitForAuthentication() throws FrameworkException {

		final long abortTime = System.currentTimeMillis() + CloudService.DEFAULT_TIMEOUT;

		while (!authenticated) {

			if (errorMessage != null) {
				throw new FrameworkException(errorCode, errorMessage);
			}

			if (System.currentTimeMillis() > abortTime) {

				throw new FrameworkException(504, "Authentication failed.");
			}

			try {
				Thread.sleep(10);
			} catch (Throwable t) {
			}
		}
	}

	public void refreshTransmissionTimeout() {
		transmissionAbortTime = System.currentTimeMillis() + CloudService.DEFAULT_TIMEOUT;
	}

	public void waitForTransmission() throws FrameworkException {

		transmissionAbortTime = System.currentTimeMillis() + CloudService.DEFAULT_TIMEOUT;

		while (context.getCurrentProgress() < context.getTotalSize()) {

			if (errorMessage != null) {
				throw new FrameworkException(errorCode, errorMessage);
			}

			if (System.currentTimeMillis() > transmissionAbortTime) {

				throw new FrameworkException(504, "Timeout while waiting for response.");
			}

			try {
				Thread.sleep(10);
			} catch (Throwable t) {
			}
		}
	}

	public void waitForClose(int timeout) throws FrameworkException {

		final long abortTime = System.currentTimeMillis() + CloudService.DEFAULT_TIMEOUT;

		while (isConnected() && System.currentTimeMillis() < abortTime) {

			try {

				Thread.sleep(10);

			} catch (Throwable t) {
			}
		}

	}

	public void setEncryptionKey(final String key, final int keyLength) throws InvalidKeyException {

		try {

			SecretKeySpec skeySpec = new SecretKeySpec(CloudService.trimToSize(DigestUtils.sha256(key), keyLength), CloudService.STREAM_CIPHER);

			decrypter.init(Cipher.DECRYPT_MODE, skeySpec);
			encrypter.init(Cipher.ENCRYPT_MODE, skeySpec);

		} catch (Throwable t) {
			t.printStackTrace();
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

		final NodeDataContainer receivedNodeData = (NodeDataContainer) receivedData;
		final PropertyMap properties = PropertyMap.databaseTypeToJavaType(SecurityContext.getSuperUserInstance(), receivedNodeData.getType(), receivedNodeData.getProperties());
		final String uuid = receivedNodeData.getSourceNodeId();
		NodeInterface newOrExistingNode = null;

		final NodeInterface existingCandidate = app.nodeQuery().and(GraphObject.id, uuid).includeDeletedAndHidden().getFirst();
		if (existingCandidate != null && existingCandidate instanceof NodeInterface) {

			newOrExistingNode = (NodeInterface) existingCandidate;

			// merge properties
			((Syncable) newOrExistingNode).updateFromPropertyMap(properties);

		} else {

			// create
			newOrExistingNode = app.create(receivedNodeData.getType(), properties);
		}

		idMap.put(receivedNodeData.getSourceNodeId(), newOrExistingNode.getUuid());

		return newOrExistingNode;
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
			final NodeInterface targetStartNode = (NodeInterface) app.get(targetStartNodeId);
			final NodeInterface targetEndNode = (NodeInterface) app.get(targetEndNodeId);
			final Class relType = receivedRelationshipData.getType();
			final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

			if (targetStartNode != null && targetEndNode != null) {

				final RelationshipInterface existingCandidate = app.relationshipQuery().and(GraphObject.id, uuid).includeDeletedAndHidden().getFirst();
				final PropertyMap properties = PropertyMap.databaseTypeToJavaType(securityContext, relType, receivedRelationshipData.getProperties());

				if (existingCandidate != null) {

					// merge properties?
					((Syncable) existingCandidate).updateFromPropertyMap(properties);

					return existingCandidate;

				} else {

					return app.create(targetStartNode, targetEndNode, relType, properties);
				}
			}

		}

		logger.log(Level.WARNING, "Could not store relationship {0} -> {1}", new Object[]{sourceStartNodeId, sourceEndNodeId});

		return null;
	}

	public void beginTransaction() {
		tx = app.tx();

		if (CloudService.DEBUG) {
			System.out.println("############################### OPENING TRANSACTION " + tx + " in Thread" + Thread.currentThread());
		}
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
		} else {

			System.out.println("NO TRANSACTION!");
		}
	}

	public void endTransaction() {

		if (tx != null) {

			if (CloudService.DEBUG) {
				System.out.println("############################### CLOSING TRANSACTION " + tx + " in Thread" + Thread.currentThread());
			}

			try {

				tx.close();

			} catch (Throwable t) {

				// do not catch specific exception only, we need to be able to shut
				// down the connection gracefully, so we must make sure not to be
				// interrupted here
				t.printStackTrace();
			}

			tx = null;
		}

		data.clear();
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

	public void finishFile(final FileNodeEndChunk endChunk) throws FrameworkException {

		final FileNodeDataContainer container = fileMap.get(endChunk.getContainerId());
		if (container == null) {

			logger.log(Level.WARNING, "Received file end chunk for ID {0} without file, this should not happen!", endChunk.getContainerId());

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

	public List<SyncableInfo> listSyncables(final Set<Class<Syncable>> types) throws FrameworkException {

		final List<SyncableInfo> syncables = new LinkedList<>();

		if (types == null || types.isEmpty()) {

			for (final Page page : app.nodeQuery(Page.class).getAsList()) {
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

			for (final SchemaRelationship schemaRelationship : app.relationshipQuery(SchemaRelationship.class).getAsList()) {
				syncables.add(new SyncableInfo(schemaRelationship));
			}

		}

		for (final Class<Syncable> type : types) {

			Class cls;
			try {
				cls = Class.forName(type.getName());
			} catch (ClassNotFoundException ex) {
				continue;
			}

			if (NodeInterface.class.isAssignableFrom(type)) {

				for (final NodeInterface syncable : (Iterable<NodeInterface>) app.nodeQuery(cls).getAsList()) {
					syncables.add(new SyncableInfo((Syncable) syncable));
				}

			} else if (RelationshipInterface.class.isAssignableFrom(type)) {

				for (final RelationshipInterface syncable : (Iterable<RelationshipInterface>) app.relationshipQuery(cls).getAsList()) {
					syncables.add(new SyncableInfo((Syncable) syncable));
				}

			}

		}

//
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

	public void increaseTotal(final int total) {
		context.increaseTotal(total);
	}

	public void setError(final int errorCode, final String errorMessage) {

		this.errorMessage = errorMessage;
		this.errorCode = errorCode;

		close();
	}
}
