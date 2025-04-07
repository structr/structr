/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.function.ChangelogFunction;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 */

public class ModificationQueue {

	private static final Logger logger = LoggerFactory.getLogger(ModificationQueue.class.getName());

	private final ConcurrentSkipListMap<Long, GraphObjectModificationState> nodeModifications   = new ConcurrentSkipListMap<>();
	private final ConcurrentSkipListMap<Long, GraphObjectModificationState> relModifications    = new ConcurrentSkipListMap<>();
	private final Collection<ModificationEvent> modificationEvents                              = new ArrayDeque<>(1000);
	private final Map<String, TransactionPostProcess> postProcesses                             = new LinkedHashMap<>();
	private final Set<String> alreadyPropagated                                                 = new LinkedHashSet<>();
	private final Set<Long> ids                                                                 = new LinkedHashSet<>();
	private final Set<String> synchronizationKeys                                               = new TreeSet<>();
	private boolean doUpateChangelogIfEnabled                                                   = true;
	private boolean transactionWasSuccessful                                                    = false;
	private CallbackCounter counter                                                             = null;
	private boolean hasChanges                                                                  = false;
	private long changelogUpdateTime                                                            = 0L;
	private long outerCallbacksTime                                                             = 0L;
	private long innerCallbacksTime                                                             = 0L;
	private long postProcessingTime                                                             = 0L;
	private long validationTime                                                                 = 0L;
	private long indexingTime                                                                   = 0L;

	public ModificationQueue(final long transactionId, final String threadName) {
		this.counter = new CallbackCounter(logger, transactionId, threadName);
	}

	/**
	 * Returns a set containing the different entity types of
	 * nodes modified in this queue.
	 *
	 * @return the types
	 */
	public Set<String> getSynchronizationKeys() {
		return synchronizationKeys;
	}

	public int getSize() {
		return nodeModifications.size() + relModifications.size();
	}

	public boolean doInnerCallbacks(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		long t0                       = System.currentTimeMillis();
		boolean hasModifications      = true;

		// collect all modified nodes
		while (hasModifications) {

			hasModifications = false;

			for (GraphObjectModificationState state : getSortedModifications()) {

				if (state.wasModified()) {

					// do callback according to entry state
					if (!state.doInnerCallback(this, securityContext, errorBuffer, counter)) {
						return false;
					}

					hasModifications = true;
				}
			}
		}

		innerCallbacksTime = System.currentTimeMillis() - t0;
		if (innerCallbacksTime > 1000) {
			logger.info("{} ms ({} modifications)", innerCallbacksTime, getSize());
		}

		return true;
	}

	public boolean doValidation(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final boolean doValidation) throws FrameworkException {

		long t0 = System.currentTimeMillis();

		// do validation and indexing
		for (final GraphObjectModificationState state : getSortedModifications()) {

			// do callback according to entry state
			boolean res = state.doValidationAndIndexing(this, securityContext, errorBuffer, doValidation, counter);

			validationTime += state.getValdationTime();
			indexingTime += state.getIndexingTime();

			if (!res) {
				return false;
			}
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 1000) {

			logger.info("doValidation: {} ms ({} modifications)   ({} ms validation - {} ms indexing)", t, getSize(), validationTime, indexingTime);
		}

		return true;
	}

	public boolean doPostProcessing(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		long t0 = System.currentTimeMillis();

		for (final TransactionPostProcess process : postProcesses.values()) {

			if (!process.execute(securityContext, errorBuffer)) {
				return false;
			}
		}

		postProcessingTime = System.currentTimeMillis() - t0;
		if (postProcessingTime > 1000) {
			logger.info("doPostProcessing: {} ms", postProcessingTime);
		}

		return true;
	}

	public void doOuterCallbacks(final SecurityContext securityContext) throws FrameworkException {

		long t0 = System.currentTimeMillis();

		// copy modifications, do after transaction callbacks
		for (GraphObjectModificationState state : nodeModifications.values()) {
			state.doOuterCallback(securityContext, counter);
		}

		// copy modifications, do after transaction callbacks
		for (GraphObjectModificationState state : relModifications.values()) {
			state.doOuterCallback(securityContext, counter);
		}

		outerCallbacksTime = System.currentTimeMillis() - t0;
		if (outerCallbacksTime > 3000) {
			logger.info("doOutCallbacks: {} ms ({} modifications)", outerCallbacksTime, getSize());
		}
	}

	public void updateChangelog() {

		final boolean objectChangelog = Settings.ChangelogEnabled.getValue();
		final boolean userChangelog   = Settings.UserChangelogEnabled.getValue();

		if (doUpateChangelogIfEnabled && (objectChangelog || userChangelog)) {

			final long t0 = System.currentTimeMillis();

			for (final ModificationEvent ev: modificationEvents) {

				try {

					if (objectChangelog) {

						final GraphObject obj = ev.getGraphObject();
						final String newLog   = ev.getChangeLog();

						if (obj != null && obj.changelogEnabled()) {

							final String uuid           = ev.isDeleted() ? ev.getUuid() : obj.getUuid();
							final String typeFolderName = obj.isNode() ? "n" : "r";

							java.io.File file = ChangelogFunction.getChangeLogFileOnDisk(typeFolderName, uuid, true);

							FileUtils.write(file, newLog, "utf-8", true);
						}
					}

					if (userChangelog) {

						for (Map.Entry<String, StringBuilder> entry : ev.getUserChangeLogs().entrySet()) {

							java.io.File file = ChangelogFunction.getChangeLogFileOnDisk("u", entry.getKey(), true);

							FileUtils.write(file, entry.getValue().toString(), "utf-8", true);
						}
					}

				} catch (IOException ioex) {
					logger.error("Unable to write changelog to file: {}", ioex.getMessage());
				} catch (Throwable t) {
					logger.warn("", t);
				}
			}

			changelogUpdateTime = System.currentTimeMillis() - t0;
		}
	}

	public void clear() {

		setTransactionWasSuccessful(false);
		hasChanges = false;

		// clear collections afterwards
		alreadyPropagated.clear();
		nodeModifications.clear();
		relModifications.clear();
		modificationEvents.clear();
		ids.clear();
	}

	public void create(final Principal user, final NodeInterface node) {

		this.hasChanges = true;

		getState(node).create();

		if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

			getState(node).updateChangeLog(user, GraphObjectModificationState.Verb.create, node);
		}
	}

	public <S extends NodeInterface, T extends NodeInterface> void create(final Principal user, final RelationshipInterface relationship) {

		this.hasChanges = true;

		getState(relationship).create();

		final NodeInterface sourceNode = relationship.getSourceNode();
		final NodeInterface targetNode = relationship.getTargetNode();

		if (sourceNode != null && targetNode != null) {

			modifyEndNodes(user, sourceNode, targetNode, relationship, false);

			if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

				getState(relationship).updateChangeLog(user, GraphObjectModificationState.Verb.create, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), targetNode.getUuid());

				getState(sourceNode).updateChangeLog(user, GraphObjectModificationState.Verb.link, relationship.getType(), relationship.getUuid(), targetNode.getUuid(), GraphObjectModificationState.Direction.out);
				getState(targetNode).updateChangeLog(user, GraphObjectModificationState.Verb.link, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), GraphObjectModificationState.Direction.in);

			}
		}
	}

	public void modifyOwner(NodeInterface node) {

		this.ids.add(node.getNode().getId().getId());
		this.hasChanges = true;

		getState(node).modifyOwner();
	}

	public void modifySecurity(NodeInterface node) {

		this.ids.add(node.getNode().getId().getId());
		this.hasChanges = true;

		getState(node).modifySecurity();
	}

	public void modify(final Principal user, final NodeInterface node, final PropertyKey key, final Object previousValue, final Object newValue) {

		this.ids.add(node.getNode().getId().getId());
		this.hasChanges = true;

		getState(node).modify(user, key, previousValue, newValue);

		if (key != null&& key.requiresSynchronization()) {
			synchronizationKeys.add(key.getSynchronizationKey());
		}
	}

	public void modify(final Principal user, RelationshipInterface relationship, PropertyKey key, Object previousValue, Object newValue) {

		this.ids.add(relationship.getRelationship().getId().getId());
		this.hasChanges = true;

		getState(relationship).modify(user, key, previousValue, newValue);

		if (key != null && key.requiresSynchronization()) {
			synchronizationKeys.add(key.getSynchronizationKey());
		}
	}

	public void delete(final Principal user, final NodeInterface node) {

		this.ids.add(node.getNode().getId().getId());
		this.hasChanges = true;

		getState(node).delete(false);

		if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

			getState(node).updateChangeLog(user, GraphObjectModificationState.Verb.delete, node);
		}
	}

	public void delete(final Principal user, final RelationshipInterface relationship, final boolean passive) {

		this.ids.add(relationship.getRelationship().getId().getId());
		this.hasChanges = true;

		getState(relationship).delete(passive);

		final NodeInterface sourceNode = relationship.getSourceNodeAsSuperUser();
		final NodeInterface targetNode = relationship.getTargetNodeAsSuperUser();

		modifyEndNodes(user, sourceNode, targetNode, relationship, true);

		if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

			getState(relationship).updateChangeLog(user, GraphObjectModificationState.Verb.delete, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), targetNode.getUuid());

			getState(sourceNode).updateChangeLog(user, GraphObjectModificationState.Verb.unlink, relationship.getType(), relationship.getUuid(), targetNode.getUuid(), GraphObjectModificationState.Direction.out);
			getState(targetNode).updateChangeLog(user, GraphObjectModificationState.Verb.unlink, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), GraphObjectModificationState.Direction.in);

		}
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

		final GraphObjectModificationState state = nodeModifications.get(hash(node));
		if (state != null) {

			return state.isDeleted() || state.isPassivelyDeleted();
		}

		return false;
	}

	public boolean isDeleted(final Relationship rel) {

		final GraphObjectModificationState state = relModifications.get(hash(rel));
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
	 * Checks if the given key is present for the given graph object in the modifiedProperties of this queue.<br><br>
	 *
	 * This method is convenient if only one key has to be checked. If different
	 * actions should be taken for different keys one should rather use {@link #getModifiedProperties}.
	 *
	 * Note: This method only works for regular properties, not relationship properties (i.e. owner etc)
	 *
	 * @param graphObject The GraphObject we are interested in
	 * @param key The key to check
	 * @return
	 */
	public boolean isPropertyModified(final GraphObject graphObject, final PropertyKey key) {

		for (GraphObjectModificationState state : getSortedModifications()) {

			for (final PropertyKey k : state.getModifiedProperties().keySet()) {

				if (k.equals(key) && graphObject.getUuid().equals(state.getGraphObject().getUuid()) ) {

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

		for (GraphObjectModificationState state : getSortedModifications()) {

			for (final PropertyKey key : state.getModifiedProperties().keySet()) {

				if (!modifiedKeys.contains(key)) {

					modifiedKeys.add(key);
				}
			}
		}

		return modifiedKeys;
	}

	public GraphObjectMap getModifications(final GraphObject forObject) {

		final GraphObjectMap result = new GraphObjectMap();
		final GraphObjectModificationState state;

		if (forObject.isNode()) {

			state = getState((NodeInterface)forObject);

		} else {

			state = getState((RelationshipInterface)forObject);
		}

		final GraphObjectMap before = new GraphObjectMap();
		final GraphObjectMap after  = new GraphObjectMap();


		addLocalProperties(before, state.getRemovedProperties());

		addLocalProperties(after, state.getModifiedProperties());
		addLocalProperties(after, state.getNewProperties());

		result.put(new GenericProperty("before"),  before);
		result.put(new GenericProperty("after"),   after);
		result.put(new GenericProperty("added"),   state.getAddedRemoteProperties());
		result.put(new GenericProperty("removed"), state.getRemovedRemoteProperties());

		return result;
	}

	private void addLocalProperties(final GraphObjectMap map, final PropertyMap data) {

		data.getRawMap().forEach((key, value) -> {

			if ( !(key instanceof RelationProperty) ) {
				map.put(key, value);
			}
		});
	}

	public void disableChangelog() {
		this.doUpateChangelogIfEnabled = false;
	}

	public Map<String, Object> getTransactionStats() {

		final Map<String, Object> stats = new LinkedHashMap<>();

		stats.put("changelogUpdateTime", changelogUpdateTime);
		stats.put("outerCallbacksTime",  outerCallbacksTime);
		stats.put("innerCallbacksTime",  innerCallbacksTime);
		stats.put("postProcessingTime",  postProcessingTime);
		stats.put("validationTime",      validationTime);
		stats.put("indexingTime",        indexingTime);
		stats.put("changes",             getSize());

		return stats;
	}

	public boolean hasChanges() {
		return this.hasChanges;
	}

	public Set<Long> getIds() {
		return this.ids;
	}

	// ----- private methods -----
	private void modifyEndNodes(final Principal user, final NodeInterface startNode, final NodeInterface endNode, final RelationshipInterface rel, final boolean isDeletion) {

		// only modify if nodes are accessible
		if (startNode != null && endNode != null) {

			final RelationshipType relType = rel.getRelType();

			if ("OWNS".equals(relType.name())) {

				modifyOwner(startNode);
				modifyOwner(endNode);
			}

			if ("SECURITY".equals(relType.name())) {

				modifySecurity(startNode);
				modifySecurity(endNode);
			}

			final Relation relation  = rel.getTraits().getRelation();
			final PropertyKey source = relation.getSourceProperty();
			final PropertyKey target = relation.getTargetProperty();
			final Object sourceValue = source != null && source.isCollection() ? new LinkedList<>() : null;
			final Object targetValue = target != null && target.isCollection() ? new LinkedList<>() : null;

			modify(user, startNode, target, null, targetValue);
			modify(user, endNode, source, null, sourceValue);

			if (source != null && target != null) {

				if (isDeletion) {

					// update removed properties
					getState(startNode).remove(target, endNode);
					getState(endNode).remove(source, startNode);

				} else {


					// update added properties
					getState(startNode).add(target, endNode);
					getState(endNode).add(source, startNode);
				}

			} else {

				// dont log so much..
				//logger.warn("No properties registered for {}: source: {}, target: {}", rel.getClass(), source, target);
			}
		}
	}

	private GraphObjectModificationState getState(final NodeInterface node) {
		return getState(node, false);
	}

	private GraphObjectModificationState getState(final NodeInterface node, final boolean checkPropagation) {

		long hash = hash(node.getNode());
		GraphObjectModificationState state = nodeModifications.get(hash);

		if (state == null && !(checkPropagation && alreadyPropagated.contains(hash))) {

			state = new GraphObjectModificationState(node);
			nodeModifications.put(hash, state);
			modificationEvents.add(state);
		}

		return state;
	}

	private GraphObjectModificationState getState(final RelationshipInterface rel) {
		return getState(rel, true);
	}

	private GraphObjectModificationState getState(final RelationshipInterface rel, final boolean create) {

		long hash = hash(rel.getRelationship());
		GraphObjectModificationState state = relModifications.get(hash);

		if (state == null && create) {

			state = new GraphObjectModificationState(rel);
			relModifications.put(hash, state);
			modificationEvents.add(state);
		}

		return state;
	}

	private long hash(final Node node) {
		return node.getId().getId();
	}

	private long hash(final Relationship rel) {
		return rel.getId().getId();
	}

	private List<GraphObjectModificationState> getSortedModifications() {

		final List<GraphObjectModificationState> state = new LinkedList<>();

		state.addAll(nodeModifications.values());
		state.addAll(relModifications.values());

		Collections.sort(state, (a, b) -> {

			final long t1 = a.getTimestamp();
			final long t2 = b.getTimestamp();

			if (t1 < t2) {
				return -1;
			}

			if (t1 > t2) {
				return 1;
			}

			return 0;
		});

		return state;
	}

	public boolean transactionWasSuccessful() {
		return transactionWasSuccessful;
	}

	public void setTransactionWasSuccessful(final boolean transactionWasSuccessful) {
		this.transactionWasSuccessful = transactionWasSuccessful;
	}
}
