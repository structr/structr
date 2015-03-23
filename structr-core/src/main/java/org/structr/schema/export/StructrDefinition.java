package org.structr.schema.export;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeAttribute;

/**
 *
 * @author Christian Morgner
 */
public abstract class StructrDefinition extends TreeMap<String, Object> {

	private SchemaNode schemaNode          = null;
	protected StructrSchemaDefinition root = null;
	protected URI id                       = null;

	public StructrDefinition(final StructrSchemaDefinition root, final String id) throws URISyntaxException {

		this.root = root;
		this.id   = new URI(id);
	}

	protected abstract StructrDefinition getParent();

	public URI getId() {

		final StructrDefinition parent = getParent();
		if (parent != null) {

			final URI parentId = parent.getId();
			if (parentId != null) {

				try {
					final URI containerURI = new URI(parentId.toString() + "/");
					return containerURI.resolve(id);

				} catch (URISyntaxException urex) {
					urex.printStackTrace();
				}
			}
		}

		if (root != null) {

			final URI rootId = root.getId();
			if (rootId != null) {

				try {
					final URI containerURI = new URI(rootId.toString() + "/");
					return containerURI.resolve(id);

				} catch (URISyntaxException urex) {
					urex.printStackTrace();
				}
			}
		}

		return id;
	}

	// ----- protected methods -----
	protected Map<String, Object> getMap(final Map<String, Object> rawData, final String key, final boolean create) {

		Object value = rawData.get(key);
		if (value != null) {

			if (value instanceof Map) {
				return (Map<String, Object>)value;
			}

			throw new IllegalStateException("Invalid type, expected object, got " + value.getClass().getName());
		}

		if (create) {

			Map<String, Object> map = new TreeMap<>();
			rawData.put(key, map);

			return map;
		}

		return null;
	}

	protected Set getSet(final Map<String, Object> rawData, final String key, final boolean create) {

		Object value = rawData.get(key);
		if (value != null) {

			if (value instanceof Set) {
				return (Set)value;
			}

			throw new IllegalStateException("Invalid type, expected array, got " + value.getClass().getName());
		}

		if (create) {

			final Set list = new TreeSet<>();
			rawData.put(key, list);

			return list;
		}

		return null;
	}

	protected List getList(final Map<String, Object> rawData, final String key, final boolean create) {

		Object value = rawData.get(key);
		if (value != null) {

			if (value instanceof List) {
				return (List)value;
			}

			throw new IllegalStateException("Invalid type, expected array, got " + value.getClass().getName());
		}

		if (create) {

			final List list = new LinkedList<>();
			rawData.put(key, list);

			return list;
		}

		return null;
	}

	protected String getString(final Map<String, Object> rawData, final String key) {

		final Object value = rawData.get(key);
		if (value != null) {

			if (value instanceof String) {
				return (String)value;
			}

			return value.toString();
		}

		return null;
	}

	protected boolean getBoolean(final Map<String, Object> rawData, final String key) {

		final Object value = rawData.get(key);
		if (value != null) {

			if (value instanceof Boolean) {
				return (Boolean)value;
			}

			return Boolean.valueOf(value.toString());
		}

		return false;
	}

	protected StructrDefinition resolveJsonPointer(final String reference) {

		if (reference.startsWith("#")) {

			final String[] parts = reference.substring(1).split("[/]+");
			Object current       = root;

			for (int i=0; i<parts.length; i++) {

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

						if (current instanceof Map) {
							current = ((Map)current).get(key);
						}
					}
				}
			}

			if (current instanceof StructrDefinition) {
				return (StructrDefinition)current;
			}

		} else {

			// what now?! This is an absolute pointer that we can not currently reference..
			throw new IllegalStateException("Unsupported JSON pointer " + reference);
		}

		return null;
	}

	protected SchemaNode getSchemaNode() {
		return schemaNode;
	}

	protected void setSchemaNode(final SchemaNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	// ----- public static methods -----
	public static SchemaRelationshipNode getRelationship(final App app, final SchemaNode sourceNode, final SchemaNode targetNode, final String relationshipType) throws FrameworkException {

		SchemaRelationshipNode node = app.nodeQuery(SchemaRelationshipNode.class)
			.and(SchemaRelationshipNode.sourceId, sourceNode.getUuid())
			.and(SchemaRelationshipNode.targetId, targetNode.getUuid())
			.and(SchemaRelationshipNode.relationshipType, relationshipType)
			.getFirst();

		if (node == null) {

			node = app.create(SchemaRelationshipNode.class,
				new NodeAttribute(SchemaRelationshipNode.sourceNode, sourceNode),
				new NodeAttribute(SchemaRelationshipNode.targetNode, targetNode),
				new NodeAttribute(SchemaRelationshipNode.relationshipType, relationshipType)
			);
		}

		return node;
	}
}
