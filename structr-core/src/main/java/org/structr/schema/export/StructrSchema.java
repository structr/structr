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
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.Tx;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonSchema;

/**
 * The main class to interact with the Structr Schema API.
 *
 *
 */
public class StructrSchema {

	/**
	 * Creates JsonSchema instance from the current schema in Structr.
	 *
	 * @param app
	 *
	 * @return the current Structr schema
	 *
	 * @throws FrameworkException
	 * @throws URISyntaxException
	 */
	public static JsonSchema createFromDatabase(final App app) throws FrameworkException, URISyntaxException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchemaDefinition.initializeFromDatabase(app);

			tx.success();

			return schema;
		}
	}

	/**
	 * Parses a JsonSchema from the given string.
	 *
	 * @param source
	 *
	 * @return
	 *
	 * @throws InvalidSchemaException
	 * @throws URISyntaxException
	 */
	public static JsonSchema createFromSource(final String source) throws InvalidSchemaException, URISyntaxException {
		return StructrSchema.createFromSource(new StringReader(source));
	}

	/**
	 * Parses a JsonSchema from the given reader.
	 *
	 * @param reader
	 *
	 * @return
	 *
	 * @throws InvalidSchemaException
	 * @throws URISyntaxException
	 */
	public static JsonSchema createFromSource(final Reader reader) throws InvalidSchemaException, URISyntaxException {

		final Gson gson                      = new GsonBuilder().create();
		final Map<String, Object> rawData    = gson.fromJson(reader, Map.class);

		return StructrSchemaDefinition.initializeFromSource(rawData);
	}

	/**
	 * Creates and returns an empty JsonSchema instance.
	 *
	 * @return
	 */
	public static JsonSchema newInstance(final URI id) {
		return new StructrSchemaDefinition(id);
	}

	/**
	 * Replaces the current Structr schema with the given new schema. This
	 * method is the reverse of createFromDatabase above.
	 *
	 * @param app
	 * @param newSchema the new schema to replace the current Structr schema
	 *
	 * @throws FrameworkException
	 * @throws URISyntaxException
	 */
	public static void replaceDatabaseSchema(final App app, final JsonSchema newSchema) throws FrameworkException, URISyntaxException {

		try (final Tx tx = app.tx()) {

			for (final SchemaRelationshipNode schemaRelationship : app.nodeQuery(SchemaRelationshipNode.class).getAsList()) {
				app.delete(schemaRelationship);
			}

			for (final SchemaNode schemaNode : app.nodeQuery(SchemaNode.class).getAsList()) {
				app.delete(schemaNode);
			}

			for (final SchemaProperty schemaProperty : app.nodeQuery(SchemaProperty.class).getAsList()) {
				app.delete(schemaProperty);
			}

			for (final SchemaView schemaView : app.nodeQuery(SchemaView.class).getAsList()) {
				app.delete(schemaView);
			}
			
			newSchema.createDatabaseSchema(app);

			tx.success();
		}
	}
}
