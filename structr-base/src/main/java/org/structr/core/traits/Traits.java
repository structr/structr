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
package org.structr.core.traits;

import org.structr.api.Predicate;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.Map;
import java.util.Set;

public interface Traits {

	Set<String> getLabels();
	boolean contains(final String type);
	<T> PropertyKey<T> key(final String name);
	boolean hasKey(final String name);
	String getName();
	boolean isNodeType();
	boolean isRelationshipType();
	Set<PropertyKey> getAllPropertyKeys();
	Set<PropertyKey> getPropertyKeysForView(final String propertyView);
	<T extends LifecycleMethod> Set<T> getMethods(final Class<T> type);
	<T extends FrameworkMethod> T getMethod(final Class<T> type);
	Map<String, AbstractMethod> getDynamicMethods();
	<T> T as(final Class<T> type, final GraphObject obj);
	void registerImplementation(final TraitDefinition trait, final boolean isDynamic);
	Map<String, Map<String, PropertyKey>> removeDynamicTraits();
	Relation getRelation();
	Set<TraitDefinition> getTraitDefinitions();
	boolean isInterface();
	boolean isAbstract();
	boolean isBuiltInType();
	boolean changelogEnabled();
	Set<String> getViewNames();
	Set<String> getAllTraits();

	// ----- static methods -----
	static Traits of(final String name) {
		return TraitsImplementation.of(name);
	}

	static Trait getTrait(final String name) {
		return TraitsImplementation.getTrait(name);
	}

	static Set<PropertyKey> getPropertiesOfTrait(final String name) {
		return TraitsImplementation.getPropertiesOfTrait(name);
	}

	static Set<PropertyKey> getDefaultKeys() {
		return TraitsImplementation.getDefaultKeys();
	}

	static Traits ofRelationship(final String type1, final String relType, final String type2) {
		return TraitsImplementation.ofRelationship(type1, relType, type2);
	}

	/**
	 * Indicates whether a type (not a trait!) with the given name exists.
	 *
	 * @param name
	 * @return
	 */
	static boolean exists(final String name) {
		return TraitsImplementation.exists(name);
	}

	static Set<String> getAllTypes() {
		return TraitsImplementation.getAllTypes();
	}

	static Set<String> getAllTypes(final Predicate<Traits> filter) {
		return TraitsImplementation.getAllTypes(filter);
	}

	static <T> PropertyKey<T> key(final String type, final String name) {
		return TraitsImplementation.key(type, name);
	}

	static Set<String> getAllViews() {
		return TraitsImplementation.getAllViews();
	}

	static Map<String, Map<String, PropertyKey>> clearDynamicSchema() {
		return TraitsImplementation.clearDynamicSchema();
	}
}
