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

import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractTraitDefinition implements TraitDefinition {

	protected final String name;

	public AbstractTraitDefinition(final String name) {
		this.name = name;
	}

	public abstract boolean isRelationship();

	@Override
	public int compareTo(final TraitDefinition o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return new LinkedHashMap<>();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return new LinkedHashMap<>();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return new LinkedHashMap<>();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return new LinkedHashMap<>();
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	@Override
	public Map<String, Set<String>> getViews() {
		return new LinkedHashMap<>();
	}

	protected Relation getRelationForType(final String type) {

		final Traits traits = Traits.of(type);

		return traits.getRelation();
	}
}
