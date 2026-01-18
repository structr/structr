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

import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

import java.util.*;

/**
 */
public class TypeProperty extends StringProperty {

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

		super.setProperty(securityContext, obj, value);

		if (obj instanceof NodeInterface node) {

			final Traits traits = Traits.of(value);

			TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), node, traits, true);
		}

		return null;
	}

	public static void updateLabels(final DatabaseService graphDb, final NodeInterface node, final Traits inputType, final boolean removeUnused) {

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
	public Object getExampleValue(final String type, final String viewName) {
		return type;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("example", getExampleValue(type, viewName));

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}


		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("example", getExampleValue(type, viewName));

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}
}
