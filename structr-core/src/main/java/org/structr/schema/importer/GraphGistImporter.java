package org.structr.schema.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.logging.BufferingLogger;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.graph.BulkRebuildIndexCommand;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class GraphGistImporter extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(GraphGistImporter.class.getName());
	
	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {
		
		final String fileName = (String)attributes.get("file");
		final String source   = (String)attributes.get("source");
		final String url      = (String)attributes.get("url");
		
		if (fileName == null && source == null && url == null) {
			throw new FrameworkException(422, "Please supply file, url or source parameter.");
		}
		
		if (fileName != null && source != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}
		
		if (fileName != null && url != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}
		
		if (url != null && source != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}
		
		if (fileName != null) {

			final StringBuilder buf = new StringBuilder();

			try (final BufferedReader reader = new BufferedReader(new FileReader(fileName))) {

				String line = reader.readLine();
				while (line != null) {

					buf.append(line);
					buf.append("\n");
					line = reader.readLine();
				}

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			GraphGistImporter.importGist(buf.toString());
			
		} else if (url != null) {

			final StringBuilder buf = new StringBuilder();

			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {

				String line = reader.readLine();
				while (line != null) {

					buf.append(line);
					buf.append("\n");
					line = reader.readLine();
				}

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			GraphGistImporter.importGist(buf.toString());
			
		} else if (source != null) {
			
			GraphGistImporter.importGist(source);
		}
	}
	
	public static void importGist(final String source) throws FrameworkException {
	
		final App app                                    = StructrApp.getInstance();
		final NodeServiceCommand nodeServiceCommand      = app.command(CreateNodeCommand.class);
		final GraphDatabaseService graphDb               = app.command(GraphDatabaseCommand.class).execute();
		final ExecutionEngine engine                     = new ExecutionEngine(graphDb, new BufferingLogger());
		final ExecutionResult result                     = engine.execute(source);
		final Map<String, Map<String, Class>> properties = new LinkedHashMap<>();
		final Set<RelationshipTemplate> relationships    = new LinkedHashSet<>();
		final Map<String, SchemaNode> schemaNodes        = new LinkedHashMap<>();
		
		// second step: analyze schema
		try (final Tx tx = app.tx()) {

			// register transaction post process that rebuilds the index after successful creation
			TransactionCommand.postProcess("gist", app.command(BulkRebuildIndexCommand.class));
			
			
			
			// find all nodes that have no (string) ID property
			// (TODO: maybe look for nodes with unknown labels / types?)
			
			// analyze nodes
			for (final Node node : Iterables.filter(new PropertyContainerIdPredicate(), GlobalGraphOperations.at(graphDb).getAllNodes())) {
				
				String primaryType = getType(node);
				
				if (primaryType != null && !"ReferenceNode".equals(primaryType)) {
					
					for (final String key : node.getPropertyKeys()) {

						final Object value = node.getProperty(key);
						if (value != null) {

							Map<String, Class> propertyTypes = properties.get(primaryType);
							if (propertyTypes == null) {
								propertyTypes = new LinkedHashMap<>();
								properties.put(primaryType, propertyTypes);
							}

							propertyTypes.put(key, value.getClass());
						}
					}
					
					// create ID on imported node
					node.setProperty(GraphObject.type.dbName(), primaryType);
					node.setProperty(GraphObject.id.dbName(), nodeServiceCommand.getNextUuid());
				}
			}
			
			// analyze relationships
			for (final Relationship rel : Iterables.filter(new PropertyContainerIdPredicate(), GlobalGraphOperations.at(graphDb).getAllRelationships())) {
				
				final Node startNode          = rel.getStartNode();
				final Node endNode            = rel.getEndNode();
				final String relationshipType = rel.getType().name();
				final String startNodeType    = getType(startNode);
				final String endNodeType      = getType(endNode);
				final Set<String> typeSet     = properties.keySet();
				
				if (typeSet.contains(startNodeType) && typeSet.contains(endNodeType)) {
					relationships.add(new RelationshipTemplate(startNodeType, endNodeType, relationshipType));
				}

				// create ID on imported relationships
				rel.setProperty(GraphObject.id.dbName(), nodeServiceCommand.getNextUuid());
			}

			// create schema nodes
			for (final Entry<String, Map<String, Class>> typeEntry : properties.entrySet()) {
				
				final String type              = typeEntry.getKey();
				final Map<String, Class> props = typeEntry.getValue();
				final PropertyMap propertyMap  = new PropertyMap();
				
				// add properties
				for (final Entry<String, Class> propertyEntry : props.entrySet()) {
					propertyMap.put(new StringProperty("_".concat(propertyEntry.getKey())), propertyEntry.getValue().getSimpleName());
				}
				
				// set node type which is in "name" property
				propertyMap.put(AbstractNode.name, type);

				// create schema node
				schemaNodes.put(type, app.create(SchemaNode.class, propertyMap));
			}
			
			// create relationships
			for (final RelationshipTemplate template : relationships) {
				
				final SchemaNode startNode    = schemaNodes.get(template.getStartNodeType());
				final SchemaNode endNode     = schemaNodes.get(template.getEndNodeType());
				final String relationshipType = template.getRelType();
				final PropertyMap propertyMap = new PropertyMap();
				
				propertyMap.put(SchemaRelationship.sourceId, startNode.getUuid());
				propertyMap.put(SchemaRelationship.targetId, endNode.getUuid());
				propertyMap.put(SchemaRelationship.relationshipType, relationshipType);
				
				app.create(startNode, endNode, SchemaRelationship.class, propertyMap);
			}

			tx.success();
		}
		
		logger.log(Level.INFO, "Graph gist import successful, {0} types imported.", properties.size());
	}
	
	private static String getType(final Node node) {
		
		final Iterable<Label> labels = node.getLabels();
		final Iterator<Label> iterator = labels.iterator();
		
		if (iterator.hasNext()) {
			return iterator.next().name();
		}
		
		return null;
	}

	public static class RelationshipTemplate {
		
		private String startNodeType = null;
		private String endNodeType   = null;
		private String relType       = null;
		
		public RelationshipTemplate(final String startNodeType, final String endNodeType, final String relType) {
			this.startNodeType = startNodeType;
			this.endNodeType   = endNodeType;
			this.relType       = relType;
		}

		public String getStartNodeType() {
			return startNodeType;
		}

		public String getEndNodeType() {
			return endNodeType;
		}

		public String getRelType() {
			return relType;
		}
		
		@Override
		public int hashCode() {
			return relType.hashCode();
		}
		
		@Override
		public boolean equals(final Object o) {
			
			if (o instanceof RelationshipTemplate) {
				return ((RelationshipTemplate)o).hashCode() == hashCode();
			}
			
			return false;
		}
	}
	
	private static class PropertyContainerIdPredicate implements Predicate<PropertyContainer> {

		private static final String idName = GraphObject.id.dbName();
		
		@Override
		public boolean accept(PropertyContainer container) {
			return !container.hasProperty(idName) || !(container.getProperty(idName) instanceof String);
		}
	}
}
