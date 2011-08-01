/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

import com.vividsolutions.jts.geom.Coordinate;

import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;

import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.geo.GeoObject;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.GraphDatabaseCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class GeoHelper {

	synchronized public static Set<AbstractNode> findClosestNodes(final GeoObject node, final double radius) {

		Set<AbstractNode> result     = new HashSet<AbstractNode>();
		GraphDatabaseService graphDb =
			(GraphDatabaseService) Services.command(GraphDatabaseCommand.class).execute();
		final SpatialDatabaseService db     = new SpatialDatabaseService(graphDb);
		SimplePointLayer layer              = (SimplePointLayer) db.getLayer("Hotels");
		Command findNode                    = Services.command(FindNodeCommand.class);
		Coordinate myPosition               = node.getCoordinates();
		List<SpatialDatabaseRecord> results = layer.findClosestPointsTo(myPosition, radius);

		for (SpatialDatabaseRecord record : results) {

			Long nodeId = (Long) record.getProperty("nodeId");

			// Don't add central node itself
			if (nodeId.equals(node.getId())) {
				continue;
			}

			AbstractNode newNode = (AbstractNode) findNode.execute(new SuperUser(), nodeId);

			result.add(newNode);
		}

		return result;
	}
}
