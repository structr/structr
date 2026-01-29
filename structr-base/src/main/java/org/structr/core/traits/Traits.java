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

import org.structr.api.Predicate;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.docs.Documentable;

import java.util.Map;
import java.util.Set;

public interface Traits extends Documentable {

	Set<String> getLabels();
	boolean contains(final String type);
	<T> PropertyKey<T> key(final String name);
	<T> PropertyKey<T> keyOrGenericProperty(final String name);
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
	Map<String, Map<String, PropertyKey>> getDynamicTypes();
	Relation getRelation();
	Set<TraitDefinition> getTraitDefinitions();
	boolean isInterface();
	boolean isAbstract();
	boolean isServiceClass();
	boolean changelogEnabled();
	Set<String> getViewNames();
	Set<String> getAllTraits();
	boolean isBuiltinType();
	Traits createCopy(final TraitsInstance traitsInstance);

	// ----- static methods -----
	static Traits of(final String name) {
		return TraitsManager.getCurrentInstance().getType(name);
	}

	static Trait getTrait(final String name) {
		return TraitsManager.getCurrentInstance().getTrait(name);
	}

	static Set<PropertyKey> getPropertiesOfTrait(final String name) {
		return TraitsManager.getCurrentInstance().getPropertiesOfTrait(name);
	}

	static Set<PropertyKey> getDefaultKeys() {
		return TraitsManager.getCurrentInstance().getDefaultKeys();
	}

	static Traits ofRelationship(final String type1, final String relType, final String type2) {
		return TraitsManager.getCurrentInstance().ofRelationship(type1, relType, type2);
	}

	/**
	 * Indicates whether a type (not a trait!) with the given name exists.
	 *
	 * @param name
	 * @return
	 */
	static boolean exists(final String name) {
		return TraitsManager.getCurrentInstance().exists(name);
	}

	static Set<String> getAllTypes() {
		return TraitsManager.getCurrentInstance().getAllTypes();
	}

	static Set<String> getAllTypes(final Predicate<Traits> filter) {
		return TraitsManager.getCurrentInstance().getAllTypes(filter);
	}

	static <T> PropertyKey<T> key(final String type, final String name) {
		return TraitsManager.getCurrentInstance().key(type, name);
	}

	static Set<String> getAllViews() {
		return TraitsManager.getCurrentInstance().getAllViews();
	}
}
