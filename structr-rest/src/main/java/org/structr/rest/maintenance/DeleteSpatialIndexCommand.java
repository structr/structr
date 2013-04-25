package org.structr.rest.maintenance;

import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.neo4j.collections.rtree.RTreeRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
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
		
		
		final GraphDatabaseService graphDb = Services.getService(NodeService.class).getGraphDb();
		final List<Node> toDelete          = new LinkedList<Node>();

		
		
		for (final Node node: GlobalGraphOperations.at(graphDb).getAllNodes()) {

			try {
				if (node.hasProperty("bbox") && node.hasProperty("gtype") && node.hasProperty("id") && node.hasProperty("latitude") && node.hasProperty("longitude")) {

					toDelete.add(node);
				}
				
			} catch (Throwable t) {}
	
		}
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				for (Node node : toDelete) {
					
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
				return null;
			}
		});
	}

}
