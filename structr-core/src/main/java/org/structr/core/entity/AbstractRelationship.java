/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.entity;

import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.GetNodeByIdCommand;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;

import org.neo4j.graphdb.*;

import org.structr.common.*;
import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.UuidCreationTransformation;
import org.structr.common.error.*;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.common.error.NullPropertyToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.Services;
import org.structr.core.graph.NodeService.RelationshipIndex;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Bbstract base class for all relationship entities in structr.
 * 
 * @author Axel Morgner
 */
public abstract class AbstractRelationship implements GraphObject, Comparable<AbstractRelationship> {

	private static final Logger logger = Logger.getLogger(AbstractRelationship.class.getName());

	public static final Property<String>   combinedType  = new StringProperty("combinedType");
	public static final Property<Integer>  cascadeDelete = new IntProperty("cascadeDelete");
	
	
	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchableProperty(AbstractRelationship.class, RelationshipIndex.rel_uuid.name(), uuid);

		// register transformation for automatic uuid creation
		EntityContext.registerEntityCreationTransformation(AbstractRelationship.class, new UuidCreationTransformation());
	}

	//~--- fields ---------------------------------------------------------

	protected Class entityType     = getClass();
	private String cachedEndNodeId = null;

	private String cachedStartNodeId                = null;
	protected SecurityContext securityContext       = null;
	private boolean readOnlyPropertiesUnlocked      = false;

	// reference to database relationship
	protected Relationship dbRelationship;
	protected PropertyMap properties;
	protected String cachedUuid = null;
	protected boolean isDirty;

	//~--- constant enums -------------------------------------------------

	//~--- constructors ---------------------------------------------------

//      public enum Permission implements PropertyKey {
//              allowed, denied, read, showTree, write, execute, createNode, deleteNode, editProperties, addRelationship, removeRelationship, accessControl;
//      }
	public AbstractRelationship() {

		this.properties = new PropertyMap();
		isDirty         = true;

	}

	public AbstractRelationship(final PropertyMap properties) {

		this.properties = properties;
		isDirty         = true;

	}

	public AbstractRelationship(final SecurityContext securityContext, final PropertyMap data) {

		if (data != null) {

			this.securityContext = securityContext;
			this.properties      = data;
			this.isDirty         = true;

		}

	}

	public AbstractRelationship(final SecurityContext securityContext, final Relationship dbRel) {

		init(securityContext, dbRel);

	}

	//~--- methods --------------------------------------------------------

	/**
	 * Called when a relationship of this combinedType is instatiated. Please note that
	 * a relationship can (and will) be instantiated several times during a
	 * normal rendering turn.
	 */
	public void onRelationshipInstantiation() {

		try {

			if (dbRelationship != null) {

				Node startNode = dbRelationship.getStartNode();
				Node endNode   = dbRelationship.getEndNode();

				if ((startNode != null) && (endNode != null) && startNode.hasProperty(AbstractNode.uuid.dbName()) && endNode.hasProperty(AbstractNode.uuid.dbName())) {

					cachedStartNodeId = (String) startNode.getProperty(AbstractNode.uuid.dbName());
					cachedEndNodeId   = (String) endNode.getProperty(AbstractNode.uuid.dbName());

				}

			}

		} catch (Throwable t) {
		}
	}

	public AbstractNode identifyStartNode(RelationshipMapping namedRelation, Map<String, Object> propertySet) throws FrameworkException {

		Notion startNodeNotion = getStartNodeNotion();    // new RelationshipNotion(getStartNodeIdKey());

		startNodeNotion.setType(namedRelation.getSourceType());

		PropertyKey startNodeIdentifier = startNodeNotion.getPrimaryPropertyKey();

		if (startNodeIdentifier != null) {

			Object identifierValue = propertySet.get(startNodeIdentifier.jsonName());

			propertySet.remove(startNodeIdentifier.jsonName());

			return (AbstractNode) startNodeNotion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;

	}

	public AbstractNode identifyEndNode(RelationshipMapping namedRelation, Map<String, Object> propertySet) throws FrameworkException {

		Notion endNodeNotion = getEndNodeNotion();    // new RelationshipNotion(getEndNodeIdKey());

		endNodeNotion.setType(namedRelation.getDestType());

		PropertyKey endNodeIdentifier = endNodeNotion.getPrimaryPropertyKey();

		if (endNodeIdentifier != null) {

			Object identifierValue = propertySet.get(endNodeIdentifier.jsonName());

			propertySet.remove(endNodeIdentifier.jsonName());

			return (AbstractNode) endNodeNotion.getAdapterForSetter(securityContext).adapt(identifierValue);

		}

		return null;

	}

	public void init(final SecurityContext securityContext, final Relationship dbRel) {

		this.dbRelationship  = dbRel;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	public void init(final SecurityContext securityContext) {

		this.securityContext = securityContext;
		this.isDirty         = false;

	}

	public void init(final SecurityContext securityContext, final AbstractRelationship rel) {

		this.dbRelationship  = rel.dbRelationship;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {

		this.readOnlyPropertiesUnlocked = true;

	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				try {
					
					dbRelationship.removeProperty(key.dbName());

				} finally {}

				return null;

			}

		});
	}

	@Override
	public boolean equals(final Object o) {

		return (o != null && new Integer(this.hashCode()).equals(new Integer(o.hashCode())));

	}

	@Override
	public int hashCode() {

		if (this.dbRelationship == null) {

			return (super.hashCode());
		}

		return Long.valueOf(dbRelationship.getId()).hashCode();

	}

	@Override
	public int compareTo(final AbstractRelationship rel) {

		// TODO: implement finer compare methods, e.g. taking title and position into account
		if (rel == null) {

			return -1;
		}

		return ((Long) this.getId()).compareTo((Long) rel.getId());
	}

	public int cascadeDelete() {

		Integer cd = getProperty(AbstractRelationship.cascadeDelete);

		return (cd != null)
		       ? cd
		       : 0;

	}

	/**
	 * Indicates whether this relationship type propagates modifications
	 * in the given direction. Overwrite this method and return true for
	 * the desired direction to enable a callback on non-local node
	 * modification.
	 * 
	 * @param direction the direction for which the propagation should is to be returned
	 * @return the propagation status for the given direction
	 */
	public boolean propagatesModifications(Direction direction) {
		return false;
	}
	
	//~--- get methods ----------------------------------------------------

	@Override
	public PropertyKey getDefaultSortKey() {

		return null;

	}

	@Override
	public String getDefaultSortOrder() {

		return GraphObjectComparator.ASCENDING;

	}

	public abstract PropertyKey getStartNodeIdKey();

	public abstract PropertyKey getEndNodeIdKey();

	public Notion getEndNodeNotion() {

		return new RelationshipNotion(getEndNodeIdKey());

	}

	public Notion getStartNodeNotion() {

		return new RelationshipNotion(getStartNodeIdKey());

	}

	@Override
	public long getId() {

		return getInternalId();

	}
	
	@Override
	public String getUuid() {

		return getProperty(AbstractRelationship.uuid);

	}
	
	public long getRelationshipId() {

		return getInternalId();

	}

	public long getInternalId() {

		return dbRelationship.getId();

	}

	public PropertyMap getProperties() throws FrameworkException {

		Map<String, Object> properties = new LinkedHashMap<String, Object>();

		for (String key : dbRelationship.getPropertyKeys()) {

			properties.put(key, dbRelationship.getProperty(key));
		}

		// convert the database properties back to their java types
		return PropertyMap.databaseTypeToJavaType(securityContext, this, properties);

	}

	@Override
	public <T> T getProperty(final PropertyKey<T> key) {
		return getProperty(key, true);
	}

	private <T> T getProperty(final PropertyKey<T> key, boolean applyConverter) {

		// early null check, this should not happen...
		if (key == null || key.dbName() == null) {
			return null;
		}

		PropertyKey startNodeIdKey = getStartNodeIdKey();
		PropertyKey endNodeIdKey   = getEndNodeIdKey();

		if (startNodeIdKey != null && key.equals(startNodeIdKey)) {

			return (T) getStartNodeId();
		}

		if (endNodeIdKey != null && key.equals(endNodeIdKey)) {

			return (T)getEndNodeId();
		}
		
		return key.getProperty(securityContext, this, applyConverter);
	}

	@Override
	public Comparable getComparableProperty(final PropertyKey<? extends Comparable> key) {

		Object propertyValue = getProperty(key, false);	// get "raw" property without converter
		Class type = getClass();
		
		// check property converter
		PropertyConverter converter = key.databaseConverter(securityContext, this);
		if (converter != null) {
			
			try {
				return converter.convertForSorting(propertyValue);

			} catch(FrameworkException fex) {
				logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
					key.dbName(),
					getClass().getSimpleName(),
					fex.getMessage()
				});
			}
		}
		
		// conversion failed, may the property value itself is comparable
		if(propertyValue instanceof Comparable) {
			return (Comparable)propertyValue;
		}
		
		// last try: convertFromInput to String to make comparable
		if(propertyValue != null) {
			return propertyValue.toString();
		}
		
		return null;
	}

	/**
	 * Return database relationship
	 *
	 * @return
	 */
	public Relationship getRelationship() {

		return dbRelationship;

	}

	public AbstractNode getEndNode() {

		try {
			NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
			return (AbstractNode) nodeFactory.instantiateNode(dbRelationship.getEndNode());
			
		} catch (Throwable t) {
			// ignore
		}
		
		return null;
	}

	public AbstractNode getStartNode() {

		try {

			NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
			return (AbstractNode) nodeFactory.instantiateNode(dbRelationship.getStartNode());
			
		} catch (Throwable t) {
			// ignore
		}
		
		return null;
	}

	public AbstractNode getOtherNode(final AbstractNode node) {

		try {

			NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
			return (AbstractNode) nodeFactory.instantiateNode(dbRelationship.getOtherNode(node.getNode()));
			
		} catch (Throwable t) {
			// ignore
		}
		
		return null;

	}

	public RelationshipType getRelType() {

		return dbRelationship.getType();

	}

	/**
	 * Return all property keys.
	 *
	 * @return
	 */
	public Iterable<PropertyKey> getPropertyKeys() {

		return getPropertyKeys(PropertyView.All);

	}

	/**
	 * Return property value which is used for indexing.
	 *
	 * This is useful f.e. to filter markup from HTML to index only text
	 *
	 * @param key
	 * @return
	 */
	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		return getProperty(key);

	}

	// ----- interface GraphObject -----
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		return EntityContext.getPropertySet(this.getClass(), propertyView);

	}

	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction) {

		return null;

	}

	public List<AbstractRelationship> getRelationships(RelationshipType type, Direction dir) {

		return null;

	}

	@Override
	public String getType() {

		return getRelType().name();

	}
	
	@Override
	public PropertyContainer getPropertyContainer() {
		return dbRelationship;
	}

	public String getStartNodeId() {

		return getStartNode().getUuid();

	}

	public String getEndNodeId() {

		return getEndNode().getUuid();

	}

	public String getOtherNodeId(final AbstractNode node) {

		return getOtherNode(node).getProperty(AbstractRelationship.uuid);

	}

	private AbstractNode getNodeByUuid(final String uuid) throws FrameworkException {

		return (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(uuid);

	}

	public String getCachedStartNodeId() {

		return cachedStartNodeId;

	}

	public String getCachedEndNodeId() {

		return cachedEndNodeId;

	}

	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean beforeDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		return true;
	}
	
	@Override
	public void afterCreation(SecurityContext securityContext) {
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
	}

	@Override
	public void afterDeletion(SecurityContext securityContext) {
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
	}
	
	@Override
	public void securityModified(SecurityContext securityContext) {
	}
	
	@Override
	public void locationModified(SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
	}
	
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this, AbstractRelationship.uuid, errorBuffer);

		return !error;

	}

	//~--- set methods ----------------------------------------------------

	public void setProperties(final PropertyMap properties) throws FrameworkException {

		for (Entry<PropertyKey, Object> prop : properties.entrySet()) {

			setProperty(prop.getKey(), prop.getValue());
		}

	}

	@Override
	public <T> void setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {

		PropertyKey startNodeIdKey = getStartNodeIdKey();
		PropertyKey endNodeIdKey   = getEndNodeIdKey();
		
		if ((startNodeIdKey != null) && key.equals(startNodeIdKey)) {

			setStartNodeId((String) value);

			return;

		}

		if ((endNodeIdKey != null) && key.equals(endNodeIdKey)) {

			setEndNodeId((String) value);

			return;

		}
		
		// check for read-only properties
		//if (EntityContext.isReadOnlyProperty(type, key) || (EntityContext.isWriteOnceProperty(type, key) && (dbRelationship != null) && dbRelationship.hasProperty(key.name()))) {
		if (key.isReadOnlyProperty() || (key.isWriteOnceProperty() && (dbRelationship != null) && dbRelationship.hasProperty(key.dbName()))) {

			if (readOnlyPropertiesUnlocked) {

				// permit write operation once and
				// lock read-only properties again
				readOnlyPropertiesUnlocked = false;
				
			} else {

				throw new FrameworkException(getClass().getSimpleName(), new ReadOnlyPropertyToken(key));
			}

		}
		
		key.setProperty(securityContext, this, value);
	}

	/**
	 * Set node id of start node.
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, ends at the same end node,
	 * but starting from the node with startNodeId
	 *
	 */
	public void setStartNodeId(final String startNodeId) throws FrameworkException {

		final String type     = this.getClass().getSimpleName();
		final PropertyKey key = getStartNodeIdKey();

		// May never be null!!
		if (startNodeId == null) {

			throw new FrameworkException(type, new NullPropertyToken(key));
		}
		
		// Do nothing if new id equals old
		if (getStartNodeId().equals(startNodeId)) {
			return;
		}

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
				CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
				AbstractNode newStartNode           = getNodeByUuid(startNodeId);
				AbstractNode endNode                = getEndNode();

				if (newStartNode == null) {

					throw new FrameworkException(type, new IdNotFoundToken(startNodeId));
				}

				RelationshipType type = dbRelationship.getType();

				properties = getProperties();

				deleteRel.execute(dbRelationship);

				AbstractRelationship newRel = (AbstractRelationship) createRel.execute(newStartNode, endNode, type, properties, false);

				dbRelationship = newRel.getRelationship();

				return (null);

			}

		});

	}

	/**
	 * Set node id of end node.
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, start from the same start node,
	 * but pointing to the node with endNodeId
	 *
	 */
	public void setEndNodeId(final String endNodeId) throws FrameworkException {

		final String type     = this.getClass().getSimpleName();
		final PropertyKey key = getStartNodeIdKey();

		// May never be null!!
		if (endNodeId == null) {

			throw new FrameworkException(type, new NullPropertyToken(key));
		}
		
		// Do nothing if new id equals old
		if (getEndNodeId().equals(endNodeId)) {
			return;
		}

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
				CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
				AbstractNode startNode              = getStartNode();
				AbstractNode newEndNode             = getNodeByUuid(endNodeId);

				if (newEndNode == null) {

					throw new FrameworkException(type, new IdNotFoundToken(endNodeId));
				}

				RelationshipType type = dbRelationship.getType();

				properties = getProperties();

				deleteRel.execute(dbRelationship);

				AbstractRelationship newRel = (AbstractRelationship) createRel.execute(startNode, newEndNode, type, properties, false);

				dbRelationship = newRel.getRelationship();

				return (null);

			}

		});

	}

	/**
	 * Set relationship combinedType
	 *
	 * Internally, this method deletes the old relationship
	 * and creates a new one, with the same start and end node,
	 * but with another combinedType
	 *
	 */
	public void setType(final String type) {

		if (type != null) {

			try {

				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
						CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
						AbstractNode startNode = getStartNode();
						AbstractNode endNode   = getEndNode();

						deleteRel.execute(dbRelationship);

						dbRelationship = createRel.execute(startNode, endNode, type).getRelationship();

						return (null);

					}

				});

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to set relationship type", fex);

			}

		}

	}

}
