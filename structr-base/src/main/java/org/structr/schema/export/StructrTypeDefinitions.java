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
package org.structr.schema.export;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.FunctionProperty;
import org.structr.core.traits.Traits;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaService;
import org.structr.schema.openapi.common.OpenAPIAllOf;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.result.OpenAPIExampleAnyResult;
import org.structr.schema.openapi.result.OpenAPIMultipleResponseSchema;
import org.structr.schema.openapi.result.OpenAPISingleResponseSchema;
import org.structr.schema.openapi.schema.OpenAPIStructrTypeSchemaOutput;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class StructrTypeDefinitions implements StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrTypeDefinitions.class);

	private final Set<StructrRelationshipTypeDefinition> relationships = new TreeSet<>();
	private final Set<StructrTypeDefinition> typeDefinitions           = new TreeSet<>();
	private StructrSchemaDefinition root                               = null;

	public static ConcurrentSkipListSet<String> openApiSerializedSchemaTypes = new ConcurrentSkipListSet<>();

	StructrTypeDefinitions(final StructrSchemaDefinition root) {
		this.root = root;
	}

	public Set<StructrTypeDefinition> getTypes() {
		return typeDefinitions;
	}

	public JsonType getType(final String name, final boolean create) {

		for (final JsonType type : typeDefinitions) {

			if (name.equals(type.getName())) {
				return type;
			}
		}

		if (create) {

			// create
			return addType(name);
		}

		return null;
	}

	public JsonObjectType addType(final String name) {

		if ("XMPPClient".equals(name)) {
			Thread.dumpStack();
		}

		final JsonType type = getType(name, false);
		if (type != null) {

			return (JsonObjectType)type;
		}

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, name);

		typeDefinitions.add(def);

		return def;

	}

	public void removeType(final String name) {

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, name);

		typeDefinitions.remove(def);

	}

	public void createDatabaseSchema(final App app, final JsonSchema.ImportMode importMode) throws FrameworkException {

		final Map<String, SchemaRelationshipNode> schemaRels = new LinkedHashMap<>();
		final Map<String, SchemaNode> schemaNodes            = new LinkedHashMap<>();
		final Set<String> blacklist                          = SchemaService.getBlacklist();

		// collect list of schema nodes
		app.nodeQuery("SchemaNode").getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n.as(SchemaNode.class)); });
		app.nodeQuery("SchemaRelationshipNode").getAsList().stream().forEach(n -> { schemaRels.put(n.getName(), n.as(SchemaRelationshipNode.class)); });

		// iterate type definitions
		for (final StructrTypeDefinition type : typeDefinitions) {

			if (type.isBlacklisted(blacklist)) {

				schemaNodes.remove(type.getName());
				continue;
			}

			final AbstractSchemaNode schemaNode = type.createDatabaseSchema(schemaNodes, schemaRels, app);
			if (schemaNode != null) {

				type.setSchemaNode(schemaNode);
			}
		}

		// resolve schema relationships
		for (final StructrRelationshipTypeDefinition rel : relationships) {

			if (rel.isBlacklisted(blacklist)) {
				continue;
			}

			rel.resolveEndpointTypesForDatabaseSchemaCreation(schemaNodes, app);
		}
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {

		for (final StructrTypeDefinition type : typeDefinitions) {

			if (key.equals(type.getName())) {
				return type;
			}
		}

		return null;
	}

	public Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		for (final StructrTypeDefinition type : typeDefinitions) {

			map.put(type.getName(), type.serialize());
		}

		return map;
	}

	public Map<String, Object> serializeOpenAPI(final Map<String, Object> schemas, final String tag) {

		final Map<String, Object> map = new TreeMap<>();

		map.putAll(this.serializeOpenAPIForTypes(typeDefinitions, schemas, tag, null));

		schemas.putAll(map);

		while (!StructrTypeDefinitions.openApiSerializedSchemaTypes.isEmpty()) {

			Set<StructrTypeDefinition> typesInReferencesButNotInOutputYet = new HashSet<>();
			for (final String schemaReference : StructrTypeDefinitions.openApiSerializedSchemaTypes) {
				for (StructrTypeDefinition type : typeDefinitions) {
					if (!schemas.containsKey(schemaReference) && StringUtils.equals(type.getName(), schemaReference)) {
						typesInReferencesButNotInOutputYet.add(type);
					}
				}
			}

			StructrTypeDefinitions.openApiSerializedSchemaTypes.clear();

			final Map<String, Object> otherReferencesMap = new TreeMap<>();
			if (!typesInReferencesButNotInOutputYet.isEmpty()) {
				otherReferencesMap.putAll(serializeOpenAPIForTypes(typesInReferencesButNotInOutputYet, schemas, tag, PropertyView.Public));
			}

			schemas.putAll(otherReferencesMap);
		}

		// now generate the response schemas for each type schema reference
		// Single and Multiple ResponseSchemas only differ in the 'result' key. It's either object or array.
		final Iterator<Entry<String, Object>> iterator = schemas.entrySet().iterator();
		final Map<String, Object> additionalSchemas    = new HashMap<>();

		while (iterator.hasNext()) {

			final Entry<String, Object> entry = iterator.next();

			additionalSchemas.put(entry.getKey() + "SingleResponseSchema", new OpenAPIAllOf(
				new OpenAPISchemaReference("GetBaseResponse"),
				new OpenAPISingleResponseSchema(new OpenAPISchemaReference(entry.getKey()))
			));

			additionalSchemas.put(entry.getKey() + "MultipleResponseSchema", new OpenAPIAllOf(
				new OpenAPISchemaReference("GetBaseResponse"),
				new OpenAPIMultipleResponseSchema(new OpenAPISchemaReference(entry.getKey()))
			));
		}

		StructrTypeDefinitions.openApiSerializedSchemaTypes.clear();
		schemas.putAll(additionalSchemas);

		return schemas;
	}

	public Map<String, Object> serializeOpenAPIResponses(final Map<String, Object> responses, final String tag) {

		final ConfigurationProvider configuration = StructrApp.getConfiguration();
		final Map<String, Object> map             = new TreeMap<>();

		for (final StructrTypeDefinition<?> type : typeDefinitions) {

			if (type.isSelected(tag) && type.includeInOpenAPI()) {

				final Traits traits         = Traits.of(type.name);
				final Set<String> viewNames = traits.getViewNames();

				viewNames.removeAll(StructrTypeDefinition.VIEW_BLACKLIST);
				viewNames.remove("all");

				for (String viewName : viewNames) {

					final String typeName = type.getName() + (viewName == null || StringUtils.equals(PropertyView.Public, viewName) ? "" : "." + viewName);

					String viewNameStringForReference = StringUtils.equals(viewName, PropertyView.Public) ? "" : "." + viewName;
					map.put(typeName + "MultipleResponse",
							new OpenAPIRequestResponse(
								"The request was executed successfully.",
								new OpenAPISchemaReference(type.getName() + viewNameStringForReference + "MultipleResponseSchema"),
								new OpenAPIExampleAnyResult(List.of(), true),
								null,
								false,
								null
							)
					);

					map.put(typeName + "SingleResponse",
						new OpenAPIRequestResponse("The request was executed successfully.",
							new OpenAPISchemaReference(type.getName() + viewNameStringForReference + "SingleResponseSchema"),
							new OpenAPIExampleAnyResult(Map.of(), true),
							null,
							false,
							null
						)
					);
				}

				for (final StructrMethodDefinition method : type.methods) {

					if (method.isSelected(tag)) {

						map.put(type.getName() + "." + method.getName() + "MethodResponse",
							new OpenAPIRequestResponse("The request was executed successfully.",
								new OpenAPISchemaReference(type.getName() + "." + method.getName() + "ResponseSchema", null)
							)
						);
					}
				}
			}
		}

		return map;
	}

	private Set<String> getViewNamesOfType(final StructrTypeDefinition type, final String view) {

		final Traits traits         = Traits.of(type.name);
		final Set<String> viewNames = traits.getViewNames();

		viewNames.remove(StructrTypeDefinition.VIEW_BLACKLIST);
		viewNames.remove("all");

		// fixme: what does this do??
		/*
		viewNames = viewNames.stream().filter(viewName -> {
			if (StringUtils.isNotEmpty(view) && !StringUtils.equals(view, viewName)) {
				return false;
			}
			return StringUtils.equals(viewName, "all") || !StructrTypeDefinition.VIEW_BLACKLIST.contains(viewName);
		}).collect(Collectors.toSet());
		*/

		return viewNames;
	}

	private Map<String, Object> serializeOpenAPIForTypes(final Set<StructrTypeDefinition> typeDefinitions, final Map<String, Object> schemas, final String tag, String view) {

		final Set<String> typeWhiteList = new LinkedHashSet<>(Arrays.asList("User", "File", "Image", "NodeInterface"));
		final Map<String, Object> map   = new TreeMap<>();

		for (final StructrTypeDefinition<?> type : typeDefinitions) {

			if (StringUtils.isNotEmpty(view) || typeWhiteList.contains(type.getName()) || type.isSelected(tag) && type.includeInOpenAPI()) {

				for (final String viewName : this.getViewNamesOfType(type, view)) {

					final List<Map<String, Object>> allOf = new LinkedList<>();
					final Map<String, Object> typeMap = new TreeMap<>();
					final String typeName = type.getName() + (viewName == null || StringUtils.equals(PropertyView.Public, viewName) ? "" : "." + viewName);

					map.put(typeName, typeMap);
					typeMap.put("allOf", allOf);

					// default base type AbstractNode
					allOf.add(new OpenAPISchemaReference("#/components/schemas/AbstractNode", PropertyView.Public));

					// add actual type definition
					allOf.add(new OpenAPIStructrTypeSchemaOutput(type, viewName, 0));

					// genereate schema for readFunction returnType
					type.visitProperties(key -> {

						if (key instanceof FunctionProperty && StringUtils.isEmpty(key.typeHint())) {
							map.put(
								type.getName() + "." + key.jsonName() + "PropertySchema",
								key.describeOpenAPIOutputSchema(typeName, viewName)
							);
						}

					}, viewName);
				}

				for (final StructrMethodDefinition method : type.methods) {

					if (method.isSelected(tag)) {

						map.put(type.getName() + "." + method.getName() + "ResponseSchema",
								method.getOpenAPISuccessResponse()
						);
					}
				}
			}
		}

		return map;
	}

	public Map<String, Object> serializeOpenAPIOperations(final String tag) {

		final App app                 = StructrApp.getInstance();
		final Map<String, Object> map = new TreeMap<>();

		try (final Tx tx = app.tx()) {

			for (final StructrTypeDefinition type : typeDefinitions) {

				if (type.isSelected(tag) && (StringUtils.isNotBlank(tag) && type.includeInOpenAPI())) {

					Set<String> viewNames = this.getViewNamesOfType(type, null);

					map.putAll(type.serializeOpenAPIOperations(tag, viewNames));
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

		}

		return map;
	}

	// ----- package methods -----
	void deserialize(final Map<String, Object> source) {
		deserialize(source, null);
	}

	void deserialize(final Map<String, Object> source, final List<String> nodeIds) {

		for (final Entry<String, Object> entry : source.entrySet()) {

			final String name  = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof Map) {

				final Map<String, Object> map    = (Map)value;
				final StructrTypeDefinition type = StructrTypeDefinition.deserialize(root, name, map);

				if (type != null) {

					typeDefinitions.add(type);

					if (type instanceof StructrRelationshipTypeDefinition r) {
						relationships.add(r);
					}

				}

			} else {

				throw new IllegalStateException("Invalid JSON object for " + name + ", expected object, got " + value.getClass().getSimpleName());
			}
		}

		// initialize reference properties after all types are done
		for (final StructrTypeDefinition type : typeDefinitions) {
			type.initializeReferenceProperties();
		}
	}

	void deserialize(final App app) throws FrameworkException {

		final Map<String, SchemaNode> schemaNodes = new LinkedHashMap<>();

		// collect list of schema nodes
		app.nodeQuery("SchemaNode").getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n.as(SchemaNode.class)); });

		// iterate
		for (final SchemaNode schemaNode : schemaNodes.values()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(schemaNodes, root, schemaNode);
			if (type != null) {

				try {
					typeDefinitions.add(type);

				} catch (Throwable t) {

					System.out.println(schemaNode.getName());
					t.printStackTrace();
				}
			}
		}

		for (final NodeInterface node : app.nodeQuery("SchemaRelationshipNode").getAsList()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(schemaNodes, root, node.as(SchemaRelationshipNode.class));
			if (type != null) {

				typeDefinitions.add(type);

				if (type instanceof StructrRelationshipTypeDefinition) {
					relationships.add((StructrRelationshipTypeDefinition)type);
				}
			}
		}

	}

	void addType(final StructrTypeDefinition type) {
		typeDefinitions.add(type);
	}

	void removeType(final StructrTypeDefinition type) {
		typeDefinitions.remove(type);
	}

	Set<StructrRelationshipTypeDefinition> getRelationships() {
		return relationships;
	}

	void diff(final StructrTypeDefinitions staticSchema) throws FrameworkException {

		final Map<String, StructrTypeDefinition> databaseTypes = getMappedTypes();
		final Map<String, StructrTypeDefinition> structrTypes  = staticSchema.getMappedTypes();
		final Set<String> typesOnlyInDatabase                  = new TreeSet<>(databaseTypes.keySet());
		final Set<String> typesOnlyInStructrSchema             = new TreeSet<>(structrTypes.keySet());
		final Set<String> bothTypes                            = new TreeSet<>(databaseTypes.keySet());

		final Set<String> toMigrate = Set.of(
			"AbstractMinifiedFileMINIFICATIONFile", "ImagePICTURE_OFUser", "ImageTHUMBNAILImage", "UserHOME_DIRFolder", "UserWORKING_DIRFolder",
			"AbstractFileCONTAINS_NEXT_SIBLINGAbstractFile", "FolderCONTAINSAbstractFile", "FolderCONTAINSFile", "FolderCONTAINSFolder",
			"FolderCONTAINSImage", "VideoFileHAS_CONVERTED_VIDEOVideoFile", "VideoFileHAS_POSTER_IMAGEImage",
			"AbstractFileCONFIGURED_BYStorageConfiguration"
		);

		typesOnlyInDatabase.removeAll(structrTypes.keySet());
		typesOnlyInStructrSchema.removeAll(databaseTypes.keySet());
		bothTypes.retainAll(structrTypes.keySet());

		// types that exist in the only database
		for (final String key : typesOnlyInDatabase) {

			final StructrTypeDefinition type = databaseTypes.get(key);
			if (type.isBuiltinType() || toMigrate.contains(key)) {

				handleRemovedBuiltInType(type);

			} else {

				// type should be ok, probably created by user
			}
		}

		// find detailed differences in the intersection of both schemas
		for (final String name : bothTypes) {

			final StructrTypeDefinition localType = databaseTypes.get(name);
			final StructrTypeDefinition otherType = structrTypes.get(name);
			final Traits nodeType                 = getNodeType(name);

			// compare types
			localType.diff(nodeType, otherType);
		}
	}

	private Map<String, StructrTypeDefinition> getMappedTypes() {

		final LinkedHashMap<String, StructrTypeDefinition> mapped = new LinkedHashMap<>();

		for (final StructrTypeDefinition def : getTypes()) {

			mapped.put(def.getName(), def);
		}

		return mapped;
	}

	private void handleRemovedBuiltInType(final StructrTypeDefinition type) throws FrameworkException {

		/*
		final AbstractSchemaNode schemaNode = type.getSchemaNode();
		if (schemaNode != null) {

			final Traits traits = Traits.of(type.getName());
			if (traits != null) {

				// move extends relationship to internal type
				for (final SchemaNode subtype : schemaNode.getProperty(SchemaNode.extendedByClasses)) {

					subtype.setProperty(SchemaNode.extendsClassInternal, traits.getName());
				}
			}

			StructrApp.getInstance().delete(schemaNode);
		}
		*/
	}

	private Traits getNodeType(final String name) {
		return Traits.of(name);
	}
}
