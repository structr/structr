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
package org.structr.core.traits.definitions;

import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;

import java.util.Set;

import static org.structr.core.GraphObject.SYSTEM_CATEGORY;

public final class RelationshipInterfaceTraitDefinition extends AbstractTraitDefinition {

	public RelationshipInterfaceTraitDefinition() {
		super("RelationshipInterface");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String>        internalTimestamp  = new StringProperty("internalTimestamp").systemInternal().unvalidated().writeOnce().category(SYSTEM_CATEGORY);
		final Property<String>        relType            = new RelationshipTypeProperty();
		final SourceId                sourceId           = new SourceId("sourceId");
		final TargetId                targetId           = new TargetId("targetId");
		final Property<NodeInterface> sourceNode         = new SourceNodeProperty("sourceNode");
		final Property<NodeInterface> targetNode         = new TargetNodeProperty("targetNode");

		return Set.of(
			internalTimestamp,
			relType,
			sourceId,
			targetId,
			sourceNode,
			targetNode
		);
	}
}
