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
package org.structr.core.traits.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.RelationshipTraitDefinition;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.Map;
import java.util.Set;

import static org.structr.core.entity.Relation.Multiplicity.Many;

public class PrincipalFAVORITEFavoritableDefinition extends RelationshipTraitDefinition {

	public PrincipalFAVORITEFavoritableDefinition() {
		super("PrincipalFAVORITEFavoritable");
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
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	@Override
	protected String getSourceType() {
		return "Principal";
	}

	@Override
	protected String getTargetType() {
		return "Favoritable";
	}

	@Override
	protected String getRelationshipType() {
		return "FAVORITE";
	}

	@Override
	protected Relation.Multiplicity getSourceMultiplicity() {
		return Many;
	}

	@Override
	protected Relation.Multiplicity getTargetMultiplicity() {
		return Many;
	}
}
