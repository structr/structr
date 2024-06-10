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
package org.structr.memory;

import org.structr.api.Transaction;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.memory.index.filter.Filter;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
public class MemoryTransaction implements Transaction {

	private static final AtomicLong idCounter = new AtomicLong();

	private final MemoryRelationshipRepository createdRelationships            = new MemoryRelationshipRepository(true);
	private final MemoryNodeRepository createdNodes                            = new MemoryNodeRepository();

	//private final Map<MemoryIdentity, MemoryRelationship> createdRelationships = new LinkedHashMap<>();
	private final Map<MemoryIdentity, MemoryRelationship> deletedRelationships = new LinkedHashMap<>();
	//private final Map<MemoryIdentity, MemoryNode> createdNodes                 = new LinkedHashMap<>();
	private final Set<MemoryEntity> modifiedEntities                           = new LinkedHashSet<>();
	private final Set<MemoryIdentity> deletedNodes                             = new LinkedHashSet<>();
	private final long transactionId                                           = idCounter.incrementAndGet();
	private MemoryDatabaseService db                                           = null;
	private boolean success                                                    = false;

	public MemoryTransaction(final MemoryDatabaseService db) {
		this.db = db;
	}

	@Override
	public void failure() {
	}

	@Override
	public void success() {
		success = true;
	}

	@Override
	public long getTransactionId() {
		return transactionId;
	}

	@Override
	public boolean isSuccessful() {
		return success;
	}

	@Override
	public void setIsPing(final boolean isPing) {
	}

	@Override
	public void close() {

		if (success) {

			for (final MemoryEntity entity : modifiedEntities) {

				entity.commit(transactionId);
			}

			db.commitTransaction(createdNodes.getMasterData(), createdRelationships.getMasterData(), deletedNodes, deletedRelationships);

		} else {

			for (final MemoryEntity entity : modifiedEntities) {

				entity.rollback(transactionId);
			}

			db.rollbackTransaction();
		}

		createdNodes.getMasterData().values().stream().forEach(n -> n.unlock());
		createdRelationships.getMasterData().values().stream().forEach(r -> r.unlock());
	}

	@Override
	public Node getNode(final Identity id) {
		return getNodeById((MemoryIdentity) id);
	}

	@Override
	public Relationship getRelationship(final Identity id) {
		return getRelationshipById((MemoryIdentity)id);
	}

	public void create(final MemoryNode newNode) {
		createdNodes.add(newNode);
	}

	public void create(final MemoryRelationship newRelationship) {
		createdRelationships.add(newRelationship);
	}

	public void modify(final MemoryEntity entity) {
		modifiedEntities.add(entity);
	}

	public void delete(final MemoryNode toDelete) {

		final MemoryIdentity id = toDelete.getIdentity();

		deletedNodes.add(id);
	}

	public void delete(final MemoryRelationship toDelete) {

		final MemoryIdentity id = toDelete.getIdentity();

		deletedRelationships.put(id, toDelete);
	}

	// ----- package-private methods -----
	Iterable<MemoryNode> getNodes(final Filter<MemoryNode> filter) {

		final List<Iterable<MemoryNode>> sources = new LinkedList<>();

		// FIXME: this might return wrong data when newly created nodes match the filter but are not filtered
		sources.add(createdNodes.values(filter));
		sources.add(db.getNodes(filter));

		// return union of new and existing nodes, filtered for deleted nodes
		return Iterables.filter(n -> !deletedNodes.contains(n.getIdentity()), Iterables.flatten(sources));
	}

	Iterable<MemoryRelationship> getRelationships(final Filter<MemoryRelationship> filter) {

		final List<Iterable<MemoryRelationship>> sources = new LinkedList<>();

		sources.add(createdRelationships.values(filter));
		sources.add(db.getRelationships(filter));

		// return union of new and existing nodes
		return Iterables.filter(r -> !deletedRelationships.containsKey(r.getIdentity()), Iterables.flatten(sources));
	}

	MemoryNode getNodeById(final MemoryIdentity id) {

		// deleted, dont return value
		if (deletedNodes.contains(id)) {
			return null;
		}

		MemoryNode candidate = createdNodes.get(id);
		if (candidate != null) {

			return candidate;
		}

		candidate = db.getNodeFromRepository(id);
		if (candidate != null) {

			return candidate;
		}

		return null;
	}

	MemoryRelationship getRelationshipById(final MemoryIdentity id) {

		// deleted, dont return value
		if (deletedRelationships.containsKey(id)) {
			return null;
		}

		MemoryRelationship candidate = createdRelationships.get(id);
		if (candidate != null) {

			return candidate;
		}

		candidate = db.getRelationshipFromRepository(id);
		if (candidate != null) {

			return candidate;
		}

		return null;
	}

	boolean isDeleted(final MemoryIdentity id) {

		if (id.isNode()) {

			return deletedNodes.contains(id);
		}

		return deletedRelationships.containsKey(id);
	}

	boolean exists(final MemoryIdentity id) {

		if (id.isNode()) {

			return createdNodes.contains(id) || db.exists(id);
		}

		return createdRelationships.contains(id) || db.exists(id);
	}

	@Override
	public void prefetch(String type1, String type2, Set<String> keys) {

	}

	@Override
	public void prefetch(String query, Set<String> keys) {

	}
}
