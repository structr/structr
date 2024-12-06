package org.structr.core.entity;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SourceFile;

import java.util.Map;
import java.util.Set;

public interface SchemaRelationshipNode extends AbstractSchemaNode {

	NodeInterface getSourceNode();
	NodeInterface getTargetNode();
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
	String getSchemaNodeSourceType();
	String getSourceType();
	String getTargetType();
	String getSchemaNodeTargetType();
	String getResourceSignature();
	String getInverseResourceSignature();
	String getRelationshipType();

	PropagationDirection getPermissionPropagation();

	void resolveCascadingEnums(final JsonSchema.Cascade delete, final JsonSchema.Cascade autoCreate) throws FrameworkException;

	void setPreviousSourceJsonName(String propertyName) throws FrameworkException;
	void setPreviousTargetJsonName(String propertyName) throws FrameworkException;
	void setAutocreationFlag(Long aLong) throws FrameworkException;
	void setCascadingDeleteFlag(final Long flag) throws FrameworkException;

	Map<String, Object> resolveCascadingFlags();

	Long getAutocreationFlag();
	Long getCascadingDeleteFlag();

	boolean isPartOfBuiltInSchema();
	Iterable<NodeInterface> getSchemaGrants();

	String getPreviousTargetJsonName();

	String getPreviousSourceJsonName();
}
