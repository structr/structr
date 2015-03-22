package org.structr.schema.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;


/**
 *
 * @author Christian Morgner
 */
public class StructrSchemaDefinition extends StructrDefinition implements JsonSchema {

	StructrSchemaDefinition() throws URISyntaxException {
		super(null, JsonSchema.SCHEMA_ID);
	}

	StructrSchemaDefinition(final JsonSchema source) throws URISyntaxException {

		super(null, JsonSchema.SCHEMA_ID);

		put(JsonSchema.KEY_SCHEMA, JsonSchema.SCHEMA_ID);
		createFromSource(source);
	}

	@Override
	public StructrDefinition getParent() {
		return null;
	}

	// ----- interface JsonSchema -----
	@Override
	public Set<JsonType> getTypes() {

		final Map<String, StructrTypeDefinition> types = getTypeDefinitions();
		final Set<JsonType> typeSet              = new TreeSet<>();

		for (final StructrTypeDefinition type : types.values()) {
			typeSet.add(type);
		}

		return typeSet;
	}

	@Override
	public JsonType addType(final String name) throws URISyntaxException {

		final StructrTypeDefinition newType = new StructrTypeDefinition(this, "definitions/" + name);
		newType.setName(name);

		if (getTypeDefinitions().containsKey(name)) {
			throw new IllegalStateException("Type " + name + " already exists.");
		}

		// store new type
		getTypeDefinitions().put(name, newType);

		return newType;
	}

	@Override
	public String toString() {

		final Gson gson           = new GsonBuilder().setPrettyPrinting().create();
		final StringWriter writer = new StringWriter();

		gson.toJson(this, writer);

		return writer.getBuffer().toString();
	}

	@Override
	public String getTitle() {
		return (String)get(JsonSchema.KEY_TITLE);
	}

	@Override
	public void setTitle(final String title) {
		put(JsonSchema.KEY_TITLE, title);
	}

	@Override
	public String getDescription() {
		return (String)get(JsonSchema.KEY_DESCRIPTION);
	}

	@Override
	public void setDescription(final String description) {
		put(JsonSchema.KEY_DESCRIPTION, description);
	}

	// ----- package methods -----
	Map<String, StructrTypeDefinition> getTypeDefinitions() {
		return (Map)getMap(this, JsonSchema.KEY_DEFINITIONS, true);
	}

	void createFromDatabase(final App app) throws FrameworkException, URISyntaxException {

		final List<SchemaNode> types                             = app.nodeQuery(SchemaNode.class).sort(AbstractNode.name).getAsList();
		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();

		for (final SchemaNode schemaNode : types) {

			final String name = schemaNode.getName();
			final StructrTypeDefinition type = new StructrTypeDefinition(this, "definitions/" + name);
			type.createFromDatabase(schemaNode);

			typeDefinitions.put(name, type);
		}
	}

	void createDatabaseSchema(final App app) throws FrameworkException {

		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();
		for (final StructrTypeDefinition type : typeDefinitions.values()) {

			type.setSchemaNode(app.create(SchemaNode.class, type.getName()));
		}

		// Create properties in a separate run because all SchemaNodes must exist
		// before any property is created in order to resolve $ref references.
		for (final StructrTypeDefinition type : typeDefinitions.values()) {
			type.createDatabaseSchemaProperties(app, type.getSchemaNode());
		}
	}

	void createFromSource(final Map<String, Object> source) throws InvalidSchemaException, URISyntaxException {

		setDescription(getString(source, JsonSchema.KEY_DESCRIPTION));
		setTitle(getString(source, JsonSchema.KEY_TITLE));

		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();

		final Map<String, Object> types = getMap(source, KEY_DEFINITIONS, false);
		if (types != null) {

			for (final Entry<String, Object> entry : types.entrySet()) {

				final String key   = entry.getKey();
				final Object value = entry.getValue();

				if (value instanceof Map) {

					final Map<String, Object> map   = (Map<String, Object>)value;
					final StructrTypeDefinition def = new StructrTypeDefinition(this, "definitions/" + key);

					def.setName(key);
					def.createFromSource(map);

					typeDefinitions.put(key, def);

				} else {

					throw new InvalidSchemaException("Type definition " + key + " has wrong type, expecting object.");
				}
			}

		} else {

			throw new InvalidSchemaException("No type definitions found.");
		}

		if (typeDefinitions.isEmpty()) {
			remove(JsonSchema.KEY_DEFINITIONS);
		}
	}

	// ----- private methods -----
	private void createFromSource(final JsonSchema source) throws URISyntaxException {

		setDescription(source.getDescription());
		setTitle(source.getTitle());

		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();

		for (final JsonType type : source.getTypes()) {

			final String name = type.getName();
			typeDefinitions.put(name, new StructrTypeDefinition(this, "definitions/" + name, type));
		}
	}
}
