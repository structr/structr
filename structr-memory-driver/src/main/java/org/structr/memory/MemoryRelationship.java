/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.Arrays;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;

/**
 *
 * @author Christian Morgner
 */
public class MemoryRelationship extends MemoryEntity implements Relationship {

	private RelationshipType relType  = null;
	private MemoryIdentity sourceNode = null;
	private MemoryIdentity targetNode = null;

	public MemoryRelationship(final MemoryDatabaseService db, final MemoryIdentity identity, final RelationshipType relType, final MemoryIdentity sourceNode, final MemoryIdentity targetNode) {

		super(db, identity);

		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
		this.relType    = relType;
	}

	@Override
	public Node getStartNode() {
		return db.getNodeById(sourceNode);
	}

	@Override
	public Node getEndNode() {
		return db.getNodeById(targetNode);
	}

	@Override
	public Node getOtherNode(Node node) {

		if (node.getId().equals(sourceNode)) {
			return getEndNode();
		}

		return getStartNode();
	}

	@Override
	public RelationshipType getType() {
		return relType;
	}

	public MemoryIdentity getSourceNodeIdentity() {
		return sourceNode;
	}

	public MemoryIdentity getTargetNodeIdentity() {
		return targetNode;
	}

	@Override
	public void delete(boolean deleteRelationships) throws NotInTransactionException {
		db.delete(this);

	}

	public boolean isEqualTo(final MemoryRelationship rel) {
		return rel.getSourceNodeIdentity().equals(sourceNode) && rel.getTargetNodeIdentity().equals(targetNode) && rel.getType().name().equals(relType.name());
	}

	@Override
	public Iterable<String> getLabels() {
		return Arrays.asList(relType.name());
	}

	public String getUniquenessKey() {
		return sourceNode.getId() + relType.name() + targetNode.getId();
	}

	@Override
	protected void updateCache() {
		db.updateCache(this);
	}
}
