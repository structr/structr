/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.LinkedList;
import java.util.List;
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

/**
 *
 * @author axel
 */
public class GeoHelper {

    public static List<AbstractNode> findClosestNodes(final GeoObject node, final double radius) {

        List<AbstractNode> result = new LinkedList<AbstractNode>();

        GraphDatabaseService graphDb = (GraphDatabaseService) Services.command(GraphDatabaseCommand.class).execute();
        final SpatialDatabaseService db = new SpatialDatabaseService(graphDb);

        SimplePointLayer layer = (SimplePointLayer) db.getLayer("Hotels");

        Command findNode = Services.command(FindNodeCommand.class);
        
        Coordinate myPosition = node.getCoordinates();
        List<SpatialDatabaseRecord> results =
                layer.findClosestPointsTo(myPosition, radius);

        for (SpatialDatabaseRecord record : results) {

            AbstractNode newNode = (AbstractNode) findNode.execute(new SuperUser(), record.getProperty("nodeId"));
            result.add(newNode);
        }
        return result;

    }
}
