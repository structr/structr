package org.structr.rest.maintenance;

import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.rest.resource.MaintenanceParameterResource;


/**
 *
 * @author Christian Morgner
 */
public class DeleteSpatialIndexCommand extends NodeServiceCommand implements MaintenanceCommand {

	static {
		
		MaintenanceParameterResource.registerMaintenanceCommand("deleteSpatialIndex", DeleteSpatialIndexCommand.class);
	}
	
	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {
		
		
		GraphDatabaseService graphDb = Services.getService(NodeService.class).getGraphDb();
		
		TraversalDescription description = Traversal.description()
			
			.depthFirst()
			.uniqueness(Uniqueness.NODE_RECENT)
			
			.relationships(org.neo4j.collections.rtree.RTreeRelationshipTypes.RTREE_REFERENCE, Direction.OUTGOING)
			
		;
		
		Node referenceNode = graphDb.getNodeById(0);
		description.traverse(referenceNode);

	}

}
