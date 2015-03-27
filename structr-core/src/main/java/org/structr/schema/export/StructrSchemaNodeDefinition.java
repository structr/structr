package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.entity.SchemaNode;
import org.structr.schema.json.JsonNode;
import org.structr.schema.json.JsonRelationship;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrSchemaNodeDefinition extends StructrTypeDefinition implements JsonNode {

	public StructrSchemaNodeDefinition(final StructrSchemaDefinition root, final String id) throws URISyntaxException {
		super(root, id);
	}

	public StructrSchemaNodeDefinition(final StructrSchemaDefinition root, final String id, final JsonType source) throws URISyntaxException {
		super(root, id, source);
	}

	@Override
	public JsonRelationship relate(final JsonType type, final String relationship, final Cardinality cardinality) throws URISyntaxException {

		final String name = getName() + relationship + type.getName();

		final StructrSchemaRelationshipDefinition def = new StructrSchemaRelationshipDefinition(root, "/definitions/" + name);

		root.getTypeDefinitions().put(name, def);


		return def;
	}

	@Override
	public AbstractSchemaNode createDatabaseNode(final App app) throws FrameworkException {
		return app.create(SchemaNode.class, getName());
	}

	@Override
	void createFromDatabase(final AbstractSchemaNode schemaNode) throws URISyntaxException {

		final Set<String> requiredProperties = new TreeSet<>();

		setName(schemaNode.getProperty(AbstractNode.name));
		readLocalProperties(requiredProperties, schemaNode);
		readRemoteProperties(requiredProperties, schemaNode);
		readViews(schemaNode);
		readMethods(schemaNode);

		// "required"
		if (!requiredProperties.isEmpty()) {
			put(JsonSchema.KEY_REQUIRED, requiredProperties);
		}

		readSuperclass(schemaNode);
	}
}
