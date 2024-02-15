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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.InvalidSchemaException;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.entity.*;
import org.structr.core.graph.Tx;

import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * The main class to interact with the Structr Schema API.
 *
 *
 */
public class StructrSchema {

	private static final Logger logger = LoggerFactory.getLogger(StructrSchema.class);

	/**
	 * Creates JsonSchema instance from the current schema in Structr.
	 *
	 * @param app
	 *
	 * @return the current Structr schema
	 *
	 * @throws FrameworkException
	 */
	public static JsonSchema createFromDatabase(final App app) throws FrameworkException {
		return createFromDatabase(app, null);
	}

	/**
	 * Creates JsonSchema instance from the current schema in Structr,
	 * including only a subset of schema nodes.
	 *
	 * @param app
	 * @param types
	 *
	 * @return the current Structr schema
	 *
	 * @throws FrameworkException
	 */
	public static JsonSchema createFromDatabase(final App app, final List<String> types) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchemaDefinition.initializeFromDatabase(app, types);

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
	 * @throws FrameworkException
	 * @throws URISyntaxException
	 * @throws InvalidSchemaException
	 */
	public static JsonSchema createFromSource(final String source) throws InvalidSchemaException, URISyntaxException, FrameworkException {
		return StructrSchema.createFromSource(new StringReader(source));
	}

	/**
	 * Parses a JsonSchema from the given reader.
	 *
	 * @param reader
	 *
	 * @return
	 * @throws FrameworkException
	 * @throws URISyntaxException
	 * @throws InvalidSchemaException
	 */
	public static JsonSchema createFromSource(final Reader reader) throws InvalidSchemaException, URISyntaxException, FrameworkException {

		final Gson gson                      = new GsonBuilder().create();
		final Map<String, Object> rawData    = gson.fromJson(reader, Map.class);

		return StructrSchemaDefinition.initializeFromSource(rawData);
	}

	/**
	 * Creates a minimal JsonSchema object without any classes
	 *
	 * @return
	 * @throws FrameworkException
	 * @throws URISyntaxException
	 * @throws InvalidSchemaException
	 */
	public static JsonSchema createEmptySchema() throws InvalidSchemaException, URISyntaxException, FrameworkException {
		return StructrSchema.createFromSource(JsonSchema.EMPTY_SCHEMA);
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
	 */
	public static void replaceDatabaseSchema(final App app, final JsonSchema newSchema) throws FrameworkException {

		Services.getInstance().setOverridingSchemaTypesAllowed(true);

		try (final Tx tx = app.tx()) {

			app.deleteAllNodesOfType(SchemaRelationshipNode.class);
			app.deleteAllNodesOfType(SchemaNode.class);
			app.deleteAllNodesOfType(SchemaMethod.class);
			app.deleteAllNodesOfType(SchemaMethodParameter.class);
			app.deleteAllNodesOfType(SchemaProperty.class);
			app.deleteAllNodesOfType(SchemaView.class);
			app.deleteAllNodesOfType(SchemaGrant.class);

			newSchema.createDatabaseSchema(JsonSchema.ImportMode.replace);

			tx.success();

		} catch (Throwable ex) {

			ex.printStackTrace();

			if (ex instanceof FrameworkException) {

				throw (FrameworkException)ex;

			} else {

				logger.error(ExceptionUtils.getStackTrace(ex));

				final FrameworkException fex = new FrameworkException(500, "Unable to import schema");
				fex.initCause(ex);

				throw fex;
			}
		}
	}

	/**
	 * Extend the current Structr schema by the elements contained in the given new schema.
	 *
	 * @param app
	 * @param newSchema the new schema to add to the current Structr schema
	 *
	 * @throws FrameworkException
	 */
	public static void extendDatabaseSchema(final App app, final JsonSchema newSchema) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			newSchema.createDatabaseSchema(JsonSchema.ImportMode.extend);

			tx.success();

		} catch (Exception ex) {

			logger.error(ExceptionUtils.getStackTrace(ex));

			if (ex instanceof FrameworkException) {
				throw (FrameworkException)ex;
			}
		}
	}
}
