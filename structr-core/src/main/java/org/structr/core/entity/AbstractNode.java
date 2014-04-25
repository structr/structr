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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.Ownership;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//~--- classes ----------------------------------------------------------------

/**
 * Abstract base class for all node entities in structr.
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public abstract class AbstractNode implements NodeInterface, AccessControllable {

	private static final String regexDecimal = "^-?\\d*\\.\\d+$";
	private static final String regexInteger = "^-?\\d+$";
	private static final String regexSciNot  = "^-?\\d*\\.\\d+e-?\\d+$";
	private static final String regexDouble  = regexDecimal + "|" + regexInteger + "|" + regexSciNot;

	private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());
	private static final ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\$\\{[^}]*\\}");
	private static final ThreadLocalMatcher threadLocalFunctionMatcher = new ThreadLocalMatcher("([a-zA-Z0-9_]+)\\((.+)\\)");
	private static final ThreadLocalMatcher threadLocalDoubleMatcher   = new ThreadLocalMatcher(regexDouble);
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
	 * @param value
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
	 * @param key
	 * @return 
	 */
	public Object getTemporaryProperty(final PropertyKey key) {
		return cachedConvertedProperties.get(key);
	}

	/**
	 * Set a property in database backend. This method needs to be wrappend into
	 * a StructrTransaction, otherwise Neo4j will throw a NotInTransactionException!
	 * Set property only if value has changed.
	 *
	 * @param <T>
	 * @param key
	 * @throws org.structr.common.error.FrameworkException
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
	
	/**
	 * Test if the given object array has a minimum length and
	 * all its elements are not null.
	 * 
	 * @param array
	 * @param minLength If null, don't do length check
	 * @return 
	 */
	private static boolean arrayHasMinLengthAndAllElementsNotNull(final Object[] array, final Integer minLength) {

		if (array == null) {
			return false;
		}
		
		for (final Object element : array) {

			if (element == null) {
				return false;
			}

		}
		
		return minLength != null ? array.length >= minLength : true;

	}
	
	/**
	 * Test if the given object array has exact the given length and
	 * all its elements are not null.
	 * 
	 * @param array
	 * @param length If null, don't do length check
	 * @return 
	 */
	private static boolean arrayHasLengthAndAllElementsNotNull(final Object[] array, final Integer length) {

		if (array == null) {
			return false;
		}
		
		for (final Object element : array) {

			if (element == null) {
				return false;
			}

		}
		
		return length != null ? array.length == length : true;

	}

	static {

		functions.put("md5", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? DigestUtils.md5Hex(sources[0].toString())
					: "";

			}

		});
		functions.put("upper", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? sources[0].toString().toUpperCase()
					: "";

			}

		});
		functions.put("lower", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? sources[0].toString().toLowerCase()
					: "";

			}

		});
		functions.put("join", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				final List list = new ArrayList();
				for (final Object source : sources) {

					if (source instanceof Collection) {

						list.addAll((Collection)source);

					} else {

						list.add(source);
					}
				}

				return StringUtils.join(list, "");
			}

		});
		functions.put("split", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final String toSplit = sources[0].toString();
					String splitExpr     = "[,;]+";

					if (sources.length >= 2) {
						splitExpr = sources[1].toString();
					}

					return Arrays.asList(toSplit.split(splitExpr));
				}

				return "";
			}

		});
		functions.put("abbr", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					try {
						int maxLength = Double.valueOf(sources[1].toString()).intValue();

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? StringUtils.capitalize(sources[0].toString())
					: "";

			}
		});
		functions.put("titleize", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources.length < 2 || sources[0] == null) {
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
				}
				return StringUtils.join(out, " ");

			}

		});
		functions.put("num", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						return Double.parseDouble(sources[0].toString());

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}
		});
		functions.put("clean", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result;

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? encodeURL(sources[0].toString())
					: "";

			}

		});
		functions.put("if", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources[0] == null || StringUtils.isEmpty(sources[0].toString())) {

					return "true";
				} else {
					return "false";
				}

			}

		});
		functions.put("equal", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

				return result;

			}

		});
		functions.put("lt", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					try {

						Double result = Double.parseDouble(sources[0].toString());

						for (int i = 1; i < sources.length; i++) {

							result -= Double.parseDouble(sources[i].toString());

						}

						return result;

					} catch (Throwable t) {

						return t.getMessage();

					}
				}

				return "";

			}

		});
		functions.put("mult", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

				return result;

			}

		});
		functions.put("quot", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				Double result = 0.0d;

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					try {

						result = Double.parseDouble(sources[0].toString()) / Double.parseDouble(sources[1].toString());

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return result;

			}

		});
		functions.put("round", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				Double result = 0.0d;

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					try {

						Double f1 = Double.parseDouble(sources[0].toString());
						double f2 = Math.pow(10, (Double.parseDouble(sources[1].toString())));
						long r = Math.round(f1 * f2);

						result = (double) r / f2;

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return result;

			}

		});
		functions.put("max", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				Object result   = "";
				String errorMsg = "ERROR! Usage: ${max(val1, val2)}. Example: ${max(5,10)}";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					try {
						result = Math.max(Double.parseDouble(sources[0].toString()), Double.parseDouble(sources[1].toString()));

					} catch (Throwable t) {

						logger.log(Level.WARNING, "Could not determine max() of {0} and {1}", new Object[]{sources[0], sources[1]});
						result = errorMsg;
					}

				} else {

					result = "";
				}

				return result;

			}

		});
		functions.put("min", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				Object result   = "";
				String errorMsg = "ERROR! Usage: ${min(val1, val2)}. Example: ${min(5,10)}";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					try {
						result = Math.min(Double.parseDouble(sources[0].toString()), Double.parseDouble(sources[1].toString()));

					} catch (Throwable t) {

						logger.log(Level.WARNING, "Could not determine min() of {0} and {1}", new Object[]{sources[0], sources[1]});
						result = errorMsg;
					}

				} else {

					result = "";
				}

				return result;

			}

		});
		functions.put("config", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final String configKey    = sources[0].toString();
					final String defaultValue = sources.length >= 2 ? sources[1].toString() : "";

					return StructrApp.getConfigurationValue(configKey, defaultValue);
				}

				return "";
			}
		});
		functions.put("date_format", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";
				String errorMsg = "ERROR! Usage: ${date_format(value, pattern)}. Example: ${date_format(Tue Feb 26 10:49:26 CET 2013, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

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
				public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";
				String errorMsg = "ERROR! Usage: ${number_format(value, ISO639LangCode, pattern)}. Example: ${number_format(12345.6789, 'en', '#,##0.00')}";

				if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					try {

						Double val = Double.parseDouble(sources[0].toString());
						String langCode = sources[1].toString();
						String pattern = sources[2].toString();

						result = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.forLanguageTag(langCode))).format(val);

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					return "true".equals(sources[0].toString()) ? "false" : "true";

				}

					return "";
			}

		});
		functions.put("and", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				boolean result = true;

				if (sources != null) {

					for (Object i : sources) {

						try {

							result &= "true".equals(i.toString());

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				boolean result = false;

				if (sources != null) {

					for (Object i : sources) {

						try {

							result |= "true".equals(i.toString());

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return Boolean.toString(result);
			}

		});
		functions.put("get", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof NodeInterface) {

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
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof List) {
					return ((List)sources[0]).get(0);
				}

				return "";
			}
		});
		functions.put("last", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) &&  sources[0] instanceof List) {

					final List list = (List)sources[0];
					return list.get(list.size() - 1);
				}

				return "";
			}
		});
		functions.put("nth", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof List) {

					final List list = (List)sources[0];
					final int pos   = Double.valueOf(sources[1].toString()).intValue();
					final int size  = list.size();

					return list.get(Math.min(Math.max(0, pos), size));
				}

				return "";
			}
		});
		functions.put("each", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof List) {

					final List list   = (List)sources[0];
					final String expr = sources[1].toString();

					for (final Object obj : list) {

						if (obj instanceof AbstractNode) {

							final AbstractNode node = (AbstractNode)obj;
							node.extractFunctions(node.getSecurityContext(), new ActionContext(entity), expr);
						}
					}
				}

				return "";
			}
		});

		// ----- BEGIN functions with side effects -----
		functions.put("print", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					for (Object i : sources) {

						System.out.print(i);
					}

					System.out.println();
				}

				return "";
			}

		});
		functions.put("read", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = AbstractNode.getSandboxFileName(sources[0].toString());
						if (sandboxFilename != null) {

							final File file = new File(sandboxFilename);
							if (file.exists() && file.length() < 10000000) {

								try (final FileInputStream fis = new FileInputStream(file)) {

									return IOUtils.toString(fis, "utf-8");
								}
							}
						}

					} catch (IOException ioex) {
						ioex.printStackTrace();
					}
				}

				return "";
			}

		});
		functions.put("write", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = AbstractNode.getSandboxFileName(sources[0].toString());
						if (sandboxFilename != null) {

							final File file = new File(sandboxFilename);
							if (!file.exists()) {

								try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file, false))) {

									for (int i=1; i<sources.length; i++) {
										if (sources[i] != null) {
											IOUtils.write(sources[i].toString(), writer);
										}
									}

									writer.flush();
								}

							} else {

								logger.log(Level.SEVERE, "Trying to overwrite an existing file, please use append() for that purpose.");
							}
						}

					} catch (IOException ioex) {
						ioex.printStackTrace();
					}
				}

				return "";
			}

		});
		functions.put("append", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = AbstractNode.getSandboxFileName(sources[0].toString());
						if (sandboxFilename != null) {

							final File file = new File(sandboxFilename);

							try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file, true))) {

								for (int i=1; i<sources.length; i++) {
									IOUtils.write(sources[i].toString(), writer);
								}

								writer.flush();
							}
						}

					} catch (IOException ioex) {
						ioex.printStackTrace();
					}
				}

				return "";
			}

		});
		functions.put("xml", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof String) {

					try {

						final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						if (builder != null) {

							final String xml          = (String)sources[0];
							final StringReader reader = new StringReader(xml);
							final InputSource src     = new InputSource(reader);

							return builder.parse(src);
						}

					} catch (IOException | SAXException | ParserConfigurationException ex) {
						ex.printStackTrace();
					}
				}

				return "";
			}

		});
		functions.put("xpath", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof Document) {

					try {

						XPath xpath = XPathFactory.newInstance().newXPath();
						return xpath.evaluate(sources[1].toString(), sources[0], XPathConstants.STRING);

					} catch (XPathExpressionException ioex) {
						ioex.printStackTrace();
					}
				}

				return "";
			}

		});
		functions.put("set", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof NodeInterface) {

						final NodeInterface source            = (NodeInterface)sources[0];
						final Map<String, Object> properties  = new LinkedHashMap<>();
						final SecurityContext securityContext = source.getSecurityContext();
						final Gson gson                       = new GsonBuilder().create();
						final Class type                      = source.getClass();
						final int sourceCount                 = sources.length;

						if (sources.length == 3 && sources[2] != null && sources[1].toString().matches("[a-zA-Z0-9_]+")) {

							properties.put(sources[1].toString(), sources[2]);

						} else {

							// we either have and odd number of items, or two multi-value items.
							for (int i=1; i<sourceCount; i++) {

								properties.putAll(deserialize(gson, sources[i].toString()));
							}
						}

						// store values in entity
						final PropertyMap map = PropertyMap.inputTypeToJavaType(securityContext, type, properties);
						for (final Entry<PropertyKey, Object> entry : map.entrySet()) {

							source.setProperty(entry.getKey(), entry.getValue());
						}

					} else {

						throw new FrameworkException(422, "Invalid use of builtin method set, usage: set(entity, params..)");
					}

				}

				return "";
			}

		});
		functions.put("send_plaintext_mail", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 6)) {

					final String from        = sources[0].toString();
					final String fromName    = sources[1].toString();
					final String to          = sources[2].toString();
					final String toName      = sources[3].toString();
					final String subject     = sources[4].toString();
					final String textContent = sources[5].toString();

					try {
						MailHelper.sendSimpleMail(from, fromName, to, toName, null, null, from, subject, textContent);

					} catch (EmailException eex) {
						eex.printStackTrace();
					}
				}

				return "";
			}
		});
		functions.put("send_html_mail", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 6)) {

					final String from        = sources[0].toString();
					final String fromName    = sources[1].toString();
					final String to          = sources[2].toString();
					final String toName      = sources[3].toString();
					final String subject     = sources[4].toString();
					final String htmlContent = sources[5].toString();
					final String textContent = sources[6].toString();

					try {
						MailHelper.sendHtmlMail(from, fromName, to, toName, null, null, from, subject, htmlContent, textContent);

					} catch (EmailException eex) {
						eex.printStackTrace();
					}
				}

				return "";
			}
		});
		functions.put("geocode", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

					final Gson gson      = new GsonBuilder().create();
					final String street  = sources[0].toString();
					final String city    = sources[1].toString();
					final String country = sources[2].toString();

					GeoCodingResult result = GeoHelper.geocode(street, null, null, city, null, country);
					if (result != null) {

						final Map<String, Object> map = new LinkedHashMap<>();

						map.put("latitude", result.getLatitude());
						map.put("longitude", result.getLongitude());

						return serialize(gson, map);
					}

				}

				return "";
			}

		});
		functions.put("find", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					final SecurityContext securityContext = entity.getSecurityContext();
					final ConfigurationProvider config    = StructrApp.getConfiguration();
					final Query query                     = StructrApp.getInstance(securityContext).nodeQuery();

					// the type to query for
					Class type = null;

					if (sources.length >= 1 && sources[0] != null) {

						type  = config.getNodeEntityClass(sources[0].toString());
						if (type != null) {

							query.andTypes(type);
						}
					}

					switch (sources.length) {

						case 7: // third (key,value) tuple

							final PropertyKey key3 = config.getPropertyKeyForJSONName(type, sources[5].toString());
							if (key3 != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key3.isSearchable()) {
									throw new FrameworkException(400, "Search key " + key3.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key3.inputConverter(securityContext);
								Object value                           = sources[6].toString();

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								query.and(key3, value);
							}

						case 5: // second (key,value) tuple

							final PropertyKey key2 = config.getPropertyKeyForJSONName(type, sources[3].toString());
							if (key2 != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key2.isSearchable()) {
									throw new FrameworkException(400, "Search key " + key2.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key2.inputConverter(securityContext);
								Object value                           = sources[4].toString();

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								query.and(key2, value);
							}

						case 3: // (key,value) tuple

							final PropertyKey key1 = config.getPropertyKeyForJSONName(type, sources[1].toString());
							if (key1 != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key1.isSearchable()) {
									throw new FrameworkException(400, "Search key " + key1.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key1.inputConverter(securityContext);
								Object value                           = sources[2].toString();

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								query.and(key1, value);
							}
							break;
					}

					// return search results
					return query.getAsList();
				}

				return "";
			}

		});
		functions.put("create", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					final SecurityContext securityContext = entity.getSecurityContext();
					final App app                         = StructrApp.getInstance(securityContext);
					final ConfigurationProvider config    = StructrApp.getConfiguration();
					PropertyMap propertyMap               = new PropertyMap();

					// the type to query for
					Class type = null;

					if (sources.length >= 1 && sources[0] != null) {


						type  = config.getNodeEntityClass(sources[0].toString());

						if (type.equals(entity.getClass())) {
							throw new FrameworkException(422, "Cannot create() entity of the same type in save action.");
						}
					}

					switch (sources.length) {

						case 7: // third (key,value) tuple

							final PropertyKey key3 = config.getPropertyKeyForJSONName(type, sources[5].toString());
							if (key3 != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key3.isSearchable()) {
									throw new FrameworkException(400, "Search key " + key3.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key3.inputConverter(securityContext);
								Object value                           = sources[6].toString();

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								propertyMap.put(key3, value);
							}

						case 5: // second (key,value) tuple

							final PropertyKey key2 = config.getPropertyKeyForJSONName(type, sources[3].toString());
							if (key2 != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key2.isSearchable()) {
									throw new FrameworkException(400, "Search key " + key2.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key2.inputConverter(securityContext);
								Object value                           = sources[4].toString();

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								propertyMap.put(key2, value);
							}

						case 3: // (key,value) tuple

							final PropertyKey key1 = config.getPropertyKeyForJSONName(type, sources[1].toString());
							if (key1 != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key1.isSearchable()) {
									throw new FrameworkException(400, "Search key " + key1.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key1.inputConverter(securityContext);
								Object value                           = sources[2].toString();

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								propertyMap.put(key1, value);
							}
							break;
					}

					if (type != null) {

						app.create(type, propertyMap);

					} else {

						throw new FrameworkException(422, "Unknown type in create() save action.");
					}

				}

				return "";
			}
		});
		functions.put("delete", new Function<Object, Object>() {

			@Override
			public Object apply(final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					final App app = StructrApp.getInstance(entity.getSecurityContext());
					for (final Object obj : sources) {

						if (obj instanceof NodeInterface) {

							app.delete((NodeInterface)obj);
							continue;
						}

						if (obj instanceof RelationshipInterface) {

							app.delete((RelationshipInterface)obj);
							continue;
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

					} else {

						value = "";
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
			String parameter     = functionMatcher.group(2);
			String functionName  = functionGroup.substring(0, functionGroup.length());

			final Function<Object, Object> function = functions.get(functionName);
			final List results                      = new ArrayList();

			if (function != null) {

				if (parameter.contains(",")) {

					final String[] parameters = split(parameter);

					// collect results from comma-separated function parameter
					for (int i = 0; i < parameters.length; i++) {

						addAll(results, extractFunctions(securityContext, actionContext, StringUtils.strip(parameters[i])));
					}

					return function.apply(this, results.toArray());

				} else {

					addAll(results, extractFunctions(securityContext, actionContext, StringUtils.strip(parameter)));

					return function.apply(this, results.toArray());

				}
			}

		}

		// if any of the following conditions match, the literal source value is returned
		if (source.startsWith("\"") && source.endsWith("\"")) {

			return source.substring(1, source.length() - 1);

		} else if (source.startsWith("'") && source.endsWith("'")) {

			return source.substring(1, source.length() - 1);

		} else if (StringUtils.isNotBlank(source) && isNumeric(source)) {

			// return numeric value
			return Double.parseDouble(source);

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

	protected static boolean isNumeric(final String source) {
		return threadLocalDoubleMatcher.get().reset(source).matches();
	}

	protected static String getSandboxFileName(final String source) throws IOException {

		final File sandboxFile = new File(source);
		final String fileName  = sandboxFile.getName();
		final String basePath  = StructrApp.getConfigurationValue(Services.BASE_PATH);

		if (!basePath.isEmpty()) {

			final String defaultExchangePath = basePath.endsWith("/") ? basePath.concat("exchange") : basePath.concat("/exchange");
			String exchangeDir               = StructrApp.getConfigurationValue(Services.DATA_EXCHANGE_PATH, defaultExchangePath);

			if (!exchangeDir.endsWith("/")) {
				exchangeDir = exchangeDir.concat("/");
			}

			// create exchange directory
			final File dir = new File(exchangeDir);
			if (!dir.exists()) {

				dir.mkdirs();
			}

			// return sandboxed file name
			return exchangeDir.concat(fileName);


		} else {

			logger.log(Level.WARNING, "Unable to determine base.path from structr.conf, no data input/output possible.");
		}

		return null;
	}

	// ----- private methods -----
	private void addAll(final List results, final Object partialResult) {

		if (partialResult instanceof Object[]) {

			results.addAll(Arrays.asList((Object[])partialResult));

		} else {

			results.add(partialResult);
		}
	}
}
