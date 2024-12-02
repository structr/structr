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

import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.*;

/**
 * A named collection of traits that a node can have.
 */
public class Traits {

	private static final Map<String, Trait> types = new LinkedHashMap<>();

	private final Map<Class, FrameworkMethod> overwritableMethods = new LinkedHashMap<>();
	private final Map<Class, Set> composableMethods                     = new LinkedHashMap<>();
	private final Map<String, PropertyKey> propertyKeys                 = new LinkedHashMap<>();

	private final boolean isNodeType;
	private final String typeName;

	Traits(final String typeName, final boolean isNodeType) {

		this.typeName   = typeName;
		this.isNodeType = isNodeType;
	}

	public Set<String> getLabels() {
		return Collections.unmodifiableSet(types.keySet());
	}

	public boolean contains(final String type) {
		return types.containsKey(type);
	}

	public Trait get(final String type) {
		return types.get(type);
	}

	public <T> PropertyKey<T> key(final String name) {

		for (final Trait trait : types.values()) {

			if (trait.hasKey(name)) {
				return trait.key(name);
			}
		}

		return null;
	}

	public String getName() {
		return typeName;
	}

	public boolean isNodeType() {
		return isNodeType;
	}

	public Set<PropertyKey> getPropertySet(final String propertyView) {

		final Set<PropertyKey> set = new LinkedHashSet<>();

		for (final Trait trait : types.values()) {

			set.addAll(trait.getPropertyKeys(propertyView));
		}

		return set;
	}

	public <T> Set<T> getMethods(final Class<T> type) {
		return composableMethods.get(type);
	}

	public <T extends FrameworkMethod> T getMethod(final Class<T> type) {
		return (T) overwritableMethods.get(type);
	}

	public void registerImplementation(final Trait trait) {

		// composable methods (like callbacks etc.)
		for (final LifecycleMethod operation : trait.getLifecycleMethods()) {

			final Class type = operation.getClass();

			composableMethods.computeIfAbsent(type, k -> new LinkedHashSet()).add(operation);
		}

		// overwritable methods
		for (final FrameworkMethod operation : trait.getFrameworkMethods()) {

			final Class type                   = operation.getClass();
			final FrameworkMethod parent = overwritableMethods.put(type, operation);

			// replace currently registered implementation and install as super implementation
			if (parent != null) {

				operation.setSuper(parent);
			}
		}

		// properties
		for (final PropertyKey key : trait.getPropertyKeys()) {

			propertyKeys.put(key.jsonName(), key);
		}
	}

	// ----- static methods -----
	public static Traits of(final String name) {
		//return typeToTraitsMap.get(name);
		return null;
	}
}
