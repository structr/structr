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
package org.structr.rest.traits.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.Map;
import java.util.Set;

import static org.structr.core.entity.Relation.Multiplicity.Many;
import static org.structr.core.entity.Relation.Multiplicity.One;

/**
 *
 *
 */
public class SubjectEventRelationship extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public SubjectEventRelationship() {
		super(StructrTraits.SUBJECT_EVENT_RELATIONSHIP);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.NODE_INTERFACE;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.LOG_EVENT;
	}

	@Override
	public String getRelationshipType() {
		return "SUBJECT";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return One;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return Many;
	}

	@Override
	public int getCascadingDeleteFlag() {
		return 0;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.TARGET_TO_SOURCE;
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	@Override
	public boolean isInternal() {
		return true;
	}
}
