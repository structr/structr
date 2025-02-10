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

import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.ScriptMethod;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.*;

public class Trait {

	private static final Set<String> DEFAULT_PROPERTY_KEYS = new LinkedHashSet<>(Arrays.asList("id", "type", "name"));

	private final Map<String, AbstractMethod> dynamicMethods                      = new LinkedHashMap<>();
	private final Map<Class, FrameworkMethod> frameworkMethods                    = new LinkedHashMap<>();
	private final Map<Class, NodeTraitFactory> nodeTraitFactories                 = new LinkedHashMap<>();
	private final Map<Class, RelationshipTraitFactory> relationshipTraitFactories = new LinkedHashMap<>();
	private final Map<Class, LifecycleMethod> lifecycleMethods                    = new LinkedHashMap<>();
	private final Map<String, PropertyKey> propertyKeys                           = new LinkedHashMap<>();
	private final Map<String, Set<String>> views                                  = new LinkedHashMap<>();

	private final Relation relation;
	private final String name;
	private final boolean isRelationship;
	private final boolean isDynamic;

	public Trait(final TraitDefinition traitDefinition, final boolean isDynamic) {

		this.name           = traitDefinition.getName();
		this.isRelationship = traitDefinition.isRelationship();
		this.isDynamic      = isDynamic;
		this.relation       = traitDefinition.getRelation();

		initializeFrom(traitDefinition);
	}

	public final void initializeFrom(final TraitDefinition traitDefinition) {

		// properties need to be registered first so they are available in lifecycle methods etc.
		for (final PropertyKey key : traitDefinition.getPropertyKeys()) {

			final String name = key.jsonName();

			// register property key
			propertyKeys.put(name, key);

			// set declaring trait
			key.setDeclaringTrait(this);

			// add key to "all" view
			this.views.computeIfAbsent("all", k -> new LinkedHashSet<>()).add(name);

			// add dynamic keys to "custom" view
			if (key.isDynamic() || DEFAULT_PROPERTY_KEYS.contains(name)) {
				this.views.computeIfAbsent("custom", k -> new LinkedHashSet<>()).add(name);
			}
		}

		lifecycleMethods.putAll(traitDefinition.getLifecycleMethods());
		frameworkMethods.putAll(traitDefinition.getFrameworkMethods());

		// dynamic methods
		for (final AbstractMethod method : traitDefinition.getDynamicMethods()) {
			this.dynamicMethods.put(method.getName(), method);
		}

		// views
		for (final Map.Entry<String, Set<String>> views : traitDefinition.getViews().entrySet()) {

			final Set<String> set = this.views.computeIfAbsent(views.getKey(), k -> new LinkedHashSet<>());

			set.addAll(views.getValue());
		}


		// trait implementations
		this.nodeTraitFactories.putAll(traitDefinition.getNodeTraitFactories());
		this.relationshipTraitFactories.putAll(traitDefinition.getRelationshipTraitFactories());
	}

	@Override
	public String toString() {
		return "Trait(" + getName() + ")";
	}

	public String getName() {
		return this.name;
	}

	public Set<String> getPropertyKeysForView(final String viewName) {
		return views.get(viewName);
	}

	public <T extends LifecycleMethod> T getLifecycleMethod(final Class<T> type) {
		return (T) lifecycleMethods.get(type);
	}

	public <T extends FrameworkMethod> T getFrameworkMethod(final Class<T> type) {
		return (T) frameworkMethods.get(type);
	}

	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return relationshipTraitFactories;
	}

	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return nodeTraitFactories;
	}

	public Map<String, AbstractMethod> getDynamicMethods() {
		return dynamicMethods;
	}

	public Set<String> getViewNames() {
		return views.keySet();
	}

	public Map<String, Set<String>> getViews() {
		return views;
	}

	public Map<String, PropertyKey> getPropertyKeys() {
		return propertyKeys;
	}

	public Relation getRelation() {
		return relation;
	}

	public boolean isRelationship() {
		return isRelationship;
	}

	public void registerDynamicMethod(final SchemaMethod method) {

		if (method.isLifecycleMethod()) {

			final Class<LifecycleMethod> type = method.getMethodType();
			if (type != null) {

				lifecycleMethods.put(type, method.asLifecycleMethod());

			} else {

				// report error?
			}

		} else {

			dynamicMethods.put(method.getName(), new ScriptMethod(method));
		}
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public void registerDynamicProperty(final SchemaProperty schemaProperty) throws FrameworkException {

		final PropertyKey key = schemaProperty.createKey(null);
		if (key != null) {

			propertyKeys.put(key.jsonName(), key);
		}
	}
}
