package org.structr.core.entity;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.graph.NodeInterface;

import java.util.Map;
import java.util.Set;

public interface SchemaRelationshipNode extends AbstractSchemaNode {

	SchemaNode getSourceNode();
	SchemaNode getTargetNode();
	String getClassName();
	String getMultiplicity(final Map<String, SchemaNode> schemaNodes, final String propertyNameToCheck);
	String getRelatedType(final Map<String, SchemaNode> schemaNodes, final String propertyNameToCheck);
	String getSourceNotion();
	String getTargetNotion();
	String getMultiplicity(final boolean outgoing);
	String getPropertyName(final String relatedClassName, final Set<String> existingPropertyNames, final boolean outgoing);

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

	Map<String, Object> resolveCascadingFlags();

	Long getAutocreationFlag();
	Long getCascadingDeleteFlag();

	boolean isPartOfBuiltInSchema();
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

	static String getPropertyName(final String relatedClassName, final Set<String> existingPropertyNames, final boolean outgoing, final String relationshipTypeName, final String _sourceType, final String _targetType, final String _targetJsonName, final String _targetMultiplicity, final String _sourceJsonName, final String _sourceMultiplicity) {

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
