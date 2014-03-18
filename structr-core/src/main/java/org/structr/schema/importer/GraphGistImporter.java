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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.logging.BufferingLogger;
import org.neo4j.tooling.GlobalGraphOperations;
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
			
				GraphGistImporter.importGist(extractSources(new FileInputStream(fileName)));

			} else if (url != null) {

				GraphGistImporter.importGist(extractSources(new URL(url).openStream()));

			} else if (source != null) {

				GraphGistImporter.importGist(extractSources(new ByteArrayInputStream(source.getBytes())));
			}

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
	
	public static void importGist(final List<String> sources) throws FrameworkException {
	
		final App app                                     = StructrApp.getInstance();
		final NodeServiceCommand nodeServiceCommand       = app.command(CreateNodeCommand.class);
		final ConfigurationProvider configuration         = Services.getInstance().getConfigurationProvider();
		final GraphDatabaseService graphDb                = app.command(GraphDatabaseCommand.class).execute();
		final ExecutionEngine engine                      = new ExecutionEngine(graphDb, new BufferingLogger());
		final Set<NodeInfo> nodeTypes                     = new LinkedHashSet<>();
		final Map<Long, TypeInfo> nodeTypeInfoMap         = new LinkedHashMap<>();
		final Set<RelationshipInfo> relationships         = new LinkedHashSet<>();
		final Map<String, SchemaNode> schemaNodes         = new LinkedHashMap<>();
		final Map<NodeInfo, List<Node>> nodeMap           = new LinkedHashMap<>();
		final Map<String, List<TypeInfo>> typeInfoTypeMap = new LinkedHashMap<>();
		final List<TypeInfo> reducedTypeInfos             = new LinkedList<>();
		final List<TypeInfo> typeInfos                    = new LinkedList<>();

		// nothing to do
		if (sources.isEmpty()) {
			return;
		}
		
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

				final NodeInfo nodeInfo = new NodeInfo(node);
				
				// extract node info and set UUID
				nodeTypes.add(nodeInfo);
				
				List<Node> nodes = nodeMap.get(nodeInfo);
				if (nodes == null) {
					nodes = new LinkedList<>();
					nodeMap.put(nodeInfo, nodes);
				}
				
				nodes.add(node);
			}
			
			// nodeTypes now contains all existing node types and their property sets
			identifyCommonBaseClasses(nodeTypes, nodeMap, typeInfos);

			// group type infos by type
			collectTypeInfos(typeInfos, typeInfoTypeMap);
			
			// reduce type infos with more than one type 
			reduceTypeInfos(typeInfoTypeMap, reducedTypeInfos);
			
			// intersect property sets of type infos
			intersectPropertySets(reducedTypeInfos);
			
			// set type and ID on newly created nodes
			final Map<String, TypeInfo> reducedTypeInfoMap = new LinkedHashMap<>();
			for (final TypeInfo info : reducedTypeInfos) {
				
				final String type = info.getPrimaryType();

				// map TypeInfo to type for later use
				reducedTypeInfoMap.put(type, info);
				
				System.out.println("#######################################");
				System.out.println(info);
				
				for (final Node node : info.getNodes()) {
					
					node.setProperty(GraphObject.id.dbName(), nodeServiceCommand.getNextUuid());
					node.setProperty(GraphObject.type.dbName(), type);
					
					// store type info for imported node
					nodeTypeInfoMap.put(node.getId(), info);
				}
			}
			
			// analyze relationships
			for (final Relationship rel : Iterables.filter(new StructrAndSpatialPredicate(false, false, true), GlobalGraphOperations.at(graphDb).getAllRelationships())) {
				
				final Node startNode          = rel.getStartNode();
				final Node endNode            = rel.getEndNode();
				
				// make sure node has been successfully identified above
				if (startNode.hasProperty("type") && endNode.hasProperty("type")) {
					
					final TypeInfo startTypeInfo  = nodeTypeInfoMap.get(startNode.getId());
					final TypeInfo endTypeInfo    = nodeTypeInfoMap.get(endNode.getId());
					final String relationshipType = rel.getType().name();
					final String startNodeType    = startTypeInfo.getPrimaryType();
					final String endNodeType      = endTypeInfo.getPrimaryType();

					relationships.add(new RelationshipInfo(startNodeType, endNodeType, relationshipType));

					// create combined type on imported relationship
					if (startNodeType != null && endNodeType != null) {

						final String combinedType = startNodeType.concat(relationshipType).concat(endNodeType); 
						rel.setProperty(GraphObject.type.dbName(), combinedType);
					}

					// create ID on imported relationship
					rel.setProperty(GraphObject.id.dbName(), nodeServiceCommand.getNextUuid());
				}
			}

			// group relationships by type
			final Map<String, List<RelationshipInfo>> relTypeInfoMap = new LinkedHashMap<>();
			for (final RelationshipInfo relInfo : relationships) {
				
				final String relType         = relInfo.getRelType();
				List<RelationshipInfo> infos = relTypeInfoMap.get(relType);
				
				if (infos == null) {
					
					infos = new LinkedList<>();
					relTypeInfoMap.put(relType, infos);
				}
				
				infos.add(relInfo);
			}

			// reduce relationship infos into one
			final List<RelationshipInfo> reducedRelationshipInfos = new LinkedList<>();
			for (final List<RelationshipInfo> infos : relTypeInfoMap.values()) {
				
				reducedRelationshipInfos.addAll(reduceNodeTypes(infos, reducedTypeInfoMap));
			}
			
			// create schema nodes
			for (final TypeInfo typeInfo : reducedTypeInfos) {
				
				final String type = typeInfo.getPrimaryType();
				if (!"ReferenceNode".equals(type)) {

					final Map<String, Class> props = typeInfo.getPropertySet();
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
						
					} else if (!typeInfo.getOtherTypes().isEmpty()) {

						// only the first supertype is supported
						propertyMap.put(SchemaNode.extendsClass, typeInfo.getSuperclass(reducedTypeInfoMap));
					}

					// create schema node
					schemaNodes.put(type, app.create(SchemaNode.class, propertyMap));
				}
			}
			
			// create relationships
			for (final RelationshipInfo template : reducedRelationshipInfos) {
				
				final SchemaNode startNode    = schemaNodes.get(template.getStartNodeType());
				final SchemaNode endNode      = schemaNodes.get(template.getEndNodeType());
				final String relationshipType = template.getRelType();
				final PropertyMap propertyMap = new PropertyMap();
				
				propertyMap.put(SchemaRelationship.sourceId, startNode.getUuid());
				propertyMap.put(SchemaRelationship.targetId, endNode.getUuid());
				propertyMap.put(SchemaRelationship.relationshipType, relationshipType);
				
				app.create(startNode, endNode, SchemaRelationship.class, propertyMap);
			}
			
			
			tx.success();
		}
		
		logger.log(Level.INFO, "Graph gist import successful, {0} types imported.", typeInfos.size());
	}
	
	public static List<String> extractSources(final InputStream source) {

		final List<String> sources = new LinkedList<>();
		final StringBuilder buf    = new StringBuilder();

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(source))) {

			String line          = reader.readLine();
			boolean beforeCypher = false;
			boolean inCypher     = false;

			while (line != null) {

				final String trimmedLine = line.trim().replaceAll("[\\s]+", "");

				// make sure only "graph" blocks are parsed
				if (inCypher && "----".equals(trimmedLine)) {

					inCypher = false;
					beforeCypher = false;

					if (buf.toString().toUpperCase().contains("CREATE")) {
						
						sources.add(buf.toString());
						buf.setLength(0);
					}
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

				line = reader.readLine();
			}

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		return sources;
	}
	
	// ----- private static methods -----
	private static void identifyCommonBaseClasses(final Set<NodeInfo> nodeTypes, final Map<NodeInfo, List<Node>> nodeMap, final List<TypeInfo> typeInfos) {
		
		// next we need to identify common base classes, which can be found by
		// finding all NodeInfo entries that share at least one type

		for (final NodeInfo nodeInfo : nodeTypes) {

			final Set<String> allTypes = nodeInfo.getTypes();
			for (final String type : allTypes) {

				final TypeInfo typeInfo = new TypeInfo(type, allTypes, nodeMap.get(nodeInfo));
				typeInfos.add(typeInfo);
				typeInfo.registerPropertySet(nodeInfo.getProperties());
			}
		}

	}
	
	private static void collectTypeInfos(final List<TypeInfo> typeInfos, final Map<String, List<TypeInfo>> typeInfoTypeMap) {

		// collect type infos by type (to detect multiples)
		for (final TypeInfo info : typeInfos) {

			final String type       = info.getPrimaryType();
			List<TypeInfo> typeInfo = typeInfoTypeMap.get(type);

			if (typeInfo == null) {

				typeInfo = new LinkedList<>();
				typeInfoTypeMap.put(type, typeInfo);
			}

			typeInfo.add(info);
		}

	}
	
	private static void reduceTypeInfos(final Map<String, List<TypeInfo>> typeInfoTypeMap, final List<TypeInfo> reducedTypeInfos) {
		
		for (final Entry<String, List<TypeInfo>> entry : typeInfoTypeMap.entrySet()) {

			final List<TypeInfo> listOfTypeInfosWithSamePrimaryType = entry.getValue();
			TypeInfo firstTypeInfo                                  = null;

			for (final TypeInfo typeInfo : listOfTypeInfosWithSamePrimaryType) {

				if (firstTypeInfo == null) {

					firstTypeInfo = typeInfo;

				} else {

					firstTypeInfo.intersectPropertySets(typeInfo.getPropertySet());

					// "save" node references for later use
					firstTypeInfo.getNodes().addAll(typeInfo.getNodes());
				}
			}

			// firstTypeInfo now contains the intersection of all type infos of a given type
			reducedTypeInfos.add(firstTypeInfo);
			
			// set hierarchy level
			firstTypeInfo.setHierarchyLevel(listOfTypeInfosWithSamePrimaryType.size());
		}

	}

	private static void intersectPropertySets(final List<TypeInfo> reducedTypeInfos) {
		
		// substract property set from type info with more than one
		// occurrence from type infos with the given superclass
		for (final TypeInfo info : reducedTypeInfos) {

			if (info.getHierarchyLevel() > 1) {

				final Set<String> supertypeKeySet = info.getPropertySet().keySet();

				for (final TypeInfo subType : reducedTypeInfos) {

					final Set<String> subtypeKeySet = subType.getPropertySet().keySet();

					// only substract property set if it is a true subtype (and not the same :))
//					if ( subType.getUsages() == 1 && subType.hasSuperclass(info.getPrimaryType())) {
					if (subType.getHierarchyLevel() < info.getHierarchyLevel() && subType.hasSuperclass(info.getPrimaryType())) {

						subtypeKeySet.removeAll(supertypeKeySet);
					}

				}
			}
		}
	}
	
	private static List<RelationshipInfo> reduceNodeTypes(final List<RelationshipInfo> sourceList, Map<String, TypeInfo> typeInfos) {
		
		final List<RelationshipInfo> reducedList = new LinkedList<>();
		final Set<String> startNodeTypes         = new LinkedHashSet<>();
		final Set<String> endNodeTypes           = new LinkedHashSet<>();
		String relType                           = null;

		for (final RelationshipInfo info : sourceList) {

			startNodeTypes.add(info.getStartNodeType());
			endNodeTypes.add(info.getEndNodeType());
			
			// set relType on first hit (should all be the same!)
			if (relType == null) {
				relType = info.getRelType();
			}
		}
		
		int startTypeCount     = startNodeTypes.size();
		int endTypeCount       = endNodeTypes.size();
		String commonStartType = null;
		String commonEndType   = null;

		if (startTypeCount == 1) {

			commonStartType = startNodeTypes.iterator().next();

		} else {						

			commonStartType = reduceTypeToCommonSupertype(startNodeTypes, typeInfos);
		}

		if (endTypeCount == 1) {

			commonEndType = endNodeTypes.iterator().next();

		} else {
			
			commonEndType = reduceTypeToCommonSupertype(endNodeTypes, typeInfos);
		}

		if (commonStartType != null && commonEndType != null) {

			System.out.println("Common start type found: " + commonStartType);
			System.out.println("Common end type found: " + commonEndType);
			
			reducedList.add(new RelationshipInfo(commonStartType, commonEndType, relType));
		}

		return reducedList;
	}
	
	private static String reduceTypeToCommonSupertype(final Set<String> types, final Map<String, TypeInfo> typeInfos) {
		
		// the idea here is to build a list of lists which contains all the superclasses of each type. For
		// the following example, we consider the following type hierarchy:
		//  Type0 -> Super1 -> Super2
		//  Type1 -> Super3 -> Super2
		//  Type2 -> Super2 -> Super4

		// Super2 Super2 Super4
		// Super1 Super3 Super2
		// Type0  Type1  Type2

		// We can see that there is a common base class to all of the types if there is a type that is found
		// in every column of the list of sets, i.e. there is one type that is found in every set. We can
		// identify that by intersecting all the sets with each other.
		
		// build list of types
		final List<Set<String>> listOfSetsOfTypes = new LinkedList<>();
		
		// iterate types
		for (String type : types) {
			
			final Set<String> listOfTypes = new LinkedHashSet<>();
			String currentType            = type;
			
			listOfSetsOfTypes.add(listOfTypes);
			
			while (currentType != null) {
				
				// insert at the front of the list
				listOfTypes.add(currentType);

				// fetch type info
				final TypeInfo typeInfo = typeInfos.get(currentType);
				if (typeInfo != null) {

					currentType = typeInfo.getSuperclass(typeInfos);
					
				} else {
					
					// no type info => exit loop
					currentType = null;
				}
			}
		}

		// try to find a common element in all the sets
		final Set<String> intersection = new LinkedHashSet<>();
		boolean first                  = true;
		
		for (final Set<String> set : listOfSetsOfTypes) {
		
			if (first) {
				
				// first iteration: fill intersection with first elements
				first = false;
				intersection.addAll(set);
				
			} else {
				
				// intersect
				intersection.retainAll(set);
			}
		}
		
		if (!intersection.isEmpty()) {
			
			// return first element, because we cannot decide (yet)
			// which of the multiple common base classes we want
			
			return intersection.iterator().next();
		}
		
		return null;
	}
}
