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
import org.structr.core.entity.SchemaMethod;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.definitions.TraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public interface Traits {

	Set<String> getLabels();
	boolean contains(String type);
	TraitDefinition get(String type);
	<T> PropertyKey<T> key(String name);
	boolean hasKey(String name);
	String getName();
	boolean isNodeType();
	boolean isRelationshipType();
	Set<PropertyKey> getDefaultKeys();
	Set<PropertyKey> getAllPropertyKeys();
	Set<PropertyKey> getPropertyKeysForView(String propertyView);
	<T extends LifecycleMethod> Set<T> getMethods(Class<T> type);
	<T extends FrameworkMethod> T getMethod(Class<T> type);
	Map<String, AbstractMethod> getDynamicMethods();
	<T> T as(Class<T> type, GraphObject obj);
	void registerImplementation(TraitDefinition trait);
	Relation getRelation();
	Set<TraitDefinition> getTraitDefinitions();
	boolean isInterface();
	boolean isAbstract();
	boolean isBuiltInType();
	boolean changelogEnabled();
	Set<String> getViewNames();
	Set<String> getAllTraits();
	void registerDynamicMethod(final SchemaMethod method);

	// ----- static methods -----
	static Traits of(String name) {
		return TraitsImplementation.of(name);
	}

	static Set<PropertyKey> getPropertiesOfTrait(final String name) {
		return TraitsImplementation.getPropertiesOfTrait(name);
	}

	static Traits ofRelationship(String type1, String relType, String type2) {
		return TraitsImplementation.ofRelationship(type1, relType, type2);
	}

	static boolean exists(String name) {
		return TraitsImplementation.exists(name);
	}

	static Set<String> getAllTypes() {
		return TraitsImplementation.getAllTypes();
	}

	static Set<String> getAllTypes(Predicate<Traits> filter) {
		return TraitsImplementation.getAllTypes(filter);
	}

	static <T> PropertyKey<T> key(String type, String name) {
		return TraitsImplementation.key(type, name);
	}

	static PropertyKey<String> idProperty() {
		return TraitsImplementation.idProperty();
	}

	static PropertyKey<String> nameProperty() {
		return TraitsImplementation.nameProperty();
	}

	static PropertyKey<String> typeProperty() {
		return TraitsImplementation.typeProperty();
	}

	static PropertyKey<Date> createdDateProperty() {
		return TraitsImplementation.createdDateProperty();
	}

	static Set<String> getAllViews() {
		return TraitsImplementation.getAllViews();
	}

	static Map<String, Map<String, PropertyKey>> removeDynamicTypes() {
		return TraitsImplementation.removeDynamicTypes();
	}
}
