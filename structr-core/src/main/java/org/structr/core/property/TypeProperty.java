/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import java.util.LinkedHashSet;
import java.util.Set;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.search.SearchCommand;

/**
 *
 * @author Christian Morgner
 */
public class TypeProperty extends StringProperty {

	public TypeProperty() {

		super("type");

		readOnly();
		indexed();
		writeOnce();
	}

	@Override
	public void setProperty(SecurityContext securityContext, final GraphObject obj, String value) throws FrameworkException {

		super.setProperty(securityContext, obj, value);

		RelationshipFactory.invalidateCache();
		NodeFactory.invalidateCache();

		if (obj instanceof NodeInterface) {

			final Class type              = StructrApp.getConfiguration().getNodeEntityClass(value);
			final Set<Label> intersection = new LinkedHashSet<>();
			final Set<Label> toRemove     = new LinkedHashSet<>();
			final Set<Label> toAdd        = new LinkedHashSet<>();
			final Node dbNode             = ((NodeInterface)obj).getNode();

			// collect labels that are already present on a node
			for (final Label label : dbNode.getLabels()) {
				toRemove.add(label);
			}

			// collect new labels
			for (final Class supertype : SearchCommand.typeAndAllSupertypes(type)) {

				final String supertypeName = supertype.getName();

				if (supertypeName.startsWith("org.structr.") || supertypeName.startsWith("com.structr.")) {
					toAdd.add(DynamicLabel.label(supertype.getSimpleName()));
				}
			}

			// calculate intersection
			intersection.addAll(toAdd);
			intersection.retainAll(toRemove);

			// calculate differences
			toAdd.removeAll(intersection);
			toRemove.removeAll(intersection);

			// remove difference
			for (final Label remove : toRemove) {
				dbNode.removeLabel(remove);
			}

			// add difference
			for (final Label add : toAdd) {
				dbNode.addLabel(add);
			}
		}
	}
}
