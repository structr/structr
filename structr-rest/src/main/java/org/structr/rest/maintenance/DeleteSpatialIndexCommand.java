package org.structr.rest.maintenance;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
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

	private static final Logger logger = Logger.getLogger(DeleteSpatialIndexCommand.class.getName());
	
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
		Traverser traverser = description.traverse(referenceNode);

		for (Node node: traverser.nodes()) {
		
			logger.log(Level.INFO, "Deleting node {0}", node);
			
			try {
				
				for (Relationship rel : node.getRelationships()) {

					rel.delete();
				}

				node.delete();
				
			} catch (Throwable t) {
				
				t.printStackTrace();
			}
		}
	}

}
