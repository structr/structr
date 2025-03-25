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
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.*;

/**
 * A named collection of traits that a node can have.
 */
public class TraitsImplementation implements Traits {

	private static final Map<String, Traits> globalTypeMap = new LinkedHashMap<>();
	private static final Map<String, Trait> globalTraitMap = new LinkedHashMap<>();

	private final Map<String, TraitDefinition> traits                   = new LinkedHashMap<>();
	private final Map<String, PropertyKey> keyCache                     = new LinkedHashMap<>();
	private final Map<Class, FrameworkMethod> frameworkMethodCache      = new LinkedHashMap<>();
	private final Map<Class, Set<LifecycleMethod>> lifecycleMethodCache = new LinkedHashMap<>();
	private final Set<Trait> localTraitsCache                           = new LinkedHashSet<>();
	private Map<String, AbstractMethod> dynamicMethodCache              = null;

	private final boolean isNodeType;
	private final boolean isRelationshipType;
	private final boolean isBuiltInType;
	private final boolean isServiceClass;
	private final boolean changelogEnabled;
	private final String typeName;

	TraitsImplementation(final String typeName, final boolean isBuiltInType, final boolean isNodeType, final boolean isRelationshipType, final boolean changelogEnabled, final boolean isServiceClass) {

		this.typeName           = typeName;
		this.isNodeType         = isNodeType;
		this.isBuiltInType      = isBuiltInType;
		this.isRelationshipType = isRelationshipType;
		this.changelogEnabled   = changelogEnabled;
		this.isServiceClass     = isServiceClass;

		globalTypeMap.put(typeName, this);
	}

	@Deprecated
	/**
	 * Marked as deprecated to find problems in migrated code.
	 */
	public boolean equals(final Object o) {
		return false;
	}

	@Override
	public Set<String> getLabels() {

		final Set<String> labels = new LinkedHashSet<>();

		for (final Trait trait : getTraits()) {

			labels.add(trait.getLabel());
		}

		return labels;
	}

	@Override
	public boolean contains(final String type) {
		return getLabels().contains(type);
	}

	@Override
	public <T> PropertyKey<T> key(final String name) {
		return key(name, true);
	}

	private <T> PropertyKey<T> key(final String name, final boolean throwException) {

		PropertyKey<T> key = keyCache.get(name);
		if (key != null) {

			return key;
		}

		for (final Trait trait : getTraits()) {

			final Map<String, PropertyKey> keys = trait.getPropertyKeys();
			if (keys.containsKey(name)) {

				key = keys.get(name);
			}
		}

		// return last key, not first
		if (key != null) {

			keyCache.put(name, key);

			return key;
		}

		if (throwException) {
			throw new RuntimeException("Missing property key " + name + " of type " + typeName);
		}

		return null;
	}

	@Override
	public boolean hasKey(final String name) {
		return key(name, false) != null;
	}

	@Override
	public boolean hasDynamicMethod(final String name) {
		return false;
	}

	@Override
	public String getName() {
		return typeName;
	}

	@Override
	public boolean isNodeType() {
		return isNodeType;
	}

	@Override
	public boolean isRelationshipType() {
		return isRelationshipType;
	}

	@Override
	public boolean isBuiltinType() {
		return isBuiltInType;
	}

	/**
	 * Returns the combined property set of all traits that
	 * this type contains.
	 *
	 * @return
	 */
	@Override
	public Set<PropertyKey> getAllPropertyKeys() {

		final Set<PropertyKey> set = new LinkedHashSet<>();

		for (final Trait trait : getTraits()) {

			for (final PropertyKey key : trait.getPropertyKeys().values()) {

				// make sure that dynamic properties (which appear later in this loop) overwrite existing properties
				if (set.contains(key)) {
					set.remove(key);
				}

				set.add(key);
			}
		}

		return set;
	}

	/**
	 * Returns the combined property set of all traits that
	 * this type contains.
	 *
	 * @return
	 */
	@Override
	public Set<PropertyKey> getPropertyKeysForView(final String propertyView) {

		final LinkedHashSet<PropertyKey> set = new LinkedHashSet<>();

		for (final Trait trait : getTraits()) {

			final Set<String> names = trait.getPropertyKeysForView(propertyView);
			if (names != null) {

				for (final String keyName : names) {

					// do not throw exception here
					final PropertyKey key = key(keyName, false);
					if (key != null) {

						set.add(key);
					}
				}
			}
		}

		return set;
	}

	@Override
	public <T extends LifecycleMethod> Set<T> getMethods(final Class<T> type) {

		Set<T> methods = (Set) lifecycleMethodCache.get(type);
		if (methods != null) {

			return methods;
		}

		methods = new LinkedHashSet<>();

		for (final Trait trait : getTraits()) {

			final T method    = trait.getLifecycleMethod(type);
			if (method != null) {

				methods.add(method);
			}
		}

		// store in cache
		lifecycleMethodCache.put(type, (Set)methods);

		return methods;
	}

	@Override
	public <T extends FrameworkMethod> T getMethod(final Class<T> type) {

		T current = (T) frameworkMethodCache.get(type);

		if (current != null) {
			return current;
		}

		for (final Trait trait : getTraits()) {

			final T method = trait.getFrameworkMethod(type);
			if (method != null) {

				if (current != null) {

					method.setSuper(current);
				}

				current = method;
			}
		}

		if (current != null) {

			frameworkMethodCache.put(type, current);
		}

		return current;
	}

	@Override
	public Map<String, AbstractMethod> getDynamicMethods() {

		if (dynamicMethodCache != null) {
			return dynamicMethodCache;
		}

		dynamicMethodCache = new LinkedHashMap<>();

		for (final Trait trait : getTraits()) {

			// this is the place where we can detect clashes!
			dynamicMethodCache.putAll(trait.getDynamicMethods());
		}

		return dynamicMethodCache;
	}

	@Override
	public <T> T as(final Class<T> type, final GraphObject obj) {

		if (obj.isNode()) {

			for (final Trait trait : getTraits()) {

				final Map<Class, NodeTraitFactory> factories = trait.getNodeTraitFactories();
				final NodeTraitFactory factory               = factories.get(type);

				if (factory != null) {

					return (T) factory.newInstance(this, (NodeInterface) obj);
				}
			}

		} else {

			for (final Trait trait : getTraits()) {

				final Map<Class, RelationshipTraitFactory> factories = trait.getRelationshipTraitFactories();
				final RelationshipTraitFactory factory               = factories.get(type);

				if (factory != null) {

					return (T) factory.newInstance(this, (RelationshipInterface) obj);
				}
			}
		}

		throw new RuntimeException("Type " + this.typeName + " does not define a factory for " + type.getSimpleName());
	}

	@Override
	public Relation getRelation() {

		for (final Trait trait : getTraits()) {

			final Relation rel = trait.getRelation();
			if (rel != null) {

				return rel;
			}
		}

		return null;
	}

	@Override
	public Set<TraitDefinition> getTraitDefinitions() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(traits.values()));
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public boolean isBuiltInType() {
		return isBuiltInType;
	}

	@Override
	public boolean isServiceClass() {
		return isServiceClass;
	}

	@Override
	public boolean changelogEnabled() {
		return changelogEnabled;
	}

	@Override
	public Set<String> getViewNames() {

		final Set<String> names = new LinkedHashSet<>();

		for (final Trait trait : getTraits()) {

			names.addAll(trait.getViewNames());
		}

		return names;
	}

	@Override
	public Set<String> getAllTraits() {
		return getLabels();
	}

	@Override
	public synchronized void registerImplementation(final TraitDefinition traitDefinition, final boolean isDynamic) {

		final String name  = traitDefinition.getName();
		Trait trait        = globalTraitMap.get(name);

		if (trait == null) {

			trait = new Trait(traitDefinition, isDynamic);
			globalTraitMap.put(name, trait);
		}

		traits.put(name, traitDefinition);
	}

	@Override
	public Map<String, Map<String, PropertyKey>> removeDynamicTraits() {

		// clear caches
		keyCache.clear();
		frameworkMethodCache.clear();
		lifecycleMethodCache.clear();
		localTraitsCache.clear();
		dynamicMethodCache = null;

		final Map<String, Map<String, PropertyKey>> removedProperties = new LinkedHashMap<>();
		final Set<String> traitsToRemove                              = new LinkedHashSet<>();

		for (final Trait trait : getTraits()) {

			final String traitName = trait.getName();
			String indexName = traitName;

			// Use relationship type instead of the type name for relationships
			// because the return value is for index update purposes.
			if (trait.isRelationship()) {

				final Relation relation = trait.getRelation();
				if (relation != null) {

					indexName = relation.name();
				}
			}

			if (trait.isDynamic()) {

				// dynamic trait => we can remove all property keys
				removedProperties.computeIfAbsent(indexName, k -> new LinkedHashMap<>()).putAll(trait.getPropertyKeys());

				// mark trait for removal
				traitsToRemove.add(traitName);
			}
		}

		traits.keySet().removeAll(traitsToRemove);

		// remove all traits from global map
		globalTraitMap.keySet().removeAll(traitsToRemove);

		return removedProperties;
	}

	// ----- private methods -----
	private Set<Trait> getTraits() {

		if (!localTraitsCache.isEmpty()) {
			return localTraitsCache;
		}

		final Set<Trait> localTraitsCache = new LinkedHashSet<>();

		for (final String name : traits.keySet()) {

			final Trait trait = globalTraitMap.get(name);
			if (trait != null) {

				localTraitsCache.add(trait);
			}
		}

		return localTraitsCache;
	}

	// ----- static methods -----
	static Traits of(String name) {

		final Traits traits = TraitsImplementation.globalTypeMap.get(name);
		if (traits != null) {

			return traits;
		}

		throw new RuntimeException("Missing trait definition for " + name + ".");
	}

	public static Trait getTrait(final String type) {
		return globalTraitMap.get(type);
	}

	/**
	 * Returns the default set of property keys, which is
	 * id, type and name.
	 * @return
	 */
	public static Set<PropertyKey> getDefaultKeys() {

		final Set<PropertyKey> keys = new LinkedHashSet<>();
		final Traits nodeTraits     = Traits.of(StructrTraits.NODE_INTERFACE);

		keys.add(nodeTraits.key(GraphObjectTraitDefinition.ID_PROPERTY));
		keys.add(nodeTraits.key(GraphObjectTraitDefinition.TYPE_PROPERTY));
		keys.add(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

		return keys;
	}

	static Set<PropertyKey> getPropertiesOfTrait(final String name) {

		for (final Trait trait : globalTraitMap.values()) {

			if (name.equals(trait.getLabel())) {

				return new LinkedHashSet<>(trait.getPropertyKeys().values());
			}
		}

		return Set.of();
	}

	static Traits ofRelationship(String type1, String relType, String type2) {

		final Traits traits1 = Traits.of(type1);
		final Traits traits2 = Traits.of(type2);

		for (final Traits traits : globalTypeMap.values()) {

			if (traits.isRelationshipType()) {

				final Relation relation = traits.getRelation();
				if (relation != null) {

					final String sourceType   = relation.getSourceType();
					final String targetType   = relation.getTargetType();
					final String relationType = relation.name();

					if (traits1.contains(sourceType) && traits2.contains(targetType) && relationType.equals(relType)) {

						return traits;
					}
				}
			}
		}

		return null;
	}

	static boolean exists(final String name) {
		return TraitsImplementation.globalTypeMap.containsKey(name);
	}

	static Set<String> getAllTypes() {
		return getAllTypes(null);
	}

	static Set<String> getAllTypes(final Predicate<Traits> filter) {

		final Set<String> types = new LinkedHashSet<>();

		for (final Traits trait : TraitsImplementation.globalTypeMap.values()) {

			if (filter == null || filter.accept(trait)) {

				types.add(trait.getName());
			}
		}

		return types;
	}

	static <T> PropertyKey<T> key(final String type, final String name) {

		final Traits traits = Traits.of(type);
		if (traits != null) {

			final PropertyKey<T> key = traits.key(name);

			if (key != null) {

				return key;
			}

			throw new RuntimeException("Invalid key " + type + "." + name + " requested!");
		}

		// fixme
		return null;
	}

	static Set<String> getAllViews() {

		final Set<String> allViews = new LinkedHashSet<>();

		for (final Traits traits : TraitsImplementation.globalTypeMap.values()) {

			traits.getViewNames().forEach(allViews::add);
		}

		return allViews;
	}

	/**
	 * Removes all dynamic types and returns a map with the names and property keys.
	 * Note: this is a special method for Structr's index updater, so we return the
	 * relationship type name for relationship types.
	 *
	 * @return
	 */
	static Map<String, Map<String, PropertyKey>> clearDynamicSchema() {

		final Map<String, Map<String, PropertyKey>> removedClasses = new LinkedHashMap<>();

		for (final Iterator<Traits> it = TraitsImplementation.globalTypeMap.values().iterator(); it.hasNext(); ) {

			final Traits traits = it.next();

			removedClasses.putAll(traits.removeDynamicTraits());

			if (!traits.isBuiltInType()) {

				// remove this type from the global map
				it.remove();
			}
		}

		return removedClasses;
	}
}
