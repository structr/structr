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
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.Map;
import java.util.Set;

/**
 * A named collection of properties and methods with a
 * factory that can instantiate implementations.
 */
public interface TraitDefinition {

	String getName();

	Map<Class, LifecycleMethod> getLifecycleMethods();
	Map<Class, FrameworkMethod> getFrameworkMethods();
	Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories();
	Map<Class, NodeTraitFactory> getNodeTraitFactories();
	Set<AbstractMethod> getDynamicMethods();

	// FIXME
	//Set<String> requiredTraits();

	Set<PropertyKey> getPropertyKeys();

	Relation getRelation();

	default boolean isAbstract() {
		return false;
	}

	default boolean isInterface() {
		return false;
	}
}
