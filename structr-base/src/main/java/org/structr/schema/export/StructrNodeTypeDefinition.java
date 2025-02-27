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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 *
 */
public class StructrNodeTypeDefinition extends StructrTypeDefinition<SchemaNode> implements JsonObjectType {

	StructrNodeTypeDefinition(final StructrSchemaDefinition root, final String name) {
		super(root, name);
	}

	@Override
	public JsonReferenceType relate(final JsonObjectType type) {
		return relate(type, SchemaRelationshipNode.getDefaultRelationshipType(getName(), type.getName()));
	}

	@Override
	public JsonReferenceType relate(URI externalTypeReference) {

		final String type = StructrApp.resolveSchemaId(externalTypeReference);
		if (type != null) {

			return relate(externalTypeReference, SchemaRelationshipNode.getDefaultRelationshipType(getName(), type));
		}

		throw new IllegalStateException("External reference " + externalTypeReference + " not found.");
	}

	@Override
	public JsonReferenceType relate(final JsonObjectType type, final String relationship) {
		return relate(type, relationship, Cardinality.ManyToMany);
	}

	@Override
	public JsonReferenceType relate(URI externalTypeReference, String relationship) {
		return relate(externalTypeReference, relationship, Cardinality.ManyToMany);
	}

	@Override
	public JsonReferenceType relate(final JsonObjectType type, final String relationship, final Cardinality cardinality) {

		final String sourcePropertyName = getPropertyName(type.getName(), false,  relationship, cardinality);
		final String targetPropertyName = getPropertyName(type.getName(), true, relationship, cardinality);

		return relate(type, relationship, cardinality, sourcePropertyName, targetPropertyName);
	}

	@Override
	public JsonReferenceType relate(URI externalTypeReference, String relationship, Cardinality cardinality) {

		final String type = StructrApp.resolveSchemaId(externalTypeReference);
		if (type != null) {

			final String sourcePropertyName = getPropertyName(type, false, relationship, cardinality);
			final String targetPropertyName = getPropertyName(type, true,  relationship, cardinality);

			return relate(externalTypeReference, relationship, cardinality, sourcePropertyName, targetPropertyName);
		}

		throw new IllegalStateException("External reference " + externalTypeReference + " not found.");
	}

	@Override
	public JsonReferenceType relate(final JsonObjectType type, final String relationship, final Cardinality cardinality, final String sourceAttributeName, final String targetAttributeName) {

		final String relationshipTypeName           = getName() + relationship + type.getName();
		final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, relationshipTypeName);

		// initialize
		def.setSourcePropertyName(sourceAttributeName);
		def.setTargetPropertyName(targetAttributeName);
		def.setRelationship(relationship);
		def.setCardinality(cardinality);
		def.setSourceType(getId());
		def.setTargetType(type.getId());

		root.addType(def);

		return def;
	}

	@Override
	public JsonReferenceType relate(URI externalTypeReference, String relationship, Cardinality cardinality, String sourceAttributeName, String targetAttributeName) {

		final String type = StructrApp.resolveSchemaId(externalTypeReference);
		if (type != null) {

			final String relationshipTypeName           = getName() + relationship + type;
			final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, relationshipTypeName);

			// initialize
			def.setSourcePropertyName(sourceAttributeName);
			def.setTargetPropertyName(targetAttributeName);
			def.setRelationship(relationship);
			def.setCardinality(cardinality);
			def.setSourceType(getId());
			def.setTargetType(externalTypeReference);

			root.addType(def);

			return def;

		}

		throw new IllegalStateException("External reference " + externalTypeReference + " not found.");
	}

	@Override
	public JsonReferenceType relate(final Class type, String relationship, Cardinality cardinality, String sourceAttributeName, String targetAttributeName) {

		final String relationshipTypeName           = getName() + relationship + type.getSimpleName();
		final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, relationshipTypeName);

		// initialize
		def.setSourcePropertyName(sourceAttributeName);
		def.setTargetPropertyName(targetAttributeName);
		def.setRelationship(relationship);
		def.setCardinality(cardinality);
		def.setSourceType(getId());
		def.setTargetType(StructrApp.getSchemaBaseURI().resolve(getStaticTypeReference(type)));

		root.addType(def);

		return def;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		Map<String, Object> serializedProperties = (Map)map.get(JsonSchema.KEY_PROPERTIES);
		if (serializedProperties == null) {

			serializedProperties = new TreeMap<>();
			map.put(JsonSchema.KEY_PROPERTIES, serializedProperties);
		}

		// serialize remote properties
		for (final StructrRelationshipTypeDefinition rel : root.getRelationships()) {

			if (getId().equals(rel.getSourceType())) {

				// outgoing
				final Map<String, Object> property = rel.serializeRelationshipProperty(true);
				if (property != null) {

					serializedProperties.put(rel.getTargetPropertyName(), property);
				}
			}

			if (getId().equals(rel.getTargetType())) {

				// incoming
				final Map<String, Object> property = rel.serializeRelationshipProperty(false);
				if (property != null) {

					serializedProperties.put(rel.getSourcePropertyName(), property);
				}
			}
		}

		// remove empty objects from json
		if (serializedProperties.isEmpty()) {
			map.remove(JsonSchema.KEY_PROPERTIES);
		}

		return map;
	}

	@Override
	public boolean isBlacklisted(final Set<String> blacklist) {
		return blacklist.contains(this.name);
	}

	// ----- package methods -----
	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaNode schemaNode) {

		super.deserialize(schemaNodes, schemaNode);

		// moved from abstract schema node
		this.isInterface                 = schemaNode.isInterface();
		this.isAbstract                  = schemaNode.isAbstract();
		this.includeInOpenAPI            = schemaNode.includeInOpenAPI();
		this.visibleToPublicUsers        = schemaNode.defaultVisibleToPublic();
		this.visibleToAuthenticatedUsers = schemaNode.defaultVisibleToAuth();
		this.category                    = schemaNode.getCategory();

		// $extends
		final Set<String> inheritedTraits = schemaNode.getInheritedTraits();
		if (inheritedTraits != null) {

			this.inheritedTraits.addAll(inheritedTraits);
		}

		for (final SchemaGrant grant : schemaNode.getSchemaGrants()) {

			final StructrGrantDefinition newGrant = StructrGrantDefinition.deserialize(this, grant);
			if (newGrant != null) {

				grants.add(newGrant);
			}
		}
	}

	@Override
	SchemaNode createSchemaNode(final Map<String, SchemaNode> schemaNodes, final Map<String, SchemaRelationshipNode> schemaRels, final App app, final PropertyMap createProperties) throws FrameworkException {

		// re-use existing schema nodes here!
		final SchemaNode existingNode = schemaNodes.get(name);
		if (existingNode != null) {

			return existingNode;
		}

		final Traits traits = Traits.of(StructrTraits.SCHEMA_NODE);

		createProperties.put(traits.key("name"),            name);
		createProperties.put(traits.key("inheritedTraits"), inheritedTraits.toArray(new String[0]));

		final SchemaNode newNode = app.create(StructrTraits.SCHEMA_NODE, createProperties).as(SchemaNode.class);

		schemaNodes.put(name, newNode);

		return newNode;
	}

	// ----- private methods -----
	private String getPropertyName(final String targetTypeName, final boolean outgoing, final String relationshipTypeName, final Cardinality cardinality) {

		final String sourceTypeName = getName();

		String _sourceMultiplicity = null;
		String _targetMultiplicity = null;

		switch (cardinality) {

			case OneToOne:
				_sourceMultiplicity = "1";
				_targetMultiplicity = "1";
				break;

			case OneToMany:
				_sourceMultiplicity = "1";
				_targetMultiplicity = "*";
				break;

			case ManyToOne:
				_sourceMultiplicity = "*";
				_targetMultiplicity = "1";
				break;

			case ManyToMany:
				_sourceMultiplicity = "*";
				_targetMultiplicity = "*";
				break;
		}

		return SchemaRelationshipNode.getPropertyName(root.getExistingPropertyNames(), outgoing, relationshipTypeName, sourceTypeName, targetTypeName, null, _targetMultiplicity, null, _sourceMultiplicity);
	}
}
