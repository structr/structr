/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.net;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.net.common.KeyHelper;
import org.structr.net.data.RemoteTransaction;
import org.structr.net.data.time.PseudoTime;
import org.structr.net.peer.Peer;
import org.structr.net.repository.DefaultRepository;
import org.structr.net.repository.ExternalChangeListener;
import org.structr.net.repository.ObjectListener;
import org.structr.net.repository.RepositoryObject;
import org.structr.schema.ConfigurationProvider;

/**
 *
 */
public class PeerToPeerService implements RunnableService, ExternalChangeListener {

	public static final String PEER_UUID_KEY          = "peer.config.uuid";
	public static final String INITIAL_PEER_KEY       = "peer.config.initial.peer";
	public static final String BIND_ADDRESS_KEY       = "peer.config.bind.address";
	public static final String VERBOSE_KEY            = "peer.config.verbose";
	public static final String PUBLIC_KEY_CONFIG_KEY  = "peer.key.public.file";
	public static final String PRIVATE_KEY_CONFIG_KEY = "peer.key.private.file";

	private static final Logger logger = LoggerFactory.getLogger(PeerToPeerService.class.getName());

	private DefaultRepository repository = null;
	private boolean initialized          = false;
	private Peer peer                    = null;

	@Override
	public void startService() {

		if (initialized) {

			peer.start();
			logger.info("Service started");
		}
	}

	@Override
	public void stopService() {

		if (initialized) {

			peer.stop();
		}
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return initialized && peer.isRunning();
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		logger.info("Initializing..");

		try {

			final KeyPair keyPair = getOrCreateKeyPair();
			if (keyPair != null) {

				final String initialPeer = Settings.getOrCreateStringSetting(INITIAL_PEER_KEY, "255.255.255.255").getValue();
				final String bindAddress = Settings.getOrCreateStringSetting(BIND_ADDRESS_KEY, "0.0.0.0").getValue();
				final String peerId      = Settings.getOrCreateStringSetting(PEER_UUID_KEY, StructrApp.getInstance().getInstanceId()).getValue();
				final boolean verbose    = Settings.getBooleanSetting(VERBOSE_KEY).getValue();

				logger.info("{}: {}", new Object[] { BIND_ADDRESS_KEY, bindAddress });
				logger.info("{}: {}", new Object[] { INITIAL_PEER_KEY, initialPeer });
				logger.info("{}: {}", new Object[] { PRIVATE_KEY_CONFIG_KEY, Settings.getOrCreateStringSetting(PRIVATE_KEY_CONFIG_KEY).getValue() });
				logger.info("{}: {}", new Object[] { PUBLIC_KEY_CONFIG_KEY, Settings.getOrCreateStringSetting(PUBLIC_KEY_CONFIG_KEY).getValue() });
				logger.info("{}: {}", new Object[] { PEER_UUID_KEY, peerId });
				logger.info("{}: {}", new Object[] { VERBOSE_KEY, verbose });

				this.repository = new DefaultRepository(peerId);
				this.peer       = new Peer(getOrCreateKeyPair(), repository, bindAddress, initialPeer);

				this.peer.initializeServer();
				this.peer.setVerbose(verbose);

				this.repository.addExternalChangeListener(this);
				this.repository.setPeer(peer);

				initialized = true;
			}

		} catch (Throwable t) {
			logger.warn("Unable to initialize PeerToPeerService", t);
		}

		return initialized;
	}

	@Override
	public void shutdown() {

		if (initialized) {
			peer.stop();
		}
	}

	@Override
	public void initialized() {
	}

	@Override
	public String getName() {
		return PeerToPeerService.class.getSimpleName();
	}

	@Override
	public boolean isVital() {
		return false;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "peer-to-peer";
	}

	// ----- repository connection -----
	public void create(final SharedNodeInterface sharedNode) {

		final String uuid     = sharedNode.getUuid();
		final String type     = sharedNode.getType();
		final String user     = sharedNode.getUserId();
		final PseudoTime time = sharedNode.getCreationPseudoTime();

		logger.info("Shared node with UUID {} CREATED in Structr at {}", new Object[] { sharedNode.getUuid(), time.toString() } );

		repository.create(uuid, type, peer.getUuid(), user, time, sharedNode.getData());
	}

	public void delete(final String uuid) {

		final PseudoTime time = peer.getPseudoTemporalEnvironment().current();

		logger.info("Shared node with UUID {} DELETED in Structr at {}", new Object[] { uuid, time } );

		repository.delete(uuid, time);
	}

	public void update(final SharedNodeInterface sharedNode) {

		final RepositoryObject obj = repository.getObject(sharedNode.getUuid());
		if (obj != null) {

			final PseudoTime time          = sharedNode.getLastModificationPseudoTime();
			final Map<String, Object> data = sharedNode.getData();
			final String type              = sharedNode.getType();
			final String user              = sharedNode.getUserId();

			logger.info("Shared node with UUID {} UPDATED in Structr at {}", new Object[] { sharedNode.getUuid(), time.toString() } );

			repository.update(obj, type, peer.getUuid(), user, time, data);
		}
	}

	public void setProperty(final String uuid, final String key, final Object value) throws FrameworkException {

		logger.info("Attempting to modify shared node with UUID {}: {} = {} in Structr", new Object[] { uuid, key, value });

		final RepositoryObject sharedObject = repository.getObject(uuid);
		if (sharedObject != null) {

			try (final RemoteTransaction tx = peer.beginTx(sharedObject)) {

				tx.setProperty(sharedObject, key, value);
				tx.commit();

			} catch (Exception ex) {

				System.out.println("timeout");

				throw new FrameworkException(500, ex.getMessage());
			}

		} else {

			System.out.println("No such object " + uuid);
		}
	}

	public void addObjectToRepository(final SharedNodeInterface node) {

		final String objectId       = node.getProperty(GraphObject.id);
		final String objectType     = node.getProperty(GraphObject.type);
		final PseudoTime modified   = node.getLastModificationPseudoTime();
		final PseudoTime created    = node.getCreationPseudoTime();
		final String transactionId  = NodeServiceCommand.getNextUuid();
		final RepositoryObject obj  = repository.add(objectId, objectType, peer.getUuid(), node.getUserId(), created);

		//logger.info("External object with UUID {} ADDED at {}, created at {}", new Object[] { node.getUuid(), modified, created });

		for (final Entry<String, Object> entry : node.getData().entrySet()) {
			obj.setProperty(modified, transactionId, entry.getKey(), entry.getValue());
		}

		repository.complete(transactionId);

		// add update listener
		obj.addListener(new ObjectUpdater(objectId));
	}

	public PseudoTime getTime() {
		return peer.getPseudoTemporalEnvironment().next();
	}

	// ----- interface UpdateListener -----
	@Override
	public void onQuery() {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final Map<String, SharedNodeInterface> nodes = new LinkedHashMap<>();
			for (final SharedNodeInterface node : app.nodeQuery(SharedNodeInterface.class).getAsList()) {
				nodes.put(node.getUuid(), node);
			}

			if (nodes.size() != repository.objectCount()) {

				logger.info("Rebuilding list of shared objects: {} vs. {}", new Object[] { nodes.size(), repository.objectCount() } );

				repository.clear();

				for (final SharedNodeInterface node : nodes.values()) {
					addObjectToRepository(node);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	@Override
	public void onCreate(final RepositoryObject object, final Map<String, Object> data) {

		logger.info("New object with UUID {} created in repository", object.getUuid());

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Class type = config.getNodeEntityClass(object.getType());

		if (type != null) {

			final App app = StructrApp.getInstance(getUserContext(object));
			try (final Tx tx = app.tx()) {

				// create new node
				final SharedNodeInterface newNode = app.create(type,
					new NodeAttribute<>(GraphObject.id, object.getUuid()),
					new NodeAttribute<>(SharedNodeInterface.lastModifiedPseudoTime, object.getLastModificationTime().toString()),
					new NodeAttribute<>(SharedNodeInterface.createdPseudoTime, object.getCreationTime().toString())
				);

				// store data
				for (final Entry<String, Object> entry : data.entrySet()) {

					final PropertyKey key = StructrApp.key(type, entry.getKey());
					if (key != null) {
						newNode.setProperty(app, key, entry.getValue());
					}
				}

				// add listener to store future updates
				object.addListener(new ObjectUpdater(object.getUuid()));

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}

		} else {

			System.out.println("Type " + object.getType() + " not found, NOT creating object with ID " + object.getUuid());
		}
	}

	@Override
	public void onDelete(final RepositoryObject object) {

		final App app = StructrApp.getInstance(getUserContext(object));
		try (final Tx tx = app.tx()) {

			final SharedNodeInterface node = app.get(SharedNodeInterface.class, object.getUuid());
			if (node != null) {

				app.delete(node);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	@Override
	public void onModify(final RepositoryObject object, final Map<String, Object> data) {
	}

	@Override
	public void onAdd() {
	}

	@Override
	public void onRemove() {
	}

	// ----- private methods -----
	private SecurityContext getUserContext(final RepositoryObject obj) {

		final String userId = obj.getUserId();
		Principal principal = null;

		if (userId != null) {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				// TODO: change this, userId is currently the name of a user,
				//       WITHOUT ANY AUTHENTICATION.
				principal = app.nodeQuery(Principal.class).and(Principal.name, userId).getFirst();
				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}

		} else {

			System.out.println("Object without userId!");
		}

		// return user
		if (principal != null) {
			return SecurityContext.getInstance(principal, AccessMode.Backend);
		}

		return SecurityContext.getSuperUserInstance();
	}

	private KeyPair getOrCreateKeyPair() {

		final String privateKeyFileName = Settings.getOrCreateStringSetting(PRIVATE_KEY_CONFIG_KEY).getValue();
		final String publicKeyFileName  = Settings.getOrCreateStringSetting(PUBLIC_KEY_CONFIG_KEY).getValue();

		if (privateKeyFileName == null) {
			logger.warn("No private key file name set for PeerToPeerService, aborting. Please set a value for {} in structr.conf.", PRIVATE_KEY_CONFIG_KEY);
			return null;
		}

		if (publicKeyFileName == null) {
			logger.warn("No public key file name set for PeerToPeerService, aborting. Please set  value for {} in structr.conf.", PUBLIC_KEY_CONFIG_KEY);
			return null;
		}

		try {

			final File privateKeyFile = new File(privateKeyFileName);
			final File publicKeyFile  = new File(publicKeyFileName);

			if (!privateKeyFile.exists()) {
				logger.warn("Private key file {} not found, aborting.", privateKeyFileName);
				return null;
			}

			if (!publicKeyFile.exists()) {
				logger.warn("Public key file {} not found, aborting.", publicKeyFileName);
				return null;
			}

			final String privkeyBase64 = getKey(Files.readAllLines(privateKeyFile.toPath()));
			final String pubkeyBase64  = getKey(Files.readAllLines(publicKeyFile.toPath()));

			if (privkeyBase64 == null || privkeyBase64.isEmpty()) {
				logger.warn("No private key found in file {}, aborting", privateKeyFileName);
				return null;
			}

			if (pubkeyBase64 == null || pubkeyBase64.isEmpty()) {
				logger.warn("No public key found in file {}, aborting", publicKeyFileName);
				return null;
			}

			final Decoder decoder = Base64.getDecoder();
			return KeyHelper.fromBytes("RSA", decoder.decode(privkeyBase64), decoder.decode(pubkeyBase64));

		} catch (IOException ex) {
			logger.error("", ex);
		}

		return null;
	}

	private String getKey(final List<String> lines) {

		// return the first non-empty line that does not begin with a comment char
		for (final String line : lines) {

			if (line.contains("#")) {

				final String cleanedLine = line.substring(0, line.indexOf("#"));
				if (!cleanedLine.isEmpty()) {

					return cleanedLine;
				}

			} else if (!line.isEmpty()) {

				return line;
			}
		}

		return null;
	}

	// ----- nested classes -----
	private class ObjectUpdater implements ObjectListener {

		private String uuid = null;

		public ObjectUpdater(final String uuid) {
			this.uuid = uuid;
		}

		@Override
		public void onPropertyChange(final RepositoryObject obj, final String key, final Object value) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final App app                      = StructrApp.getInstance(getUserContext(obj));

			try (final Tx tx = app.tx()) {

				final SharedNodeInterface node = app.get(SharedNodeInterface.class, uuid);
				if (node != null) {

					// store data
					final PropertyKey propertyKey = StructrApp.key(node.getClass(), key);
					if (propertyKey != null) {

						node.setProperty(app, propertyKey, value);
					}

					// update last modified date
					node.setProperty(app, SharedNodeInterface.lastModifiedPseudoTime, obj.getLastModificationTime().toString());

					tx.success();
				}

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}
		}
	}
}
