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
package org.structr.core.traits;

import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.Map;
import java.util.Set;

/**
 * A named collection of properties and methods with a
 * factory that can instantiate implementations.
 */
public interface TraitDefinition extends Comparable<TraitDefinition> {

	String getName();
	String getLabel();

	Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance);
	Map<Class, FrameworkMethod> getFrameworkMethods();
	Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories();
	Map<Class, NodeTraitFactory> getNodeTraitFactories();
	Set<AbstractMethod> getDynamicMethods();
	Map<String, Set<String>> getViews();
	boolean isRelationship();

	Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance);

	Relation getRelation();

	default boolean isAbstract() {
		return false;
	}

	default boolean isInterface() {
		return false;
	}

	/**
	 * Override this method to return a short description of this type.
	 *
	 * Write a short sentence that start with "This type..." and describe
	 * what the type is used for.
	 *
	 * @return
	 */
	default String getShortDescription() {
		return null;
	}

	/**
	 * Override this method to return a detailed description of
	 * this type. If possible, please observe the following guidelines:
	 *
	 * 2. Write a "How It Works" paragraph with heading 3.
	 * 3. Write a "Common Use Cases" paragraph with heading 3.
	 * 4. Write additional paragraphs with noteworthy things about the type.
	 *
	 * @return
	 */
	default String getLongDescription() {
		return null;
	}
}
