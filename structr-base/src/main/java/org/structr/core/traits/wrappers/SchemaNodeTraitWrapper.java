package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

import java.util.List;

public class SchemaNodeTraitWrapper extends AbstractSchemaNodeTraitWrapper implements SchemaNode {

	public SchemaNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Iterable<SchemaGrant> getSchemaGrants() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("schemaGrants");

		 return Iterables.map(n -> n.as(SchemaGrant.class), wrappedObject.getProperty(key));
	}

	@Override
	public SchemaNode getExtendsClass() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("extendsClass"));
		if (node != null) {

			return node.as(SchemaNode.class);
		}

		return null;
	}

	@Override
	public String getSummary() {
		return wrappedObject.getProperty(traits.key("summary"));
	}

	@Override
	public String getIcon() {
		return wrappedObject.getProperty(traits.key("icon"));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key("description"));
	}

	@Override
	public String getCategory() {
		return wrappedObject.getProperty(traits.key("category"));
	}

	@Override
	public boolean isInterface() {
		return wrappedObject.getProperty(traits.key("isInterface"));
	}

	@Override
	public boolean isAbstract() {
		return wrappedObject.getProperty(traits.key("isAbstract"));
	}

	@Override
	public boolean isBuiltinType() {
		return wrappedObject.getProperty(traits.key("isBuiltinType"));
	}

	@Override
	public boolean changelogDisabled() {
		return wrappedObject.getProperty(traits.key("changelogDisabled"));
	}

	@Override
	public boolean defaultVisibleToPublic() {
		return wrappedObject.getProperty(traits.key("defaultVisibleToPublic"));
	}

	@Override
	public boolean defaultVisibleToAuth() {
		return wrappedObject.getProperty(traits.key("defaultVisibleToAuth"));
	}

	@Override
	public boolean includeInOpenAPI() {
		return wrappedObject.getProperty(traits.key("includeInOpenAPI"));
	}

	@Override
	public String[] getTags() {
		return wrappedObject.getProperty(traits.key("tags"));
	}

	@Override
	public Iterable<SchemaRelationshipNode> getRelatedTo() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("relatedTo");

		return Iterables.map(n -> n.as(SchemaRelationshipNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<SchemaRelationshipNode> getRelatedFrom() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("relatedFrom");

		return Iterables.map(n -> n.as(SchemaRelationshipNode.class), wrappedObject.getProperty(key));
	}
}
