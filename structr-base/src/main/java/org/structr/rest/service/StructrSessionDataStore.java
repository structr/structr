/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.service;

import java.util.Collections;
import org.apache.commons.collections.map.LRUMap;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SessionDataNodeTraitDefinition;
import org.structr.rest.auth.AuthHelper;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class StructrSessionDataStore extends AbstractSessionDataStore {

	private static final Logger logger = LoggerFactory.getLogger(StructrSessionDataStore.class.getName());

	private static final Map<String, SessionData> anonymousSessionCache = Collections.synchronizedMap(new LRUMap(100_000));

	@Override
	public boolean doExists(final String id) throws Exception {
		return exists(id);
	}

	@Override
	public synchronized void doStore(final String id, final SessionData data, final long lastSaveTime) throws Exception {

		assertInitialized();

		final SecurityContext ctx = SecurityContext.getSuperUserInstance();
		final App app             = StructrApp.getInstance(ctx);

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("StructrSessionDataStore store");

			final NodeInterface user = AuthHelper.getPrincipalForSessionId(id);

			if (user != null) {

				final Traits sessionTraits = Traits.of(StructrTraits.SESSION_DATA_NODE);
				final NodeInterface node   = getOrCreateSessionDataNode(app, sessionTraits, id);
				if (node != null) {

					final PropertyMap properties = new PropertyMap();

					properties.put(sessionTraits.key(SessionDataNodeTraitDefinition.LAST_ACCESSED_PROPERTY), new Date(data.getLastAccessed()));
					properties.put(sessionTraits.key(SessionDataNodeTraitDefinition.CONTEXT_PATH_PROPERTY),  data.getContextPath());
					properties.put(sessionTraits.key(SessionDataNodeTraitDefinition.VHOST_PROPERTY),         data.getVhost());

					node.setProperties(ctx, properties);
				}

			} else {

				anonymousSessionCache.put(id, data);
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.info("Unable to store session data for session id " + id + ".", ex);
		}
	}

	@Override
	public boolean isPassivating() {
		return true;
	}

	@Override
	public synchronized boolean exists(final String id) throws Exception {

		if (anonymousSessionCache.containsKey(id)) {
			return true;
		}

		assertInitialized();

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("StructrSessionDataStore exists");

			final NodeInterface node = app.nodeQuery(StructrTraits.SESSION_DATA_NODE).key(Traits.of(StructrTraits.SESSION_DATA_NODE).key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY), id).getFirst();

			tx.success();

			return node != null;

		} catch (FrameworkException ex) {

			logger.info("Unable to determine if session data for " + id + " exists.", ex);
		}

		return false;
	}

	@Override
	public synchronized SessionData load(final String id) throws Exception {

		if (anonymousSessionCache.containsKey(id)) {
			return anonymousSessionCache.get(id);
		}

		assertInitialized();

		final App app      = StructrApp.getInstance();
		SessionData result = null;

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("StructrSessionDataStore load");

			final Traits traits      = Traits.of(StructrTraits.SESSION_DATA_NODE);
			final NodeInterface node = app.nodeQuery(StructrTraits.SESSION_DATA_NODE).key(traits.key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY), id).getFirst();
			if (node != null) {

				result = new SessionData(
						id,
						node.getProperty(traits.key(SessionDataNodeTraitDefinition.CONTEXT_PATH_PROPERTY)),
						node.getProperty(traits.key(SessionDataNodeTraitDefinition.VHOST_PROPERTY)),
						node.getCreatedDate().getTime(),
						node.getLastModifiedDate().getTime(),
						node.getLastModifiedDate().getTime(),
						-1
				);
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.info("Unable to load session data for session id " + id + ".", ex);
		}

		return result;
	}

	@Override
	public synchronized boolean delete(final String id) throws Exception {

		if (anonymousSessionCache.containsKey(id)) {
			anonymousSessionCache.remove(id);
			return true;
		}

		assertInitialized();

		final Traits traits = Traits.of(StructrTraits.SESSION_DATA_NODE);
		final App app       = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("StructrSessionDataStore delete");

			// delete nodes
			for (final NodeInterface node : app.nodeQuery(StructrTraits.SESSION_DATA_NODE).key(traits.key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY), id).getAsList()) {

				app.delete(node);
			}

			tx.success();

			return true;

		} catch (FrameworkException ex) {

			logger.info("Unable to load session data for session id " + id + ".", ex);
		}

		return false;
	}

	@Override
	public SessionData doLoad(final String id) throws Exception {
		return load(id);
	}

	@Override
	public synchronized Set<String> doCheckExpired(final Set<String> candidates, final long sessionTimeout) {

		final Date timeoutDate = new Date(System.currentTimeMillis() - sessionTimeout);

		assertInitialized();

		for (Map.Entry<String,SessionData> entry : anonymousSessionCache.entrySet()) {

			SessionData data = entry.getValue();
			if ( (new Date().getTime() - data.getLastAccessed()) > sessionTimeout) {

				candidates.add(entry.getKey());
			}
		}

		final Traits traits = Traits.of(StructrTraits.SESSION_DATA_NODE);
		final App app       = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("StructrSessionDataStore doCheckExpired");

			for (final NodeInterface node : app.nodeQuery(StructrTraits.SESSION_DATA_NODE).range(traits.key(SessionDataNodeTraitDefinition.LAST_ACCESSED_PROPERTY), new Date(0), timeoutDate).getAsList()) {

				candidates.add(node.getProperty(traits.key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY)));
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.info("Unable to determine list of expired session candidates.");
		}

		return candidates;
	}

	@Override
	public synchronized Set<String> doGetExpired(final long sessionTimeout) {
		final Date timeoutDate    = new Date(System.currentTimeMillis() - sessionTimeout);

		assertInitialized();

		Set<String> candidates = new HashSet<>();

		for (Map.Entry<String,SessionData> entry : anonymousSessionCache.entrySet()) {

			SessionData data = entry.getValue();
			if ( (new Date().getTime() - data.getLastAccessed()) > sessionTimeout) {

				candidates.add(entry.getKey());
			}
		}

		final Traits traits = Traits.of(StructrTraits.SESSION_DATA_NODE);
		final App app       = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("StructrSessionDataStore doGetExpired");

			for (final NodeInterface node : app.nodeQuery(StructrTraits.SESSION_DATA_NODE).range(traits.key(SessionDataNodeTraitDefinition.LAST_ACCESSED_PROPERTY), new Date(0), timeoutDate).getAsList()) {

				candidates.add(node.getProperty(traits.key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY)));
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.info("Unable to determine list of expired session candidates.");
		}

		return candidates;
	}

	@Override
	public synchronized void doCleanOrphans(long timeout) {

		for (final String id : doGetExpired(timeout)) {

			try {

				delete(id);
			} catch (Exception ex) {

				logger.warn("Could not delete session data for id[" + id + "]");
			}
		}
	}

	// ----- private methods -----
	private void assertInitialized() {

		final Services services = Services.getInstance();
		if (!services.isShuttingDown() && !services.isShutdownDone()) {

			// wait for service layer to be initialized
			while (!services.isInitialized()) {

				try { Thread.sleep(1000); } catch (Throwable t) {}
			}
		}
	}

	private NodeInterface getOrCreateSessionDataNode(final App app, final Traits traits, final String id) throws FrameworkException {

		NodeInterface node = app.nodeQuery(StructrTraits.SESSION_DATA_NODE).key(traits.key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY), id).getFirst();
		if (node == null) {

			node= app.create(StructrTraits.SESSION_DATA_NODE, new NodeAttribute<>(traits.key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY), id));
		}

		return node;
	}
}