/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.TransactionCommand;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class StructrSchemaDefinition implements JsonSchema, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrSchemaDefinition.class.getName());

	protected final Set<String> existingPropertyNames       = new TreeSet<>();
	private StructrTypeDefinitions typeDefinitions          = null;
	private StructrGlobalSchemaMethods userDefinedFunctions = null;
	private String description                              = null;
	private String title                                    = null;
	private URI id                                          = null;

	public StructrSchemaDefinition(final URI id) {

		this.typeDefinitions      = new StructrTypeDefinitions(this);
		this.userDefinedFunctions = new StructrGlobalSchemaMethods();
		this.id                   = id;
	}

	@Override
	public URI getId() {
		return id;
	}

	public StructrTypeDefinitions getTypeDefinitionsObject() {
		return typeDefinitions;
	}

	public Set<StructrTypeDefinition> getTypeDefinitions() {
		return typeDefinitions.getTypes();
	}

	public List<Map<String, Object>> getUserDefinedFunctions() {
		return userDefinedFunctions.serialize();
	}

	@Override
	public JsonType getType(final String name) {
		return typeDefinitions.getType(name, true);
	}

	@Override
	public JsonType getType(final String name, final boolean create) {
		return typeDefinitions.getType(name, create);
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(final String title) {
		this.title = title;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(final String description) {
		this.description = description;
	}

	@Override
	public JsonObjectType addType(final String name) {
		return typeDefinitions.addType(name);
	}

	@Override
	public Iterable<JsonType> getTypes() {
		return (Iterable)typeDefinitions.getTypes();
	}

	@Override
	public void removeType(final String name) {
		typeDefinitions.removeType(name);
	}

	@Override
	public String toString() {

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final Map<String, Object> serializedForm = serialize();

		return gson.toJson(serializedForm);
	}

	@Override
	public void createDatabaseSchema(final ImportMode importMode) throws Exception {

		final App app = StructrApp.getInstance();

		typeDefinitions.createDatabaseSchema(app, importMode);
		userDefinedFunctions.createDatabaseSchema(app, importMode);
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {

		if ("definitions".equals(key)) {
			return typeDefinitions;
		}

		return null;
	}

	@Override
	public Object resolveURI(final URI uri) {

		final URI id = getId();
		if (id != null) {

			final URI rel = id.relativize(uri);
			if (!rel.isAbsolute()) {

				final String relString = "#/" + rel.toString();
				return resolveJsonPointer(relString);
			}
		}

		return null;
	}

	@Override
	public String toJsonPointer(final URI uri) {

		final URI pointer = id.relativize(uri);
		if (!pointer.isAbsolute()) {

			final String jsonPointer = pointer.toString();
			if (jsonPointer.startsWith("#/")) {

				return jsonPointer;

			} else if (jsonPointer.startsWith("/")) {

				return "#" + jsonPointer;

			} else {

				return "#/" + jsonPointer;
			}
		}

		return pointer.toString();
	}

	@Override
	public void diff(final JsonSchema schema) throws Exception {

		final StructrSchemaDefinition staticSchema = (StructrSchemaDefinition)schema; // provoke ClassCastException if type doesn't match

		this.typeDefinitions.diff(staticSchema.typeDefinitions);
	}

	public Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		map.put(JsonSchema.KEY_DEFINITIONS, typeDefinitions.serialize());
		map.put(JsonSchema.KEY_METHODS,     userDefinedFunctions.serialize());

		return map;
	}

	public boolean hasMethodSourceCodeInFiles() {

		final Set<StructrTypeDefinition> types = getTypeDefinitions();
		if (!types.isEmpty()) {

			for (final StructrTypeDefinition<?> typeDefinition : types) {

				for (final JsonMethod method : typeDefinition.getMethods()) {

					final String source = method.getSource();
					if (source != null && source.startsWith("./")) {

						return true;
					}
				}
			}
		}

		return false;
	}

	// ----- OpenAPI -----
	public Map<String, Object> serializeOpenAPIOperations(final String tag) {

		final Map<String, Object> operations = new TreeMap<>();

		operations.putAll(userDefinedFunctions.serializeOpenAPIOperations(tag));
		operations.putAll(typeDefinitions.serializeOpenAPIOperations(tag));

		return operations;
	}

	// ----- package methods -----
	void deserialize(final Map<String, Object> source) {

		final Map<String, Object> definitions = (Map<String, Object>) source.get(JsonSchema.KEY_DEFINITIONS);
		if (definitions != null) {

			typeDefinitions.deserialize(definitions);

		} else {

			throw new IllegalStateException("Invalid JSON object for schema definitions, missing value for 'definitions'.");
		}

		final List<Map<String, Object>> userFunctions = (List<Map<String, Object>>) source.get(JsonSchema.KEY_METHODS);
		if (userFunctions != null) {

			userDefinedFunctions.deserialize(userFunctions);

		} else {

			final String title = "Deprecation warning";
			final String text = "This schema snapshot was created with an older version of Structr. More recent versions support global schema methods. Please re-create the snapshot with the latest version to avoid compatibility issues.";

			final Map<String, Object> deprecationBroadcastData = new TreeMap();
			deprecationBroadcastData.put("type", "WARNING");
			deprecationBroadcastData.put("title", title);
			deprecationBroadcastData.put("text", text);
			TransactionCommand.simpleBroadcastGenericMessage(deprecationBroadcastData);

			logger.info(title + ": " + text);
		}

		final Object idValue = source.get(JsonSchema.KEY_ID);
		if (idValue != null) {

			this.id = URI.create(idValue.toString());
		}
	}

	void deserialize(final App app) throws FrameworkException {
		typeDefinitions.deserialize(app);
		userDefinedFunctions.deserialize(app);
	}

	void clearGlobalMethods() {
		userDefinedFunctions.clear();
	}

	StructrDefinition resolveJsonPointer(final String reference) {

		if (reference.startsWith("#")) {

			final String[] parts = reference.substring(1).split("[/]+");
			Object current       = this;

			for (int i = 0; i < parts.length; i++) {

				final String key = parts[i].trim();

				if (StringUtils.isNotBlank(key)) {

					if (StringUtils.isNumeric(key)) {

						final int index = Integer.valueOf(key);

						if (current instanceof List) {

							current = ((List)current).get(index);

						} else {

							throw new IllegalStateException("Invalid JSON pointer " + reference + ", expected array at position " + i + ".");
						}

					} else {

						// fix #/#/
						if ("#".equals(key)) {

							current = this;

						} else {

							if (current instanceof StructrDefinition) {

								current = ((StructrDefinition)current).resolveJsonPointerKey(key);

							} else if (current instanceof Map) {

								current = ((Map)current).get(key);
							}
						}
					}
				}
			}

			if (current instanceof StructrDefinition) {

				return (StructrDefinition)current;
			}
		}

		// invalid JSON pointers return null
		return null;
	}

	void addType(final StructrTypeDefinition type) {
		typeDefinitions.addType(type);
	}

	void addType(final StructrRelationshipTypeDefinition type) {

		typeDefinitions.getRelationships().add(type);
		typeDefinitions.addType(type);
	}

	Set<StructrRelationshipTypeDefinition> getRelationships() {
		return typeDefinitions.getRelationships();
	}

	Set<String> getExistingPropertyNames() {
		return existingPropertyNames;
	}

	// ----- static methods -----
	static JsonSchema initializeFromSource(final Map<String, Object> source) throws FrameworkException {

		final Object idValue = source.get(JsonSchema.KEY_ID);
		URI id               = null;

		if (idValue != null) {

			try {

				id = new URI(idValue.toString());

			} catch (URISyntaxException ex) {
				logger.warn("", ex);
			}

		} else {

			id = getInstanceBasedSchemaURI(StructrApp.getInstance());

		}

		final StructrSchemaDefinition schema = new StructrSchemaDefinition(id);
		schema.deserialize(source);

		return schema;
	}

	private static URI getInstanceBasedSchemaURI (final App app) throws FrameworkException {

		URI id = null;

		try {

			id = new URI("https://structr.org/schema/" + app.getInstanceId() + "/#");

		} catch (URISyntaxException ex) {
			logger.warn("", ex);
		}

		return id;
	}

	static JsonSchema initializeFromDatabase(final App app) throws FrameworkException {
		return initializeFromDatabase(app, null);
	}

	static JsonSchema initializeFromDatabase(final App app, final List<String> types) throws FrameworkException {

		final StructrSchemaDefinition schema = new StructrSchemaDefinition(getInstanceBasedSchemaURI(app));
		schema.deserialize(app);

		if (types != null && !types.isEmpty()) {

			final Set<String> schemaTypes = new LinkedHashSet<>(schema.getTypeDefinitions().stream().map(t -> t.getName()).collect(Collectors.toSet()));

			for (final String toRemove : schemaTypes) {

				if (!types.contains(toRemove)) {

					schema.removeType(toRemove);
				}
			}

			// when user selected types for export he can not have selected global schema methods ==> delete them
			schema.clearGlobalMethods();
		}


		return schema;

	}
}
