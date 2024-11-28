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
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A named collection of properties and methods with a
 * factory that can instantiate implementations.
 */
public class Trait implements GraphObjectTrait {

	/**
		TODO: Traits replace Class in the new implementation, we
	        need to implement an abstraction for isValid and all the
	        lifecycle methods so they can be stored in a Trait object.

	        The JSON schema is a good candidate to look for clues..
	*/

	private static final Map<String, Trait> availableTraits  = new LinkedHashMap<>();

	private Map<String, PropertyKey> properties = new LinkedHashMap<>();
	private ImplementationFactory factory       = null;
	private String name                         = null;

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
	public Trait getImplementation(final PropertyContainer obj) {
		return factory.createImplementation(obj);
	}

	public Trait getImplementation() {
		return factory.createImplementation(null);
	}

	public <T> PropertyKey<T> key(final String name) {
		return (PropertyKey<T>)properties.get(name);
	}

	public String getName() {
		return name;
	}

	public Collection<PropertyKey> getProperties(final String view) {
		return properties.values();
	}

	// ----- interface GraphObjectTrait ------
	public boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
		return true;
	}

	public void onCreation(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
	}

	public void onModification(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
	}

	public void onDeletion(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
	}

	public void afterCreation(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException {
	}

	public void afterModification(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException {
	}

	public void afterDeletion(final GraphObject obj, final SecurityContext securityContext, final PropertyMap properties) {
	}

	public void ownerModified(final GraphObject obj, final SecurityContext securityContext) {
	}

	public void securityModified(final GraphObject obj, final SecurityContext securityContext) {
	}

	public void locationModified(final GraphObject obj, final SecurityContext securityContext) {
	}

	public void propagatedModification(final GraphObject obj, final SecurityContext securityContext) {
	}

	// ----- public static methods -----
	public static Trait of(final String name) {
		return availableTraits.get(name);
	}
}
