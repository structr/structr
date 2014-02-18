package org.structr.schema.importer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.logging.BufferingLogger;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.common.StructrAndSpatialPredicate;
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
import org.structr.schema.ReloadSchema;

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
		
		try {
			
			if (fileName != null) {
			
				GraphGistImporter.importGist(extractSource(new FileInputStream(fileName)));

			} else if (url != null) {

				GraphGistImporter.importGist(extractSource(new URL(url).openStream()));

			} else if (source != null) {

				GraphGistImporter.importGist(extractSource(new ByteArrayInputStream(source.getBytes())));
			}

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
	
	public static void importGist(final List<String> sources) throws FrameworkException {
	
		final App app                                    = StructrApp.getInstance();
		final NodeServiceCommand nodeServiceCommand      = app.command(CreateNodeCommand.class);
		final GraphDatabaseService graphDb               = app.command(GraphDatabaseCommand.class).execute();
		final ExecutionEngine engine                     = new ExecutionEngine(graphDb, new BufferingLogger());
		final Map<String, Map<String, Class>> properties = new LinkedHashMap<>();
		final Set<RelationshipTemplate> relationships    = new LinkedHashSet<>();
		final Map<String, SchemaNode> schemaNodes        = new LinkedHashMap<>();

		// first step: execute cypher queries
		for (final String source : sources) {
			
			try {
				// be very tolerant here, just execute everything
				engine.execute(source);
				
			} catch (Throwable t) {
				// ignore
				t.printStackTrace();
			}
		}
		
		// second step: analyze schema of newly created nodes, skip existing ones (structr & spatial)
		try (final Tx tx = app.tx()) {

			// register transaction post process that rebuilds the index after successful creation
			TransactionCommand.postProcess("reloadschema", new ReloadSchema());
			TransactionCommand.postProcess("gist", app.command(BulkRebuildIndexCommand.class));
			
			
			
			// find all nodes that have no (string) ID property
			// (TODO: maybe look for nodes with unknown labels / types?)
			
			// analyze nodes
			for (final Node node : Iterables.filter(new StructrAndSpatialPredicate(false, false, true), GlobalGraphOperations.at(graphDb).getAllNodes())) {
				
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
					
					// create ID and type on imported node
					node.setProperty(GraphObject.id.dbName(), nodeServiceCommand.getNextUuid());
					node.setProperty(GraphObject.type.dbName(), primaryType);
				}
			}
			
			// analyze relationships
			for (final Relationship rel : Iterables.filter(new StructrAndSpatialPredicate(false, false, true), GlobalGraphOperations.at(graphDb).getAllRelationships())) {
				
				final Node startNode          = rel.getStartNode();
				final Node endNode            = rel.getEndNode();
				final String relationshipType = rel.getType().name();
				final String startNodeType    = getType(startNode);
				final String endNodeType      = getType(endNode);
				final Set<String> typeSet     = properties.keySet();
				
				if (typeSet.contains(startNodeType) && typeSet.contains(endNodeType)) {
					relationships.add(new RelationshipTemplate(startNodeType, endNodeType, relationshipType));
				}

				// create combined type on imported relationship
				if (startNodeType != null && endNodeType != null) {
					
					final String combinedType = startNodeType.concat(relationshipType).concat(endNodeType); 
					rel.setProperty(GraphObject.type.dbName(), combinedType);
				}
			
				// create ID on imported relationship
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
	
	private static List<String> extractSource(final InputStream source) {

		final List<String> sources = new LinkedList<>();
		final StringBuilder buf    = new StringBuilder();

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(source))) {

			String line        = reader.readLine();
			boolean beforeCypher = false;
			boolean afterCypher  = false;
			boolean inCypher     = false;

			while (line != null) {

				final String trimmedLine = line.trim().replaceAll("[\\s]+", "");

				// make sure only "graph" blocks are parsed
				if (afterCypher) {
					
					if ("//graph".equals(trimmedLine)) {
						
						// add cypher statements to list of sources
						sources.add(buf.toString());
						buf.setLength(0);

						afterCypher = false;
					}
					
					if ("//table".equals(trimmedLine)) {
						
						// discard buffer
						buf.setLength(0);
						afterCypher = false;
					}
				}

				if (!afterCypher) {
					
					if (inCypher && "----".equals(trimmedLine)) {

						inCypher = false;
						beforeCypher = false;
						afterCypher = true;
					}

					if (inCypher) {

						buf.append(line);
						buf.append("\n");
					}

					if ("[source,cypher]".equals(trimmedLine)) {
						beforeCypher = true;
					}

					if (beforeCypher && "----".equals(trimmedLine)) {
						inCypher     = true;
						beforeCypher = false;
					}
				}

				line = reader.readLine();
			}

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		return sources;
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
			return startNodeType.concat(relType).concat(endNodeType).hashCode();
		}
		
		@Override
		public boolean equals(final Object o) {
			
			if (o instanceof RelationshipTemplate) {
				return ((RelationshipTemplate)o).hashCode() == hashCode();
			}
			
			return false;
		}
	}
}
