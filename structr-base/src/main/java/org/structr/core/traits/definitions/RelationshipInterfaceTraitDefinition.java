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
package org.structr.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;

import java.util.Map;
import java.util.Set;

import static org.structr.core.GraphObject.SYSTEM_CATEGORY;

public final class RelationshipInterfaceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String INTERNAL_TIMESTAMP_PROPERTY = "internalTimestamp";
	public static final String REL_TYPE_PROPERTY           = "relType";
	public static final String SOURCE_ID_PROPERTY          = "sourceId";
	public static final String TARGET_ID_PROPERTY          = "targetId";
	public static final String SOURCE_NODE_PROPERTY        = "sourceNode";
	public static final String TARGET_NODE_PROPERTY        = "targetNode";

	public RelationshipInterfaceTraitDefinition() {
		super(StructrTraits.RELATIONSHIP_INTERFACE);
	}

	@Override
	public boolean isRelationship() {
		return true;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String>        internalTimestamp  = new StringProperty(INTERNAL_TIMESTAMP_PROPERTY).systemInternal().unvalidated().writeOnce().category(SYSTEM_CATEGORY);
		final Property<String>        relType            = new RelationshipTypeProperty();
		final SourceId                sourceId           = new SourceId(SOURCE_ID_PROPERTY);
		final TargetId                targetId           = new TargetId(TARGET_ID_PROPERTY);
		final Property<NodeInterface> sourceNode         = new SourceNodeProperty(SOURCE_NODE_PROPERTY);
		final Property<NodeInterface> targetNode         = new TargetNodeProperty(TARGET_NODE_PROPERTY);

		return newSet(
			internalTimestamp,
			relType,
			sourceId,
			targetId,
			sourceNode,
			targetNode
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
				PropertyView.Public,
				newSet(
						"id", "type", "relType", "sourceId", "targetId"
				),
				PropertyView.Ui,
				newSet(
						"id", "type", "relType", "sourceId", "targetId"
				)
		);
	}
}
