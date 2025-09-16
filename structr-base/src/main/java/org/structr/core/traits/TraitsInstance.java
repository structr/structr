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

import org.structr.api.Predicate;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.definitions.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TraitsInstance {

	private final Map<String, Traits> globalTypeMap = new ConcurrentHashMap<>();
	private final Map<String, Trait> globalTraitMap = new ConcurrentHashMap<>();
	private final String name;

	public TraitsInstance(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "TraitsInstance(" + name + ", " + globalTypeMap.size() + ", " + globalTraitMap.size() + ")";
	}

	TraitsInstance createCopy(final String name) {

		final TraitsInstance newInstance = new TraitsInstance(name);

		for (Trait trait : globalTraitMap.values()) {
			newInstance.globalTraitMap.put(trait.getName(), trait.createCopy(newInstance));
		}

		for (Traits traits : globalTypeMap.values()) {
			newInstance.globalTypeMap.put(traits.getName(), traits.createCopy(newInstance));
		}

		return newInstance;
	}

	void registerType(final String name, final Traits traits) {
		globalTypeMap.put(name, traits);
	}

	Traits getType(final String name) {
		return globalTypeMap.get(name);
	}

	void registerTrait(final String name, final Trait traits) {
		globalTraitMap.put(name, traits);
	}

	void unregisterTraits(final Set<String> names) {
		globalTraitMap.keySet().removeAll(names);
	}

	public Traits getTraits(String name) {

		final Traits traits = globalTypeMap.get(name);
		if (traits != null) {

			return traits;
		}

		throw new RuntimeException("Missing trait definition for " + name + ".");
	}

	public Trait getTrait(final String type) {

		return globalTraitMap.get(type);
	}

	/**
	 * Returns the default set of property keys, which is
	 * id, type and name.
	 * @return
	 */
	public Set<PropertyKey> getDefaultKeys() {

		final Set<PropertyKey> keys = new LinkedHashSet<>();
		final Traits nodeTraits     = getTraits(StructrTraits.NODE_INTERFACE);

		keys.add(nodeTraits.key(GraphObjectTraitDefinition.ID_PROPERTY));
		keys.add(nodeTraits.key(GraphObjectTraitDefinition.TYPE_PROPERTY));
		keys.add(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

		return keys;
	}

	public Set<PropertyKey> getPropertiesOfTrait(final String name) {

		for (final Trait trait : globalTraitMap.values()) {

			if (name.equals(trait.getLabel())) {

				return new LinkedHashSet<>(trait.getPropertyKeys().values());
			}
		}

		return Set.of();
	}

	public Traits ofRelationship(String type1, String relType, String type2) {

		final Traits traits1 = getTraits(type1);
		final Traits traits2 = getTraits(type2);

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

	public boolean exists(final String name) {

		return globalTypeMap.containsKey(name);
	}

	public Set<String> getAllTypes() {

		return getAllTypes(null);
	}

	public Set<String> getAllTypes(final Predicate<Traits> filter) {

		final Set<String> types = new LinkedHashSet<>();

		for (final Traits trait : globalTypeMap.values()) {

			if (filter == null || filter.accept(trait)) {

				types.add(trait.getName());
			}
		}

		return types;
	}

	public <T> PropertyKey<T> key(final String type, final String name) {

		final Traits traits = getTraits(type);
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

	public Set<String> getAllViews() {

		final Set<String> allViews = new LinkedHashSet<>();

		for (final Traits traits : globalTypeMap.values()) {

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
	public Map<String, Map<String, PropertyKey>> getDynamicSchemaTypes() {

		final Map<String, Map<String, PropertyKey>> dynamicTypes = new LinkedHashMap<>();

		for (final Traits traits : globalTypeMap.values()) {

			dynamicTypes.putAll(traits.getDynamicTypes());
		}

		return dynamicTypes;
	}

	public void registerBaseType(final TraitDefinition definition) {

		final Traits traits = new TraitsImplementation(this, definition.getName(), true, false, false, false, false);

		traits.registerImplementation(definition, false);
	}

	public void registerNodeInterface() {

		final Traits traits = new TraitsImplementation(this, StructrTraits.NODE_INTERFACE, true, true, false, false, false);

		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
	}

	public void registerRelationshipInterface() {

		final Traits traits = new TraitsImplementation(this, StructrTraits.RELATIONSHIP_INTERFACE, true, false, true, false, false);

		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);
	}

	public void registerNodeType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new TraitsImplementation(this, typeName, true, true, false, false, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
		traits.registerImplementation(new AccessControllableTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, false);
		}
	}

	public void registerRelationshipType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new TraitsImplementation(this, typeName, true, false, true, false, false);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, false);
		}
	}

	public void registerDynamicNodeType(final String typeName, final boolean changelogEnabled, final boolean isServiceClass, final TraitDefinition... definitions) {

		Traits traits;

		// do not overwrite types
		if (getAllTypes(null).contains(typeName)) {

			// caution: this might return a relationship type..
			traits = getTraits(typeName);

		} else {

			traits = new TraitsImplementation(this, typeName, false, true, false, changelogEnabled, isServiceClass);

			// Node types consist of at least the following traits
			traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
			traits.registerImplementation(new GraphObjectTraitDefinition(), false);
			traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
			traits.registerImplementation(new AccessControllableTraitDefinition(), false);
		}

		// add implementation (allow extension of existing types)
		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, true);
		}
	}

	public void registerDynamicRelationshipType(final String typeName, final boolean changelogEnabled, final TraitDefinition... definitions) {

		// do not overwrite types
		if (getAllTypes(null).contains(typeName)) {
			return;
		}

		final Traits traits = new TraitsImplementation(this, typeName, false, false, true, changelogEnabled, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, true);
		}
	}
}
