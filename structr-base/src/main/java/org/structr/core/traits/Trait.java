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

import org.structr.api.graph.PropertyContainer;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A named collection of properties and methods with a
 * factory that can instantiate implementations.
 */
public class Trait<T> {

	private static final Map<String, Trait<?>> availableTraits  = new LinkedHashMap<>();

	private Map<String, PropertyKey<?>> properties = new LinkedHashMap<>();
	private ImplementationFactory<T> factory       = null;
	private String name                            = null;

	Trait(final String name, final ImplementationFactory<T> factory) {

		this.name    = name;
		this.factory = factory;
	}

	void registerProperty(final PropertyKey<?> property) {
		properties.put(property.jsonName(), property);
	}

	boolean hasKey(final String name) {
		return properties.containsKey(name);
	}

	// ----- public methods -----
	public T getImplementation(final GraphTrait obj) {
		return factory.createImplementation(obj.getPropertyContainer());
	}

	public T getImplementation(final PropertyContainer obj) {
		return factory.createImplementation(obj);
	}

	public T getImplementation() {
		return factory.createImplementation(null);
	}

	public <T> PropertyKey<T> key(final String name) {
		return (PropertyKey<T>)properties.get(name);
	}

	public String getName() {
		return name;
	}

	public Collection<PropertyKey<?>> getProperties() {
		return properties.values();
	}

	// ----- public static methods -----
	public static <T> Trait<T> of(final Class<T> type) {
		return Trait.of(type.getSimpleName());
	}

	public static <T> Trait<T> of(final String name) {
		return (Trait<T>) availableTraits.get(name);
	}

	static <S extends PropertyContainer, T> Trait<T> create(final Class<T> type, final ImplementationFactory<T> factory) {

		final String name = type.getSimpleName();

		if (availableTraits.containsKey(name)) {

			return (Trait<T>) availableTraits.get(name);
		}

		final Trait<T> newTrait = new Trait<T>(name, factory);

		availableTraits.put(name, newTrait);

		return newTrait;
	}
}
