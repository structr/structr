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
package org.structr.core.property;

import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.GenericNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;

import java.util.*;

/**
 *
 *
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

		if (obj instanceof NodeInterface) {

			final Class type = StructrApp.getConfiguration().getNodeEntityClass(value);

			TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), (NodeInterface)obj, type, true);
		}

		return null;
	}

	public static Set<String> getLabelsForType(final Class type) {

		final Set<String> result = new LinkedHashSet<>();

		// collect new labels
		for (final Class supertype : SearchCommand.typeAndAllSupertypes(type)) {

			final String supertypeName = supertype.getName();

			if (supertypeName.startsWith("org.structr.") || supertypeName.startsWith("com.structr.")) {
				result.add(supertype.getSimpleName());
			}
		}

		return result;
	}

	public static void updateLabels(final DatabaseService graphDb, final NodeInterface node, final Class inputType, final boolean removeUnused) {

		final Set<String> intersection = new LinkedHashSet<>();
		final Set<String> toRemove     = new LinkedHashSet<>();
		final Set<String> toAdd        = new LinkedHashSet<>();
		final Node dbNode              = node.getNode();
		final List<String> labels      = Iterables.toList(dbNode.getLabels());
		Class singleLabelType          = inputType;

		// include optional tenant identifier when modifying labels
		final String tenantIdentifier = graphDb.getTenantIdentifier();
		if (tenantIdentifier != null) {

			toAdd.add(tenantIdentifier);
			labels.remove(tenantIdentifier);
		}

		// initialize type property from single label on unknown nodes
		if (node instanceof GenericNode && labels.size() == 1 && !dbNode.hasProperty("type")) {

			final String singleLabelTypeName = labels.get(0);
			final Class typeCandidate        = StructrApp.getConfiguration().getNodeEntityClass(singleLabelTypeName);

			if (typeCandidate != null) {
				singleLabelType = typeCandidate;
			}

			dbNode.setProperty("type", labels.get(0));
		}

		// collect labels that are already present on a node
		for (final String label : labels) {
			toRemove.add(label);
		}

		// collect new labels
		for (final Class supertype : SearchCommand.typeAndAllSupertypes(singleLabelType)) {

			final String supertypeName = supertype.getName();

			if (supertypeName.startsWith("org.structr.") || supertypeName.startsWith("com.structr.")) {
				toAdd.add(supertype.getSimpleName());
			}
		}

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
		for (final String add : toAdd) {
			dbNode.addLabel(add);
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
