/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.converter;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.web.common.RelType;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class PathsConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(PathsConverter.class.getName());

	public PathsConverter(SecurityContext securityContext) {
		super(securityContext);
	}

	public PathsConverter(SecurityContext securityContext, GraphObject entity) {
		super(securityContext, entity);
	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public Object convert(Object source) {

		// read only
		return null;
	}

	@Override
	public Object revert(Object source) {

		final AbstractNode startNode         = (AbstractNode) currentObject;
		final TraversalDescription localDesc = Traversal.description().depthFirst().uniqueness(Uniqueness.NODE_PATH).relationships(RelType.CONTAINS, Direction.INCOMING);
		final String uuidPropertyName        = GraphObject.id.dbName();

		Set<String> treeAddresses = new HashSet<>();

		// do traversal
		for (Path path : localDesc.traverse(startNode.getNode())) {

			String pageId       = (String) path.endNode().getProperty(uuidPropertyName);
			String treeAddress  = "";
			boolean isConnected = false;

			for (Relationship r : path.relationships()) {
				
				isConnected = true;

				if (!r.hasProperty(pageId)) {

					// We found a relationship without pageId as key.
					// That means that the path is invalid.
					isConnected = false;

					break;
				}

				Integer pos = Integer.parseInt(r.getProperty(pageId).toString());

				treeAddress = "_" + pos + treeAddress;

			}

			if (isConnected) {

				treeAddresses.add(pageId + treeAddress);
				logger.log(Level.FINE, "{0}{1}", new Object[]{pageId, treeAddress});

			}

		}

		return treeAddresses;

	}

}
