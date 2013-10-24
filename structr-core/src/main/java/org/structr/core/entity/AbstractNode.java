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

import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.BooleanProperty;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.*;
import org.structr.common.GraphObjectComparator;
import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.UuidCreationTransformation;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NullArgumentToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.EntityContext;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.Services;
import org.structr.core.graph.NodeRelationshipStatisticsCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.structr.core.Incoming;
import org.structr.core.Outgoing;
import org.structr.core.IterableAdapter;
import org.structr.core.entity.relationship.Ownership;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.End;

//~--- classes ----------------------------------------------------------------

/**
 * Abstract base class for all node entities in structr.
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public abstract class AbstractNode implements NodeInterface, AccessControllable {

	private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());

	// properties
	public static final Property<String>          name             = new StringProperty("name").indexed();
	public static final Property<String>          createdBy        = new StringProperty("createdBy").readOnly().writeOnce();
	public static final Property<Boolean>         deleted          = new BooleanProperty("deleted").indexed();
	public static final Property<Boolean>         hidden           = new BooleanProperty("hidden").indexed();
      
	public static final End<Principal>        owner            = new End<Principal>("owner", Ownership.class, true);
	public static final Property<String>          ownerId          = new EntityIdProperty("ownerId", owner);

	public static final View defaultView = new View(AbstractNode.class, PropertyView.Public, uuid, type);
	
	public static final View uiView = new View(AbstractNode.class, PropertyView.Ui,
		uuid, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate
	);
	
	//~--- static initializers --------------------------------------------

	static {

		// register transformation for automatic uuid creation
		EntityContext.registerEntityCreationTransformation(AbstractNode.class, new UuidCreationTransformation());
	}

	//~--- fields ---------------------------------------------------------

	protected PropertyMap cachedConvertedProperties  = new PropertyMap();
	protected PropertyMap cachedRawProperties        = new PropertyMap();
	protected Principal cachedOwnerNode              = null;
	protected Class entityType                       = getClass();

	// request parameters
	protected SecurityContext securityContext        = null;
	private boolean readOnlyPropertiesUnlocked       = false;

	// reference to database node
	protected String cachedUuid = null;
	protected Node dbNode;

	//~--- constructors ---------------------------------------------------

	public AbstractNode() {}

	public AbstractNode(SecurityContext securityContext, final Node dbNode) {

		init(securityContext, dbNode);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void onNodeCreation() {
	}

	@Override
	public void onNodeInstantiation() {
	}
	
	@Override
	public void onNodeDeletion() {
	}
	
	@Override
	public final void init(final SecurityContext securityContext, final Node dbNode) {

		this.dbNode          = dbNode;
		this.securityContext = securityContext;
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
	
	public SecurityContext getSecurityContext() {
		return securityContext;
	}
	
	@Override
	public boolean equals(final Object o) {

		if (o == null) {

			return false;
		}

		if (!(o instanceof AbstractNode)) {

			return false;
		}

		return (new Integer(this.hashCode()).equals(new Integer(o.hashCode())));

	}

	@Override
	public int hashCode() {

		if (this.dbNode == null) {

			return (super.hashCode());
		}

		return Long.valueOf(dbNode.getId()).hashCode();

	}

	@Override
	public int compareTo(final NodeInterface node) {

		if(node == null) {
			return -1;
		}
		
		
		String name = getName();
		
		if(name == null) {
			return -1;
		}
		
		
		String nodeName = node.getName();
		
		if(nodeName == null) {
			return -1;
		}
		
		return name.compareTo(nodeName);
	}

	/**
	 * Implement standard toString() method
	 */
	@Override
	public String toString() {

		if (dbNode == null) {

			return "AbstractNode with null database node";
		}

		try {

			String name = dbNode.hasProperty(AbstractNode.name.dbName())
				      ? (String) dbNode.getProperty(AbstractNode.name.dbName())
				      : "<null name>";
			String type = dbNode.hasProperty(AbstractNode.type.dbName())
				      ? (String) dbNode.getProperty(AbstractNode.type.dbName())
				      : "<AbstractNode>";
			String id   = dbNode.hasProperty(AbstractNode.uuid.dbName())
				      ? (String) dbNode.getProperty(AbstractNode.uuid.dbName())
				      : Long.toString(dbNode.getId());

			return name + " (" + type + "," + id + ")";

		} catch (Throwable ignore) {
			logger.log(Level.WARNING, ignore.getMessage());
		}

		return "<AbstractNode>";

	}

	/**
	 * Can be used to permit the setting of a read-only
	 * property once. The lock will be restored automatically
	 * after the next setProperty operation. This method exists
	 * to prevent automatic set methods from setting a read-only
	 * property while allowing a manual set method to override this
	 * default behaviour.
	 */
	@Override
	public void unlockReadOnlyPropertiesOnce() {

		this.readOnlyPropertiesUnlocked = true;

	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {

		if (this.dbNode != null) {

			if (key == null) {

				logger.log(Level.SEVERE, "Tried to set property with null key (action was denied)");

				return;

			}

			// check for read-only properties
			if (key.isReadOnly()) {

				// allow super user to set read-only properties
				if (readOnlyPropertiesUnlocked || securityContext.isSuperUser()) {

					// permit write operation once and
					// lock read-only properties again
					readOnlyPropertiesUnlocked = false;
				} else {

					throw new FrameworkException(this.getType(), new ReadOnlyPropertyToken(key));
				}

			}

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					dbNode.removeProperty(key.dbName());
					
					// remove from index
					removeFromIndex(key);
					
					return null;

				}

			});

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public PropertyKey getDefaultSortKey() {
		return AbstractNode.name;
	}

	@Override
	public String getDefaultSortOrder() {
		return GraphObjectComparator.ASCENDING;
	}

	@Override
	public String getType() {
		return getProperty(AbstractNode.type);
	}
	
	@Override
	public PropertyContainer getPropertyContainer() {
		return dbNode;
	}

	/**
	 * Get name from underlying db node
	 *
	 * If name is null, return node id as fallback
	 */
	@Override
	public String getName() {
		
		String name = getProperty(AbstractNode.name);
		if (name == null) {
			name = getNodeId().toString();
		}

		return name;
	}

	/**
	 * Get id from underlying db
	 */
	@Override
	public long getId() {

		if (dbNode == null) {

			return -1;
		}

		return dbNode.getId();

	}

	@Override
	public String getUuid() {

		return getProperty(AbstractNode.uuid);

	}

	public Long getNodeId() {

		return getId();

	}

	public String getIdString() {

		return Long.toString(getId());

	}

	/**
	 * Indicates whether this node is visible to public users.
	 * 
	 * @return whether this node is visible to public users
	 */
	public boolean getVisibleToPublicUsers() {
		return getProperty(visibleToPublicUsers);
	}

	/**
	 * Indicates whether this node is visible to authenticated users.
	 * 
	 * @return whether this node is visible to authenticated users
	 */
	public boolean getVisibleToAuthenticatedUsers() {
		return getProperty(visibleToPublicUsers);
	}

	/**
	 * Indicates whether this node is hidden.
	 * 
	 * @return whether this node is hidden
	 */
	public boolean getHidden() {
		return getProperty(hidden);
	}

	/**
	 * Indicates whether this node is deleted.
	 * 
	 * @return whether this node is deleted
	 */
	public boolean getDeleted() {
		return getProperty(deleted);
	}

	/**
	 * Returns the property set for the given view as an Iterable.
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {
		return EntityContext.getPropertySet(this.getClass(), propertyView);
	}

	/**
	 * Return property value which is used for indexing.
	 *
	 * This is useful f.e. to filter markup from HTML to index only text,
	 * or to get dates as long values.
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

	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key.
	 * 
	 * @param propertyKey the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
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

	public String getPropertyMD5(final PropertyKey key) {

		Object value = getProperty(key);

		if (value instanceof String) {

			return DigestUtils.md5Hex((String) value);
		} else if (value instanceof byte[]) {

			return DigestUtils.md5Hex((byte[]) value);
		}

		logger.log(Level.WARNING, "Could not create MD5 hex out of value {0}", value);

		return null;

	}

	/**
	 * Returns the property value for the given key as a List of Strings,
	 * split on [\r\n].
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a List of Strings
	 */
	public List<String> getStringListProperty(final PropertyKey<List<String>> key) {

		Object propertyValue = getProperty(key);
		List<String> result  = new LinkedList<String>();

		if (propertyValue == null) {

			return null;
		}

		if (propertyValue instanceof String) {

			// Split by carriage return / line feed
			String[] values = StringUtils.split(((String) propertyValue), "\r\n");

			result = Arrays.asList(values);
		} else if (propertyValue instanceof String[]) {

			String[] values = (String[]) propertyValue;

			result = Arrays.asList(values);

		}

		return result;

	}

	/**
	 * Returns the property value for the given key as an Array of Strings.
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as an Array of Strings
	 */
	public String getStringArrayPropertyAsString(final PropertyKey<String[]> key) {

		Object propertyValue = getProperty(key);
		StringBuilder result = new StringBuilder();

		if (propertyValue instanceof String[]) {

			int i           = 0;
			String[] values = (String[]) propertyValue;

			for (String value : values) {

				result.append(value);

				if (i < values.length - 1) {

					result.append("\r\n");
				}

			}

		}

		return result.toString();

	}

	/**
	 * Returns the property value for the given key as a Comparable
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Comparable
	 */
	@Override
	public <T> Comparable getComparableProperty(final PropertyKey<T> key) {

		T propertyValue = getProperty(key);
		
		// check property converter
		PropertyConverter<T, ?> converter = key.databaseConverter(securityContext, this);
		if (converter != null) {

			try {
				return converter.convertForSorting(propertyValue);

			} catch(Throwable t) {
				
				t.printStackTrace();
				
				logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
					key.dbName(),
					getClass().getSimpleName(),
					t.getMessage()
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
	 * Returns the property value for the given key as a Iterable
	 * 
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Iterable
	 */
	public Iterable getIterableProperty(final PropertyKey<? extends Iterable> propertyKey) {
		return (Iterable)getProperty(propertyKey);
	}

	/**
	 * Returns a list of related nodes for which a modification propagation is configured
	 * via the relationship. Override this method to return a set of nodes that should
	 * receive propagated modifications.
	 * 
	 * @return a set of nodes to which modifications should be propagated
	 */
	public Set<AbstractNode> getNodesForModificationPropagation() {
		return null;
	}

	/**
	 * Returns database node.
	 *
	 * @return the database node
	 */
	public Node getNode() {

		return dbNode;

	}

	/**
	 * Return the (cached) incoming relationship between this node and the
	 * given principal which holds the security information.
	 *
	 * @param principal
	 * @return incoming security relationship
	 */
	public Security getSecurityRelationship(final Principal p) {

		if (p == null) {

			return null;
		}

		for (Security r : getIncomingRelationships(Security.class)) {
			
			if (r.getStartNode().equals(p)) {
				
				return r;
				
			}
		}
		
		return null;

	}

	/**
	 * Return all relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends AbstractRelationship> Iterable<T> getRelationships(final Class<T> type) {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		AbstractRelationship template  = getRelationshipForType(type);
		Direction dir                  = template.getDirectionForType(entityType);
		RelationshipType relType       = template.getRelationshipType();
		Iterable<Relationship> rels    = null;
		
		if (type != null && dir != null) {
			
			rels = dbNode.getRelationships(relType, dir);
			
		} else if (type != null) {
			
			rels = dbNode.getRelationships(relType);
			
		} else if (dir != null) {
			
			rels = dbNode.getRelationships(dir);
		}
		
		return new IterableAdapter<Relationship, T>(rels, factory);
	}

	/**
	 * Return all relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends AbstractRelationship> Iterable<T> getRelationships() {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		return new IterableAdapter<Relationship, T>(dbNode.getRelationships(), factory);
	}

	/**
	 * Return all relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends Incoming> Iterable<T> getReverseRelationships(final Class<T> type) {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		RelationshipInterface template = getRelationshipForType(type);
		Direction dir                  = template.getDirectionForType(entityType).reverse();
		RelationshipType relType       = template.getRelationshipType();
		Iterable<Relationship> rels    = null;
		
		if (type != null && dir != null) {
			
			rels = dbNode.getRelationships(relType, dir);
			
		} else if (type != null) {
			
			rels = dbNode.getRelationships(relType);
			
		} else if (dir != null) {
			
			rels = dbNode.getRelationships(dir);
		}
		
		return new IterableAdapter<Relationship, T>(rels, factory);
	}

	/**
	 * Return all relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends Outgoing & Incoming> Iterable<T> getAllRelationships(final Class<T> type) {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		RelationshipInterface template = getRelationshipForType(type);
		RelationshipType relType       = template.getRelationshipType();
		Iterable<Relationship> rels    = null;
		
		if (type != null) {
			
			rels = dbNode.getRelationships(relType, Direction.BOTH);
		}			
		
		return new IterableAdapter<Relationship, T>(rels, factory);
	}

	/**
	 * Return all incoming relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends Incoming> Iterable<T> getIncomingRelationships(final Class<T> type) {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		RelationshipInterface template = getRelationshipForType(type);
		RelationshipType relType       = template.getRelationshipType();
		Iterable<Relationship> rels    = null;
		
		if (type != null) {
			
			rels = dbNode.getRelationships(relType, Direction.INCOMING);
		}
		
		return new IterableAdapter<Relationship, T>(rels, factory);
	}

	/**
	 * Return all incoming relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends Incoming> Iterable<T> getIncomingRelationships() {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		return new IterableAdapter<Relationship, T>(dbNode.getRelationships(Direction.INCOMING), factory);
	}

	/**
	 * Return all outgoing relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends Outgoing> Iterable<T> getOutgoingRelationships(final Class<T> type) {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		RelationshipInterface template = getRelationshipForType(type);
		RelationshipType relType       = template.getRelationshipType();
		Iterable<Relationship> rels    = null;
		
		if (type != null) {
			
			rels = dbNode.getRelationships(relType, Direction.OUTGOING);
		}
		
		return new IterableAdapter<Relationship, T>(rels, factory);
	}

	/**
	 * Return all outgoing relationships of given type and direction in lazy way.
	 *
	 * @return list with relationships
	 */
	@Override
	public <T extends Outgoing> Iterable<T> getOutgoingRelationships() {

		RelationshipFactory<T> factory = new RelationshipFactory<T>(securityContext);
		return new IterableAdapter<Relationship, T>(dbNode.getRelationships(Direction.OUTGOING), factory);
	}

	/**
	 * Return statistical information on all relationships of this node
	 *
	 * @return number of relationships
	 */
	public Map<RelationshipType, Long> getRelationshipInfo(Direction dir) {

		try {

			return (Map<RelationshipType, Long>) Services.command(securityContext, NodeRelationshipStatisticsCommand.class).execute(this, dir);

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to get relationship info", fex);

		}

		return null;

	}

	/**
	 * Returns the owner node of this node, following an INCOMING OWNS relationship.
	 *
	 * @return the owner node of this node
	 */
	public Principal getOwnerNode() {

		if (cachedOwnerNode == null) {

			for (Incoming s : getIncomingRelationships(Ownership.class)) {

				Principal n = s.getStartNode();
				if (n == null) {
					
					logger.log(Level.WARNING, "Could not determine owner node!");
					
					return null;
				}

				cachedOwnerNode = (Principal) n;
				break;
			}
		}

		return cachedOwnerNode;
	}

	/**
	 * Returns the database ID of the owner node of this node.
	 * 
	 * @return the database ID of the owner node of this node
	 */
	public Long getOwnerId() {

		return getOwnerNode().getId();

	}

	/**
	 * Return a list with the connected principals (user, group, role)
	 * @return
	 */
	public List<Principal> getSecurityPrincipals() {

		List<Principal> principalList = new LinkedList<Principal>();

		// check any security relationships
		for (Incoming r : getIncomingRelationships(Security.class)) {

			// check security properties
			Principal principalNode = r.getStartNode();

			principalList.add(principalNode);
		}

		return principalList;

	}

	/**
	 * Return true if this node has a relationship of given type and direction.
	 *
	 * @param type
	 * @param dir
	 * @return
	 */
	public boolean hasRelationship(final Class<? extends AbstractRelationship> type) {
		return this.getRelationships(type).iterator().hasNext();

	}

	// ----- interface AccessControllable -----
	public boolean isGranted(final Permission permission, final Principal principal) {

		if (principal == null) {

			return false;
		}

		// just in case ...
		if (permission == null) {

			return false;
		}

		// superuser
		if (principal instanceof SuperUser) {

			return true;
		}

		// user has full control over his/her own user node
		if (this.equals(principal)) {

			return true;
		}

		Security r = getSecurityRelationship(principal);

		if ((r != null) && r.isAllowed(permission)) {

			return true;
		}

		// Now check possible parent principals
		for (Principal parent : principal.getParents()) {

			if (isGranted(permission, parent)) {

				return true;
			}

		}

		return false;

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

		error |= ValidationHelper.checkStringNotBlank(this, uuid, errorBuffer);
		error |= ValidationHelper.checkStringNotBlank(this, type, errorBuffer);

		return !error;

	}

	public boolean isVisibleToPublicUsers() {

		return getVisibleToPublicUsers();

	}

	public boolean isVisibleToAuthenticatedUsers() {

		return getProperty(visibleToAuthenticatedUsers);

	}

	public boolean isNotHidden() {

		return !getHidden();

	}

	public boolean isHidden() {

		return getHidden();

	}
	
	public Date getVisibilityStartDate() {
		return getProperty(visibilityStartDate);
	}
	
	public Date getVisibilityEndDate() {
		return getProperty(visibilityEndDate);
	}

	public Date getCreatedDate() {
		return getProperty(createdDate);
	}
	
	public Date getLastModifiedDate() {
		return getProperty(lastModifiedDate);
	}

	// ----- end interface AccessControllable -----
	public boolean isNotDeleted() {

		return !getDeleted();

	}

	@Override
	public boolean isDeleted() {

		return getDeleted();

	}

	/**
	 * Return true if node is the root node
	 *
	 * @return
	 */
	public boolean isRootNode() {

		return getId() == 0;

	}

	public boolean isVisible() {

		return securityContext.isVisible(this);

	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Split String value and set as String[] property in database backend
	 *
	 * @param key
	 * @param stringList
	 *
	 */
	public void setPropertyAsStringArray(final PropertyKey<String[]> key, final String value) throws FrameworkException {

		String[] values = StringUtils.split(((String) value), "\r\n");

		setProperty(key, values);

	}

	/**
	 * Store a non-persistent value in this entity.
	 * 
	 * @param key
	 * @param value 
	 */
	public void setTemporaryProperty(final PropertyKey key, Object value) {
		cachedConvertedProperties.put(key, value);
		cachedRawProperties.put(key, value);
	}
	
	/**
	 * Retrieve a previously stored non-persistent value from this entity.
	 */
	public Object getTemporaryProperty(final PropertyKey key) {
		return cachedConvertedProperties.get(key);
	}

	/**
	 * Set a property in database backend. This method needs to be wrappend into
	 * a StructrTransaction, otherwise Neo4j will throw a NotInTransactionException!
	 * Set property only if value has changed.
	 *
	 * @param key
	 * @param convertedValue
	 * @param updateIndex
	 */
	@Override
	public <T> void setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {

		T oldValue = getProperty(key);

		// check null cases
		if ((oldValue == null) && (value == null)) {

			return;
		}

		// no old value exists, set property
		if ((oldValue == null) && (value != null)) {

			setPropertyInternal(key, value);

			return;

		}

		// old value exists and is NOT equal
		if ((oldValue != null) && !oldValue.equals(value)) {

			setPropertyInternal(key, value);
		}

	}

	private <T> void setPropertyInternal(final PropertyKey<T> key, final T value) throws FrameworkException {

		if (key == null) {

			logger.log(Level.SEVERE, "Tried to set property with null key (action was denied)");

			throw new FrameworkException(getClass().getSimpleName(), new NullArgumentToken(base));

		}
		
		// check for read-only properties
		if (key.isReadOnly() || (key.isWriteOnce() && (dbNode != null) && dbNode.hasProperty(key.dbName()))) {

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

//	public void setOwner(final AbstractNode owner) {
//
//		try {
//
//			Services.command(securityContext, SetOwnerCommand.class).execute(this, owner);
//
//		} catch (FrameworkException fex) {
//
//			logger.log(Level.WARNING, "Unable to set owner node", fex);
//
//		}
//
//	}

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
		
		for (Index<Node> index : Services.getService(NodeService.class).getNodeIndices()) {
			
			synchronized (index) {
				
				index.remove(dbNode);
			}
		}
	}
	
	public void removeFromIndex(PropertyKey key) {
		
		for (Index<Node> index : Services.getService(NodeService.class).getNodeIndices()) {
			
			synchronized (index) {
				
				index.remove(dbNode, key.dbName());
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
	
	protected <T extends RelationshipInterface> T getRelationshipForType(final Class<T> type) {
		
		try {
			
			return type.newInstance();
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}
		
		return null;
	}
}
