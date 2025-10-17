/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A named collection of traits that a node can have.
 */
public class TraitsImplementation implements Traits {

	private final TraitsInstance traitsInstance;
	private final Set<String> traitNames                                = new LinkedHashSet<>();
	private final Set<Trait> traits                                     = new LinkedHashSet<>();
	private final Map<String, Wrapper<PropertyKey>> keyCache            = new LinkedHashMap<>();
	private final Map<Class, FrameworkMethod> frameworkMethodCache      = new LinkedHashMap<>();
	private final Map<Class, Set<LifecycleMethod>> lifecycleMethodCache = new LinkedHashMap<>();
	private final Map<Class, NodeTraitFactory> nodeTraitFactoryCache    = new LinkedHashMap<>();
	private Map<String, AbstractMethod> dynamicMethodCache              = null;
	private Wrapper<Relation> cachedRelation                            = null;

	private final boolean isNodeType;
	private final boolean isRelationshipType;
	private final boolean isBuiltInType;
	private final boolean isServiceClass;
	private final boolean changelogEnabled;
	private final String typeName;

	TraitsImplementation(final TraitsInstance traitsInstance, final String typeName, final boolean isBuiltInType, final boolean isNodeType, final boolean isRelationshipType, final boolean changelogEnabled, final boolean isServiceClass) {

		this.traitsInstance     = traitsInstance;
		this.typeName           = typeName;
		this.isNodeType         = isNodeType;
		this.isBuiltInType      = isBuiltInType;
		this.isRelationshipType = isRelationshipType;
		this.changelogEnabled   = changelogEnabled;
		this.isServiceClass     = isServiceClass;
	}

	public TraitsImplementation createCopy(final TraitsInstance traitsInstance) {

		final TraitsImplementation copy = new TraitsImplementation(traitsInstance, typeName, isBuiltInType, isNodeType, isRelationshipType, changelogEnabled, isServiceClass);

		copy.traitNames.addAll(traitNames);
		copy.traits.addAll(traits);

		return copy;
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

		// use wrapper to cache null values as well
		Wrapper<PropertyKey> wrapper = keyCache.get(name);
		if (wrapper != null) {

			return wrapper.value;
		}

		PropertyKey<T> key = null;

		for (final Trait trait : getTraits()) {

			final Map<String, PropertyKey> keys = trait.getPropertyKeys();
			if (keys.containsKey(name)) {

				key = keys.get(name);
			}
		}

		// FIXME: If this stays here... it leads to the effect, that at first attempt we get an error message and afterward
		//        we NEVER AGAIN get an error message because the cached value (even if useless) is being used.
		//        BUT: If we move the cache-add into the next if statement, we always get an error, even if we were to allow unknown keys via a setting
		keyCache.put(name, new Wrapper(key));

		// return last key, not first
		if (key != null) {

			return key;
		}

		if (throwException) {
			throw new RuntimeException("Missing property key '" + name + "' of type '" + typeName + "'.");
		}

		return null;
	}

	@Override
	public boolean hasKey(final String name) {
		return key(name, false) != null;
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

			final NodeTraitFactory cachedFactory = nodeTraitFactoryCache.get(type);
			if (cachedFactory != null) {

				return (T) cachedFactory.newInstance(this, (NodeInterface) obj);
			}

			for (final Trait trait : getTraits()) {

				final Map<Class, NodeTraitFactory> factories = trait.getNodeTraitFactories();
				final NodeTraitFactory factory               = factories.get(type);

				if (factory != null) {

					nodeTraitFactoryCache.put(type, factory);

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

		if (cachedRelation != null) {

			return cachedRelation.value;
		}

		for (final Trait trait : getTraits()) {

			final Relation rel = trait.getRelation();
			if (rel != null) {

				cachedRelation = new Wrapper(rel);

				return rel;
			}
		}

		// cache null as well
		cachedRelation = new Wrapper(null);

		return null;
	}

	@Override
	public Set<TraitDefinition> getTraitDefinitions() {

		final Set<TraitDefinition> set = new LinkedHashSet<>();

		for (final Trait trait : traits) {

			set.add(trait.getDefinition());
		}

		return set;
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
	public Map<String, Map<String, PropertyKey>> getDynamicTypes() {

		final Map<String, Map<String, PropertyKey>> dynamicProperties = new LinkedHashMap<>();
		final Set<String> dynamicTraits                               = new LinkedHashSet<>();

		for (final Trait trait : traits) {

			String indexName = trait.getName();

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
				dynamicProperties.computeIfAbsent(indexName, k -> new LinkedHashMap<>()).putAll(trait.getPropertyKeys());

				// mark trait for removal
				dynamicTraits.add(trait.getName());
			}
		}

		return dynamicProperties;
	}

	// ----- private methods -----
	private Set<Trait> getTraits() {
		return traits;
	}

	public void addTrait(final String trait) {
		traitNames.add(trait);
	}

	public void resolveTraits() {

		traits.clear();

		final Set<String> resolvedTraits = new LinkedHashSet<>();
		final Set<String> seenTraits     = new LinkedHashSet<>();

		for (final String traitName : traitNames) {

			// depth-first
			recurse(resolvedTraits, seenTraits, traitName, 0);
		}

		for (final String name : resolvedTraits) {

			final Trait resolvedTrait = traitsInstance.getTrait(name);
			if (resolvedTrait != null) {

				traits.add(resolvedTrait);
			}
		}
	}

	private void recurse(final Set<String> resolvedTraits, final Set<String> seenTraits, final String name, final int depth) {

		if (!seenTraits.add(name)) {
			return;
		}

		if (traitsInstance.exists(name)) {

			final TraitsImplementation impl = (TraitsImplementation) traitsInstance.getTraits(name);

			for (final String trait : impl.traitNames) {

				recurse(resolvedTraits, seenTraits, trait, depth + 1);
			}
		}

		resolvedTraits.add(name);
	}

	class Wrapper<T> {

		T value;

		public Wrapper(final T value) {
			this.value = value;
		}
	}
}
