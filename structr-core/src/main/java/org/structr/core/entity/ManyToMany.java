/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.core.entity;

import java.util.LinkedHashMap;
import java.util.List;
import org.structr.api.graph.Direction;
import static org.structr.api.graph.Direction.BOTH;
import static org.structr.api.graph.Direction.INCOMING;
import static org.structr.api.graph.Direction.OUTGOING;
import org.structr.common.SecurityContext;
import org.structr.common.error.DuplicateRelationshipToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;

/**
 *
 *
 */
public abstract class ManyToMany<S extends NodeInterface, T extends NodeInterface> extends AbstractRelationship<S, T> implements Relation<S, T, ManyStartpoint<S>, ManyEndpoint<T>> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public ManyStartpoint<S> getSource() {
		return new ManyStartpoint<>(this);
	}

	@Override
	public ManyEndpoint<T> getTarget() {
		return new ManyEndpoint<>(this);
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.NONE;
	}

	@Override
	public void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		final LinkedHashMap<String, Object> props = new LinkedHashMap();
		props.put("sourceId",       sourceNode.getUuid());
		props.put("targetId",       targetNode.getUuid());
		props.put("structrRelType", getClass().getSimpleName());

		final List<GraphObject> existingRelationships = StructrApp.getInstance().cypher("MATCH (s:" + sourceNode.getType() + ")-[r]->(t:" + targetNode.getType() + ") WHERE s.id = {sourceId} AND r.type = {structrRelType} AND t.id = {targetId} RETURN r", props);

		if (existingRelationships.size() > 0) {
			throw new FrameworkException(422, "Relationship already exists", new DuplicateRelationshipToken(getClass().getSimpleName(), "Relationship already exists"));
		}
	}

	@Override
	public Notion getEndNodeNotion() {
		return new RelationshipNotion(getTargetIdProperty());

	}

	@Override
	public Notion getStartNodeNotion() {
		return new RelationshipNotion(getSourceIdProperty());
	}

	@Override
	public Direction getDirectionForType(final Class<? extends NodeInterface> type) {
		return super.getDirectionForType(getSourceType(), getTargetType(), type);
	}

	@Override
	public Class getOtherType(final Class type) {

		switch (getDirectionForType(type)) {

			case INCOMING: return getSourceType();
			case OUTGOING: return getTargetType();
			case BOTH:     return getSourceType();	// don't know...
		}

		return null;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public boolean isInternal() {
		return false;
	}
}
