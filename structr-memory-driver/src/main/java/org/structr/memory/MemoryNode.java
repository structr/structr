/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;

/**
 *
 * @author Christian Morgner
 */
public class MemoryNode extends MemoryEntity implements Node {

	private Set<Label> labels = new LinkedHashSet<>();

	public MemoryNode(final MemoryDatabaseService db, final MemoryIdentity identity) {
		super(db, identity);
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {
		lock();
		return db.createRelationship(this, (MemoryNode)endNode, relationshipType);
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType, final Map<String, Object> properties) {

		lock();

		final Relationship rel = createRelationshipTo(endNode, relationshipType);

		rel.setProperties(properties);

		return rel;
	}

	@Override
	public void addLabel(final Label label) {
		lock();
		labels.add(label);
	}

	@Override
	public void removeLabel(final Label label) {
		lock();
		labels.remove(label);
	}

	public boolean hasLabel(final Label label) {
		return labels.contains(label);
	}

	@Override
	public Iterable<Label> getLabels() {
		return labels;
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType relationshipType, final Node targetNode) {

		final MemoryTransaction tx    = db.getCurrentTransaction(true);
		final MemoryIdentity sourceId = getIdentity();
		final MemoryIdentity targetId = (MemoryIdentity)targetNode.getId();
		final String name             = relationshipType.name();

		return Iterables.first(Iterables.filter(r -> {

			return
				   sourceId.equals(r.getSourceNodeIdentity())
				&& targetId.equals(r.getTargetNodeIdentity())
				&& name.equals(r.getType().name());

		}, tx.getRelationships())) != null;
	}

	@Override
	public Iterable<Relationship> getRelationships() {
		return db.getRelationships(this);
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction) {
		return db.getRelationships(this, direction);
	}

	@Override
	public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType) {
		return db.getRelationships(this, direction, relationshipType);
	}

	@Override
	public void delete(boolean deleteRelationships) throws NotInTransactionException {
		lock();
		db.delete(this);
	}
}
