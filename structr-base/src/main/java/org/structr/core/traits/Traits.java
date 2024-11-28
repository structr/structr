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
public class Traits {

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

	public boolean isNodeType() {
		return isNodeType;
	}

	public GraphObjectTrait getGraphObjectTrait() {
		return new GraphObjectTraitHandler(this);
	}

	public PropertyContainerTrait getPropertyContainerTrait() {
		return new PropertyContainerTraitHandler(this);
	}

	public AccessControllableTrait getAccessControllableTrait() {
		return null;
	}

	public NodeInterfaceTrait getNodeInterfaceTrait() {
		return null;
	}

	// ----- static methods -----
	public static Traits of(final String name) {
		return typeToTraitsMap.get(name);
	}
}
