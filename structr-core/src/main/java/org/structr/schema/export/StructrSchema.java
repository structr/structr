package org.structr.schema.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.StringReader;
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
 * @author Christian Morgner
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

			final StructrSchemaDefinition schema = new StructrSchemaDefinition();
			schema.createFromDatabase(app);

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

		final StructrSchemaDefinition schema = new StructrSchemaDefinition();
		final Gson gson                      = new GsonBuilder().create();
		final Map<String, Object> rawData    = gson.fromJson(reader, Map.class);

		schema.createFromSource(rawData);

		return schema;
	}

	/**
	 * Creates and returns an empty JsonSchema instance.
	 *
	 * @return
	 */
	public static JsonSchema newInstance() {

		try {
			return new StructrSchemaDefinition();

		} catch (URISyntaxException uex) {

			// something is very wrong if this exception is thrown in the above code...
			uex.printStackTrace();
		}

		return null;
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

			// construct a StructrSchemaDefinition around the given schema
			// and create the corresponding Structr schema nodes
			new StructrSchemaDefinition(newSchema).createDatabaseSchema(app);

			tx.success();
		}
	}
}
