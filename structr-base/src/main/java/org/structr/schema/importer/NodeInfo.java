/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.schema.importer;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;

import java.util.*;

/**
 *
 *
 */

public class NodeInfo {

	private final Map<String, Class> properties = new LinkedHashMap<>();
	private final Set<String> types             = new LinkedHashSet<>();
	private int hashCode                        = 0;

	public NodeInfo(final Node node) {

		extractProperties(node);
		extractTypes(node);

		calcHashCode();
	}

	private void calcHashCode() {

		this.hashCode = 13;

		hashCode += types.hashCode();
		hashCode += properties.hashCode() * 31;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {

		if (other instanceof NodeInfo) {

			return hashCode() == other.hashCode();
		}

		return false;
	}

	public boolean hasType(final String type) {
		return types.contains(type);
	}

	public Map<String, Class> getProperties() {
		return properties;
	}

	public Set<String> getTypes() {
		return types;
	}

	// ----- private methods -----
	private void extractProperties(final Node node) {

		for (final String key : node.getPropertyKeys()) {

			final Object value = node.getProperty(key);
			if (value != null) {
				properties.put(key, value.getClass());
			}
		}
	}

	private void extractTypes(final Node node) {

		// first try: labels
		// AM 2015-06-26: Changed the behaviour here: In case of multiple labels, don't put them all
		// into the set of potential types but rather create a combined type.
		// E.g. a node with the two labels 'Person' and 'Entity' will get a type 'EntityPerson'
		final List<String> labelStrings = Iterables.toList(node.getLabels());
		addType(StringUtils.join(labelStrings, ""));

		// second try: type attribute
		if (node.hasProperty("type")) {

			final String type = node.getProperty("type").toString();
			addType(type.replaceAll("[\\W]+", ""));
		}

		// deactivate relationship type nodes for now..

		/*

		// third try: incoming relationships
		final Set<String> incomingTypes = new LinkedHashSet<>();
		for (final Relationship incoming : node.getRelationships(Direction.INCOMING)) {
			incomingTypes.add(incoming.getType().name());
		}

		// (if all incoming relationships are of the same type,
		// it is very likely that this is a type-defining trait)
		if (incomingTypes.size() == 1) {
			 return CaseHelper.toUpperCamelCase(incomingTypes.iterator().next().toLowerCase());
		}

		// forth try: outgoing relationships
		final Set<String> outgoingTypes = new LinkedHashSet<>();
		for (final Relationship outgoing : node.getRelationships(Direction.OUTGOING)) {
			outgoingTypes.add(outgoing.getType().name());
		}

		// (if all outgoing relationships are of the same type,
		// it is very likely that this is a type-defining trait)
		if (outgoingTypes.size() == 1) {
			return CaseHelper.toUpperCamelCase(outgoingTypes.iterator().next().toLowerCase());
		}
		*/

		if (types.isEmpty() && !properties.keySet().isEmpty()) {

			// fifth try: analyze properties
			final StringBuilder buf = new StringBuilder("NodeWith");
			for (final String key : properties.keySet()) {

				buf.append(StringUtils.capitalize(key));
			}

			types.add(buf.toString());
		}
	}

	private void addType (final String type) {

		if (type != null && !type.equals("")) {
			types.add(type);
		}

	}
}
