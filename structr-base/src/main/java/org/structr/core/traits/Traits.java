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
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.*;

/**
 * A named collection of traits that a node can have.
 */
public class Traits implements GraphObjectTrait {

	private static final Map<String, Traits> typeToTraitsMap = new TreeMap<>();

	private final Map<String, Trait> traits = new LinkedHashMap<>();
	private final boolean isNodeType;
	private final String type;

	private Traits(final String type, final boolean isNodeType) {

		this.type       = type;
		this.isNodeType = isNodeType;
	}

	public Set<String> getLabels() {
		return Collections.unmodifiableSet(traits.keySet());
	}

	public boolean contains(final String type) {
		return traits.containsKey(type);
	}

	public Trait get(final String type) {
		return traits.get(type);
	}

	public <T> PropertyKey<T> key(final String name) {

		for (final Trait trait : traits.values()) {

			if (trait.hasKey(name)) {
				return trait.key(name);
			}
		}

		return null;
	}

	public String getName() {
		return type;
	}

	public Set<PropertyKey> getPropertySet(final String view) {

		final Set<PropertyKey> keys = new LinkedHashSet<>();

		for (final Trait trait : traits.values()) {

			keys.addAll(trait.getProperties(view));
		}

		return keys;
	}

	public boolean isNodeType() {
		return isNodeType;
	}

	// ----- interface GraphObjectTrait ------
	public boolean isGranted(final GraphObject propertyContainer, final Permission permission, final SecurityContext securityContext, final boolean isCreation) {
		return true;
	}

	public boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

		boolean isValid = true;

		for (final Trait trait : traits.values()) {

			isValid &= trait.isValid(obj, errorBuffer);
		}

		return isValid;
	}

	public void onCreation(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.onCreation(obj, securityContext, errorBuffer);
		}
	}

	public void onModification(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.onModification(obj, securityContext, errorBuffer, modificationQueue);
		}
	}

	public void onDeletion(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.onDeletion(obj, securityContext, errorBuffer, properties);
		}
	}

	public void afterCreation(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.afterCreation(obj, securityContext);
		}
	}

	public void afterModification(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.afterModification(obj, securityContext);
		}
	}

	public void afterDeletion(final GraphObject obj, final SecurityContext securityContext, final PropertyMap properties) {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.afterDeletion(obj, securityContext, properties);
		}
	}

	public void ownerModified(final GraphObject obj, final SecurityContext securityContext) {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.ownerModified(obj, securityContext);
		}
	}

	public void securityModified(final GraphObject obj, final SecurityContext securityContext) {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.securityModified(obj, securityContext);
		}
	}

	public void locationModified(final GraphObject obj, final SecurityContext securityContext) {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.locationModified(obj, securityContext);
		}
	}

	public void propagatedModification(final GraphObject obj, final SecurityContext securityContext) {

		// must use for loop here because Consumer doesn't throw exception
		for (final Trait trait : traits.values()) {
			trait.propagatedModification(obj, securityContext);
		}
	}

	@Override
	public PropertyContainer getPropertyContainer(GraphObject graphObject) {
		return null;
	}

	// ----- interface PropertyContainerTrait -----
	public Set<PropertyKey> getPropertyKeys(final GraphObject propertyContainer, final String propertyView) {

		// select only those traits that implement the propertyContainer trait!
		// we can "override" methods this way!
	}

	@Override
	public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey) {
		return null;
	}

	@Override
	public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter) {
		return null;
	}

	@Override
	public <T> Object setProperty(final GraphObject propertyContainer, final PropertyKey<T> key, T value) throws FrameworkException {
		return null;
	}

	@Override
	public <T> Object setProperty(final GraphObject propertyContainer, final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException {
		return null;
	}

	@Override
	public void setProperties(final GraphObject propertyContainer, final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
	}

	@Override
	public void setProperties(final GraphObject propertyContainer, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {
	}

	// ----- static methods -----
	public static Traits of(final String name) {
		return typeToTraitsMap.get(name);
	}

	public static void registerNodeType(String type, NodeTrait... traits) {



	}
}
