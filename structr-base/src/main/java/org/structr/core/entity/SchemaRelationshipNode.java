/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.entity;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.graphobject.IsValid;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SchemaRelationshipNode extends AbstractSchemaNode {

	SchemaNode getSourceNode();
	SchemaNode getTargetNode();
	String getClassName();
	String getSourceNotion();
	String getTargetNotion();
	String getPropertyName(final Set<String> existingPropertyNames, final boolean outgoing);

	String getSourceMultiplicity();
	String getTargetMultiplicity();
	String getSourceJsonName();
	String getTargetJsonName();
	String getPreviousTargetJsonName();
	String getPreviousSourceJsonName();
	String getSchemaNodeSourceType();
	String getSourceType();
	String getTargetType();
	String getSchemaNodeTargetType();
	String getResourceSignature();
	String getInverseResourceSignature();
	String getRelationshipType();

	void resolveCascadingEnums(final JsonSchema.Cascade delete, final JsonSchema.Cascade autoCreate) throws FrameworkException;

	void setPreviousSourceJsonName(String propertyName) throws FrameworkException;
	void setPreviousTargetJsonName(String propertyName) throws FrameworkException;
	void setAutocreationFlag(Long aLong) throws FrameworkException;
	void setCascadingDeleteFlag(final Long flag) throws FrameworkException;
	void setRelationshipType(final String relType) throws FrameworkException;
	void setSourceMultiplicity(final String sourceMultiplicity) throws FrameworkException;
	void setTargetMultiplicity(final String targetMultiplicity) throws FrameworkException;

	Map<String, Object> resolveCascadingFlags();

	Long getAutocreationFlag();
	Long getCascadingDeleteFlag();

	Iterable<SchemaGrant> getSchemaGrants();

	// permission propagation
	PropagationDirection getPermissionPropagation();
	PropagationMode getReadPropagation();
	PropagationMode getWritePropagation();
	PropagationMode getDeletePropagation();
	PropagationMode getAccessControlPropagation();
	String getPropertyMask();

	void setSourceNode(final SchemaNode sourceSchemaNode) throws FrameworkException;
	void setTargetNode(final SchemaNode targetSchemaNode) throws FrameworkException;
	void setSourceType(final String substring) throws FrameworkException;
	void setTargetType(final String substring) throws FrameworkException;
	void setSourceJsonName(final String sourcePropertyName) throws FrameworkException;
	void setTargetJsonName(final String targetPropertyName) throws FrameworkException;

	TraitDefinition getTraitDefinition(final TraitsInstance traitsInstance);

	PropertyKey createKey(final TraitsInstance traitsInstance, final SchemaNode entity, final boolean outgoing) throws FrameworkException;
	List<IsValid> createValidators(final SchemaNode entity) throws FrameworkException;

	// ----- public static methods -----
	static String getDefaultRelationshipType(final SchemaRelationshipNode rel) {
		return getDefaultRelationshipType(rel.getSourceNode(), rel.getTargetNode());
	}

	static String getDefaultRelationshipType(final SchemaNode sourceNode, final SchemaNode targetNode) {
		return getDefaultRelationshipType(sourceNode.getName(), targetNode.getName());
	}

	static String getDefaultRelationshipType(final String sourceType, final String targetType) {
		return sourceType + "_" + targetType;
	}

	static String getPropertyName(final SchemaRelationshipNode node, final Set<String> existingPropertyNames, final boolean outgoing) {

		final String relationshipTypeName = node.getRelationshipType().toLowerCase();
		final String _sourceType          = node.getSchemaNodeSourceType();
		final String _targetType          = node.getSchemaNodeTargetType();
		final String _targetJsonName      = node.getTargetJsonName();
		final String _targetMultiplicity  = node.getTargetMultiplicity();
		final String _sourceJsonName      = node.getSourceJsonName();
		final String _sourceMultiplicity  = node.getSourceMultiplicity();

		final String propertyName = SchemaRelationshipNode.getPropertyName(existingPropertyNames, outgoing, relationshipTypeName, _sourceType, _targetType, _targetJsonName, _targetMultiplicity, _sourceJsonName, _sourceMultiplicity);

		try {
			if (outgoing) {

				if (_targetJsonName == null) {

					node.setPreviousTargetJsonName(propertyName);
				}

			} else {

				if (_sourceJsonName == null) {

					node.setPreviousSourceJsonName(propertyName);
				}
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return propertyName;
	}

	static String getPropertyName(final Set<String> existingPropertyNames, final boolean outgoing, final String relationshipTypeName, final String _sourceType, final String _targetType, final String _targetJsonName, final String _targetMultiplicity, final String _sourceJsonName, final String _sourceMultiplicity) {

		String propertyName = "";

		if (outgoing) {


			if (_targetJsonName != null) {

				// FIXME: no automatic creation?
				propertyName = _targetJsonName;

			} else {

				if ("1".equals(_targetMultiplicity)) {

					propertyName = CaseHelper.toLowerCamelCase(relationshipTypeName) + CaseHelper.toUpperCamelCase(_targetType);

				} else {

					propertyName = CaseHelper.plural(CaseHelper.toLowerCamelCase(relationshipTypeName) + CaseHelper.toUpperCamelCase(_targetType));
				}
			}

		} else {


			if (_sourceJsonName != null) {
				propertyName = _sourceJsonName;
			} else {

				if ("1".equals(_sourceMultiplicity)) {

					propertyName = CaseHelper.toLowerCamelCase(_sourceType) + CaseHelper.toUpperCamelCase(relationshipTypeName);

				} else {

					propertyName = CaseHelper.plural(CaseHelper.toLowerCamelCase(_sourceType) + CaseHelper.toUpperCamelCase(relationshipTypeName));
				}
			}
		}

		if (existingPropertyNames.contains(propertyName)) {

			// First level: Add direction suffix
			propertyName += outgoing ? "Out" : "In";
			int i=0;

			// New name still exists: Add number
			while (existingPropertyNames.contains(propertyName)) {
				propertyName += ++i;
			}

		}

		existingPropertyNames.add(propertyName);

		return propertyName;
	}
}
