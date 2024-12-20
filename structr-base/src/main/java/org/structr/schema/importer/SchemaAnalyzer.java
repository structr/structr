/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.Traits;
import org.structr.schema.ConfigurationProvider;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import org.structr.common.helper.ValidationHelper;

import static org.structr.core.entity.SchemaNode.schemaNodeNamePattern;

public class SchemaAnalyzer extends NodeServiceCommand implements MaintenanceCommand {

	private static final String userHome = System.getProperty("user.home");
	private static final Logger logger = LoggerFactory.getLogger(SchemaAnalyzer.class.getName());

	private final static String SCHEMA_ANALYZE_STATUS   = "SCHEMA_ANALYZE_STATUS";


	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final long startTime = System.currentTimeMillis();
		customHeaders.put("start", new Date(startTime).toString());

		final Map<String, Object> broadcastData = new HashMap();
		broadcastData.put("start",   startTime);

		publishBeginMessage(SCHEMA_ANALYZE_STATUS, broadcastData);



		analyzeSchema(SCHEMA_ANALYZE_STATUS);



		final long endTime = System.currentTimeMillis();
		DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

		customHeaders.put("end", new Date(endTime).toString());
		customHeaders.put("duration", duration);


		broadcastData.remove("start");
		broadcastData.put("end", endTime);
		broadcastData.put("duration", duration);
		publishEndMessage(SCHEMA_ANALYZE_STATUS, broadcastData);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	public void analyzeSchema(final String statusMessageType) {

		final String schemaAnalyzerTmpPath = userHome + File.separator + ".structrSchemaAnalyzer";

		final File analyzerFolder = new File(schemaAnalyzerTmpPath + File.separator);
		if (analyzerFolder.exists()) {
			for (final File existingLFC : analyzerFolder.listFiles((File dir, String name) -> name.endsWith(".lfc"))) {
				existingLFC.delete();
			}
		}

		final App app                                       = StructrApp.getInstance();
		final Map<NodeInfo, Collection<Identity>> nodeIdMap = new LinkedHashMap<>();
		final DatabaseService graphDb                       = app.getDatabaseService();
		final ConfigurationProvider configuration           = Services.getInstance().getConfigurationProvider();
		final Set<NodeInfo> nodeTypes                       = new LinkedHashSet<>();
		final Set<RelationshipInfo> relationships           = new LinkedHashSet<>();
		final Map<String, SchemaNode> schemaNodes           = new LinkedHashMap<>();
		final Map<String, List<TypeInfo>> typeInfoTypeMap   = new LinkedHashMap<>();
		final List<TypeInfo> reducedTypeInfos               = new LinkedList<>();
		final List<TypeInfo> typeInfos                      = new LinkedList<>();
		Iterable<Relationship> relIterable                  = null;
		Iterable<Node> nodeIterable                         = null;

		info("Fetching all nodes iterator..");

		try (final Tx tx = app.tx()) {

			nodeIterable = graphDb.getAllNodes();
			tx.success();

		} catch(FrameworkException fex) {
			logger.warn("", fex);
		}

		info("Starting to analyze nodes..");
		publishProgressMessage(statusMessageType, "Starting to analyze nodes..");

		bulkOperation(SecurityContext.getSuperUserInstance(), nodeIterable, 100000, "Analyzing nodes", new BulkGraphOperation<Node>() {

			@Override
			public boolean handleGraphObject(final SecurityContext securityContext, final Node node) throws FrameworkException {

				final NodeInfo nodeInfo = new NodeInfo(node);

				// hashcode of nodeInfo is derived from its property and type signature!
				nodeTypes.add(nodeInfo);

				Collection<Identity> collection = nodeIdMap.get(nodeInfo);
				if (collection == null) {

					collection = new LinkedHashSet<>();
					nodeIdMap.put(nodeInfo, collection);

				}

				// add node ID to collection
				collection.add(node.getId());

				return true;
			}
		});

		info("Identifying common base classes..");
		publishProgressMessage(statusMessageType, "Identifying common base classes..");

		try (final Tx tx = app.tx(true, false, false)) {

			// nodeTypes now contains all existing node types and their property sets
			identifyCommonBaseClasses(app, nodeTypes, nodeIdMap, typeInfos);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}


		info("Collecting type information..");
		publishProgressMessage(statusMessageType, "Collecting type information..");

		try (final Tx tx = app.tx(true, false, false)) {

			// group type infos by type
			collectTypeInfos(typeInfos, typeInfoTypeMap);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		info("Aggregating type information..");
		publishProgressMessage(statusMessageType, "Aggregating type information..");

		try (final Tx tx = app.tx(true, false, false)) {

			// reduce type infos with more than one type
			reduceTypeInfos(typeInfoTypeMap, reducedTypeInfos);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		info("Identifying property sets..");
		publishProgressMessage(statusMessageType, "Identifying property sets..");

		try (final Tx tx = app.tx(true, false, false)) {

			// intersect property sets of type infos
			intersectPropertySets(reducedTypeInfos);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}


		info("Sorting result..");

		try (final Tx tx = app.tx(true, false, false)) {

			// sort type infos
			Collections.sort(reducedTypeInfos, new HierarchyComparator(false));

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		publishProgressMessage(statusMessageType, "Starting with setting node types for identified types.");

		final Map<String, TypeInfo> reducedTypeInfoMap = new LinkedHashMap<>();

		for (final TypeInfo info : reducedTypeInfos) {

			final String type = info.getPrimaryType();

			// map TypeInfo to type for later use
			reducedTypeInfoMap.put(type, info);

			info("Starting with setting of type and ID for type {}", type);

			bulkOperation(SecurityContext.getSuperUserInstance(), info.getNodeIds(), 10000, "Setting type and ID", new BulkGraphOperation<Identity>() {

				@Override
				public boolean handleGraphObject(SecurityContext securityContext, Identity nodeId) throws FrameworkException {

					final Node node = graphDb.getNodeById(nodeId);

					node.setProperty("id", NodeServiceCommand.getNextUuid());
					node.setProperty("type", type);

					return true;
				}
			});
		}

		info("Fetching all relationships iterator..");

		try (final Tx tx = app.tx(true, false, false)) {

			relIterable = graphDb.getAllRelationships();
			tx.success();

		} catch(FrameworkException fex) {
			logger.warn("", fex);
		}

		info("Starting with analyzing relationships..");
		publishProgressMessage(statusMessageType, "Starting with analyzing relationships..");

		bulkOperation(SecurityContext.getSuperUserInstance(), relIterable, 10000, "Analyzing relationships", new BulkGraphOperation<Relationship>() {

			@Override
			public boolean handleGraphObject(SecurityContext securityContext, Relationship rel) throws FrameworkException {

				final Node startNode          = rel.getStartNode();
				final Node endNode            = rel.getEndNode();

				// make sure node has been successfully identified above
				if (startNode.hasProperty("type") && endNode.hasProperty("type")) {

					final String relationshipType = rel.getType().name();
					final String startNodeType    = (String)startNode.getProperty("type");
					final String endNodeType      = (String)endNode.getProperty("type");

					relationships.add(new RelationshipInfo(startNodeType, endNodeType, relationshipType));

					// create combined type on imported relationship
					if (startNodeType != null && endNodeType != null) {

						final String combinedType = getCombinedType(startNodeType, relationshipType, endNodeType);

						logger.debug("Combined relationship type {} found for rel type {}, start node type {}, end node type {}", new Object[]{combinedType, relationshipType, startNodeType, endNodeType});

						rel.setProperty("type", combinedType);
					}

					// create ID on imported relationship
					rel.setProperty("id", NodeServiceCommand.getNextUuid());
				}

				return true;
			}
		});


		info("Grouping relationships..");
		publishProgressMessage(statusMessageType, "Grouping relationships..");

		// group relationships by type
		final Map<String, List<RelationshipInfo>> relTypeInfoMap = new LinkedHashMap<>();
		for (final RelationshipInfo relInfo : relationships) {

			//final String relType         = relInfo.getRelType();
			final String combinedType = getCombinedType(relInfo.getStartNodeType(), relInfo.getRelType(), relInfo.getEndNodeType());
			List<RelationshipInfo> infos = relTypeInfoMap.get(combinedType);

			if (infos == null) {

				infos = new LinkedList<>();
				relTypeInfoMap.put(combinedType, infos);
			}

			infos.add(relInfo);
		}

		info("Aggregating relationship information..");
		publishProgressMessage(statusMessageType, "Aggregating relationship information..");

		final List<RelationshipInfo> reducedRelationshipInfos = new ArrayList<>();
		if (Settings.InheritanceDetection.getValue()) {

			// reduce relationship infos into one
			for (final List<RelationshipInfo> infos : relTypeInfoMap.values()) {

				reducedRelationshipInfos.addAll(reduceNodeTypes(infos, reducedTypeInfoMap));
			}

		} else {

			reducedRelationshipInfos.addAll(relationships);
		}

		info("Starting with schema node creation..");
		publishProgressMessage(statusMessageType, "Starting with schema node creation for " + reducedTypeInfos.size() + " node types..");

		bulkOperation(SecurityContext.getSuperUserInstance(), reducedTypeInfos, 100000, "Creating schema nodes", new BulkGraphOperation<TypeInfo>() {

			@Override
			public boolean handleGraphObject(SecurityContext securityContext, TypeInfo typeInfo) throws FrameworkException {

				final String type = typeInfo.getPrimaryType();

				if (!ValidationHelper.isValidStringMatchingRegex(type, schemaNodeNamePattern)) {
					publishWarningMessage("Schema Analysis Error", "Type '" + type + "' does not match regular expression '" + schemaNodeNamePattern + "'<br>Analysis will continue but no changes will be made.");
					logger.warn("Schema Analysis Error: Type '{}' does not match regular expression '{}' - Analysis will continue but no changes will be made.", type, schemaNodeNamePattern);
				}

				if (!"ReferenceNode".equals(type)) {

					final Map<String, Class> props = typeInfo.getPropertySet();
					final PropertyMap propertyMap  = new PropertyMap();

					// add properties
					for (final Map.Entry<String, Class> propertyEntry : props.entrySet()) {

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
					propertyMap.put(Traits.nameProperty(), type);

					// check if there is an existing Structr entity with the same type and make the dynamic class extend the existing class if yes.
					final Traits existingType = Traits.of(type);
					if (existingType != null && !existingType.getName().equals("org.structr.dynamic." + type)) {

						final NodeInterface schemaNode = app.nodeQuery("SchemaNode").andName(type).getFirst();
						if (schemaNode != null) {

							propertyMap.put(Traits.of("SchemaNode").key("extendsClass"), schemaNode);
						}

					} else if (!typeInfo.getOtherTypes().isEmpty()) {

						final String superclassName = typeInfo.getSuperclass(reducedTypeInfoMap);
						final NodeInterface schemaNode = app.nodeQuery("SchemaNode").andName(superclassName).getFirst();

						if (schemaNode != null) {

							propertyMap.put(Traits.of("SchemaNode").key("extendsClass"), schemaNode);
						}

						/*
						// only the first supertype is supported
						propertyMap.put(SchemaNode.extendsClass, typeInfo.getSuperclass(reducedTypeInfoMap));
						*/
					}

					final NodeInterface existingNode = app.nodeQuery("SchemaNode").andName(type).getFirst();
					if (existingNode != null) {

						for (final Map.Entry<PropertyKey, Object> entry : propertyMap.entrySet()) {

							existingNode.setProperty(entry.getKey(), entry.getValue());
						}

						schemaNodes.put(type, existingNode.as(SchemaNode.class));

					} else {

						// create schema node
						schemaNodes.put(type, app.create("SchemaNode", propertyMap).as(SchemaNode.class));
					}
				}

				return true;
			}
		});


		info("Starting with schema relationship creation..");
		publishProgressMessage(statusMessageType, "Starting with schema relationship creation for " + reducedRelationshipInfos.size() + " relationship types..");

		bulkOperation(SecurityContext.getSuperUserInstance(), reducedRelationshipInfos, 100000, "Creating schema relationships", new BulkGraphOperation<RelationshipInfo>() {

			@Override
			public boolean handleGraphObject(SecurityContext securityContext, RelationshipInfo template) throws FrameworkException {

				final String startNodeType    = template.getStartNodeType();
				final String endNodeType      = template.getEndNodeType();

				if (startNodeType != null && endNodeType != null) {

					final SchemaNode startNode    = schemaNodes.get(startNodeType);
					final SchemaNode endNode      = schemaNodes.get(endNodeType);

					if (startNode != null && endNode != null) {

						final String relationshipType = template.getRelType();
						final PropertyMap propertyMap = new PropertyMap();
						final Traits traits           = Traits.of("SchemaRelationshipNode");

						propertyMap.put(traits.key("sourceId"),         startNode.getUuid());
						propertyMap.put(traits.key("targetId"),         endNode.getUuid());
						propertyMap.put(traits.key("relationshipType"), relationshipType);

						app.create("SchemaRelationshipNode", propertyMap);

					} else {

						info("Unable to create schema relationship node for {} -> {}, no schema nodes found", startNodeType, endNodeType);
						publishWarningMessage("Unable to create schema relationship node", "No schema nodes found for relationship: " + startNodeType + " -> " + endNodeType);

					}
				}

				return true;
			}
		});

		info("Starting with index rebuild..");
		publishProgressMessage(statusMessageType, "Starting with index rebuild..");

		// rebuild index
		app.command(BulkRebuildIndexCommand.class).execute(Collections.EMPTY_MAP);
	}

	public void importCypher(final List<String> sources) {

		final App app = StructrApp.getInstance();

		// nothing to do
		if (sources.isEmpty()) {
			return;
		}

		//
		try (final Tx tx = app.tx(true, false, false)) {

			// first step: execute cypher queries
			for (final String source : sources) {

				// be very tolerant here, just execute everything
				Iterables.count(app.query(source, Collections.emptyMap()));
			}

			tx.success();

		} catch (FrameworkException fex) {}
	}

	// ----- private static methods -----
	private static String getCombinedType(final String startNodeType, final String relationshipType, final String endNodeType) {

		return startNodeType.concat(relationshipType).concat(endNodeType);

	}

	private static void identifyCommonBaseClasses(final App app, final Set<NodeInfo> nodeTypes, final Map<NodeInfo, Collection<Identity>> nodeIds, final List<TypeInfo> typeInfos) {

		// next we need to identify common base classes, which can be found by
		// finding all NodeInfo entries that share at least one type

		for (final NodeInfo nodeInfo : nodeTypes) {

			final Set<String> allTypes = nodeInfo.getTypes();
			for (final String type : allTypes) {

				final TypeInfo typeInfo = new TypeInfo(type, allTypes, nodeIds.get(nodeInfo));
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

		for (final Map.Entry<String, List<TypeInfo>> entry : typeInfoTypeMap.entrySet()) {

			final List<TypeInfo> listOfTypeInfosWithSamePrimaryType = entry.getValue();
			TypeInfo firstTypeInfo                                  = null;

			for (final TypeInfo typeInfo : listOfTypeInfosWithSamePrimaryType) {

				if (firstTypeInfo == null) {

					firstTypeInfo = typeInfo;

				} else {

					firstTypeInfo.combinePropertySets(typeInfo.getPropertySet());

					// "save" node references for later use
					firstTypeInfo.getNodeIds().addAll(typeInfo.getNodeIds());
				}
			}

			if (firstTypeInfo != null) {

				// firstTypeInfo now contains the intersection of all type infos of a given type
				reducedTypeInfos.add(firstTypeInfo);

				// set hierarchy level
				firstTypeInfo.setHierarchyLevel(listOfTypeInfosWithSamePrimaryType.size());
			}
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

		final List<RelationshipInfo> reducedList = new ArrayList<>();
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
		final List<Set<TypeInfo>> listOfSetsOfTypes = new ArrayList<>();

		// iterate types
		for (String type : types) {

			final Set<TypeInfo> listOfTypes = new LinkedHashSet<>();
			String currentType              = type;

			listOfSetsOfTypes.add(listOfTypes);

			while (currentType != null) {

				// fetch type info
				final TypeInfo typeInfo = typeInfos.get(currentType);
				if (typeInfo != null) {

					listOfTypes.add(typeInfo);
					currentType = typeInfo.getSuperclass(typeInfos);

				} else {

					// no type info => exit loop
					currentType = null;
				}
			}
		}

		// try to find a common element in all the sets
		final Set<TypeInfo> intersection = new LinkedHashSet<>();
		boolean first                    = true;

		for (final Set<TypeInfo> set : listOfSetsOfTypes) {

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

			final List<TypeInfo> typeInfoList = new ArrayList<>(intersection);

			// sort list according to type hierarchy
			Collections.sort(typeInfoList, new HierarchyComparator(false));

			// return first element, because we cannot decide (yet)
			// which of the multiple common base classes we want

			return typeInfoList.get(0).getPrimaryType();
		}

		return null;
	}

	static class HierarchyComparator implements Comparator<TypeInfo> {

		private boolean reverse = false;

		public HierarchyComparator(final boolean reverse) {
			this.reverse = reverse;
		}

		@Override
		public int compare(TypeInfo o1, TypeInfo o2) {

			if (reverse) {

				return Integer.valueOf(o1.getHierarchyLevel()).compareTo(o2.getHierarchyLevel());
			} else {

				return Integer.valueOf(o2.getHierarchyLevel()).compareTo(o1.getHierarchyLevel());
			}
		}
	}

}