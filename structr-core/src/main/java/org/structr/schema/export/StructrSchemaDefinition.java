/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 *
 */
public class StructrSchemaDefinition implements JsonSchema, StructrDefinition {

	protected final Set<String> existingPropertyNames = new TreeSet<>();
	private StructrTypeDefinitions typeDefinitions    = null;
	private String description                        = null;
	private String title                              = null;
	private URI id                                    = null;

	StructrSchemaDefinition(final URI id) {

		this.typeDefinitions = new StructrTypeDefinitions(this);
		this.id              = id;
	}

	@Override
	public URI getId() {
		return id;
	}

	@Override
	public JsonType getType(final String name) {
		return typeDefinitions.getType(name);
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
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public JsonObjectType addType(String name) throws URISyntaxException {
		return typeDefinitions.addType(name);
	}

	@Override
	public String toString() {

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final Map<String, Object> serializedForm = serialize();

		return gson.toJson(serializedForm);
	}

	@Override
	public void createDatabaseSchema(final App app) throws FrameworkException {
		typeDefinitions.createDatabaseSchema(app);
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

	// ----- package methods -----
	Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		map.put(JsonSchema.KEY_DEFINITIONS, typeDefinitions.serialize());
		map.put(JsonSchema.KEY_ID, getId());

		return map;
	}

	void deserialize(final Map<String, Object> source) {

		final Map<String, Object> definitions = (Map<String, Object>)source.get(JsonSchema.KEY_DEFINITIONS);
		if (definitions != null) {

			typeDefinitions.deserialize(definitions);

		} else {

			throw new IllegalStateException("Invalid JSON object for schema definitions, missing value for 'definitions'.");
		}

		final Object idValue = source.get(JsonSchema.KEY_ID);
		if (idValue != null) {

			this.id = URI.create(idValue.toString());
		}
	}

	void deserialize(final App app) throws FrameworkException {
		typeDefinitions.deserialize(app);
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
	static JsonSchema initializeFromSource(final Map<String, Object> source) {

		final Object idValue = source.get(JsonSchema.KEY_ID);
		URI id               = null;

		if (idValue != null) {

			try {

				id = new URI(idValue.toString());

			} catch (URISyntaxException ex) {
				ex.printStackTrace();
			}
		}

		final StructrSchemaDefinition schema = new StructrSchemaDefinition(id);
		schema.deserialize(source);

		return schema;
	}

	static JsonSchema initializeFromDatabase(final App app) throws FrameworkException {

		URI id = null;

		try {

			id = new URI("https://structr.org/schema/" + app.getInstanceId() + "/#");

		} catch (URISyntaxException ex) {
			ex.printStackTrace();
		}


		final StructrSchemaDefinition schema = new StructrSchemaDefinition(id);
		schema.deserialize(app);

		return schema;

	}
}
