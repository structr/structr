/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.graph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;

/**
 *
 *
 */

public class ModificationQueue {

	private static final Logger logger = LoggerFactory.getLogger(ModificationQueue.class.getName());

	private final boolean auditLogEnabled                                                   = "true".equals(StructrApp.getConfigurationValue(Services.APPLICATION_CHANGELOG_ENABLED, "false"));
	private final ConcurrentSkipListMap<String, GraphObjectModificationState> modifications = new ConcurrentSkipListMap<>();
	private final Collection<ModificationEvent> modificationEvents                          = new ArrayDeque<>(1000);
	private final Map<String, TransactionPostProcess> postProcesses                         = new LinkedHashMap<>();
	private final Set<String> alreadyPropagated                                             = new LinkedHashSet<>();
	private final Set<String> synchronizationKeys                                           = new TreeSet<>();

	/**
	 * Returns a set containing the different entity types of
	 * nodes modified in this queue.
	 *
	 * @return the types
	 */
	public Set<String> getSynchronizationKeys() {
		return synchronizationKeys;
	}

	public boolean doInnerCallbacks(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		long t0                  = System.currentTimeMillis();
		boolean hasModifications = true;

		// collect all modified nodes
		while (hasModifications) {

			hasModifications = false;

			for (GraphObjectModificationState state : modifications.values()) {

				if (state.wasModified()) {

					// do callback according to entry state
					if (!state.doInnerCallback(this, securityContext, errorBuffer)) {
						return false;
					}

					hasModifications = true;
				}
			}
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 1000) {
			logger.info("{} ms", t);
		}

		return true;
	}

	public boolean doValidation(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final boolean doValidation) throws FrameworkException {

		long t0 = System.currentTimeMillis();

		// do validation and indexing
		for (Entry<String, GraphObjectModificationState> entry : modifications.entrySet()) {

			// do callback according to entry state
			if (!entry.getValue().doValidationAndIndexing(this, securityContext, errorBuffer, doValidation)) {
				return false;
			}
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 3000) {
			logger.info("doValidation: {} ms", t);
		}

		return true;
	}

	public boolean doPostProcessing(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		for (final TransactionPostProcess process : postProcesses.values()) {

			if (!process.execute(securityContext, errorBuffer)) {
				return false;
			}
		}

		return true;
	}

	public void doOuterCallbacks(final SecurityContext securityContext) {

		long t0 = System.currentTimeMillis();

		// copy modifications, do after transaction callbacks
		for (GraphObjectModificationState state : modifications.values()) {
			state.doOuterCallback(securityContext);
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 3000) {
			logger.info("doOutCallbacks: {} ms", t);
		}
	}

	public void updateAuditLog() {

		if (auditLogEnabled && !modificationEvents.isEmpty()) {

			for (final ModificationEvent ev: modificationEvents) {

				if (!ev.isDeleted()) {

					try {
						final GraphObject obj = ev.getGraphObject();
						if (obj != null) {

							final String existingLog = obj.getProperty(GraphObject.structrChangeLog);
							final String newLog      = ev.getChangeLog();
							final String newValue    = existingLog != null ? existingLog + newLog : newLog;

							obj.unlockSystemPropertiesOnce();
							obj.setProperty(GraphObject.structrChangeLog, newValue);
						}

					} catch (Throwable t) {
						logger.warn("", t);
					}
				}
			}
		}
	}

	public void clear() {

		// clear collections afterwards
		alreadyPropagated.clear();
		modifications.clear();
		modificationEvents.clear();
	}

	public void create(final Principal user, final NodeInterface node) {

		getState(node).create();

		if (auditLogEnabled) {

			// record deletion of objects in audit log of the creating user, if enabled
			if (user != null) {

				getState(user).updateChangeLog(user, GraphObjectModificationState.Verb.create, node.getUuid());
			}
		}

	}

	public <S extends NodeInterface, T extends NodeInterface> void create(final Principal user, final RelationshipInterface relationship) {

		getState(relationship).create();

		final NodeInterface sourceNode = relationship.getSourceNodeAsSuperUser();
		final NodeInterface targetNode = relationship.getTargetNodeAsSuperUser();

		if (sourceNode != null && targetNode != null) {

			modifyEndNodes(user, sourceNode, targetNode, relationship.getRelType());

			getState(sourceNode).updateChangeLog(user, GraphObjectModificationState.Verb.link, relationship.getType(), targetNode.getUuid());
			getState(targetNode).updateChangeLog(user, GraphObjectModificationState.Verb.link, relationship.getType(), sourceNode.getUuid());
		}
	}

	public void modifyOwner(NodeInterface node) {
		getState(node).modifyOwner();
	}

	public void modifySecurity(NodeInterface node) {
		getState(node).modifySecurity();
	}

	public void modifyLocation(NodeInterface node) {
		getState(node).modifyLocation();
	}

	public void modify(final Principal user, final NodeInterface node, final PropertyKey key, final Object previousValue, final Object newValue) {
		getState(node).modify(user, key, previousValue, newValue);

		if (key != null&& key.requiresSynchronization()) {
			synchronizationKeys.add(key.getSynchronizationKey());
		}
	}

	public void modify(final Principal user, RelationshipInterface relationship, PropertyKey key, Object previousValue, Object newValue) {
		getState(relationship).modify(user, key, previousValue, newValue);

		if (key != null && key.requiresSynchronization()) {
			synchronizationKeys.add(key.getSynchronizationKey());
		}
	}

	public void propagatedModification(NodeInterface node) {

		if (node != null) {

			GraphObjectModificationState state = getState(node, true);
			if (state != null) {

				state.propagatedModification();

				// save hash to avoid repeated propagation
				alreadyPropagated.add(hash(node));
			}
	}
	}

	public void delete(final Principal user, final NodeInterface node) {

		getState(node).delete(false);

		if (auditLogEnabled) {

			// record deletion of objects in audit log of the delting user, if enabled
			final SecurityContext securityContext = node.getSecurityContext();
			if (securityContext != null) {

				final Principal principal = securityContext.getCachedUser();
				if (principal != null) {

					getState(principal).updateChangeLog(user, GraphObjectModificationState.Verb.delete, node.getUuid());
				}
			}
		}
	}

	public void delete(final Principal user, final RelationshipInterface relationship, final boolean passive) {

		getState(relationship).delete(passive);

		final NodeInterface sourceNode = relationship.getSourceNodeAsSuperUser();
		final NodeInterface targetNode = relationship.getTargetNodeAsSuperUser();

		modifyEndNodes(user, sourceNode, targetNode, relationship.getRelType());

		getState(sourceNode).updateChangeLog(user, GraphObjectModificationState.Verb.unlink, relationship.getType(), targetNode.getUuid());
		getState(targetNode).updateChangeLog(user, GraphObjectModificationState.Verb.unlink, relationship.getType(), sourceNode.getUuid());

	}

	public Collection<ModificationEvent> getModificationEvents() {
		return modificationEvents;
	}

	public void postProcess(final String key, final TransactionPostProcess process) {

		if (!postProcesses.containsKey(key)) {

			this.postProcesses.put(key, process);
		}
	}

	public boolean isDeleted(final Node node) {

		final GraphObjectModificationState state = modifications.get("N" + node.getId());
		if (state != null) {

			return state.isDeleted() || state.isPassivelyDeleted();
		}

		return false;
	}

	public boolean isDeleted(final Relationship rel) {

		final GraphObjectModificationState state = modifications.get("R" + rel.getId());
		if (state != null) {

			return state.isDeleted() || state.isPassivelyDeleted();
		}

		return false;
	}

	public void registerNodeCallback(final NodeInterface node, final String callbackId) {
		getState(node).setCallbackId(callbackId);
	}

	public void registerRelCallback(final RelationshipInterface rel, final String callbackId) {
		getState(rel).setCallbackId(callbackId);
	}

	/**
	 * Checks if the given key is present in the modifiedProperties of this queue.<br><br>
	 *
	 * This method is convenient if only one key has to be checked. If different
	 * actions should be taken for different keys one should rather use {@link #getModifiedProperties}.
	 *
	 * Note: This method only works for regular properties, not relationship properties (i.e. owner etc)
	 *
	 * @param key The key to check
	 * @return
	 */
	public boolean isPropertyModified(final PropertyKey key) {

		for (GraphObjectModificationState state : modifications.values()) {

			for (PropertyKey k : state.getModifiedProperties().keySet()) {

				if (k.equals(key)) {

					return true;

				}

			}

		}

		return false;
	}

	/**
	 * Returns a set of all modified keys.<br><br>
	 * Useful if different actions should be taken for different keys and we
	 * don't want to iterate over the subsets multiple times.
	 *
	 * If only one key is to be checked {@link #isPropertyModified} is preferred.
	 *
	 * Note: This method only works for regular properties, not relationship properties (i.e. owner etc)
	 *
	 * @return Set with all modified keys
	 */
	public Set<PropertyKey> getModifiedProperties () {

		HashSet<PropertyKey> modifiedKeys = new HashSet<>();

		for (GraphObjectModificationState state : modifications.values()) {

			for (PropertyKey key : state.getModifiedProperties().keySet()) {

				if (!modifiedKeys.contains(key)) {

					modifiedKeys.add(key);

				}

			}

		}

		return modifiedKeys;

	}

	// ----- private methods -----
	private void modifyEndNodes(final Principal user, final NodeInterface startNode, final NodeInterface endNode, final RelationshipType relType) {

		// only modify if nodes are accessible
		if (startNode != null && endNode != null) {

			if (RelType.OWNS.equals(relType)) {

				modifyOwner(startNode);
				modifyOwner(endNode);
				return;
			}

			if (RelType.SECURITY.equals(relType)) {

				modifySecurity(startNode);
				modifySecurity(endNode);
				return;
			}

			if (RelType.IS_AT.equals(relType)) {

				modifyLocation(startNode);
				modifyLocation(endNode);
				return;
			}

			modify(user, startNode, null, null, null);
			modify(user, endNode, null, null, null);
		}
	}

	private GraphObjectModificationState getState(final NodeInterface node) {
		return getState(node, false);
	}

	private GraphObjectModificationState getState(final NodeInterface node, final boolean checkPropagation) {

		String hash = hash(node);
		GraphObjectModificationState state = modifications.get(hash);

		if (state == null && !(checkPropagation && alreadyPropagated.contains(hash))) {

			state = new GraphObjectModificationState(node);
			modifications.put(hash, state);
			modificationEvents.add(state);
		}

		return state;
	}

	private GraphObjectModificationState getState(final RelationshipInterface rel) {
		return getState(rel, true);
	}

	private GraphObjectModificationState getState(final RelationshipInterface rel, final boolean create) {

		String hash = hash(rel);
		GraphObjectModificationState state = modifications.get(hash);

		if (state == null && create) {

			state = new GraphObjectModificationState(rel);
			modifications.put(hash, state);
			modificationEvents.add(state);
		}

		return state;
	}

	private String hash(final NodeInterface node) {
		return "N" + node.getId();
	}

	private String hash(final RelationshipInterface rel) {
		return "R" + rel.getId();
	}
}
