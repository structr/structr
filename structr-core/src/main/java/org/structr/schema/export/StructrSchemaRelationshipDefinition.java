package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.schema.json.JsonRelationship;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrSchemaRelationshipDefinition extends StructrTypeDefinition implements JsonRelationship {

	public StructrSchemaRelationshipDefinition(final StructrSchemaDefinition root, final String id) throws URISyntaxException {
		super(root, id);
	}

	public StructrSchemaRelationshipDefinition(final StructrSchemaDefinition root, final String id, final JsonType source) throws URISyntaxException {
		super(root, id, source);
	}

	@Override
	public AbstractSchemaNode createDatabaseNode(final App app) throws FrameworkException {
		return app.create(SchemaRelationshipNode.class, getName());
	}

	@Override
	void createFromDatabase(final AbstractSchemaNode schemaNode) throws URISyntaxException {

		final Set<String> requiredProperties = new TreeSet<>();

		readLocalProperties(requiredProperties, schemaNode);
		readViews(schemaNode);
		readMethods(schemaNode);

		// "required"
		if (!requiredProperties.isEmpty()) {
			put(JsonSchema.KEY_REQUIRED, requiredProperties);
		}

		readSuperclass(schemaNode);
	}

	@Override
	public JsonRelationship relate(final JsonType type, final String name) throws URISyntaxException {
		throw new IllegalStateException("Cannot relate relationships.");
	}
}
