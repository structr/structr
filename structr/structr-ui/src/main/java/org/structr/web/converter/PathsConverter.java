/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.converter;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.common.RelType;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class PathsConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(PathsConverter.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source, Value value) {

		// read only
		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		AbstractNode startNode         = (AbstractNode) currentObject;
		TraversalDescription localDesc = Traversal.description().depthFirst().uniqueness(Uniqueness.NODE_PATH).relationships(RelType.CONTAINS, Direction.INCOMING);

		Set<String> treeAddresses = new HashSet<String>();

		// do traversal
		for (Path path : localDesc.traverse(startNode.getNode())) {

			String pageId       = (String) path.endNode().getProperty("uuid");
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
