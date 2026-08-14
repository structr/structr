/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.api.graph.Direction;
import org.structr.common.SecurityContext;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;

/**
 * Abstract base class for relations in Structr.
 */
public abstract class AbstractRelation {

	private final Notion startNodeNotion = new RelationshipNotion(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY);
	private final Notion endNodeNotion   = new RelationshipNotion(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY);
	private PropertyKey sourceProperty   = null;
	private PropertyKey targetProperty   = null;

	protected SecurityContext securityContext  = null;

	public void setSourceProperty(final PropertyKey source) {

		this.sourceProperty = source;
	}

	public void setTargetProperty(final PropertyKey target) {

		this.targetProperty = target;
	}

	public PropertyKey getSourceProperty() {

		return sourceProperty;
	}

	public PropertyKey getTargetProperty() {

		return targetProperty;
	}

	public Notion getEndNodeNotion() {

		return endNodeNotion;
	}

	public Notion getStartNodeNotion() {

		return startNodeNotion;
	}

	public PropertyKey<String> getSourceIdProperty() {

		return Traits.of(StructrTraits.RELATIONSHIP_INTERFACE).key(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY);
	}

	public PropertyKey<String> getTargetIdProperty() {

		return Traits.of(StructrTraits.RELATIONSHIP_INTERFACE).key(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY);
	}

	/**
	 * The direction this relation has when seen from a node of the given type: OUTGOING when such a node
	 * sits at the source end, INCOMING when it sits at the target end, BOTH when it can be at either end.
	 *
	 * @param sourceType the type at the source end of this relation
	 * @param targetType the type at the target end of this relation
	 * @param type       a NODE type - a type that only has the relation's traits, e.g. a relationship type
	 *                   name, sits at neither end and yields BOTH
	 *
	 * @return the direction, never null
	 */
	public final Direction getDirectionForType(final String sourceType, final String targetType, final String type) {

		final boolean atSourceEnd = isOfType(type, sourceType);
		final boolean atTargetEnd = isOfType(type, targetType);

		if (atSourceEnd && atTargetEnd) {

			return Direction.BOTH;
		}

		if (atSourceEnd) {

			return Direction.OUTGOING;
		}

		if (atTargetEnd) {

			return Direction.INCOMING;
		}

		// neither end, so no direction can be derived
		return Direction.BOTH;
	}

	// ----- private methods -----
	/**
	 * Whether a node of the given type sits at an end declared for otherType, which is the case when it is
	 * that type or has it as one of its traits - a relation declared for a supertype applies to the types
	 * built on it as well.
	 */
	private static boolean isOfType(final String type, final String otherType) {

		if (type.equals(otherType)) {

			return true;
		}

		final Traits traits = Traits.of(type);

		return traits != null && traits.contains(otherType);
	}
}
