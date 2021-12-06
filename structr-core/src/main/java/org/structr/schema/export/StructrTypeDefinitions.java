/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.result.OpenAPIExampleAnyResult;
import org.structr.schema.openapi.schema.OpenAPIResultSchema;
import org.structr.schema.openapi.schema.OpenAPIStructrTypeSchemaOutput;

/**
 *
 *
 */
public class StructrTypeDefinitions implements StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrTypeDefinitions.class);


	private final Set<StructrRelationshipTypeDefinition> relationships = new TreeSet<>();
	private final Set<StructrTypeDefinition> typeDefinitions           = new TreeSet<>();
	private StructrSchemaDefinition root                               = null;

	public static Set<String> openApiSerializedSchemaTypes = new TreeSet<String>();

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

		final Map<String, SchemaNode> schemaNodes = new LinkedHashMap<>();

		// collect list of schema nodes
		app.nodeQuery(SchemaNode.class).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

		// iterate type definitions
		for (final StructrTypeDefinition type : typeDefinitions) {

			final AbstractSchemaNode schemaNode = type.createDatabaseSchema(schemaNodes, app);
			if (schemaNode != null) {

				type.setSchemaNode(schemaNode);
			}
		}

		// resolve inheritance relationships
		for (final StructrTypeDefinition type : typeDefinitions) {
			type.resolveInheritanceRelationships(schemaNodes);
		}

		// resolve schema relationships
		for (final StructrRelationshipTypeDefinition rel : relationships) {
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

		return schemas;
	}

	public Map<String, Object> serializeOpenAPIResponses(final Map<String, Object> responses, final String tag) {
		final Map<String, Object> map = new TreeMap<>();

		for (final StructrTypeDefinition type : typeDefinitions) {

			if (type.isSelected(tag) && type.includeInOpenAPI()) {

				ConfigurationProvider configuration = StructrApp.getConfiguration();
				Class typeClass = configuration.getNodeEntityClass(type.name);

				if (typeClass == null) {
					Map<String, Class> interfaces = configuration.getInterfaces();
					typeClass = interfaces.get(type.name);
				};

				Set<String> viewNames = configuration.getPropertyViewsForType(typeClass);
				viewNames = viewNames.stream().filter(viewName ->  StringUtils.equals(viewName, "all") || !StructrTypeDefinition.VIEW_BLACKLIST.contains(viewName)).collect(Collectors.toSet());

				for (String viewName : viewNames) {

					final String typeName = type.getName() + (viewName == null || StringUtils.equals(PropertyView.Public, viewName) ? "" : "." + viewName);
					Map<String, Object> multipleItemsMap = new HashMap<>();

					map.put(typeName + "MultipleResponse",
							new OpenAPIRequestResponse("The request was executed successfully.",
									new OpenAPISchemaReference(type, viewName),
									new OpenAPIExampleAnyResult(List.of(), true),
									null,
									true,
									"array"
							)
					);

					map.put(typeName + "SingleResponse",
						new OpenAPIRequestResponse("The request was executed successfully.",
							new OpenAPISchemaReference(type, viewName),
							new OpenAPIExampleAnyResult(Map.of(), true)
						)
					);
				}
			}
		}

		return map;
	}

	private Map<String, Object> serializeOpenAPIForTypes (Set<StructrTypeDefinition> typeDefinitions, final Map<String, Object> schemas, final String tag, String view) {

		final Map<String, Object> map = new TreeMap<>();

		for (final StructrTypeDefinition type : typeDefinitions) {
			final Set<String> typeWhiteList = new LinkedHashSet<>(Arrays.asList("User", "File", "Image", "NodeInterface"));

			if (StringUtils.isNotEmpty(view) || typeWhiteList.contains(type.getName()) || type.isSelected(tag) && type.includeInOpenAPI()) {

				ConfigurationProvider configuration = StructrApp.getConfiguration();
				Class typeClass = configuration.getNodeEntityClass(type.name);

				if (typeClass == null) {
					Map<String, Class> interfaces = configuration.getInterfaces();
					typeClass = interfaces.get(type.name);
				};

				Set<String> viewNames = configuration.getPropertyViewsForType(typeClass);
				viewNames = viewNames.stream().filter(viewName -> {
					if (StringUtils.isNotEmpty(view) && !StringUtils.equals(view, viewName)) {
						return false;
					}
					return StringUtils.equals(viewName, "all") || !StructrTypeDefinition.VIEW_BLACKLIST.contains(viewName);
				}).collect(Collectors.toSet());

				for (String viewName : viewNames) {

					final List<Map<String, Object>> allOf = new LinkedList<>();
					final Map<String, Object> typeMap = new TreeMap<>();
					final String typeName = type.getName() + (viewName == null || StringUtils.equals(PropertyView.Public, viewName) ? "" : "." + viewName);

					map.put(typeName, typeMap);
					typeMap.put("allOf", allOf);

					// base type must be resolve and added as well, but only if base type isn't included in tag itself.
					final URI baseTypeReference = type.getExtends();

					if (StringUtils.isEmpty(view) && baseTypeReference != null && !StringUtils.equals(viewName, PropertyView.All)) {

						final Object def = root.resolveURI(baseTypeReference);
						if (def instanceof StructrTypeDefinition) {
							final StructrTypeDefinition baseType = (StructrTypeDefinition) def;
							if (!schemas.containsKey(baseType.getName()) && !baseType.includeInOpenAPI()) {
								OpenAPIStructrTypeSchemaOutput openAPIStructrTypeSchemaOutput = new OpenAPIStructrTypeSchemaOutput(baseType, PropertyView.Public, 0);
								schemas.put(baseType.getName(), openAPIStructrTypeSchemaOutput);
							}
						}
					}

					final String reference = type.resolveTypeReferenceForOpenAPI(type.getExtends());
					if (StringUtils.isEmpty(view) && reference != null ) {

						allOf.add(new OpenAPISchemaReference(reference, PropertyView.Public));

					} else if (StringUtils.isEmpty(view)) {

						// default base type AbstractNode
						allOf.add(new OpenAPISchemaReference("#/components/schemas/AbstractNode", PropertyView.Public));
					}

					// add actual type definition
					allOf.add(new OpenAPIStructrTypeSchemaOutput(type, viewName, 0));

				}
			}
		}

		return map;
	}

	public Map<String, Object> serializeOpenAPIOperations(final String tag) {

		final Map<String, Object> map = new TreeMap<>();

		for (final StructrTypeDefinition type : typeDefinitions) {

			if (type.isSelected(tag) && (StringUtils.isNotBlank(tag) && type.includeInOpenAPI())) {

				map.putAll(type.serializeOpenAPIOperations(tag));
			}
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

					if (type instanceof StructrRelationshipTypeDefinition) {
						relationships.add((StructrRelationshipTypeDefinition)type);
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
		app.nodeQuery(SchemaNode.class).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

		// iterate
		for (final SchemaNode schemaNode : schemaNodes.values()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(schemaNodes, root, schemaNode);
			if (type != null) {

				typeDefinitions.add(type);
			}
		}

		for (final SchemaRelationshipNode schemaRelationship : app.nodeQuery(SchemaRelationshipNode.class).getAsList()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(schemaNodes, root, schemaRelationship);
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

	void diff(final StructrTypeDefinitions other) throws FrameworkException {

		final Map<String, StructrTypeDefinition> databaseTypes = getMappedTypes();
		final Map<String, StructrTypeDefinition> structrTypes  = other.getMappedTypes();
		final Set<String> typesOnlyInDatabase                  = new TreeSet<>(databaseTypes.keySet());
		final Set<String> typesOnlyInStructrSchema             = new TreeSet<>(structrTypes.keySet());
		final Set<String> bothTypes                            = new TreeSet<>(databaseTypes.keySet());

		typesOnlyInDatabase.removeAll(structrTypes.keySet());
		typesOnlyInStructrSchema.removeAll(databaseTypes.keySet());
		bothTypes.retainAll(structrTypes.keySet());

		// types that exist in the only database
		for (final String key : typesOnlyInDatabase) {

			final StructrTypeDefinition type = databaseTypes.get(key);
			if (type.isBuiltinType()) {

				handleRemovedBuiltInType(type);

			} else {

				// type should be ok, probably created by user
			}
		}

		// nothing to do for this set, these types can simply be created without problems
		//System.out.println(typesOnlyInStructrSchema);


		// find detailed differences in the intersection of both schemas
		for (final String name : bothTypes) {

			final StructrTypeDefinition localType = databaseTypes.get(name);
			final StructrTypeDefinition otherType = structrTypes.get(name);

			// compare types
			localType.diff(otherType);
		}

		// the same must be done for global methods and relationships!
	}

	private Map<String, StructrTypeDefinition> getMappedTypes() {

		final LinkedHashMap<String, StructrTypeDefinition> mapped = new LinkedHashMap<>();

		for (final StructrTypeDefinition def : getTypes()) {

			mapped.put(def.getName(), def);
		}

		return mapped;
	}

	private void handleRemovedBuiltInType(final StructrTypeDefinition type) throws FrameworkException {

		logger.warn("Built-in type {} was removed or renamed in the current version of the Structr schema, deleting.", type.getName());

		// We can not determine yet if the type was deleted or renamed, so we need to delete it..
		StructrApp.getInstance().delete(type.getSchemaNode());
	}
}
