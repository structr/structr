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
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.api.schema.*;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 *
 */
public class StructrRelationshipTypeDefinition extends StructrTypeDefinition implements JsonReferenceType {

	private JsonReferenceProperty sourceReference      = null;
	private JsonReferenceProperty targetReference      = null;
	private String sourcePropertyName                  = null;
	private String targetPropertyName                  = null;
	private String relationshipType                    = null;
	private URI sourceType                             = null;
	private URI targetType                             = null;
	private Cardinality cardinality                    = null;
	private Cascade cascadingDelete                    = null;
	private Cascade cascadingCreate                    = null;
	private PropagationDirection permissionPropagation = PropagationDirection.None;
	private PropagationMode readPropagation            = PropagationMode.Remove;
	private PropagationMode writePropagation           = PropagationMode.Remove;
	private PropagationMode deletePropagation          = PropagationMode.Remove;
	private PropagationMode accessControlPropagation   = PropagationMode.Remove;
	private String aclHiddenProperties                 = null;
	private boolean isPartOfBuiltInSchema              = false;

	public StructrRelationshipTypeDefinition(final StructrSchemaDefinition root, final String name) {

		super(root, name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrPropertyDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
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

	@Override
	public PropagationDirection getPermissionPropagation() {
		return permissionPropagation;
	}

	@Override
	public PropagationMode getReadPermissionPropagation() {
		return readPropagation;
	}

	@Override
	public PropagationMode getWritePermissionPropagation() {
		return writePropagation;
	}

	@Override
	public PropagationMode getDeletePermissionPropagation() {
		return deletePropagation;
	}

	@Override
	public PropagationMode getAccessControlPermissionPropagation() {
		return accessControlPropagation;
	}

	@Override
	public JsonReferenceType setPermissionPropagation(final PropagationDirection value) {

		permissionPropagation = value;
		return this;
	}

	@Override
	public JsonReferenceType setReadPermissionPropagation(final PropagationMode value) {

		readPropagation = value;
		return this;
	}

	@Override
	public JsonReferenceType setWritePermissionPropagation(final PropagationMode value) {

		writePropagation = value;
		return this;
	}

	@Override
	public JsonReferenceType setDeletePermissionPropagation(final PropagationMode value) {

		deletePropagation = value;
		return this;
	}

	@Override
	public JsonReferenceType setAccessControlPermissionPropagation(final PropagationMode value) {

		accessControlPropagation = value;
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
		if (!PropagationDirection.None.equals(permissionPropagation)) {

			map.put(JsonSchema.KEY_ACL_RESOLUTION, permissionPropagation);

			if (!PropagationMode.Remove.equals(readPropagation)) {
				map.put(JsonSchema.KEY_ACL_READ_MASK, readPropagation);
			}

			if (!PropagationMode.Remove.equals(writePropagation)) {
				map.put(JsonSchema.KEY_ACL_WRITE_MASK, writePropagation);
			}

			if (!PropagationMode.Remove.equals(deletePropagation)) {
				map.put(JsonSchema.KEY_ACL_DELETE_MASK, deletePropagation);
			}

			if (!PropagationMode.Remove.equals(accessControlPropagation)) {
				map.put(JsonSchema.KEY_ACL_ACCESS_CONTROL_MASK, accessControlPropagation);
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
				final Object deleteValue          = cascade.get(JsonSchema.KEY_DELETE);

				if (deleteValue != null && deleteValue instanceof String) {

					this.cascadingDelete = Cascade.valueOf(deleteValue.toString());
				}

				final Object createValue = cascade.get(JsonSchema.KEY_CREATE);
				if (createValue != null && createValue instanceof String) {

					this.cascadingCreate = Cascade.valueOf(createValue.toString());
				}

			} else {

				throw new IllegalStateException("Invalid JSON source for cascade, expected object.");
			}
		}

		// ACL resolution
		final Object aclResolutionValue = source.get(JsonSchema.KEY_ACL_RESOLUTION);
		if (aclResolutionValue != null) {

			this.permissionPropagation = PropagationDirection.valueOf(aclResolutionValue.toString());
		}

		// ACL read mask
		final Object aclReadMaskValue = source.get(JsonSchema.KEY_ACL_READ_MASK);
		if (aclReadMaskValue != null) {

			this.readPropagation = PropagationMode.valueOf(aclReadMaskValue.toString());
		}

		// ACL write mask
		final Object aclWriteMaskValue = source.get(JsonSchema.KEY_ACL_WRITE_MASK);
		if (aclWriteMaskValue != null) {

			this.writePropagation = PropagationMode.valueOf(aclWriteMaskValue.toString());
		}

		// ACL delete mask
		final Object aclDeleteMaskValue = source.get(JsonSchema.KEY_ACL_DELETE_MASK);
		if (aclDeleteMaskValue != null) {

			this.deletePropagation = PropagationMode.valueOf(aclDeleteMaskValue.toString());
		}

		// ACL accessControl mask
		final Object aclAccessControlMaskValue = source.get(JsonSchema.KEY_ACL_ACCESS_CONTROL_MASK);
		if (aclAccessControlMaskValue != null) {

			this.accessControlPropagation = PropagationMode.valueOf(aclAccessControlMaskValue.toString());
		}

		// ACL hidden properties
		final Object aclHiddenPropertiesValue = source.get(JsonSchema.KEY_ACL_HIDDEN_PROPERTIES);
		if (aclHiddenPropertiesValue != null) {

			this.aclHiddenProperties = aclHiddenPropertiesValue.toString();
		}
	}

	@Override
	void deserialize(final Map<String, NodeInterface> schemaNodes, final NodeInterface node) {

		super.deserialize(schemaNodes, schemaNode);

		final SchemaRelationshipNode schemaNode = node.as(SchemaRelationshipNode.class);

		final NodeInterface sourceNode = schemaNode.getSourceNode();
		final NodeInterface targetNode = schemaNode.getTargetNode();
		final String sourceNodeType    = sourceNode != null ? sourceNode.getName() : schemaNode.getSourceType();
		final String targetNodeType    = targetNode != null ? targetNode.getName() : schemaNode.getTargetType();

		this.sourceType                = sourceNode != null ? root.getId().resolve("definitions/" + sourceNodeType) : StructrApp.getSchemaBaseURI().resolve("static/" + sourceNodeType);
		this.targetType                = targetNode != null ? root.getId().resolve("definitions/" + targetNodeType) : StructrApp.getSchemaBaseURI().resolve("static/" + targetNodeType);

		this.relationshipType          = schemaNode.getRelationshipType();
		this.sourcePropertyName        = schemaNode.getSourceJsonName();
		this.targetPropertyName        = schemaNode.getTargetJsonName();
		this.permissionPropagation     = schemaNode.getPermissionPropagation();
		this.readPropagation           = schemaNode.getReadPropagation();
		this.writePropagation          = schemaNode.getWritePropagation();
		this.deletePropagation         = schemaNode.getDeletePropagation();
		this.accessControlPropagation  = schemaNode.getAccessControlPropagation();
		this.aclHiddenProperties       = schemaNode.getPropertyMask();
		this.isPartOfBuiltInSchema     = schemaNode.isPartOfBuiltInSchema();

		if (sourcePropertyName == null) {
			sourcePropertyName = schemaNode.getPropertyName(sourceNodeType, root.getExistingPropertyNames(), false);
		}

		if (targetPropertyName == null) {
			targetPropertyName = schemaNode.getPropertyName(targetNodeType, root.getExistingPropertyNames(), true);
		}


		final Long cascadingDeleteFlag = schemaNode.getCascadingDeleteFlag();
		if (cascadingDeleteFlag != null) {

			this.cascadingDelete = getCascadingString(cascadingDeleteFlag.intValue());
		}

		final Long cascadingCreateFlag = schemaNode.getAutocreationFlag();
		if (cascadingCreateFlag != null) {

			this.cascadingCreate = getCascadingString(cascadingCreateFlag.intValue());
		}

		final String sourceMultiplicity = schemaNode.getSourceMultiplicity();
		final String targetMultiplicity = schemaNode.getTargetMultiplicity();

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
	NodeInterface createSchemaNode(final Map<String, NodeInterface> schemaNodes, final Map<String, NodeInterface> schemaRels, final App app, final PropertyMap createProperties) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();
		NodeInterface _schemaNode    = schemaRels.get(getName());

		if (_schemaNode == null) {

			_schemaNode = app.create("SchemaRelationshipNode", getName());
		}

		final Traits traits = Traits.of("SchemaRelationshipNode");

		properties.put(traits.key("relationshipType"), getRelationship());
		properties.put(traits.key("sourceJsonName"), sourcePropertyName);
		properties.put(traits.key("targetJsonName"), targetPropertyName);
		properties.put(traits.key("sourceMultiplicity"), getSourceMultiplicity(cardinality));
		properties.put(traits.key("targetMultiplicity"), getTargetMultiplicity(cardinality));
		properties.put(traits.key("cascadingDeleteFlag"), getCascadingFlag(cascadingDelete));
		properties.put(traits.key("autocreationFlag"), getCascadingFlag(cascadingCreate));

		if (permissionPropagation != null) {
			properties.put(traits.key("permissionPropagation"), permissionPropagation);
		}

		if (readPropagation != null) {
			properties.put(traits.key("readPropagation"), readPropagation);
		}

		if (writePropagation != null) {
			properties.put(traits.key("writePropagation"), writePropagation);
		}

		if (deletePropagation != null) {
			properties.put(traits.key("deletePropagation"), deletePropagation);
		}

		if (accessControlPropagation != null)  {
			properties.put(traits.key("accessControlPropagation"), accessControlPropagation);
		}

		if (aclHiddenProperties != null) {
			properties.put(traits.key("propertyMask"), aclHiddenProperties);
		}

		if (root != null) {

			if (SchemaService.DynamicSchemaRootURI.equals(root.getId())) {

				this.isPartOfBuiltInSchema = true;
				properties.put(traits.key("isPartOfBuiltInSchema"), true);
			}
		}

		_schemaNode.setProperties(SecurityContext.getSuperUserInstance(), properties);

		return _schemaNode;
	}

	@Override
	public boolean isBuiltinType() {
		return isPartOfBuiltInSchema;
	}

	@Override
	public void setIsBuiltinType() {
		this.isPartOfBuiltInSchema = true;
	}

	@Override
	public boolean isBlacklisted(final Set<String> blacklist) {

		if (blacklist.contains(name)) {
			return true;
		}

		final Object source = root.resolveURI(sourceType);
		if (source instanceof JsonType) {

			final JsonType t = (JsonType)source;

			if (blacklist.contains(t.getName())) {
				return true;
			}
		}

		final Object target = root.resolveURI(targetType);
		if (target instanceof JsonType) {

			final JsonType t = (JsonType)target;

			if (blacklist.contains(t.getName())) {
				return true;
			}
		}

		return false;
	}

	void resolveEndpointTypesForDatabaseSchemaCreation(final Map<String, NodeInterface> schemaNodes, final App app) throws FrameworkException {

		// this method is called when the creation of type and relationship
		// nodes is completed and all references can be resolved
		final NodeInterface sourceSchemaNode = resolveSchemaNode(schemaNodes, app, sourceType);
		final NodeInterface targetSchemaNode = resolveSchemaNode(schemaNodes, app, targetType);
		final Traits traits                  = Traits.of("SchemaRelationshipNode");

		final NodeInterface thisSchemaRelationship = getSchemaNode();
		if (thisSchemaRelationship != null) {

			final String prefix = "static/";
			final int start     = prefix.length();

			if (sourceSchemaNode != null) {

				thisSchemaRelationship.setProperty(traits.key("sourceNode"), sourceSchemaNode);

			} else {

				// The following code allows static Java classes to be used as endpoints for dynamic relationships.
				// The FQCN of the class is encoded in the sourceType or targetType URI as https://structr.org/v1.1/static/<fqcn>
				final URI rel     = StructrApp.getSchemaBaseURI().relativize(sourceType);
				final String path = rel.toString();

				if (path.startsWith(prefix)) {

					thisSchemaRelationship.setProperty(traits.key("sourceType"), path.substring(start));

				} else {

					throw new IllegalStateException("Unable to resolve schema node endpoints for type " + getName() + ": " + sourceType);
				}
			}

			if (targetSchemaNode != null) {

				thisSchemaRelationship.setProperty(traits.key("targetNode"), targetSchemaNode);

			} else {

				// The following code allows static Java classes to be used as endpoints for dynamic relationships.
				// The FQCN of the class is encoded in the sourceType or targetType URI as https://structr.org/v1.1/static/<fqcn>

				final URI rel     = StructrApp.getSchemaBaseURI().relativize(targetType);
				final String path = rel.toString();

				if (path.startsWith(prefix)) {

					thisSchemaRelationship.setProperty(traits.key("targetType"), path.substring(start));

				} else {

					throw new IllegalStateException("Unable to resolve schema node endpoints for type " + getName() + ": " + targetType);
				}
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
