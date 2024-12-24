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
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.definitions.TraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.*;

/**
 * A named collection of traits that a node can have.
 */
public class Traits {

	private static final Map<String, Traits> globalTraitMap    = new LinkedHashMap<>();
	private static PropertyKey<Date> cachedCreatedDateProperty = null;
	private static PropertyKey<String> cachedNameProperty      = null;
	private static PropertyKey<String> cachedTypeProperty      = null;
	private static PropertyKey<String> cachedIdProperty        = null;

	private final Map<String, TraitDefinition> types                              = new LinkedHashMap<>();
	private final Map<String, AbstractMethod> dynamicMethods                      = new LinkedHashMap<>();
	private final Map<Class, FrameworkMethod> overwritableMethods                 = new LinkedHashMap<>();
	private final Map<Class, NodeTraitFactory> nodeTraitFactories                 = new LinkedHashMap<>();
	private final Map<Class, RelationshipTraitFactory> relationshipTraitFactories = new LinkedHashMap<>();
	private final Map<Class, Set> composableMethods                               = new LinkedHashMap<>();
	private final Map<String, PropertyKey> propertyKeys                           = new LinkedHashMap<>();
	private final Map<String, Set<String>> views                                  = new LinkedHashMap<>();

	private final boolean isNodeType;
	private final String typeName;
	private final boolean isRelationshipType;
	private Relation relation;

	Traits(final String typeName, final boolean isNodeType, final boolean isRelationshipType) {

		this.typeName           = typeName;
		this.isNodeType         = isNodeType;
		this.isRelationshipType = isRelationshipType;

		globalTraitMap.put(typeName, this);
	}

	@Deprecated
	/**
	 * Marked as deprecated to find problems in migrated code.
	 */
	public boolean equals(final Object o) {
		return false;
	}

	public Set<String> getLabels() {
		return Collections.unmodifiableSet(types.keySet());
	}

	public boolean contains(final String type) {
		return types.containsKey(type);
	}

	public TraitDefinition get(final String type) {
		return types.get(type);
	}

	public <T> PropertyKey<T> key(final String name) {
		return propertyKeys.get(name);
	}

	public String getName() {
		return typeName;
	}

	public boolean isNodeType() {
		return isNodeType;
	}

	public boolean isRelationshipType() {
		return isRelationshipType;
	}

	/**
	 * Returns the default set of property keys, which is
	 * id, type and name.
	 * @return
	 */
	public Set<PropertyKey> getDefaultKeys() {

		final Set<PropertyKey> keys = new LinkedHashSet<>();

		keys.add(propertyKeys.get("id"));
		keys.add(propertyKeys.get("type"));
		keys.add(propertyKeys.get("name"));

		return keys;
	}

	/**
	 * Returns the combined property set of all traits that
	 * this type contains.
	 *
	 * @return
	 */
	public Set<PropertyKey> getAllPropertyKeys() {

		final Set<PropertyKey> set = new LinkedHashSet<>();

		for (final PropertyKey key : propertyKeys.values()) {

			set.add(key);
		}

		return set;
	}

	/**
	 * Returns the combined property set of all traits that
	 * this type contains.
	 *
	 * @return
	 */
	public Set<PropertyKey> getPropertyKeysForView(final String propertyView) {

		final Set<String> view = views.get(propertyView);
		if (view != null) {

			return new LinkedHashSet<>(view.stream().map(propertyKeys::get).toList());
		}

		return Set.of();
	}

	public <T> Set<T> getMethods(final Class<T> type) {

		final Set<T> methods = composableMethods.get(type);
		if (methods != null) {

			return methods;
		}

		return Collections.EMPTY_SET;
	}

	public <T extends FrameworkMethod> T getMethod(final Class<T> type) {
		return (T) overwritableMethods.get(type);
	}

	public Map<String, AbstractMethod> getDynamicMethods() {
		return dynamicMethods;
	}

	public <T> T as(final Class<T> type, final GraphObject obj) {

		if (obj.isNode()) {

			final NodeTraitFactory factory = nodeTraitFactories.get(type);
			if (factory != null) {

				return (T) factory.newInstance(this, (NodeInterface) obj);
			}

		} else {

			final RelationshipTraitFactory factory = relationshipTraitFactories.get(type);
			if (factory != null) {

				return (T) factory.newInstance(this, (RelationshipInterface) obj);
			}

		}

		return null;
	}

	public void registerImplementation(final TraitDefinition trait) {

		// composable methods (like callbacks etc.)
		for (final Map.Entry<Class, LifecycleMethod> entry : trait.getLifecycleMethods().entrySet()) {

			final Class type = entry.getKey();

			composableMethods.computeIfAbsent(type, k -> new LinkedHashSet()).add(entry.getValue());
		}

		// overwritable methods
		for (final Map.Entry<Class, FrameworkMethod> entry : trait.getFrameworkMethods().entrySet()) {

			final Class type             = entry.getKey();
			final FrameworkMethod method = entry.getValue();
			final FrameworkMethod parent = overwritableMethods.put(type, method);

			// replace currently registered implementation and install as super implementation
			if (parent != null) {

				method.setSuper(parent);
			}
		}

		// dynamic methods
		for (final AbstractMethod method : trait.getDynamicMethods()) {
			this.dynamicMethods.put(method.getName(), method);
		}

		// properties
		for (final PropertyKey key : trait.getPropertyKeys()) {

			propertyKeys.put(key.jsonName(), key);

			key.setDeclaringTrait(trait);
		}

		// views


		// trait implementations
		this.nodeTraitFactories.putAll(trait.getNodeTraitFactories());
		this.relationshipTraitFactories.putAll(trait.getRelationshipTraitFactories());

		// relation (for relationship types)
		this.relation = trait.getRelation();
	}

	public Relation getRelation() {
		return relation;
	}

	public Set<TraitDefinition> getTraits() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(types.values()));
	}

	// ----- static methods -----
	public static Traits of(final String name) {

		final Traits traits = globalTraitMap.get(name);
		if (traits != null) {

			return traits;
		}

		throw new RuntimeException("Missing trait definition for " + name + ".");
	}

	public static Set<String> getAllTypes(final Predicate<Traits> filter) {

		final Set<String> types = new LinkedHashSet<>();

		for (final Traits trait : globalTraitMap.values()) {

			if (filter == null || filter.accept(trait)) {

				types.add(trait.getName());
			}
		}

		return types;
	}

	public static <T> PropertyKey<T> key(final String type, final String name) {

		final Traits traits = Traits.of(type);
		if (traits != null) {

			return traits.key(name);
		}

		// fixme
		return null;
	}

	public static PropertyKey<String> idProperty() {

		if (cachedIdProperty == null) {
			cachedIdProperty = Traits.key("GraphObject", "id");
		}

		return cachedIdProperty;
	}

	public static PropertyKey<String> nameProperty() {

		if (cachedNameProperty == null) {
			cachedNameProperty = Traits.key("NodeInterface", "name");
		}

		return cachedNameProperty;
	}

	public static PropertyKey<String> typeProperty() {

		if (cachedTypeProperty == null) {
			cachedTypeProperty = Traits.key("GraphObject", "type");
		}

		return cachedTypeProperty;
	}

	public static PropertyKey<Date> createdDateProperty() {

		if (cachedCreatedDateProperty == null) {
			cachedCreatedDateProperty = Traits.key("GraphObject", "createdDate");
		}

		return cachedCreatedDateProperty;
	}

	public boolean isInterface() {
		return false;
	}

	public boolean isAbstract() {
		return false;
	}

	public Set<String> getViewNames() {

		// TODO: implement, as unmodifiable collection!
		return Set.of();
	}

	public static Set<String> getAllViews() {
		return Set.of();
	}

	public Set<String> getAllTraits() {
		return Collections.unmodifiableSet(types.keySet());
	}
}
