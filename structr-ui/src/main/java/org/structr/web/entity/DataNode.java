package org.structr.web.entity;

import java.util.List;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.LinkedListNode;
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

	// ----- exported methods from LinkedTreeNode -----
	public void appendChild(final String key, final LinkedTreeNode childElement) throws FrameworkException {
		treeAppendChild(createTreeRelationshipType(key), childElement);
	}
	
	public void insertBefore(final String key, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		treeInsertBefore(createTreeRelationshipType(key), newChild, refChild);
	}
	
	public void insertAfter(final String key, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		treeInsertAfter(createTreeRelationshipType(key), newChild, refChild);
	}

	public void removeChild(final String key, final LinkedTreeNode childToRemove) throws FrameworkException {
		treeRemoveChild(createTreeRelationshipType(key), childToRemove);
	}
	
	public void replaceChild(final String key, final LinkedTreeNode newChild, final LinkedTreeNode oldChild) throws FrameworkException {
		treeReplaceChild(createTreeRelationshipType(key), newChild, oldChild);
	}
	
	public LinkedTreeNode getFirstChild(final String key) {
		return treeGetFirstChild(createTreeRelationshipType(key));
	}
	
	public LinkedTreeNode getLastChild(final String key) {
		return treeGetLastChild(createTreeRelationshipType(key));
	}
	
	public LinkedTreeNode getChild(final String key, final int position) {
		return treeGetChild(createTreeRelationshipType(key), position);
	}
	
	public int getChildPosition(final String key, final LinkedTreeNode child) {
		return treeGetChildPosition(createTreeRelationshipType(key), child);
	}
	
	public DataNode getParent(final String key) {
		return (DataNode) treeGetParent(createTreeRelationshipType(key));
	}
	
	public List<LinkedTreeNode> getChildren(final String key) {
		return treeGetChildren(createTreeRelationshipType(key));
	}
	
	public int getChildCount(final String key) {
		return treeGetChildCount(createTreeRelationshipType(key));
	}
	
	public List<AbstractRelationship> getChildRelationships(String key) {
		return treeGetChildRelationships(createTreeRelationshipType(key));
	}

	private RelationshipType createTreeRelationshipType(String key) {
		return DynamicRelationshipType.withName(key);
	}
	
	// ----- exported methods from LinkedListNode -----
	public LinkedListNode previous(final String key) {
		return previous(key, this);
	}

	public LinkedListNode previous(final String key, final LinkedListNode currentElement) {
		return listGetPrevious(createListRelationshipType(key), currentElement);
	}

	public LinkedListNode next(final String key) {
		return next(key, this);
	}
	
	/**
	 * Returns the successor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return
	 */
	public LinkedListNode next(final String key, final LinkedListNode currentElement) {
		return listGetNext(createListRelationshipType(key), currentElement);
	}

	/**
	 * Inserts newElement before currentElement in the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the reference element
	 * @param newElement the new element
	 */
	public void insertBefore(final String key, final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {
		listInsertBefore(createListRelationshipType(key), currentElement, newElement);
	}

	/**
	 * Inserts newElement after currentElement in the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the reference element
	 * @param newElement the new element
	 */
	public void insertAfter(final String key, final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {
		listInsertAfter(createListRelationshipType(key), currentElement, newElement);
	}

	public void add(final String key, final LinkedListNode newElement) throws FrameworkException {
		listInsertAfter(createListRelationshipType(key), this, newElement);
	}
	
	/**
	 * Removes the current element from the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the element to be removed
	 */
	public void remove(final String key, final LinkedListNode currentElement) throws FrameworkException {
		listRemove(createListRelationshipType(key), currentElement);
	}

	private RelationshipType createListRelationshipType(String key) {
		return getListKey(createTreeRelationshipType(key));
	}
	
}
