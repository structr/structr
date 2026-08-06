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
package org.structr.core.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

import java.util.*;

/**
 */
public class TypeProperty extends StringProperty {

	private static final Logger logger = LoggerFactory.getLogger(TypeProperty.class.getName());

	public TypeProperty() {

		super("type");

		systemInternal();
		readOnly();
		indexed();
		writeOnce();
		nodeIndexOnly();
	}

	@Override
	public Object setProperty(SecurityContext securityContext, final GraphObject obj, String value) throws FrameworkException {

		/* The value is a TYPE NAME, and the write below relabels the node to that type, so a name no type
		   answers to has to be refused HERE. Traits.of() is a plain map lookup that returns null for an
		   unknown name (TraitsInstance.getType), and updateLabels then dereferenced it: the caller got
		   "Cannot invoke Traits.getLabels() because typeCandidate is null" - a NullPointerException from
		   inside label maintenance, naming an internal variable, for what is simply a wrong value. */
		if (value != null && !Traits.exists(value)) {

			throw new FrameworkException(422, "Cannot set ‛type‛ to ‛" + value + "‛: no such type",
				new InvalidPropertySchemaToken(obj.getType(), jsonName(), value, "no_such_type", "There is no type named ‛" + value + "‛"));
		}

		super.setProperty(securityContext, obj, value);

		if (obj instanceof NodeInterface node) {

			final Traits traits = Traits.of(value);

			TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), node, traits, true);
		}

		return null;
	}

	public static void updateLabels(final DatabaseService graphDb, final NodeInterface node, final Traits inputType, final boolean removeUnused) {

		/* No type, nothing to derive labels from. This is public API, so the guard stays even though
		   setProperty above now rejects an unknown type name before it gets here: with removeUnused
		   the alternative to returning would be stripping every label off the node, and a node with no
		   labels resolves to no type at all - unreachable and undeletable. Leave the labels alone and
		   say so, rather than fail with a NullPointerException three lines down. */
		if (inputType == null) {

			logger.warn("Not updating labels of node {}: no type given.", node != null ? node.getUuid() : null);
			return;
		}

		final Set<String> intersection = new LinkedHashSet<>();
		final Set<String> toRemove     = new LinkedHashSet<>();
		final Set<String> toAdd        = new LinkedHashSet<>();
		final Node dbNode              = node.getNode();
		final List<String> labels      = Iterables.toList(dbNode.getLabels());
		Traits typeCandidate           = inputType;

		// include optional tenant identifier when modifying labels
		final String tenantIdentifier = graphDb.getTenantIdentifier();
		if (tenantIdentifier != null) {

			toAdd.add(tenantIdentifier);
			labels.remove(tenantIdentifier);
		}

		// initialize type property from single label on unknown nodes
		if (node instanceof NodeInterface && labels.size() == 1 && !dbNode.hasProperty("type")) {

			final String singleLabelTypeName = labels.get(0);

			if (Traits.exists(singleLabelTypeName)) {

				typeCandidate = Traits.of(singleLabelTypeName);

				dbNode.setProperty("type", singleLabelTypeName);
			}
		}

		// collect labels that are already present on a node
		for (final String label : labels) {
			toRemove.add(label);
		}

		// collect new labels
		toAdd.addAll(typeCandidate.getLabels());

		// calculate intersection
		intersection.addAll(toAdd);
		intersection.retainAll(toRemove);

		// calculate differences
		toAdd.removeAll(intersection);
		toRemove.removeAll(intersection);

		if (removeUnused) {

			// remove difference
			for (final String remove : toRemove) {
				dbNode.removeLabel(remove);
			}
		}

		// add difference
		if (!toAdd.isEmpty()) {

			dbNode.addLabels(toAdd);
		}
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final int index) {
		return "Type";
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("example", getExampleValue(1));

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}


		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("example", getExampleValue(1));

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}
}
