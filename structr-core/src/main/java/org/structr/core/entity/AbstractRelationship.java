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

//~--- classes ----------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.UuidCreationTransformation;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;


/**
 * Bbstract base class for all relationship entities in structr.
 * 
 * @author Axel Morgner
 */
public abstract class AbstractRelationship<S extends NodeInterface, T extends NodeInterface> implements Comparable<AbstractRelationship>, RelationshipInterface {

	private static final Logger logger = Logger.getLogger(AbstractRelationship.class.getName());

	public static final Property<Integer> cascadeDelete = new IntProperty("cascadeDelete").writeOnce();
	
	public static final View defauflView = new View(AbstractRelationship.class, PropertyView.Public,
		uuid, type
	);
	
	static {

		EntityContext.registerEntityCreationTransformation(AbstractRelationship.class, new UuidCreationTransformation());
	}

	private boolean readOnlyPropertiesUnlocked = false;
	private String cachedEndNodeId             = null;
	private String cachedStartNodeId           = null;
	
	protected SecurityContext securityContext  = null;
	protected Relationship dbRelationship      = null;
	protected PropertyMap properties           = null;
	protected Class entityType                 = getClass();
	protected String cachedUuid                = null;
	protected boolean isDirty                  = false;

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

	public Class<? extends AbstractRelationship<T, S>> reverse() {
		return null;
	}
	
	/**
	 * Called when a relationship of this combinedType is instatiated. Please note that
	 * a relationship can (and will) be instantiated several times during a
	 * normal rendering turn.
	 */
	@Override
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

	@Override
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
	public void setSecurityContext(final SecurityContext securityContext) {
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
					
					// remove from index
					removeFromIndex(key);

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

	@Override
	public int cascadeDelete() {

		Integer value = getProperty(AbstractRelationship.cascadeDelete);

		return value != null ? value : 0;
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

	@Override
	public PropertyMap getProperties() throws FrameworkException {

		Map<String, Object> properties = new LinkedHashMap<>();

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

		return key.getProperty(securityContext, this, applyConverter);
	}

	@Override
	public <T> Comparable getComparableProperty(final PropertyKey<T> key) {

		T propertyValue = getProperty(key, false);	// get "raw" property without converter
		
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
	@Override
	public Relationship getRelationship() {

		return dbRelationship;

	}

	public T getTargetNode() {

		try {
			NodeFactory<T> nodeFactory = new NodeFactory<T>(SecurityContext.getSuperUserInstance());
			return nodeFactory.instantiate(dbRelationship.getEndNode());
			
		} catch (Throwable t) {
			// ignore
		}
		
		return null;
	}

	public S getSourceNode() {

		try {

			NodeFactory<S> nodeFactory = new NodeFactory<S>(SecurityContext.getSuperUserInstance());
			return nodeFactory.instantiate(dbRelationship.getStartNode());
			
		} catch (Throwable t) {
			// ignore
		}
		
		return null;
	}

	public AbstractNode getOtherNode(final AbstractNode node) {

		try {

			NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
			return (AbstractNode) nodeFactory.instantiate(dbRelationship.getOtherNode(node.getNode()));
			
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
		
		Object value = getProperty(key, false);
		if (value != null) {
			return value;
		}
		
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

		return getSourceNode().getUuid();

	}

	public String getEndNodeId() {

		return getTargetNode().getUuid();

	}

	public String getOtherNodeId(final AbstractNode node) {

		return getOtherNode(node).getProperty(AbstractRelationship.uuid);

	}

	public String getCachedStartNodeId() {

		return cachedStartNodeId;

	}

	public String getCachedEndNodeId() {

		return cachedEndNodeId;

	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return isValid(errorBuffer);
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		return true;
	}
	
	@Override
	public void afterCreation(SecurityContext securityContext) {
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
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

		// check for read-only properties
		//if (EntityContext.isReadOnlyProperty(type, key) || (EntityContext.isWriteOnceProperty(type, key) && (dbRelationship != null) && dbRelationship.hasProperty(key.name()))) {
		if (key.isReadOnly() || (key.isWriteOnce() && (dbRelationship != null) && dbRelationship.hasProperty(key.dbName()))) {

			if (readOnlyPropertiesUnlocked || securityContext.isSuperUser()) {

				// permit write operation once and
				// lock read-only properties again
				readOnlyPropertiesUnlocked = false;
				
			} else {

				throw new FrameworkException(getClass().getSimpleName(), new ReadOnlyPropertyToken(key));
			}

		}
		
		key.setProperty(securityContext, this, value);
	}

	@Override
	public void addToIndex() {

		for (PropertyKey key : EntityContext.getPropertySet(entityType, PropertyView.All)) {
			
			if (key.isIndexed()) {
				
				key.index(this, this.getPropertyForIndexing(key));
			}
		}
	}
	
	@Override
	public void updateInIndex() {
		
		removeFromIndex();
		addToIndex();
	}
	
	@Override
	public void removeFromIndex() {
		
		for (Index<Relationship> index : Services.getService(NodeService.class).getRelationshipIndices()) {
			
			synchronized (index) {
				
				index.remove(dbRelationship);
			}
		}
	}
	
	public void removeFromIndex(PropertyKey key) {
		
		for (Index<Relationship> index : Services.getService(NodeService.class).getRelationshipIndices()) {
			
			synchronized (index) {
				
				index.remove(dbRelationship, key.dbName());
			}
		}
	}
	
	@Override
	public void indexPassiveProperties() {

		for (PropertyKey key : EntityContext.getPropertySet(entityType, PropertyView.All)) {
			
			if (key.isPassivelyIndexed()) {
				
				key.index(this, this.getPropertyForIndexing(key));
			}
		}
		
	}
}
