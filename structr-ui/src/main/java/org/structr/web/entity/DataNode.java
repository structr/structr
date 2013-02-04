package org.structr.web.entity;

import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.LinkedListNode;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.dom.Content;

/**
 *
 * @author Christian Morgner
 */
public class DataNode extends LinkedTreeNode {

	public static final EntityProperty<Type>   typeNode   = new EntityProperty<Type>("typeNode", Type.class, RelType.DEFINES_TYPE, Direction.INCOMING, true);
	public static final Property<String>       kind       = new EntityNotionProperty("kind", typeNode, new PropertyNotion(Type.kind));
	
	public static final org.structr.common.View uiView     = new org.structr.common.View(Content.class, PropertyView.Ui,     kind);
	public static final org.structr.common.View publicView = new org.structr.common.View(Content.class, PropertyView.Public, kind);

	static {
		
		EntityContext.registerSearchableProperty(DataNode.class, NodeIndex.fulltext.name(), kind);
	}
	
	public Type getTypeDefinition() {
		return super.getProperty(DataNode.typeNode);
	}

	// ----- private members -----
	private Type cachedTypeDefinition = null;
	
	@Override
	public <T> void setProperty(PropertyKey<T> key, T value) throws FrameworkException {
		
		if (cachedTypeDefinition == null) {
			cachedTypeDefinition = getTypeDefinition();
		}
		
		if (cachedTypeDefinition != null) {
			
			// use dynamic property definition as property key
			PropertyDefinition def = cachedTypeDefinition.getPropertyDefinition(key.dbName());
			if (def != null) {
				
				// initialize dynamic property key instance
				def.setDeclaringClass(getClass());
				def.setProperty(securityContext, this, value);
			}
			
		}
		
		// default to superclass
		super.setProperty(key, value);
	}
	
	@Override
	public <T> T getProperty(PropertyKey<T> key) {
		
		if (cachedTypeDefinition == null) {
			cachedTypeDefinition = getTypeDefinition();
		}
		
		if (cachedTypeDefinition != null) {
			
			// use dynamic property definition as property key
			PropertyDefinition def = cachedTypeDefinition.getPropertyDefinition(key.dbName());
			if (def != null) {
				
				// initialize dynamic property key instance
				def.setDeclaringClass(getClass());
				
				return (T)def.getProperty(securityContext, this, true);
			}
		}
		
		// default to superclass
		return super.getProperty(key);
	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {
		
		if (cachedTypeDefinition == null) {
			cachedTypeDefinition = getTypeDefinition();
		}
		
		if (cachedTypeDefinition != null) {
			
			List<PropertyKey> augmentedProperties = new LinkedList<PropertyKey>();
			
			// add property keys from superclass
			for (PropertyKey key : super.getPropertyKeys(propertyView)) {
				augmentedProperties.add(key);
			}
			
			for (PropertyDefinition propertyDefinition : cachedTypeDefinition.getPropertyDefinitions()) {
				
				// initialize dynamic property key instance
				propertyDefinition.setDeclaringClass(getClass());
				augmentedProperties.add(propertyDefinition);
			}
			
			return augmentedProperties;
		}
		
		// default to superclass
		return super.getPropertyKeys(propertyView);
	}	

	// ----- exported methods from DataNode -----
	public void appendChild(final String key, final DataNode childElement) throws FrameworkException {
		treeAppendChild(createTreeRelationshipType(key), childElement);
	}
	
	public void insertBefore(final String key, final DataNode newChild, final DataNode refChild) throws FrameworkException {
		treeInsertBefore(createTreeRelationshipType(key), newChild, refChild);
	}
	
	public void insertAfter(final String key, final DataNode newChild, final DataNode refChild) throws FrameworkException {
		treeInsertAfter(createTreeRelationshipType(key), newChild, refChild);
	}

	public void removeChild(final String key, final DataNode childToRemove) throws FrameworkException {
		treeRemoveChild(createTreeRelationshipType(key), childToRemove);
	}
	
	public void replaceChild(final String key, final DataNode newChild, final DataNode oldChild) throws FrameworkException {
		treeReplaceChild(createTreeRelationshipType(key), newChild, oldChild);
	}
	
	public DataNode getFirstChild(final String key) {
		return (DataNode)treeGetFirstChild(createTreeRelationshipType(key));
	}
	
	public boolean hasChildren(final String key) {
		return treeGetFirstChild(createTreeRelationshipType(key)) != null;
	}

	public DataNode getLastChild(final String key) {
		return (DataNode)treeGetLastChild(createTreeRelationshipType(key));
	}
	
	public DataNode getChild(final String key, final int position) {
		return (DataNode)treeGetChild(createTreeRelationshipType(key), position);
	}
	
	public int getChildPosition(final String key, final DataNode child) {
		return treeGetChildPosition(createTreeRelationshipType(key), child);
	}
	
	public DataNode getParent(final String key) {
		return (DataNode) treeGetParent(createTreeRelationshipType(key));
	}
	
	public List<DataNode> getChildren(final String key) {
		
		List<? extends LinkedTreeNode> dataNodes = treeGetChildren(createTreeRelationshipType(key));	
		return (List<DataNode>)dataNodes;
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
