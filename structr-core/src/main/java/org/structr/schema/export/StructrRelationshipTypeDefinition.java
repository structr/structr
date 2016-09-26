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

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonSchema.Cascade;
import org.structr.schema.json.JsonType;

/**
 *
 *
 */
public class StructrRelationshipTypeDefinition extends StructrTypeDefinition<SchemaRelationshipNode> implements JsonReferenceType {

	private JsonReferenceProperty sourceReference = null;
	private JsonReferenceProperty targetReference = null;
	private String sourcePropertyName             = null;
	private String targetPropertyName             = null;
	private String relationshipType               = null;
	private URI sourceType                        = null;
	private URI targetType                        = null;
	private Cardinality cardinality               = null;
	private Cascade cascadingDelete               = null;
	private Cascade cascadingCreate               = null;
	private String aclResolution                  = null;
	private String aclReadMask                    = null;
	private String aclWriteMask                   = null;
	private String aclDeleteMask                  = null;
	private String aclAccessControlMask           = null;
	private String aclHiddenProperties            = null;

	public StructrRelationshipTypeDefinition(final StructrSchemaDefinition root, final String name) {

		super(root, name);
	}

	@Override
	public String getRelationship() {
		return relationshipType;
	}

	@Override
	public JsonReferenceType setRelationship(final String relationship) {

		this.relationshipType = relationship;
		return this;
	}

	@Override
	public JsonReferenceType setCardinality(final Cardinality cardinality) {

		this.cardinality = cardinality;
		return this;
	}

	@Override
	public Cardinality getCardinality() {
		return cardinality;
	}

	@Override
	public URI getSourceType() {
		return sourceType;
	}

	@Override
	public URI getTargetType() {
		return targetType;
	}

	@Override
	public JsonReferenceType setSourcePropertyName(final String sourcePropertyName) {

		this.sourcePropertyName = sourcePropertyName;
		return this;
	}

	@Override
	public String getSourcePropertyName() {
		return sourcePropertyName;
	}

	@Override
	public JsonReferenceType setTargetPropertyName(final String targetPropertyName) {

		this.targetPropertyName = targetPropertyName;
		return this;
	}

	@Override
	public String getTargetPropertyName() {
		return targetPropertyName;
	}

	@Override
	public JsonReferenceProperty getSourceProperty() {
		return sourceReference;
	}

	@Override
	public JsonReferenceProperty getTargetProperty() {
		return targetReference;
	}

	@Override
	public Cascade getCascadingDelete() {
		return cascadingDelete;
	}

	@Override
	public Cascade getCascadingCreate() {
		return cascadingCreate;
	}

	@Override
	public JsonReferenceType setCascadingDelete(final Cascade cascade) {

		this.cascadingDelete = cascade;
		return this;
	}

	@Override
	public JsonReferenceType setCascadingCreate(final Cascade cascade) {

		this.cascadingCreate = cascade;
		return this;
	}

	// ----- package methods ------
	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		map.put(JsonSchema.KEY_RELATIONSHIP, relationshipType);
		map.put(JsonSchema.KEY_LINK_SOURCE, root.toJsonPointer(sourceType));
		map.put(JsonSchema.KEY_LINK_TARGET, root.toJsonPointer(targetType));
		map.put(JsonSchema.KEY_SOURCE_NAME, sourcePropertyName);
		map.put(JsonSchema.KEY_TARGET_NAME, targetPropertyName);


		// only write values that differ from the default
		if (!SchemaRelationshipNode.Direction.None.name().equals(aclResolution)) {

			map.put(JsonSchema.KEY_ACL_RESOLUTION, aclResolution);

			if (!SchemaRelationshipNode.Propagation.Remove.name().equals(aclReadMask)) {
				map.put(JsonSchema.KEY_ACL_READ_MASK, aclReadMask);
			}

			if (!SchemaRelationshipNode.Propagation.Remove.name().equals(aclWriteMask)) {
				map.put(JsonSchema.KEY_ACL_WRITE_MASK, aclWriteMask);
			}

			if (!SchemaRelationshipNode.Propagation.Remove.name().equals(aclDeleteMask)) {
				map.put(JsonSchema.KEY_ACL_DELETE_MASK, aclDeleteMask);
			}

			if (!SchemaRelationshipNode.Propagation.Remove.name().equals(aclAccessControlMask)) {
				map.put(JsonSchema.KEY_ACL_ACCESS_CONTROL_MASK, aclAccessControlMask);
			}

			if (aclHiddenProperties != null) {
				map.put(JsonSchema.KEY_ACL_HIDDEN_PROPERTIES, aclHiddenProperties);
			}
		}

		final Map<String, Object> cascade = new TreeMap<>();

		if (cascadingCreate != null) {
			cascade.put(JsonSchema.KEY_CREATE, cascadingCreate.name());
		}
		if (cascadingDelete != null) {
			cascade.put(JsonSchema.KEY_DELETE, cascadingDelete.name());
		}

		if (!cascade.isEmpty()) {
			map.put(JsonSchema.KEY_CASCADE, cascade);
		}

		if (cardinality != null && !Cardinality.ManyToMany.equals(cardinality)) {
			map.put(JsonSchema.KEY_CARDINALITY, cardinality.name());
		}

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object relValue = source.get(JsonSchema.KEY_RELATIONSHIP);
		if (relValue != null && relValue instanceof String) {

			this.relationshipType = relValue.toString();
		}

		final Object sourceValue = source.get(JsonSchema.KEY_LINK_SOURCE);
		if (sourceValue != null && sourceValue instanceof String) {

			String sourceURI = sourceValue.toString();
			if (sourceURI.startsWith("#/")) {

				sourceURI = sourceURI.substring(2);
			}

			this.sourceType = root.getId().resolve(URI.create(sourceURI));
		}

		final Object targetValue = source.get(JsonSchema.KEY_LINK_TARGET);
		if (targetValue != null && targetValue instanceof String) {

			String targetURI = targetValue.toString();
			if (targetURI.startsWith("#/")) {

				targetURI = targetURI.substring(2);
			}

			this.targetType = root.getId().resolve(URI.create(targetURI));
		}

		final Object cardinalityValue = source.get(JsonSchema.KEY_CARDINALITY);
		if (cardinalityValue != null) {

			this.cardinality = Cardinality.valueOf(cardinalityValue.toString());

		} else {

			this.cardinality = Cardinality.ManyToMany;
		}

		final Object sourceNameValue = source.get(JsonSchema.KEY_SOURCE_NAME);
		if (sourceNameValue != null) {

			this.sourcePropertyName = sourceNameValue.toString();
		}

		final Object targetNameValue = source.get(JsonSchema.KEY_TARGET_NAME);
		if (targetNameValue != null) {

			this.targetPropertyName = targetNameValue.toString();
		}

		final Object cascadeObject = source.get(JsonSchema.KEY_CASCADE);
		if (cascadeObject != null) {

			if (cascadeObject instanceof Map) {

				final Map<String, Object> cascade = (Map)cascadeObject;
				if (cascade != null) {

					final Object deleteValue = cascade.get(JsonSchema.KEY_DELETE);
					if (deleteValue != null && deleteValue instanceof String) {

						this.cascadingDelete = Cascade.valueOf(deleteValue.toString());
					}

					final Object createValue = cascade.get(JsonSchema.KEY_CREATE);
					if (createValue != null && createValue instanceof String) {

						this.cascadingCreate = Cascade.valueOf(createValue.toString());
					}
				}

			} else {

				throw new IllegalStateException("Invalid JSON source for cascade, expected object.");
			}
		}

		// ACL resolution
		final Object aclResolutionValue = source.get(JsonSchema.KEY_ACL_RESOLUTION);
		if (aclResolutionValue != null) {

			this.aclResolution = aclResolutionValue.toString();
		}

		// ACL read mask
		final Object aclReadMaskValue = source.get(JsonSchema.KEY_ACL_READ_MASK);
		if (aclReadMaskValue != null) {

			this.aclReadMask = aclReadMaskValue.toString();
		}

		// ACL write mask
		final Object aclWriteMaskValue = source.get(JsonSchema.KEY_ACL_WRITE_MASK);
		if (aclWriteMaskValue != null) {

			this.aclWriteMask = aclWriteMaskValue.toString();
		}

		// ACL delete mask
		final Object aclDeleteMaskValue = source.get(JsonSchema.KEY_ACL_DELETE_MASK);
		if (aclDeleteMaskValue != null) {

			this.aclDeleteMask = aclDeleteMaskValue.toString();
		}

		// ACL accessControl mask
		final Object aclAccessControlMaskValue = source.get(JsonSchema.KEY_ACL_ACCESS_CONTROL_MASK);
		if (aclAccessControlMaskValue != null) {

			this.aclAccessControlMask = aclAccessControlMaskValue.toString();
		}

		// ACL hidden properties
		final Object aclHiddenPropertiesValue = source.get(JsonSchema.KEY_ACL_HIDDEN_PROPERTIES);
		if (aclHiddenPropertiesValue != null) {

			this.aclHiddenProperties = aclHiddenPropertiesValue.toString();
		}
	}

	@Override
	void deserialize(final SchemaRelationshipNode schemaNode) {

		super.deserialize(schemaNode);

		final SchemaNode sourceNode = schemaNode.getProperty(SchemaRelationshipNode.sourceNode);
		final SchemaNode targetNode = schemaNode.getProperty(SchemaRelationshipNode.targetNode);
		final String sourceNodeType = sourceNode.getClassName();
		final String targetNodeType = targetNode.getClassName();


		this.sourceType           = root.getId().resolve("definitions/" + sourceNodeType);
		this.targetType           = root.getId().resolve("definitions/" + targetNodeType);
		this.relationshipType     = schemaNode.getProperty(SchemaRelationshipNode.relationshipType);
		this.sourcePropertyName   = schemaNode.getProperty(SchemaRelationshipNode.sourceJsonName);
		this.targetPropertyName   = schemaNode.getProperty(SchemaRelationshipNode.targetJsonName);
		this.aclResolution        = schemaNode.getProperty(SchemaRelationshipNode.permissionPropagation).name();
		this.aclReadMask          = schemaNode.getProperty(SchemaRelationshipNode.readPropagation).name();
		this.aclWriteMask         = schemaNode.getProperty(SchemaRelationshipNode.writePropagation).name();
		this.aclDeleteMask        = schemaNode.getProperty(SchemaRelationshipNode.deletePropagation).name();
		this.aclAccessControlMask = schemaNode.getProperty(SchemaRelationshipNode.accessControlPropagation).name();
		this.aclHiddenProperties  = schemaNode.getProperty(SchemaRelationshipNode.propertyMask);

		if (sourcePropertyName == null) {
			sourcePropertyName = schemaNode.getPropertyName(sourceNodeType, root.getExistingPropertyNames(), false);
		}

		if (targetPropertyName == null) {
			targetPropertyName = schemaNode.getPropertyName(targetNodeType, root.getExistingPropertyNames(), true);
		}


		final Long cascadingDeleteFlag = schemaNode.getProperty(SchemaRelationshipNode.cascadingDeleteFlag);
		if (cascadingDeleteFlag != null) {

			this.cascadingDelete = getCascadingString(cascadingDeleteFlag.intValue());
		}

		final Long cascadingCreateFlag = schemaNode.getProperty(SchemaRelationshipNode.autocreationFlag);
		if (cascadingCreateFlag != null) {

			this.cascadingCreate = getCascadingString(cascadingCreateFlag.intValue());
		}

		final String sourceMultiplicity = schemaNode.getProperty(SchemaRelationship.sourceMultiplicity);
		final String targetMultiplicity = schemaNode.getProperty(SchemaRelationship.targetMultiplicity);

		if ("1".equals(sourceMultiplicity)) {

			if ("1".equals(targetMultiplicity)) {

				this.cardinality = Cardinality.OneToOne;
			} else {

				this.cardinality = Cardinality.OneToMany;
			}

		} else {

			if ("1".equals(targetMultiplicity)) {

				this.cardinality = Cardinality.ManyToOne;
			} else {

				this.cardinality = Cardinality.ManyToMany;
			}
		}
	}


	@Override
	SchemaRelationshipNode createSchemaNode(final App app) throws FrameworkException {

		final SchemaRelationshipNode _schemaNode = app.create(SchemaRelationshipNode.class, getName());

		_schemaNode.setProperty(SchemaRelationshipNode.relationshipType, getRelationship());
		_schemaNode.setProperty(SchemaRelationshipNode.sourceJsonName, sourcePropertyName);
		_schemaNode.setProperty(SchemaRelationshipNode.targetJsonName, targetPropertyName);
		_schemaNode.setProperty(SchemaRelationshipNode.sourceMultiplicity, getSourceMultiplicity(cardinality));
		_schemaNode.setProperty(SchemaRelationshipNode.targetMultiplicity, getTargetMultiplicity(cardinality));
		_schemaNode.setProperty(SchemaRelationshipNode.cascadingDeleteFlag, getCascadingFlag(cascadingDelete));
		_schemaNode.setProperty(SchemaRelationshipNode.autocreationFlag, getCascadingFlag(cascadingCreate));

		if (aclResolution != null) {
			_schemaNode.setProperty(SchemaRelationshipNode.permissionPropagation, SchemaRelationshipNode.Direction.valueOf(aclResolution));
		}

		if (aclReadMask != null) {
			_schemaNode.setProperty(SchemaRelationshipNode.readPropagation, SchemaRelationshipNode.Propagation.valueOf(aclReadMask));
		}

		if (aclWriteMask != null) {
			_schemaNode.setProperty(SchemaRelationshipNode.writePropagation, SchemaRelationshipNode.Propagation.valueOf(aclWriteMask));
		}

		if (aclDeleteMask != null) {
			_schemaNode.setProperty(SchemaRelationshipNode.deletePropagation, SchemaRelationshipNode.Propagation.valueOf(aclDeleteMask));
		}

		if (aclAccessControlMask != null)  {
			_schemaNode.setProperty(SchemaRelationshipNode.accessControlPropagation, SchemaRelationshipNode.Propagation.valueOf(aclAccessControlMask));
		}

		if (aclHiddenProperties != null) {
			_schemaNode.setProperty(SchemaRelationshipNode.propertyMask, aclHiddenProperties);
		}

		return _schemaNode;
	}

	void resolveEndpointTypesForDatabaseSchemaCreation(final App app) throws FrameworkException {

		// this method is called when the creation of type and relationship
		// nodes is completed and all references can be resolved
		final SchemaNode sourceSchemaNode = resolveSchemaNode(app, sourceType);
		final SchemaNode targetSchemaNode = resolveSchemaNode(app, targetType);

		if (sourceSchemaNode != null && targetSchemaNode != null) {

			final AbstractSchemaNode thisSchemaRelationship = getSchemaNode();
			if (thisSchemaRelationship != null) {

				thisSchemaRelationship.setProperty(SchemaRelationshipNode.sourceNode, sourceSchemaNode);
				thisSchemaRelationship.setProperty(SchemaRelationshipNode.targetNode, targetSchemaNode);

			} else {

				throw new IllegalStateException("Unable to resolve schema node endpoints for type " + getName());
			}

		} else {

			throw new IllegalStateException("Unable to resolve schema node endpoints for type " + getName());
		}
	}

	Map<String, Object> serializeRelationshipProperty(final boolean outgoing) {

		final Map<String, Object> map   = new TreeMap<>();
		final Map<String, Object> items = new TreeMap<>();

		if (outgoing) {

			switch (cardinality) {

				case OneToOne:
				case ManyToOne:
					map.put(JsonSchema.KEY_TYPE, "object");
					map.put(JsonSchema.KEY_REFERENCE, root.toJsonPointer(targetType));
					break;

				case OneToMany:
				case ManyToMany:
					map.put(JsonSchema.KEY_TYPE, "array");
					map.put(JsonSchema.KEY_ITEMS, items);
					items.put(JsonSchema.KEY_REFERENCE, root.toJsonPointer(targetType));
					break;
			}

		} else {

			switch (cardinality) {

				case OneToOne:
				case OneToMany:
					map.put(JsonSchema.KEY_TYPE, "object");
					map.put(JsonSchema.KEY_REFERENCE, root.toJsonPointer(sourceType));
					break;

				case ManyToOne:
				case ManyToMany:
					map.put(JsonSchema.KEY_TYPE, "array");
					map.put(JsonSchema.KEY_ITEMS, items);
					items.put(JsonSchema.KEY_REFERENCE, root.toJsonPointer(sourceType));
					break;
			}

		}

		map.put(JsonSchema.KEY_LINK, root.toJsonPointer(getId()));

		return map;
	}

	void setSourceType(final URI sourceType) {

		final Object type = root.resolveURI(sourceType);
		if (type instanceof StructrTypeDefinition ) {

			this.sourceReference = new SourcePropertyReference((StructrTypeDefinition)type, sourcePropertyName);
		}

		this.sourceType = sourceType;
	}

	void setTargetType(final URI targetType) {

		final Object type = root.resolveURI(targetType);
		if (type instanceof StructrTypeDefinition ) {

			this.targetReference = new TargetPropertyReference((StructrTypeDefinition)type, targetPropertyName);
		}

		this.targetType = targetType;
	}

	// ----- private methods -----
	private String getSourceMultiplicity(final Cardinality cardinality) {

		switch (cardinality) {

			case OneToOne:
			case OneToMany:
				return "1";

			case ManyToOne:
			case ManyToMany:
				return "*";
		}

		return null;
	}

	private String getTargetMultiplicity(final Cardinality cardinality) {

		switch (cardinality) {

			case OneToOne:
			case ManyToOne:
				return "1";

			case OneToMany:
			case ManyToMany:
				return "*";
		}

		return null;
	}

	private Cascade getCascadingString(final int cascadingFlag) {

		switch (cascadingFlag) {

			case Relation.NONE:
				return null;

			case Relation.SOURCE_TO_TARGET:
				return Cascade.sourceToTarget;

			case Relation.TARGET_TO_SOURCE:
				return Cascade.targetToSource;

			case Relation.ALWAYS:
				return Cascade.always;
		}

		return null;
	}

	private long getCascadingFlag(final Cascade cascade) {

		if (cascade != null) {

			switch (cascade) {

				case sourceToTarget:
					return Relation.SOURCE_TO_TARGET;

				case targetToSource:
					return Relation.TARGET_TO_SOURCE;

				case always:
					return Relation.ALWAYS;
			}
		}

		return 0;
	}

	// ----- nested classes -----
	private class SourcePropertyReference extends StructrReferenceProperty {

		private SourcePropertyReference(final JsonType parent, final String name) {
			super(parent, name);
		}

		@Override
		public URI getId() {
			return URI.create(targetType.toString() + "/properties/" + getName());
		}

		@Override
		public String getName() {
			return sourcePropertyName;
		}

		@Override
		public String getType() {

			switch (cardinality) {

				case OneToOne:
				case OneToMany:
					return "object";

				case ManyToMany:
				case ManyToOne:
					return "array";
			}

			return null;
		}

		@Override
		public JsonProperty setName(final String name) {

			StructrRelationshipTypeDefinition.this.sourcePropertyName = name;
			return this;
		}
	}

	private class TargetPropertyReference extends StructrReferenceProperty {

		private TargetPropertyReference(final JsonType parent, final String name) {
			super(parent, name);
		}

		@Override
		public URI getId() {
			return URI.create(sourceType.toString() + "/properties/" + getName());
		}

		@Override
		public String getName() {
			return targetPropertyName;
		}

		@Override
		public String getType() {

			switch (cardinality) {

				case OneToOne:
				case ManyToOne:
					return "object";

				case OneToMany:
				case ManyToMany:
					return "array";
			}

			return null;
		}

		@Override
		public JsonProperty setName(final String name) {

			StructrRelationshipTypeDefinition.this.targetPropertyName = name;
			return this;
		}

	}
}
