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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.structr.common.AccessControllable;
import org.structr.common.GraphObjectComparator;
import org.structr.common.MailHelper;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NullArgumentToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.Ownership;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import static org.structr.core.entity.AbstractNode.functions;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeRelationshipStatisticsCommand;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
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

	public static final String NULL_STRING                       = "___NULL___";

	public static final String ERROR_MESSAGE_MD5                 = "Usage: ${md5(string)}. Example: ${md5(this.email)}";
	public static final String ERROR_MESSAGE_UPPER               = "Usage: ${upper(string)}. Example: ${upper(this.nickName)}";
	public static final String ERROR_MESSAGE_LOWER               = "Usage: ${lower(string)}. Example: ${lower(this.email)}";
	public static final String ERROR_MESSAGE_JOIN                = "Usage: ${join(values...)}. Example: ${join(this.firstName, this.lastName)}";
	public static final String ERROR_MESSAGE_SPLIT               = "Usage: ${split(value)}. Example: ${split(this.commaSeparatedItems)}";
	public static final String ERROR_MESSAGE_ABBR                = "Usage: ${abbr(longString, maxLength)}. Example: ${abbr(this.title, 20)}";
	public static final String ERROR_MESSAGE_CAPITALIZE          = "Usage: ${capitalize(string)}. Example: ${capitalize(this.nickName)}";
	public static final String ERROR_MESSAGE_TITLEIZE            = "Usage: ${titleize(string, separator}. Example: ${titleize(this.lowerCamelCaseString, \"_\")}";
	public static final String ERROR_MESSAGE_NUM                 = "Usage: ${num(string)}. Example: ${num(this.numericalStringValue)}";
	public static final String ERROR_MESSAGE_INT                 = "Usage: ${int(string)}. Example: ${int(this.numericalStringValue)}";
	public static final String ERROR_MESSAGE_RANDOM              = "Usage: ${random(num)}. Example: ${set(this, \"password\", random(8))}";
	public static final String ERROR_MESSAGE_INDEX_OF            = "Usage: ${index_of(string, word)}. Example: ${index_of(this.name, \"the\")}";
	public static final String ERROR_MESSAGE_CONTAINS            = "Usage: ${contains(string, word)}. Example: ${contains(this.name, \"the\")}";
	public static final String ERROR_MESSAGE_SUBSTRING           = "Usage: ${substring(string, start, length)}. Example: ${substring(this.name, 19, 3)}";
	public static final String ERROR_MESSAGE_LENGTH              = "Usage: ${length(string)}. Example: ${length(this.name)}";
	public static final String ERROR_MESSAGE_REPLACE             = "Usage: ${replace(template, source)}. Example: ${replace(\"${this.id}\", this)}";
	public static final String ERROR_MESSAGE_CLEAN               = "Usage: ${clean(string)}. Example: ${clean(this.stringWithNonWordChars)}";
	public static final String ERROR_MESSAGE_URLENCODE           = "Usage: ${urlencode(string)}. Example: ${urlencode(this.email)}";
	public static final String ERROR_MESSAGE_IF                  = "Usage: ${if(condition, trueValue, falseValue)}. Example: ${if(empty(this.name), this.nickName, this.name)}";
	public static final String ERROR_MESSAGE_DO                  = "Usage: ${do(condition, trueStatement, falseStatement)}. Example: ${do(empty(this.name), \"print(\"empty\")\", \"print(\"not empty\")\")}";
	public static final String ERROR_MESSAGE_EMPTY               = "Usage: ${empty(string)}. Example: ${if(empty(possibleEmptyString), \"empty\", \"non-empty\")}";
	public static final String ERROR_MESSAGE_EQUAL               = "Usage: ${equal(value1, value2)}. Example: ${equal(this.children.size, 0)}";
	public static final String ERROR_MESSAGE_ADD                 = "Usage: ${add(values...)}. Example: ${add(1, 2, 3, this.children.size)}";
	public static final String ERROR_MESSAGE_INT_SUM             = "Usage: ${int_sum(list)}. Example: ${int_sum(extract(this.children, \"number\"))}";
	public static final String ERROR_MESSAGE_DOUBLE_SUM          = "Usage: ${double_sum(list)}. Example: ${double_sum(extract(this.children, \"amount\"))}";
	public static final String ERROR_MESSAGE_EXTRACT             = "Usage: ${extract(list, propertyName)}. Example: ${extract(this.children, \"amount\")}";
	public static final String ERROR_MESSAGE_MERGE               = "Usage: ${merge(list1, list2, list3, ...)}. Example: ${merge(this.children, this.siblings)}";
	public static final String ERROR_MESSAGE_SORT                = "Usage: ${sort(list1, key [, true])}. Example: ${sort(this.children, \"name\")}";
	public static final String ERROR_MESSAGE_LT                  = "Usage: ${lt(value1, value2)}. Example: ${if(lt(this.children, 2), \"Less than two\", \"Equal to or more than two\")}";
	public static final String ERROR_MESSAGE_GT                  = "Usage: ${gt(value1, value2)}. Example: ${if(gt(this.children, 2), \"More than two\", \"Equal to or less than two\")}";
	public static final String ERROR_MESSAGE_LTE                 = "Usage: ${lte(value1, value2)}. Example: ${if(lte(this.children, 2), \"Equal to or less than two\", \"More than two\")}";
	public static final String ERROR_MESSAGE_GTE                 = "Usage: ${gte(value1, value2)}. Example: ${if(gte(this.children, 2), \"Equal to or more than two\", \"Less than two\")}";
	public static final String ERROR_MESSAGE_SUBT                = "Usage: ${subt(value1, value)}. Example: ${subt(5, 2)}";
	public static final String ERROR_MESSAGE_MULT                = "Usage: ${mult(value1, value)}. Example: ${mult(5, 2)}";
	public static final String ERROR_MESSAGE_QUOT                = "Usage: ${quot(value1, value)}. Example: ${quot(5, 2)}";
	public static final String ERROR_MESSAGE_ROUND               = "Usage: ${round(value1 [, decimalPlaces])}. Example: ${round(2.345678, 2)}";
	public static final String ERROR_MESSAGE_MAX                 = "Usage: ${max(value1, value2)}. Example: ${max(this.children, 10)}";
	public static final String ERROR_MESSAGE_MIN                 = "Usage: ${min(value1, value2)}. Example: ${min(this.children, 5)}";
	public static final String ERROR_MESSAGE_CONFIG              = "Usage: ${config(keyFromStructrConf)}. Example: ${config(\"base.path\")}";
	public static final String ERROR_MESSAGE_DATE_FORMAT         = "Usage: ${date_format(value, pattern)}. Example: ${date_format(this.creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";
	public static final String ERROR_MESSAGE_PARSE_DATE          = "Usage: ${parse_date(value, pattern)}. Example: ${parse_format(\"2014-01-01\", \"yyyy-MM-dd\")}";
	public static final String ERROR_MESSAGE_NUMBER_FORMAT       = "Usage: ${number_format(value, ISO639LangCode, pattern)}. Example: ${number_format(12345.6789, 'en', '#,##0.00')}";
	public static final String ERROR_MESSAGE_TEMPLATE            = "Usage: ${template(name, locale, source)}. Example: ${template(\"TEXT_TEMPLATE_1\", \"en_EN\", this)}";
	public static final String ERROR_MESSAGE_NOT                 = "Usage: ${not(bool1, bool2)}. Example: ${not(\"true\", \"true\")}";
	public static final String ERROR_MESSAGE_AND                 = "Usage: ${and(bool1, bool2)}. Example: ${and(\"true\", \"true\")}";
	public static final String ERROR_MESSAGE_OR                  = "Usage: ${or(bool1, bool2)}. Example: ${or(\"true\", \"true\")}";
	public static final String ERROR_MESSAGE_GET                 = "Usage: ${get(entity, propertyKey)}. Example: ${get(this, \"children\")}";
	public static final String ERROR_MESSAGE_GET_ENTITY          = "Cannot evaluate first argument to entity, must be entity or single element list of entities.";
	public static final String ERROR_MESSAGE_FIRST               = "Usage: ${first(collection)}. Example: ${first(this.children)}";
	public static final String ERROR_MESSAGE_LAST                = "Usage: ${last(collection)}. Example: ${last(this.children)}";
	public static final String ERROR_MESSAGE_NTH                 = "Usage: ${nth(collection)}. Example: ${nth(this.children, 2)}";
	public static final String ERROR_MESSAGE_EVAL                = "Usage: ${eval(expression...)}. Example: ${eval(\"print(this.name)\", \"delete(this)\"}";
	public static final String ERROR_MESSAGE_MERGE_PROPERTIES    = "Usage: ${merge_properties(source, target , mergeKeys...)}. Example: ${merge_properties(this, parent, \"eMail\")}";
	public static final String ERROR_MESSAGE_EACH                = "Usage: ${each(collection, expression)}. Example: ${each(this.children, \"set(this, \"email\", lower(get(this.email))))\")}";
	public static final String ERROR_MESSAGE_PRINT               = "Usage: ${print(objects...)}. Example: ${print(this.name, \"test\")}";
	public static final String ERROR_MESSAGE_READ                = "Usage: ${read(filename)}. Example: ${read(\"text.xml\")}";
	public static final String ERROR_MESSAGE_WRITE               = "Usage: ${write(filename, value)}. Example: ${write(\"text.txt\", this.name)}";
	public static final String ERROR_MESSAGE_APPEND              = "Usage: ${append(filename, value)}. Example: ${append(\"test.txt\", this.name)}";
	public static final String ERROR_MESSAGE_XML                 = "Usage: ${xml(xmlSource)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\")}";
	public static final String ERROR_MESSAGE_XPATH               = "Usage: ${xpath(xmlDocument, expression)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\")}";
	public static final String ERROR_MESSAGE_SET                 = "Usage: ${set(entity, propertyKey, value)}. Example: ${set(this, \"email\", lower(this.email))}";
	public static final String ERROR_MESSAGE_SEND_PLAINTEXT_MAIL = "Usage: ${send_plaintext_mail(fromAddress, fromName, toAddress, toName, subject, content)}.";
	public static final String ERROR_MESSAGE_SEND_HTML_MAIL      = "Usage: ${send_html_mail(fromAddress, fromName, toAddress, toName, subject, content)}.";
	public static final String ERROR_MESSAGE_GEOCODE             = "Usage: ${geocode(street, city, country)}. Example: ${set(this, geocode(this.street, this.city, this.country))}";
	public static final String ERROR_MESSAGE_FIND                = "Usage: ${find(type, key, value)}. Example: ${find(\"User\", \"email\", \"tester@test.com\"}";
	public static final String ERROR_MESSAGE_CREATE              = "Usage: ${create(type, key, value)}. Example: ${create(\"Feedback\", \"text\", this.text)}";
	public static final String ERROR_MESSAGE_DELETE              = "Usage: ${delete(entity)}. Example: ${delete(this)}";


	private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());
	private static final ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\$\\{[^}]*\\}");
	private static final ThreadLocalMatcher threadLocalFunctionMatcher = new ThreadLocalMatcher("([a-zA-Z0-9_]+)\\((.*)\\)");
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

				} catch (Throwable t) {

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
	 * @param propertyKey the property key to retrieve the value for
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

	private <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationshipsAsSuperUser(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(SecurityContext.getSuperUserInstance());
		final R template                     = getRelationshipForType(type);

		return new IterableAdapter<>(template.getSource().getRawSource(SecurityContext.getSuperUserInstance(), dbNode, null), factory);
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

		for (Security r : getIncomingRelationshipsAsSuperUser(Security.class)) {

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
	 * @param dir
	 * @return number of relationships
	 */
	public Map<RelationshipType, Long> getRelationshipInfo(final Direction dir) throws FrameworkException {
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

			final Ownership ownership = getIncomingRelationshipAsSuperUser(PrincipalOwnsNode.class);
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
		for (Security r : getIncomingRelationshipsAsSuperUser(Security.class)) {

			// check security properties
			Principal principalNode = r.getSourceNode();

			principalList.add(principalNode);
		}

		return principalList;

	}

	private <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(final Class<R> type) {

		final RelationshipFactory<R> factory = new RelationshipFactory<>(SecurityContext.getSuperUserInstance());
		final R template                     = getRelationshipForType(type);
		final Relationship relationship      = template.getSource().getRawSource(SecurityContext.getSuperUserInstance(), dbNode, null);

		if (relationship != null) {
			return factory.adapt(relationship);
		}

		return null;
	}

	/**
	 * Return true if this node has a relationship of given type and direction.
	 *
	 * @param type
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

		functions.put("error", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entity.getClass(), sources[0].toString());
					ctx.raiseError(entity.getType(), new ErrorToken(422, key) {

						@Override
						public JsonElement getContent() {
							return new JsonPrimitive(getErrorToken());
						}

						@Override
						public String getErrorToken() {
							return sources[1].toString();
						}
					});


				} else if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entity.getClass(), sources[0].toString());
					ctx.raiseError(entity.getType(), new SemanticErrorToken(key) {

						@Override
						public JsonElement getContent() {

							JsonObject obj = new JsonObject();

							if (sources[2] instanceof Number) {

								obj.add(getErrorToken(), new JsonPrimitive((Number)sources[2]));

							} else if (sources[2] instanceof Boolean) {

								obj.add(getErrorToken(), new JsonPrimitive((Boolean)sources[2]));

							} else {

								obj.add(getErrorToken(), new JsonPrimitive(sources[2].toString()));
							}

							return obj;
						}

						@Override
						public String getErrorToken() {
							return sources[1].toString();
						}
					});
				}

				return null;
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_MD5;
			}
		});
		functions.put("md5", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? DigestUtils.md5Hex(sources[0].toString())
					: "";

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_MD5;
			}
		});
		functions.put("upper", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? sources[0].toString().toUpperCase()
					: "";

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_UPPER;
			}

		});
		functions.put("lower", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? sources[0].toString().toLowerCase()
					: "";

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_LOWER;
			}

		});
		functions.put("join", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_JOIN;
			}

		});
		functions.put("split", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_SPLIT;
			}

		});
		functions.put("abbr", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_ABBR;
			}

		});
		functions.put("capitalize", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? StringUtils.capitalize(sources[0].toString())
					: "";

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_CAPITALIZE;
			}
		});
		functions.put("titleize", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_TITLEIZE;
			}

		});
		functions.put("num", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						return Double.valueOf(sources[0].toString());

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_NUM;
			}
		});
		functions.put("int", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof Number) {
						return ((Number)sources[0]).intValue();
					}

					try {
						return Double.valueOf(sources[0].toString()).intValue();

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_INT;
			}
		});
		functions.put("random", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof Number) {

					try {
						return RandomStringUtils.randomAlphanumeric(((Number)sources[0]).intValue());

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_RANDOM;
			}
		});
		functions.put("index_of", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					final String source = sources[0].toString();
					final String part   = sources[1].toString();

					return source.indexOf(part);
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_INDEX_OF;
			}
		});
		functions.put("contains", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					final String source = sources[0].toString();
					final String part   = sources[1].toString();

					return source.contains(part);
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_CONTAINS;
			}
		});
		functions.put("substring", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

					final String source    = sources[0].toString();
					final int sourceLength = source.length();
					final int start        = parseInt(sources[1]);
					final int length       = parseInt(sources[2]);
					final int end          = start + length;

					if (start >= 0 && start < sourceLength && end >= 0 && end < sourceLength && start <= end) {

						return source.substring(start, end);
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_SUBSTRING;
			}
		});
		functions.put("length", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					return sources[0].toString().length();
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_SUBSTRING;
			}
		});
		functions.put("replace", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final String template = sources[0].toString();
					AbstractNode node     = null;

					if (sources[1] instanceof AbstractNode) {
						node = (AbstractNode)sources[1];
					}

					if (sources[1] instanceof List) {

						final List list = (List)sources[1];
						if (list.size() == 1 && list.get(0) instanceof AbstractNode) {

							node = (AbstractNode)list.get(0);
						}
					}

					if (node != null) {

						// recursive replacement call, be careful here
						return node.replaceVariables(entity.getSecurityContext(), ctx, template);
					}

					return "";
				}

				return usage();

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_REPLACE;
			}
		});
		functions.put("clean", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_CLEAN;
			}

		});
		functions.put("urlencode", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? encodeURL(sources[0].toString())
					: "";

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_URLENCODE;
			}

		});
		functions.put("if", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources[0] == null || sources.length < 3) {

					return "";
				}

				if ("true".equals(sources[0]) || Boolean.TRUE.equals(sources[0])) {

					return sources[1];

				} else {

					return sources[2];
				}

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_IF;
			}

		});
		functions.put("do", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources[0] == null || sources.length < 3) {

					return "";
				}

				final AbstractNode node = (AbstractNode)entity;

				if ("true".equals(sources[0]) || Boolean.TRUE.equals(sources[0])) {

					node.extractFunctions(entity.getSecurityContext(), ctx, sources[1].toString());

				} else {

					node.extractFunctions(entity.getSecurityContext(), ctx, sources[2].toString());
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_DO;
			}

		});
		functions.put("empty", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources[0] == null || StringUtils.isEmpty(sources[0].toString())) {

					return true;

				} else {
					return false;
				}

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_EMPTY;
			}

		});
		functions.put("equal", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				logger.log(Level.FINE, "Length: {0}", sources.length);

				if (sources.length < 2) {

					return true;
				}

				logger.log(Level.FINE, "Comparing {0} to {1}", new java.lang.Object[]{sources[0], sources[1]});

				if (sources[0] == null && sources[1] == null) {
					return true;
				}

				if (sources[0] == null || sources[1] == null) {
					return false;
				}

				return valueEquals(sources[0], sources[1]);
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_EQUAL;
			}

		});
		functions.put("add", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_ADD;
			}

		});
		functions.put("double_sum", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				double result = 0.0;

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof Collection) {

						for (final Number num : (Collection<Number>)sources[0]) {

							result += num.doubleValue();
						}
					}
				}

				return result;

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_DOUBLE_SUM;
			}

		});
		functions.put("int_sum", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				int result = 0;

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof Collection) {

						for (final Number num : (Collection<Number>)sources[0]) {

							result += num.intValue();
						}
					}
				}

				return result;

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_INT_SUM;
			}

		});
		functions.put("extract", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					// no property key given, maybe we should extract a list of lists?
					if (sources[0] instanceof Collection) {

						final List extraction = new LinkedList();

						for (final Object obj : (Collection)sources[0]) {

							if (obj instanceof Collection) {

								extraction.addAll((Collection)obj);
							}
						}

						return extraction;
					}



				} else if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof Collection && sources[1] instanceof String) {

						final ConfigurationProvider config = StructrApp.getConfiguration();
						final List extraction              = new LinkedList();
						final String keyName               = (String)sources[1];

						for (final Object obj : (Collection)sources[0]) {

							if (obj instanceof GraphObject) {

								final PropertyKey key = config.getPropertyKeyForJSONName(obj.getClass(), keyName);
								final Object value = ((GraphObject)obj).getProperty(key);
								if (value != null) {

									extraction.add(value);
								}
							}
						}

						return extraction;
					}
				}

				return null;

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_EXTRACT;
			}

		});
		functions.put("merge", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				final List list = new ArrayList();
				for (final Object source : sources) {

					if (source instanceof Collection) {

						// filter null objects
						for (Object obj : (Collection)source) {
							if (obj != null) {

								list.add(obj);
							}
						}

					} else if (source != null) {

						list.add(source);
					}
				}

				return list;
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_MERGE;
			}

		});
		functions.put("sort", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof List && sources[1] instanceof String) {

						final List list         = (List)sources[0];
						final String sortKey    = sources[1].toString();
						final Iterator iterator = list.iterator();

						if (iterator.hasNext()) {

							final Object firstElement = iterator.next();
							if (firstElement instanceof GraphObject) {

								final Class type          = firstElement.getClass();
								final PropertyKey key     = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sortKey);
								final boolean descending  = sources.length == 3 && sources[2] != null && "true".equals(sources[2].toString());

								if (key != null) {

									List<GraphObject> sortCollection = (List<GraphObject>)list;
									Collections.sort(sortCollection, new GraphObjectComparator(key, descending));
								}
							}

						}
					}
				}

				return sources[0];
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_SORT;
			}

		});
		functions.put("lt", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = AbstractNode.getDoubleForComparison(sources[0]);
					double value2 = AbstractNode.getDoubleForComparison(sources[1]);

					return value1 < value2;
				}

				return result;

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_LT;
			}
		});
		functions.put("gt", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = AbstractNode.getDoubleForComparison(sources[0]);
					double value2 = AbstractNode.getDoubleForComparison(sources[1]);

					return value1 > value2;
				}

				return result;

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_GT;
			}
		});
		functions.put("lte", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = AbstractNode.getDoubleForComparison(sources[0]);
					double value2 = AbstractNode.getDoubleForComparison(sources[1]);

					return value1 <= value2;
				}

				return result;

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_LTE;
			}
		});
		functions.put("gte", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = AbstractNode.getDoubleForComparison(sources[0]);
					double value2 = AbstractNode.getDoubleForComparison(sources[1]);

					return value1 >= value2;
				}

				return result;

			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_GTE;
			}
		});
		functions.put("subt", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_SUBT;
			}
		});
		functions.put("mult", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_MULT;
			}
		});
		functions.put("quot", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					try {

						return Double.parseDouble(sources[0].toString()) / Double.parseDouble(sources[1].toString());

					} catch (Throwable t) {

						return t.getMessage();

					}

				} else {

					if (sources != null) {

						if (sources.length > 0 && sources[0] != null) {
							return Double.valueOf(sources[0].toString());
						}

						return "";
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_QUOT;
			}
		});
		functions.put("round", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					try {

						Double f1 = Double.parseDouble(sources[0].toString());
						double f2 = Math.pow(10, (Double.parseDouble(sources[1].toString())));
						long r = Math.round(f1 * f2);

						return (double) r / f2;

					} catch (Throwable t) {

						return t.getMessage();

					}

				} else {

					return "";
				}
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_ROUND;
			}
		});
		functions.put("max", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_MAX;
			}
		});
		functions.put("min", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_MIN;
			}
		});
		functions.put("config", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final String configKey    = sources[0].toString();
					final String defaultValue = sources.length >= 2 ? sources[1].toString() : "";

					return StructrApp.getConfigurationValue(configKey, defaultValue);
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_CONFIG;
			}
		});
		functions.put("date_format", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 2) {
					return ERROR_MESSAGE_DATE_FORMAT;
				}

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					Date date = null;

					if (sources[0] instanceof Date) {

						date = (Date)sources[0];

					} else {

						try {

							// parse with format from IS
							date = new SimpleDateFormat(ISO8601DateProperty.PATTERN).parse(sources[0].toString());

						} catch (ParseException ex) {
							ex.printStackTrace();
						}

					}

					// format with given pattern
					return new SimpleDateFormat(sources[1].toString()).format(date);
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_DATE_FORMAT;
			}
		});
		functions.put("parse_date", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 2) {
					return ERROR_MESSAGE_PARSE_DATE;
				}

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					String dateString = sources[0].toString();

					if (StringUtils.isBlank(dateString)) {
						return "";
					}

					String pattern = sources[1].toString();

					try {
						// parse with format from IS
						return new SimpleDateFormat(pattern).parse(dateString);

					} catch (ParseException ex) {
						logger.log(Level.WARNING, "Could not parse date " + dateString + " and format it to pattern " + pattern, ex);
					}

				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_PARSE_DATE;
			}
		});
		functions.put("number_format", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 3) {
					return ERROR_MESSAGE_NUMBER_FORMAT;
				}

				if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					try {

						Double val = Double.parseDouble(sources[0].toString());
						String langCode = sources[1].toString();
						String pattern = sources[2].toString();

						return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.forLanguageTag(langCode))).format(val);

					} catch (Throwable t) { 	}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_NUMBER_FORMAT;
			}
		});
		functions.put("template", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 3) {
					return ERROR_MESSAGE_TEMPLATE;
				}

				if (arrayHasLengthAndAllElementsNotNull(sources, 3) && sources[2] instanceof AbstractNode) {

					final App app                       = StructrApp.getInstance(entity.getSecurityContext());
					final String name                   = sources[0].toString();
					final String locale                 = sources[1].toString();
					final MailTemplate template         = app.nodeQuery(MailTemplate.class).andName(name).and(MailTemplate.locale, locale).getFirst();
					final AbstractNode templateInstance = (AbstractNode)sources[2];

					if (template != null) {

						final String text = template.getProperty(MailTemplate.text);
						if (text != null) {

							// recursive replacement call, be careful here
							return templateInstance.replaceVariables(entity.getSecurityContext(), ctx, text);
						}
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_TEMPLATE;
			}
		});
		functions.put("not", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					return !("true".equals(sources[0].toString()) || Boolean.TRUE.equals(sources[0]));

				}

				return true;
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_NOT;
			}

		});
		functions.put("and", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				boolean result = true;

				if (sources != null) {

					if (sources.length < 2) {
						return usage();
					}

					for (Object i : sources) {

						if (i != null) {

							try {

								result &= "true".equals(i.toString()) || Boolean.TRUE.equals(i);

							} catch (Throwable t) {

								return t.getMessage();

							}

						} else {

							// null is false
							return false;
						}
					}

				}

				return result;
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_AND;
			}

		});
		functions.put("or", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				boolean result = false;

				if (sources != null) {

					if (sources.length < 2) {
						return usage();
					}

					for (Object i : sources) {

						if (i != null) {

							try {

								result |= "true".equals(i.toString()) || Boolean.TRUE.equals(i);

							} catch (Throwable t) {

								return t.getMessage();

							}

						} else {

							// null is false
							result |= false;
						}
					}

				}

				return result;
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_OR;
			}
		});
		functions.put("get", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					NodeInterface node = null;

					if (sources[0] instanceof NodeInterface) {
						node = (NodeInterface)sources[0];
					}

					if (sources[0] instanceof List) {

						final List list = (List)sources[0];
						if (list.size() == 1 && list.get(0) instanceof NodeInterface) {

							node = (NodeInterface)list.get(0);
						}
					}

					if (node != null) {

						final String keyName     = sources[1].toString();
						final PropertyKey key    = StructrApp.getConfiguration().getPropertyKeyForJSONName(node.getClass(), keyName);

						if (key != null) {
							return node.getProperty(key);
						}

						return "";

					} else {

						return ERROR_MESSAGE_GET_ENTITY;
					}
				}

				return usage();
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_GET;
			}
		});
		functions.put("first", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof List && !((List)sources[0]).isEmpty()) {
					return ((List)sources[0]).get(0);
				}

				return null;
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_FIRST;
			}
		});
		functions.put("last", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) &&  sources[0] instanceof List && !((List)sources[0]).isEmpty()) {

					final List list = (List)sources[0];
					return list.get(list.size() - 1);
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_LAST;
			}
		});
		functions.put("nth", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof List && !((List)sources[0]).isEmpty()) {

					final List list = (List)sources[0];
					final int pos   = Double.valueOf(sources[1].toString()).intValue();
					final int size  = list.size();

					return list.get(Math.min(Math.max(0, pos), size));
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_NTH;
			}
		});
		functions.put("eval", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				for (Object i : sources) {

					if (i != null && i instanceof String) {

						final AbstractNode node = (AbstractNode)entity;
						node.extractFunctions(entity.getSecurityContext(), ctx, i.toString());
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_EVAL;
			}
		});
		functions.put("merge_properties", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof GraphObject && sources[1] instanceof GraphObject) {

					final ConfigurationProvider config = StructrApp.getConfiguration();
					final Set<PropertyKey> mergeKeys   = new LinkedHashSet<>();
					final GraphObject source           = (GraphObject)sources[0];
					final GraphObject target           = (GraphObject)sources[1];
					final int paramCount               = sources.length;

					for (int i=2; i<paramCount; i++) {

						final String keyName     = sources[i].toString();
						final PropertyKey key    = config.getPropertyKeyForJSONName(target.getClass(), keyName);

						mergeKeys.add(key);
					}

					for (final PropertyKey key : mergeKeys) {

						final Object sourceValue = source.getProperty(key);
						final Object targetValue = target.getProperty(key);

						if (targetValue == null && sourceValue != null) {
							target.setProperty(key, sourceValue);
						}

					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_MERGE_PROPERTIES;
			}
		});
		functions.put("each", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof List) {

					final List list         = (List)sources[0];
					final String expr       = sources[1].toString();
					final AbstractNode node = (AbstractNode)entity;

					for (final Object obj : list) {

						node.extractFunctions(entity.getSecurityContext(), new ActionContext(entity, obj), expr);
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_EACH;
			}
		});

		// ----- BEGIN functions with side effects -----
		functions.put("print", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					for (Object i : sources) {

						System.out.print(i);
					}

					System.out.println();
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_PRINT;
			}
		});
		functions.put("read", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_READ;
			}
		});
		functions.put("write", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_WRITE;
			}
		});
		functions.put("append", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_APPEND;
			}
		});
		functions.put("xml", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_XML;
			}
		});
		functions.put("xpath", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_XPATH;
			}
		});
		functions.put("set", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_SET;
			}
		});
		functions.put("send_plaintext_mail", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_SEND_PLAINTEXT_MAIL;
			}
		});
		functions.put("send_html_mail", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 6)) {

					final String from        = sources[0].toString();
					final String fromName    = sources[1].toString();
					final String to          = sources[2].toString();
					final String toName      = sources[3].toString();
					final String subject     = sources[4].toString();
					final String htmlContent = sources[5].toString();
					String textContent       = "";

					if (sources.length == 7) {
						textContent = sources[6].toString();
					}

					try {
						MailHelper.sendHtmlMail(from, fromName, to, toName, null, null, from, subject, htmlContent, textContent);

					} catch (EmailException eex) {
						eex.printStackTrace();
					}
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_SEND_HTML_MAIL;
			}
		});
		functions.put("geocode", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_GEOCODE;
			}
		});
		functions.put("find", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_FIND;
			}
		});
		functions.put("create", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_CREATE;
			}
		});
		functions.put("delete", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final NodeInterface entity, final Object[] sources) throws FrameworkException {

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

			@Override
			public String usage() {
				return ERROR_MESSAGE_DELETE;
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

					if (extractedValue == null) {
						extractedValue = "";
					}

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

		} else if (rawValue instanceof Boolean) {

			value = Boolean.toString((Boolean) rawValue);

		} else {

			value = rawValue.toString();

		}

		// return literal null
		if (NULL_STRING.equals(value)) {
			return null;
		}

		return value;

	}

	public Object extractFunctions(SecurityContext securityContext, ActionContext actionContext, String source) throws FrameworkException {

		if ("null".equals(source)) {
			return NULL_STRING;
		}

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

				// return usage string if no parameter is present
				if (parameter == null || parameter.isEmpty()) {
					return function.usage();
				}

				if (parameter.contains(",")) {

					final String[] parameters = split(parameter);

					// collect results from comma-separated function parameter
					for (int i = 0; i < parameters.length; i++) {

						addAll(results, extractFunctions(securityContext, actionContext, StringUtils.strip(parameters[i])));
					}

					return function.apply(actionContext, this, results.toArray());

				} else {

					addAll(results, extractFunctions(securityContext, actionContext, StringUtils.strip(parameter)));

					return function.apply(actionContext, this, results.toArray());

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

	protected static Integer parseInt(final Object source) {

		if (source instanceof Integer) {

			return ((Integer)source);
		}

		if (source instanceof Number) {

			return ((Number)source).intValue();
		}

		if (source instanceof String) {

			return Integer.parseInt((String)source);
		}

		return null;
	}

	protected static boolean valueEquals(final Object obj1, final Object obj2) {

		if (obj1 instanceof Number && obj2 instanceof Number) {

			return ((Number)obj1).doubleValue() == ((Number)obj2).doubleValue();
		}

		return obj1.equals(obj2);
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

	private static double getDoubleForComparison(final Object obj) {

		if (obj instanceof Date) {

			return ((Date)obj).getTime();

		} else if (obj instanceof Number) {

			return ((Number)obj).doubleValue();

		} else {

			try {
				return Double.valueOf(obj.toString());

			} catch (Throwable t) {

				t.printStackTrace();
			}
		}

		return 0.0;
	}
}


