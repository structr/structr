/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.logging.BufferingLogger;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.common.CaseHelper;
import org.structr.common.StructrAndSpatialPredicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
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
import org.structr.schema.ConfigurationProvider;
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
		final ConfigurationProvider configuration        = Services.getInstance().getConfigurationProvider();
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
			
			// analyze nodes
			for (final Node node : Iterables.filter(new StructrAndSpatialPredicate(false, false, true), GlobalGraphOperations.at(graphDb).getAllNodes())) {

				// first step: analyze node properties
				final Map<String, Class> propertyTypes = new TreeMap<>();
				for (final String key : node.getPropertyKeys()) {

					final Object value = node.getProperty(key);
					if (value != null) {
						
						propertyTypes.put(key, value.getClass());
					}
				}
				
				// second step: try to infer type or use properties to
				// identify nodes of the same type
				final String primaryType = getType(node, propertyTypes);
				if (primaryType != null && !"ReferenceNode".equals(primaryType)) {
					
					// store propery map
					properties.put(primaryType, propertyTypes);
					
					// create ID and type on imported node
					node.setProperty(GraphObject.id.dbName(), nodeServiceCommand.getNextUuid());
					node.setProperty(GraphObject.type.dbName(), primaryType);
				}
			}
			
			// analyze relationships
			for (final Relationship rel : Iterables.filter(new StructrAndSpatialPredicate(false, false, true), GlobalGraphOperations.at(graphDb).getAllRelationships())) {
				
				final Node startNode          = rel.getStartNode();
				final Node endNode            = rel.getEndNode();
				
				// make sure node has been successfully identified above
				if (startNode.hasProperty("type") && endNode.hasProperty("type")) {
					
					final String relationshipType = rel.getType().name();
					final String startNodeType    = (String)startNode.getProperty("type");
					final String endNodeType      = (String)endNode.getProperty("type");
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
			}

			// create schema nodes
			for (final Entry<String, Map<String, Class>> typeEntry : properties.entrySet()) {
				
				final String type              = typeEntry.getKey();
				final Map<String, Class> props = typeEntry.getValue();
				final PropertyMap propertyMap  = new PropertyMap();
				
				// add properties
				for (final Entry<String, Class> propertyEntry : props.entrySet()) {
					
					final String propertyName = propertyEntry.getKey();
					final Class propertyType  = propertyEntry.getValue();
					
					// handle array types differently
					String propertyTypeName = propertyType.getSimpleName();
					if (propertyType.isArray()) {
						
						// remove "[]" from the end and append "Array" to match the appropriate parser
						propertyTypeName = propertyTypeName.substring(0, propertyTypeName.length() - 2).concat("Array");
					}
					
					propertyMap.put(new StringProperty("_".concat(propertyName)), propertyTypeName);
				}
				
				// set node type which is in "name" property
				propertyMap.put(AbstractNode.name, type);
				
				// check if there is an existing Structr entity with the same type
				// and make the dynamic class extend the existing class if yes.
				final Class existingType = configuration.getNodeEntityClass(type);
				if (existingType != null) {
					
					propertyMap.put(SchemaNode.extendsClass, existingType.getName());
				}

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
	
	private static String getType(final Node node, final Map<String, Class> properties) {
		
		// first try: label
		final Iterable<Label> labels    = node.getLabels();
		final Iterator<Label> iterator  = labels.iterator();
		
		if (iterator.hasNext()) {
			return iterator.next().name();
		}
		
		// second try: type attribute
		if (node.hasProperty("type")) {

			final String type = node.getProperty("type").toString();
			return type.replaceAll("[\\W]+", "");
		}
		
		// third try: incoming relationships
		final Set<String> incomingTypes = new LinkedHashSet<>();
		for (final Relationship incoming : node.getRelationships(Direction.INCOMING)) {
			incomingTypes.add(incoming.getType().name());
		}
		
		// (if all incoming relationships are of the same type,
		// it is very likely that this is a type-defining trait)
		if (incomingTypes.size() == 1) {
			return CaseHelper.toUpperCamelCase(incomingTypes.iterator().next().toLowerCase());
		}
		
		// forth try: outgoing relationships
		final Set<String> outgoingTypes = new LinkedHashSet<>();
		for (final Relationship outgoing : node.getRelationships(Direction.OUTGOING)) {
			outgoingTypes.add(outgoing.getType().name());
		}
		
		// (if all outgoing relationships are of the same type,
		// it is very likely that this is a type-defining trait)
		if (outgoingTypes.size() == 1) {
			return CaseHelper.toUpperCamelCase(outgoingTypes.iterator().next().toLowerCase());
		}
		
		// fifth try: analyze properties
		final StringBuilder buf = new StringBuilder("NodeWith");
		for (final String key : properties.keySet()) {
			
			buf.append(StringUtils.capitalize(key));
		}
		
		return buf.toString();
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
