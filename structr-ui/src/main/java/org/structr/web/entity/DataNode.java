package org.structr.web.entity;

import java.util.List;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.Property;
import org.structr.web.entity.dom.Content;

/**
 *
 * @author Christian Morgner
 */
public class DataNode extends LinkedTreeNode {

	public static final EntityProperty<TypeDefinition> typeDefinition = new EntityProperty<TypeDefinition>("typeDefinition", TypeDefinition.class, RelType.IS_A, true);
	public static final Property<String> typeDefinitionId             = new EntityIdProperty("typeDefinitionId", typeDefinition);

	public static final org.structr.common.View uiView                = new org.structr.common.View(Content.class, PropertyView.Ui,     typeDefinitionId);
	public static final org.structr.common.View publicView            = new org.structr.common.View(Content.class, PropertyView.Public, typeDefinitionId);



	public TypeDefinition getTypeDefinition() {
		return getProperty(DataNode.typeDefinition);
	}

	public void appendChild(final String key, final LinkedTreeNode childElement) throws FrameworkException {
		treeAppendChild(createRelationshipType(key), childElement);
	}
	
	public void insertBefore(final String key, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		treeInsertBefore(createRelationshipType(key), newChild, refChild);
	}
	
	public void insertAfter(final String key, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		treeInsertAfter(createRelationshipType(key), newChild, refChild);
	}

	public void removeChild(final String key, final LinkedTreeNode childToRemove) throws FrameworkException {
		treeRemoveChild(createRelationshipType(key), childToRemove);
	}
	
	public void replaceChild(final String key, final LinkedTreeNode newChild, final LinkedTreeNode oldChild) throws FrameworkException {
		treeReplaceChild(createRelationshipType(key), newChild, oldChild);
	}
	
	public LinkedTreeNode getFirstChild(final String key) {
		return treeGetFirstChild(createRelationshipType(key));
	}
	
	public LinkedTreeNode getLastChild(final String key) {
		return treeGetLastChild(createRelationshipType(key));
	}
	
	public LinkedTreeNode getChild(final String key, final int position) {
		return treeGetChild(createRelationshipType(key), position);
	}
	
	public int getChildPosition(final String key, final LinkedTreeNode child) {
		return treeGetChildPosition(createRelationshipType(key), child);
	}
	
	public List<LinkedTreeNode> getChildren(final String key) {
		return treeGetChildren(createRelationshipType(key));
	}
	
	public int getChildCount(final String key) {
		return treeGetChildCount(createRelationshipType(key));
	}
	
	public List<AbstractRelationship> getChildRelationships(String key) {
		return treeGetChildRelationships(createRelationshipType(key));
	}

	private RelationshipType createRelationshipType(String key) {
		return DynamicRelationshipType.withName(key);
	}
}
