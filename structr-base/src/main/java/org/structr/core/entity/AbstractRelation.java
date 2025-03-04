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

	public final Direction getDirectionForType(final String sourceType, final String targetType, final String type) {

		// FIXME: this method will most likely not do what it's supposed to do..
		if (sourceType.equals(type) && targetType.equals(type)) {
			return Direction.BOTH;
		}

		if (sourceType.equals(type)) {
			return Direction.OUTGOING;
		}

		if (targetType.equals(type)) {
			return Direction.INCOMING;
		}

		/*
		// one of these blocks is wrong..
		if (sourceType.isAssignableFrom(type) && targetType.isAssignableFrom(type)) {
			return Direction.BOTH;
		}

		if (sourceType.isAssignableFrom(type)) {
			return Direction.OUTGOING;
		}

		if (targetType.isAssignableFrom(type)) {
			return Direction.INCOMING;
		}

		// one of these blocks is wrong..
		if (type.isAssignableFrom(sourceType) && type.isAssignableFrom(targetType)) {
			return Direction.BOTH;
		}

		if (type.isAssignableFrom(sourceType)) {
			return Direction.OUTGOING;
		}

		if (type.isAssignableFrom(targetType)) {
			return Direction.INCOMING;
		}
		*/

		return Direction.BOTH;
	}
}
