/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.structr.core.property.PropertyMap;
import org.apache.commons.codec.digest.DigestUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.*;
import org.structr.common.GraphObjectComparator;
import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NullArgumentToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.Services;
import org.structr.core.graph.NodeRelationshipStatisticsCommand;

//~--- JDK imports ------------------------------------------------------------


import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.Ownership;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.Tx;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

//~--- classes ----------------------------------------------------------------

/**
 * Abstract base class for all node entities in structr.
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public abstract class AbstractNode implements NodeInterface, AccessControllable {

	private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());
	private static final ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\$\\{[^}]*\\}");
	private static final ThreadLocalMatcher threadLocalFunctionMatcher = new ThreadLocalMatcher("([a-zA-Z0-9_]+)\\((.+)\\)");
	protected static final Map<String, Function<Object, Object>> functions = new LinkedHashMap<>();


	public static final View defaultView = new View(AbstractNode.class, PropertyView.Public, id, type);

	public static final View uiView = new View(AbstractNode.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate
	);

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

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			String name = dbNode.hasProperty(AbstractNode.name.dbName())
				      ? (String) dbNode.getProperty(AbstractNode.name.dbName())
				      : "<null name>";
			String type = dbNode.hasProperty(AbstractNode.type.dbName())
				      ? (String) dbNode.getProperty(AbstractNode.type.dbName())
				      : "<AbstractNode>";
			String id   = dbNode.hasProperty(GraphObject.id.dbName())
				      ? (String) dbNode.getProperty(GraphObject.id.dbName())
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

			dbNode.removeProperty(key.dbName());

			// remove from index
			removeFromIndex(key);
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

		return getProperty(GraphObject.id);

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

		// check for custom view in content-type field
		if (securityContext != null && securityContext.hasCustomView()) {

			final Set<PropertyKey> keys  = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(entityType, propertyView));
			final Set<String> customView = securityContext.getCustomView();

			for (Iterator<PropertyKey> it = keys.iterator(); it.hasNext();) {
				if (!customView.contains(it.next().jsonName())) {

					it.remove();
				}
			}

			return keys;
		}

		// this is the default if no application/json; properties=[...] content-type header is present on the request
		return StructrApp.getConfiguration().getPropertySet(entityType, propertyView);
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

		Object value = getProperty(key, false, null);
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
		return getProperty(key, true, null);
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> key, final org.neo4j.helpers.Predicate<GraphObject> predicate) {
		return getProperty(key, true, predicate);
	}

	private <T> T getProperty(final PropertyKey<T> key, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {

		// early null check, this should not happen...
		if (key == null || key.dbName() == null) {
			return null;
		}

		return key.getProperty(securityContext, this, applyConverter, predicate);
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
		List<String> result  = new LinkedList<>();

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

		if (key != null) {

			final T propertyValue = getProperty(key);

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
	@Override
	public Node getNode() {

		return dbNode;

	}

	/**
	 * Return the (cached) incoming relationship between this node and the
	 * given principal which holds the security information.
	 *
	 * @param p
	 * @return incoming security relationship
	 */
	@Override
	public Security getSecurityRelationship(final Principal p) {

		if (p == null) {

			return null;
		}

		for (Security r : getIncomingRelationships(Security.class)) {

			if (p.equals(r.getSourceNode())) {

				return r;

			}
		}

		return null;

	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getRelationships() {
		return new IterableAdapter<>(dbNode.getRelationships(), new RelationshipFactory<R>(securityContext));
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);
		final Direction direction            = template.getDirectionForType(entityType);
		final RelationshipType relType       = template;

		return new IterableAdapter<>(dbNode.getRelationships(relType, direction), factory);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getSource().getRawSource(securityContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getSource().getRawSource(securityContext, dbNode, null), factory);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getTarget().getRawSource(securityContext, dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(securityContext);
		final R template                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getTarget().getRawSource(securityContext, dbNode, null), factory);
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getIncomingRelationships() {
		return new IterableAdapter<>(dbNode.getRelationships(Direction.INCOMING), new RelationshipFactory<R>(securityContext));
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships() {
		return new IterableAdapter<>(dbNode.getRelationships(Direction.OUTGOING), new RelationshipFactory<R>(securityContext));
	}

	/**
	 * Return statistical information on all relationships of this node
	 *
	 * @return number of relationships
	 */
	public Map<RelationshipType, Long> getRelationshipInfo(Direction dir) throws FrameworkException {
		return StructrApp.getInstance(securityContext).command(NodeRelationshipStatisticsCommand.class).execute(this, dir);
	}

	/**
	 * Returns the owner node of this node, following an INCOMING OWNS relationship.
	 *
	 * @return the owner node of this node
	 */
	@Override
	public Principal getOwnerNode() {

		if (cachedOwnerNode == null) {

			final Ownership ownership = getIncomingRelationship(PrincipalOwnsNode.class);
			if (ownership != null) {

				Principal principal = ownership.getSourceNode();
				cachedOwnerNode = (Principal) principal;
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

		List<Principal> principalList = new LinkedList<>();

		// check any security relationships
		for (Security r : getIncomingRelationships(Security.class)) {

			// check security properties
			Principal principalNode = r.getSourceNode();

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
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final Class<? extends Relation<A, B, S, T>> type) {
		return this.getRelationships(type).iterator().hasNext();
	}

	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Class<R> type) {
		return getRelationshipForType(type).getSource().hasElements(securityContext, dbNode, null);
	}

	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Class<R> type) {
		return getRelationshipForType(type).getTarget().hasElements(securityContext, dbNode, null);
	}

	// ----- interface AccessControllable -----
	@Override
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
	public void afterDeletion(SecurityContext securityContext, PropertyMap properties) {
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

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean error = false;

		error |= ValidationHelper.checkStringNotBlank(this, id, errorBuffer);
		error |= ValidationHelper.checkStringNotBlank(this, type, errorBuffer);

		return !error;

	}

	@Override
	public boolean isVisibleToPublicUsers() {

		return getVisibleToPublicUsers();

	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {

		return getProperty(visibleToAuthenticatedUsers);

	}

	@Override
	public boolean isNotHidden() {

		return !getHidden();

	}

	@Override
	public boolean isHidden() {

		return getHidden();

	}

	@Override
	public Date getVisibilityStartDate() {
		return getProperty(visibilityStartDate);
	}

	@Override
	public Date getVisibilityEndDate() {
		return getProperty(visibilityEndDate);
	}

	@Override
	public Date getCreatedDate() {
		return getProperty(createdDate);
	}

	@Override
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

	@Override
	public void addToIndex() {

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.All)) {

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

		for (Index<Node> index : Services.getInstance().getService(NodeService.class).getNodeIndices()) {

			synchronized (index) {

				index.remove(dbNode);
			}
		}
	}

	public void removeFromIndex(PropertyKey key) {

		for (Index<Node> index : Services.getInstance().getService(NodeService.class).getNodeIndices()) {

			synchronized (index) {

				index.remove(dbNode, key.dbName());
			}
		}
	}

	@Override
	public void indexPassiveProperties() {

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.All)) {

			if (key.isPassivelyIndexed()) {

				key.index(this, this.getPropertyForIndexing(key));
			}
		}
	}

	public static <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R getRelationshipForType(final Class<R> type) {

		try {

			return type.newInstance();

		} catch (Throwable t) {

			// TODO: throw meaningful exception here,
			// should be a RuntimeException that indicates
			// wrong use of Relationships etc.

			t.printStackTrace();
		}

		return null;
	}

	// ----- variable replacement functions etc. -----
	static {

		functions.put("md5", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				return ((sources != null) && (sources.length > 0) && (sources[0] != null))
					? DigestUtils.md5Hex(sources[0].toString())
					: "";

			}

		});
		functions.put("upper", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				return ((sources != null) && (sources.length > 0) && (sources[0] != null))
					? sources[0].toString().toUpperCase()
					: "";

			}

		});
		functions.put("lower", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				return ((sources != null) && (sources.length > 0) && (sources[0] != null))
					? sources[0].toString().toLowerCase()
					: "";

			}

		});
		functions.put("join", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {
				return StringUtils.join(sources);
			}

		});
		functions.put("abbr", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length > 1 && sources[0] != null && sources[1] != null) {

					try {
						int maxLength = Integer.parseInt(sources[1].toString());

						if (sources[0].toString().length() > maxLength) {

							return StringUtils.substringBeforeLast(StringUtils.substring(sources[0].toString(), 0, maxLength), " ").concat("…");

						} else {

							return sources[0];
						}

					} catch (NumberFormatException nfe) {

						return nfe.getMessage();

					}

				}

				return "";

			}

		});
		functions.put("capitalize", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				return ((sources != null) && (sources.length > 0) && (sources[0] != null))
					? StringUtils.capitalize(sources[0].toString())
					: "";

			}
		});
		functions.put("titleize", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources == null || sources.length < 2) {
					return null;
				}

				if (StringUtils.isBlank(sources[0].toString())) {
					return "";
				}

				if (sources[1] == null) {
					sources[1] = " ";
				}

				String[] in = StringUtils.split(sources[0].toString(), sources[1].toString());
				String[] out = new String[in.length];
				for (int i = 0; i < in.length; i++) {
					out[i] = StringUtils.capitalize(in[i]);
				};
				return StringUtils.join(out, " ");

			}

		});
		functions.put("clean", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result;

				if ((sources != null) && (sources.length > 0)) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					String normalized = Normalizer.normalize(sources[0].toString(), Normalizer.Form.NFD)
						.replaceAll("\\<", "")
						.replaceAll("\\>", "")
						.replaceAll("\\.", "")
						.replaceAll("\\'", "-")
						.replaceAll("\\?", "")
						.replaceAll("\\(", "")
						.replaceAll("\\)", "")
						.replaceAll("\\{", "")
						.replaceAll("\\}", "")
						.replaceAll("\\[", "")
						.replaceAll("\\]", "")
						.replaceAll("\\+", "-")
						.replaceAll("/", "-")
						.replaceAll("–", "-")
						.replaceAll("\\\\", "-")
						.replaceAll("\\|", "-")
						.replaceAll("'", "-")
						.replaceAll("!", "")
						.replaceAll(",", "")
						.replaceAll("-", " ")
						.replaceAll("_", " ")
						.replaceAll("`", "-");

					result = normalized.replaceAll("-", " ");
					result = StringUtils.normalizeSpace(result.toLowerCase());
					result = result.replaceAll("[^\\p{ASCII}]", "").replaceAll("\\p{P}", "-").replaceAll("\\-(\\s+\\-)+", "-");
					result = result.replaceAll(" ", "-");

					return result;
				}

				return null;

			}

		});
		functions.put("urlencode", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				return ((sources != null) && (sources.length > 0) && (sources[0] != null))
					? encodeURL(sources[0].toString())
					: "";

			}

		});
		functions.put("if", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources[0] == null || sources.length < 3) {

					return "";
				}

				if (sources[0].equals("true")) {

					return sources[1];
				} else {

					return sources[2];
				}

			}

		});
		functions.put("empty", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (StringUtils.isEmpty(sources[0].toString())) {

					return "true";
				} else {
					return "false";
				}

			}

		});
		functions.put("equal", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				logger.log(Level.FINE, "Length: {0}", sources.length);

				if (sources.length < 2) {

					return "true";
				}

				logger.log(Level.FINE, "Comparing {0} to {1}", new java.lang.Object[]{sources[0], sources[1]});

				if (sources[0] == null || sources[1] == null) {
					return "false";
				}

				return sources[0].equals(sources[1])
					? "true"
					: "false";

			}

		});
		functions.put("add", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				Double result = 0.0;

				if (sources != null) {

					for (Object i : sources) {

						if (i != null) {

							try {

								result += Double.parseDouble(i.toString());

							} catch (Throwable t) {

								return t.getMessage();

							}

						} else {

							result += 0.0;
						}
					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("lt", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";

				if (sources != null && sources.length == 2) {

					try {

						result = (Double.parseDouble(sources[0].toString()) < Double.parseDouble(sources[1].toString())) ? "true" : "false";

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return result;

			}
		});
		functions.put("gt", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";

				if (sources != null && sources.length == 2) {

					try {

						result = (Double.parseDouble(sources[0].toString()) > Double.parseDouble(sources[1].toString())) ? "true" : "false";

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return result;

			}
		});
		functions.put("lte", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";

				if (sources != null && sources.length == 2) {

					try {

						result = (Double.parseDouble(sources[0].toString()) <= Double.parseDouble(sources[1].toString())) ? "true" : "false";

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return result;

			}
		});
		functions.put("gte", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";

				if (sources != null && sources.length == 2) {

					try {

						result = (Double.parseDouble(sources[0].toString()) >= Double.parseDouble(sources[1].toString())) ? "true" : "false";

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return result;

			}
		});
		functions.put("subt", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length > 0) {

					try {

						Double result = Double.parseDouble(sources[0].toString());

						for (int i = 1; i < sources.length; i++) {

							result -= Double.parseDouble(sources[i].toString());

						}

						return new Double(result).toString();

					} catch (Throwable t) {

						return t.getMessage();

					}
				}

				return "";

			}

		});
		functions.put("mult", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				Double result = 1.0d;

				if (sources != null) {

					for (Object i : sources) {

						try {

							result *= Double.parseDouble(i.toString());

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("quot", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				Double result = 0.0d;

				if (sources != null && sources.length == 2) {

					try {

						result = Double.parseDouble(sources[0].toString()) / Double.parseDouble(sources[1].toString());

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("round", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				Double result = 0.0d;

				if (sources != null && sources.length == 2) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					try {

						Double f1 = Double.parseDouble(sources[0].toString());
						double f2 = Math.pow(10, (Integer.parseInt(sources[1].toString())));
						long r = Math.round(f1 * f2);

						result = (double) r / f2;

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("max", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${max(val1, val2)}. Example: ${max(5,10)}";

				if (sources != null && sources.length == 2) {

					try {
						result = Double.toString(Math.max(Double.parseDouble(sources[0].toString()), Double.parseDouble(sources[1].toString())));

					} catch (Throwable t) {
						logger.log(Level.WARNING, "Could not determine max() of {0} and {1}", new Object[]{sources[0], sources[1]});
						result = errorMsg;
					}

				}

				return result;

			}

		});
		functions.put("min", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${min(val1, val2)}. Example: ${min(5,10)}";

				if (sources != null && sources.length == 2) {

					try {
						result = Double.toString(Math.min(Double.parseDouble(sources[0].toString()), Double.parseDouble(sources[1].toString())));

					} catch (Throwable t) {
						logger.log(Level.WARNING, "Could not determine min() of {0} and {1}", new Object[]{sources[0], sources[1]});
						result = errorMsg;
					}

				}

				return result;

			}

		});
		functions.put("date_format", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${date_format(value, pattern)}. Example: ${date_format(Tue Feb 26 10:49:26 CET 2013, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";

				if (sources != null && sources.length == 2) {

					String dateString = sources[0].toString();

					if (StringUtils.isBlank(dateString)) {
						return "";
					}

					String pattern = sources[1].toString();

					try {
						// parse with format from IS
						Date d = new SimpleDateFormat(ISO8601DateProperty.PATTERN).parse(dateString);

						// format with given pattern
						result = new SimpleDateFormat(pattern).format(d);

					} catch (ParseException ex) {
						logger.log(Level.WARNING, "Could not parse date " + dateString + " and format it to pattern " + pattern, ex);
						result = errorMsg;
					}

				}

				return result;
			}
		});
		functions.put("number_format", new Function<Object, Object>() {

			@Override
				public Object apply(final NodeInterface entity, final Object[] sources) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${number_format(value, ISO639LangCode, pattern)}. Example: ${number_format(12345.6789, 'en', '#,##0.00')}";

				if (sources != null && sources.length == 3) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					try {

						Double val = Double.parseDouble(sources[0].toString());
						String langCode = sources[1].toString();
						String pattern = sources[2].toString();

						NumberFormat formatter = DecimalFormat.getInstance(new Locale(langCode));
						((DecimalFormat) formatter).applyLocalizedPattern(pattern);
						result = formatter.format(val);

					} catch (Throwable t) {

						result = errorMsg;

					}

				} else {
					result = errorMsg;
				}

				return result;
			}

		});
		functions.put("not", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources == null || sources.length == 0) {
					return "";
				}

				return sources[0].equals("true") ? "false" : "true";
			}

		});
		functions.put("and", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				boolean result = true;

				if (sources != null) {

					for (Object i : sources) {

						try {

							result &= "true".equals(i);

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return Boolean.toString(result);
			}

		});
		functions.put("or", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				boolean result = false;

				if (sources != null) {

					for (Object i : sources) {

						try {

							result |= "true".equals(i);

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return Boolean.toString(result);
			}

		});
		functions.put("print", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null) {

					for (Object i : sources) {

						System.out.print(i);
					}

					System.out.println();
				}

				return "";
			}

		});
		functions.put("geocode", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length == 3) {

					final Gson gson      = new GsonBuilder().create();
					final String street  = sources[0].toString();
					final String city    = sources[1].toString();
					final String country = sources[2].toString();

					try {
						GeoCodingResult result = GeoHelper.geocode(street, null, null, city, null, country);
						if (result != null) {

							final Map<String, Object> map = new LinkedHashMap<>();

							map.put("latitude", result.getLatitude());
							map.put("longitude", result.getLongitude());

							return serialize(gson, map);
						}

					} catch (FrameworkException fex) {

						fex.printStackTrace();
					}

				}

				return "";
			}

		});
		functions.put("set", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length > 1) {

					if (sources[0] instanceof NodeInterface) {

						final NodeInterface source            = (NodeInterface)sources[0];
						final Map<String, Object> properties  = new LinkedHashMap<>();
						final SecurityContext securityContext = source.getSecurityContext();
						final Gson gson                       = new GsonBuilder().create();
						final Class type                      = source.getClass();
						final int sourceCount                 = sources.length;

						if (sources.length == 3 && sources[1].toString().matches("[a-zA-Z0-9_]+")) {

							properties.put(sources[1].toString(), sources[2]);

						} else {

							// we either have and odd number of items, or two multi-value items.
							for (int i=1; i<sourceCount; i++) {

								properties.putAll(deserialize(gson, sources[i].toString()));
							}
						}

						try {

							// store values in entity
							final PropertyMap map = PropertyMap.inputTypeToJavaType(securityContext, type, properties);
							for (final Entry<PropertyKey, Object> entry : map.entrySet()) {

								source.setProperty(entry.getKey(), entry.getValue());
							}


						} catch (FrameworkException fex) {

							fex.printStackTrace();
						}

					} else {

						logger.log(Level.WARNING, "Invalid use of builtin method set, usage: set(entity, params..)");
					}

				}

				return "";
			}

		});
		functions.put("get", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length == 2 && sources[0] instanceof NodeInterface) {

					final NodeInterface node = (NodeInterface)sources[0];
					final String keyName     = sources[1].toString();
					final PropertyKey key    = StructrApp.getConfiguration().getPropertyKeyForJSONName(node.getClass(), keyName);

					if (key != null) {
						return node.getProperty(key);
					}
				}

				return "";
			}
		});
		functions.put("first", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length == 1 && sources[0] instanceof List) {
					return ((List)sources[0]).get(0);
				}

				return "";
			}
		});
		functions.put("last", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length == 1 && sources[0] instanceof List) {

					final List list = (List)sources[0];
					return list.get(list.size() - 1);
				}

				return "";
			}
		});
		functions.put("nth", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length == 2 && sources[0] instanceof List) {

					final List list = (List)sources[0];
					final int pos   = Integer.parseInt(sources[1].toString());
					final int size  = list.size();

					return list.get(Math.min(Math.max(0, pos), size));
				}

				return "";
			}
		});
		functions.put("each", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) {

				if (sources != null && sources.length == 2 && sources[0] instanceof List) {

					final List list   = (List)sources[0];
					final String expr = sources[1].toString();

					for (final Object obj : list) {

						if (obj instanceof AbstractNode) {

							try {
								final AbstractNode node = (AbstractNode)obj;
								node.extractFunctions(node.getSecurityContext(), new ActionContext(entity), expr);

							} catch (FrameworkException fex) {

								fex.printStackTrace();
							}
						}
					}
				}

				return "";
			}
		});

	}

	public String getPropertyWithVariableReplacement(SecurityContext securityContext, ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {

		return replaceVariables(securityContext, renderContext, getProperty(key));

	}

	public String replaceVariables(final SecurityContext securityContext, final ActionContext actionContext, final Object rawValue) throws FrameworkException {

		String value = null;

		if (rawValue == null) {

			return null;

		}

		if (rawValue instanceof String) {

			value = (String) rawValue;

			if (!actionContext.returnRawValue(securityContext)) {

				// re-use matcher from previous calls
				Matcher matcher = threadLocalTemplateMatcher.get();

				matcher.reset(value);

				while (matcher.find()) {

					String group          = matcher.group();
					String source         = group.substring(2, group.length() - 1);
					Object extractedValue = extractFunctions(securityContext, actionContext, source);

					// fetch referenced property
					if (extractedValue != null) {

						String partValue = StringUtils.remove(extractedValue.toString(), "\\");
						if (partValue != null) {

							value = value.replace(group, partValue);

						} else {

							// If the whole expression should be replaced, and partValue is null
							// replace it by null to make it possible for HTML attributes to not be rendered
							// and avoid something like ... selected="" ... which is interpreted as selected==true by
							// all browsers
							value = value.equals(group) ? null : value.replace(group, "");
						}
					}
				}

			}

		} else if (rawValue instanceof Boolean) {

			value = Boolean.toString((Boolean) rawValue);

		} else {

			value = rawValue.toString();

		}

		return value;

	}

	protected Object extractFunctions(SecurityContext securityContext, ActionContext actionContext, String source) throws FrameworkException {

		// re-use matcher from previous calls
		Matcher functionMatcher = threadLocalFunctionMatcher.get();

		functionMatcher.reset(source);

		if (functionMatcher.matches()) {

			String functionGroup = functionMatcher.group(1);
			String parameter = functionMatcher.group(2);
			String functionName = functionGroup.substring(0, functionGroup.length());
			Function<Object, Object> function = functions.get(functionName);

			if (function != null) {

				if (parameter.contains(",")) {

					String[] parameters = split(parameter);
					Object[] results = new Object[parameters.length];

					// collect results from comma-separated function parameter
					for (int i = 0; i < parameters.length; i++) {

						results[i] = extractFunctions(securityContext, actionContext, StringUtils.strip(parameters[i]));
					}

					return function.apply(this, results);

				} else {

					Object result = extractFunctions(securityContext, actionContext, StringUtils.strip(parameter));

					return function.apply(this, new Object[]{result});

				}
			}

		}

		// if any of the following conditions match, the literal source value is returned
		if (StringUtils.isNotBlank(source) && StringUtils.isNumeric(source)) {

			// return numeric value
			return source;

		} else if (source.startsWith("\"") && source.endsWith("\"")) {

			return source.substring(1, source.length() - 1);

		} else if (source.startsWith("'") && source.endsWith("'")) {

			return source.substring(1, source.length() - 1);

		} else {

			// return property key
			return actionContext.getReferencedProperty(securityContext, this, source);
		}
	}

	protected String[] split(final String source) {

		ArrayList<String> tokens = new ArrayList<>(20);
		boolean inDoubleQuotes = false;
		boolean inSingleQuotes = false;
		boolean ignoreNext = false;
		int len = source.length();
		int level = 0;
		StringBuilder currentToken = new StringBuilder(len);

		for (int i = 0; i < len; i++) {

			char c = source.charAt(i);

			// do not strip away separators in nested functions!
			if ((level != 0) || (c != ',')) {

				currentToken.append(c);
			}

			if (ignoreNext) {

				ignoreNext = false;
				continue;

			}

			switch (c) {

				case '\\':

					ignoreNext = true;

					break;

				case '(':
					level++;

					break;

				case ')':
					level--;

					break;

				case '"':
					if (inDoubleQuotes) {

						inDoubleQuotes = false;

						level--;

					} else {

						inDoubleQuotes = true;

						level++;

					}

					break;

				case '\'':
					if (inSingleQuotes) {

						inSingleQuotes = false;

						level--;

					} else {

						inSingleQuotes = true;

						level++;

					}

					break;

				case ',':
					if (level == 0) {

						tokens.add(currentToken.toString().trim());
						currentToken.setLength(0);

					}

					break;

			}

		}

		if (currentToken.length() > 0) {

			tokens.add(currentToken.toString().trim());
		}

		return tokens.toArray(new String[0]);

	}

	protected static String encodeURL(final String source) {

		try {
			return URLEncoder.encode(source, "UTF-8");

		} catch (UnsupportedEncodingException ex) {

			logger.log(Level.WARNING, "Unsupported Encoding", ex);
		}

		// fallback, unencoded
		return source;
	}

	protected static String serialize(final Gson gson, final Map<String, Object> map) {
		return gson.toJson(map, new TypeToken<Map<String, String>>() { }.getType());
	}

	protected static Map<String, Object> deserialize(final Gson gson, final String source) {
		return gson.fromJson(source, new TypeToken<Map<String, Object>>() { }.getType());
	}
}
