/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.parser;

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
import java.io.StreamTokenizer;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.mail.EmailException;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.structr.common.GraphObjectComparator;
import org.structr.common.MailHelper;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.Function;
import org.structr.schema.parser.DatePropertyParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Christian Morgner
 */
public class Functions {

	private static final Logger logger = Logger.getLogger(Functions.class.getName());
	public static final Map<String, Function<Object, Object>> functions = new LinkedHashMap<>();

	public static final String NULL_STRING = "___NULL___";

	public static final String ERROR_MESSAGE_MD5 = "Usage: ${md5(string)}. Example: ${md5(this.email)}";
	public static final String ERROR_MESSAGE_ERROR = "Usage: ${error(...)}. Example: ${error(\"base\", \"must_equal\", int(5))}";
	public static final String ERROR_MESSAGE_UPPER = "Usage: ${upper(string)}. Example: ${upper(this.nickName)}";
	public static final String ERROR_MESSAGE_LOWER = "Usage: ${lower(string)}. Example: ${lower(this.email)}";
	public static final String ERROR_MESSAGE_JOIN = "Usage: ${join(collection, separator)}. Example: ${join(this.names, \",\")}";
	public static final String ERROR_MESSAGE_CONCAT = "Usage: ${concat(values...)}. Example: ${concat(this.firstName, this.lastName)}";
	public static final String ERROR_MESSAGE_SPLIT = "Usage: ${split(value)}. Example: ${split(this.commaSeparatedItems)}";
	public static final String ERROR_MESSAGE_ABBR = "Usage: ${abbr(longString, maxLength)}. Example: ${abbr(this.title, 20)}";
	public static final String ERROR_MESSAGE_CAPITALIZE = "Usage: ${capitalize(string)}. Example: ${capitalize(this.nickName)}";
	public static final String ERROR_MESSAGE_TITLEIZE = "Usage: ${titleize(string, separator}. (Default separator is \" \") Example: ${titleize(this.lowerCamelCaseString, \"_\")}";
	public static final String ERROR_MESSAGE_NUM = "Usage: ${num(string)}. Example: ${num(this.numericalStringValue)}";
	public static final String ERROR_MESSAGE_INT = "Usage: ${int(string)}. Example: ${int(this.numericalStringValue)}";
	public static final String ERROR_MESSAGE_RANDOM = "Usage: ${random(num)}. Example: ${set(this, \"password\", random(8))}";
	public static final String ERROR_MESSAGE_RINT = "Usage: ${rint(range)}. Example: ${rint(1000)}";
	public static final String ERROR_MESSAGE_INDEX_OF = "Usage: ${index_of(string, word)}. Example: ${index_of(this.name, \"the\")}";
	public static final String ERROR_MESSAGE_CONTAINS = "Usage: ${contains(string, word)}. Example: ${contains(this.name, \"the\")}";
	public static final String ERROR_MESSAGE_SUBSTRING = "Usage: ${substring(string, start, length)}. Example: ${substring(this.name, 19, 3)}";
	public static final String ERROR_MESSAGE_LENGTH = "Usage: ${length(string)}. Example: ${length(this.name)}";
	public static final String ERROR_MESSAGE_REPLACE = "Usage: ${replace(template, source)}. Example: ${replace(\"${this.id}\", this)}";
	public static final String ERROR_MESSAGE_CLEAN = "Usage: ${clean(string)}. Example: ${clean(this.stringWithNonWordChars)}";
	public static final String ERROR_MESSAGE_URLENCODE = "Usage: ${urlencode(string)}. Example: ${urlencode(this.email)}";
	public static final String ERROR_MESSAGE_ESCAPE_JS = "Usage: ${escape_javascript(string)}. Example: ${escape_javascript(this.name)}";
	public static final String ERROR_MESSAGE_ESCAPE_JSON = "Usage: ${escape_json(string)}. Example: ${escape_json(this.name)}";
	public static final String ERROR_MESSAGE_IF = "Usage: ${if(condition, trueValue, falseValue)}. Example: ${if(empty(this.name), this.nickName, this.name)}";
	public static final String ERROR_MESSAGE_EMPTY = "Usage: ${empty(string)}. Example: ${if(empty(possibleEmptyString), \"empty\", \"non-empty\")}";
	public static final String ERROR_MESSAGE_EQUAL = "Usage: ${equal(value1, value2)}. Example: ${equal(this.children.size, 0)}";
	public static final String ERROR_MESSAGE_ADD = "Usage: ${add(values...)}. Example: ${add(1, 2, 3, this.children.size)}";
	public static final String ERROR_MESSAGE_INT_SUM = "Usage: ${int_sum(list)}. Example: ${int_sum(extract(this.children, \"number\"))}";
	public static final String ERROR_MESSAGE_DOUBLE_SUM = "Usage: ${double_sum(list)}. Example: ${double_sum(extract(this.children, \"amount\"))}";
	public static final String ERROR_MESSAGE_IS_COLLECTION = "Usage: ${is_collection(value)}. Example: ${is_collection(this)}";
	public static final String ERROR_MESSAGE_IS_ENTITY = "Usage: ${is_entity(value)}. Example: ${is_entity(this)}";
	public static final String ERROR_MESSAGE_EXTRACT = "Usage: ${extract(list, propertyName)}. Example: ${extract(this.children, \"amount\")}";
	public static final String ERROR_MESSAGE_FILTER = "Usage: ${filter(list, expression)}. Example: ${filter(this.children, gt(size(data.children), 0))}";
	public static final String ERROR_MESSAGE_MERGE = "Usage: ${merge(list1, list2, list3, ...)}. Example: ${merge(this.children, this.siblings)}";
	public static final String ERROR_MESSAGE_COMPLEMENT = "Usage: ${complement(list1, list2, list3, ...)}. (The resulting list contains no duplicates) Example: ${complement(allUsers, me)} => List of all users except myself";
	public static final String ERROR_MESSAGE_UNWIND = "Usage: ${unwind(list1, ...)}. Example: ${unwind(this.children)}";
	public static final String ERROR_MESSAGE_SORT = "Usage: ${sort(list1, key [, true])}. Example: ${sort(this.children, \"name\")}";
	public static final String ERROR_MESSAGE_LT = "Usage: ${lt(value1, value2)}. Example: ${if(lt(this.children, 2), \"Less than two\", \"Equal to or more than two\")}";
	public static final String ERROR_MESSAGE_GT = "Usage: ${gt(value1, value2)}. Example: ${if(gt(this.children, 2), \"More than two\", \"Equal to or less than two\")}";
	public static final String ERROR_MESSAGE_LTE = "Usage: ${lte(value1, value2)}. Example: ${if(lte(this.children, 2), \"Equal to or less than two\", \"More than two\")}";
	public static final String ERROR_MESSAGE_GTE = "Usage: ${gte(value1, value2)}. Example: ${if(gte(this.children, 2), \"Equal to or more than two\", \"Less than two\")}";
	public static final String ERROR_MESSAGE_SUBT = "Usage: ${subt(value1, value)}. Example: ${subt(5, 2)}";
	public static final String ERROR_MESSAGE_MULT = "Usage: ${mult(value1, value)}. Example: ${mult(5, 2)}";
	public static final String ERROR_MESSAGE_QUOT = "Usage: ${quot(value1, value)}. Example: ${quot(5, 2)}";
	public static final String ERROR_MESSAGE_ROUND = "Usage: ${round(value1 [, decimalPlaces])}. Example: ${round(2.345678, 2)}";
	public static final String ERROR_MESSAGE_MAX = "Usage: ${max(value1, value2)}. Example: ${max(this.children, 10)}";
	public static final String ERROR_MESSAGE_MIN = "Usage: ${min(value1, value2)}. Example: ${min(this.children, 5)}";
	public static final String ERROR_MESSAGE_CONFIG = "Usage: ${config(keyFromStructrConf)}. Example: ${config(\"base.path\")}";
	public static final String ERROR_MESSAGE_CONFIG_JS = "Usage: ${{Structr.config(keyFromStructrConf)}}. Example: ${{Structr.config(\"base.path\")}}";
	public static final String ERROR_MESSAGE_DATE_FORMAT = "Usage: ${date_format(value, pattern)}. Example: ${date_format(this.creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";
	public static final String ERROR_MESSAGE_DATE_FORMAT_JS = "Usage: ${{Structr.date_format(value, pattern)}}. Example: ${{Structr.date_format(Structr.get('this').creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";
	public static final String ERROR_MESSAGE_PARSE_DATE = "Usage: ${parse_date(value, pattern)}. Example: ${parse_format(\"2014-01-01\", \"yyyy-MM-dd\")}";
	public static final String ERROR_MESSAGE_PARSE_DATE_JS = "Usage: ${{Structr.parse_date(value, pattern)}}. Example: ${{Structr.parse_format(\"2014-01-01\", \"yyyy-MM-dd\")}}";
	public static final String ERROR_MESSAGE_NUMBER_FORMAT = "Usage: ${number_format(value, ISO639LangCode, pattern)}. Example: ${number_format(12345.6789, 'en', '#,##0.00')}";
	public static final String ERROR_MESSAGE_NUMBER_FORMAT_JS = "Usage: ${{Structr.number_format(value, ISO639LangCode, pattern)}}. Example: ${{Structr.number_format(12345.6789, 'en', '#,##0.00')}}";
	public static final String ERROR_MESSAGE_TEMPLATE = "Usage: ${{Structr.template(name, locale, source)}}. Example: ${{Structr.template(\"TEXT_TEMPLATE_1\", \"en_EN\", Structr.get('this'))}}";
	public static final String ERROR_MESSAGE_TEMPLATE_JS = "Usage: ${template(name, locale, source)}. Example: ${template(\"TEXT_TEMPLATE_1\", \"en_EN\", this)}";
	public static final String ERROR_MESSAGE_NOT = "Usage: ${not(bool1, bool2)}. Example: ${not(\"true\", \"true\")}";
	public static final String ERROR_MESSAGE_AND = "Usage: ${and(bool1, bool2)}. Example: ${and(\"true\", \"true\")}";
	public static final String ERROR_MESSAGE_OR = "Usage: ${or(bool1, bool2)}. Example: ${or(\"true\", \"true\")}";
	public static final String ERROR_MESSAGE_GET = "Usage: ${get(entity, propertyKey)}. Example: ${get(this, \"children\")}";
	public static final String ERROR_MESSAGE_GET_ENTITY = "Cannot evaluate first argument to entity, must be entity or single element list of entities.";
	public static final String ERROR_MESSAGE_SIZE = "Usage: ${size(collection)}. Example: ${size(this.children)}";
	public static final String ERROR_MESSAGE_FIRST = "Usage: ${first(collection)}. Example: ${first(this.children)}";
	public static final String ERROR_MESSAGE_LAST = "Usage: ${last(collection)}. Example: ${last(this.children)}";
	public static final String ERROR_MESSAGE_NTH = "Usage: ${nth(collection)}. Example: ${nth(this.children, 2)}";
	public static final String ERROR_MESSAGE_GET_COUNTER = "Usage: ${get_counter(level)}. Example: ${get_counter(1)}";
	public static final String ERROR_MESSAGE_INC_COUNTER = "Usage: ${inc_counter(level, [resetLowerLevels])}. Example: ${inc_counter(1, true)}";
	public static final String ERROR_MESSAGE_RESET_COUNTER = "Usage: ${reset_counter(level)}. Example: ${reset_counter(1)}";
	public static final String ERROR_MESSAGE_MERGE_PROPERTIES = "Usage: ${merge_properties(source, target , mergeKeys...)}. Example: ${merge_properties(this, parent, \"eMail\")}";
	public static final String ERROR_MESSAGE_KEYS = "Usage: ${keys(entity, viewName)}. Example: ${keys(this, \"ui\")}";
	public static final String ERROR_MESSAGE_EACH = "Usage: ${each(collection, expression)}. Example: ${each(this.children, \"set(this, \"email\", lower(get(this.email))))\")}";
	public static final String ERROR_MESSAGE_STORE = "Usage: ${store(key, value)}. Example: ${store('tmpUser', this.owner)}";
	public static final String ERROR_MESSAGE_STORE_JS = "Usage: ${{Structr.store(key, value)}}. Example: ${{Structr.store('tmpUser', Structr.get('this').owner)}}";
	public static final String ERROR_MESSAGE_RETRIEVE = "Usage: ${retrieve(key)}. Example: ${retrieve('tmpUser')}";
	public static final String ERROR_MESSAGE_RETRIEVE_JS = "Usage: ${{Structr.retrieve(key)}}. Example: ${{retrieve('tmpUser')}}";
	public static final String ERROR_MESSAGE_PRINT = "Usage: ${print(objects...)}. Example: ${print(this.name, \"test\")}";
	public static final String ERROR_MESSAGE_PRINT_JS = "Usage: ${{Structr.print(objects...)}}. Example: ${{Structr.print(Structr.get('this').name, \"test\")}}";
	public static final String ERROR_MESSAGE_READ = "Usage: ${read(filename)}. Example: ${read(\"text.xml\")}";
	public static final String ERROR_MESSAGE_WRITE = "Usage: ${write(filename, value)}. Example: ${write(\"text.txt\", this.name)}";
	public static final String ERROR_MESSAGE_APPEND = "Usage: ${append(filename, value)}. Example: ${append(\"test.txt\", this.name)}";
	public static final String ERROR_MESSAGE_XML = "Usage: ${xml(xmlSource)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\")}";
	public static final String ERROR_MESSAGE_XPATH = "Usage: ${xpath(xmlDocument, expression)}. Example: ${xpath(xml(this.xmlSource), \"/test/testValue\")}";
	public static final String ERROR_MESSAGE_SET = "Usage: ${set(entity, propertyKey, value)}. Example: ${set(this, \"email\", lower(this.email))}";
	public static final String ERROR_MESSAGE_SET_PRIVILEGED = "Usage: ${set_privileged(entity, propertyKey, value)}. Example: ${set_privileged(this, \"email\", lower(this.email))}";
	public static final String ERROR_MESSAGE_SET_PRIVILEGED_JS = "Usage: ${setPrvileged(entity, propertyKey, value)}. Example: ${Structr.setPrivileged(this, \"email\", lower(this.email))}";
	public static final String ERROR_MESSAGE_SEND_PLAINTEXT_MAIL = "Usage: ${send_plaintext_mail(fromAddress, fromName, toAddress, toName, subject, content)}.";
	public static final String ERROR_MESSAGE_SEND_HTML_MAIL = "Usage: ${send_html_mail(fromAddress, fromName, toAddress, toName, subject, content)}.";
	public static final String ERROR_MESSAGE_GEOCODE = "Usage: ${geocode(street, city, country)}. Example: ${set(this, geocode(this.street, this.city, this.country))}";
	public static final String ERROR_MESSAGE_FIND = "Usage: ${find(type, key, value)}. Example: ${find(\"User\", \"email\", \"tester@test.com\"}";
	public static final String ERROR_MESSAGE_SEARCH = "Usage: ${search(type, key, value)}. Example: ${search(\"User\", \"name\", \"abc\"}";
	public static final String ERROR_MESSAGE_CREATE = "Usage: ${create(type, key, value)}. Example: ${create(\"Feedback\", \"text\", this.text)}";
	public static final String ERROR_MESSAGE_DELETE = "Usage: ${delete(entity)}. Example: ${delete(this)}";
	public static final String ERROR_MESSAGE_CACHE = "Usage: ${cache(key, timeout, valueExpression)}. Example: ${cache('value', 60, GET('http://rate-limited-URL.com'))}";
	public static final String ERROR_MESSAGE_GRANT = "Usage: ${grant(principal, node, permissions)}. Example: ${grant(me, this, 'read, write, delete'))}";
	public static final String ERROR_MESSAGE_GRANT_JS = "Usage: ${Structr.grant(principal, node, permissions)}. Example: ${Structr.grant(Structr.get('me'), Structr.this, 'read, write, delete'))}";
	public static final String ERROR_MESSAGE_REVOKE = "Usage: ${revoke(principal, node, permissions)}. Example: ${revoke(me, this, 'write, delete'))}";
	public static final String ERROR_MESSAGE_REVOKE_JS = "Usage: ${Structr.revoke(principal, node, permissions)}. Example: ${Structr.revoke(Structr.('me'), Structr.this, 'write, delete'))}";
	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE = "Usage: ${unlock_readonly_properties_once(node)}. Example ${unlock_readonly_properties_once, this}";
	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE_JS = "Usage: ${Structr.unlock_readonly_properties_once(node)}. Example ${Structr.unlock_readonly_properties_once, Structr.get('this')}";
	public static final String ERROR_MESSAGE_CALL = "Usage: ${call(key [, payloads...]}. Example ${call('myEvent')}";
	public static final String ERROR_MESSAGE_CALL_JS = "Usage: ${Structr.call(key [, payloads...]}. Example ${Structr.call('myEvent')}";
	public static final String ERROR_MESSAGE_IS_LOCALE = "Usage: ${is_locale(locales...)}";
	public static final String ERROR_MESSAGE_IS_LOCALE_JS = "Usage: ${Structr.isLocale(locales...}. Example ${Structr.isLocale('de_DE', 'de_AT', 'de_CH')}";
	public static final String ERROR_MESSAGE_CYPHER = "Usage: ${cypher('MATCH (n) RETURN n')}";
	public static final String ERROR_MESSAGE_CYPHER_JS = "Usage: ${Structr.cypher(query)}. Example ${Structr.cypher('MATCH (n) RETURN n')}";

	// Special functions for relationships
	public static final String ERROR_MESSAGE_INCOMING = "Usage: ${incoming(entity [, relType])}. Example: ${incoming(this, 'PARENT_OF')}";
	public static final String ERROR_MESSAGE_INCOMING_JS = "Usage: ${Structr.incoming(entity [, relType])}. Example: ${Structr.incoming(Structr.this, 'PARENT_OF')}";
	public static final String ERROR_MESSAGE_OUTGOING = "Usage: ${outgoing(entity [, relType])}. Example: ${outgoing(this, 'PARENT_OF')}";
	public static final String ERROR_MESSAGE_OUTGOING_JS = "Usage: ${Structr.outgoing(entity [, relType])}. Example: ${outgoing(Structr.this, 'PARENT_OF')}";
	public static final String ERROR_MESSAGE_HAS_RELATIONSHIP = "Usage: ${has_relationship(entity1, entity2 [, relType])}. Example: ${has_relationship(me, user, 'FOLLOWS')} (ignores direction of the relationship)";
	public static final String ERROR_MESSAGE_HAS_RELATIONSHIP_JS = "Usage: ${Structr.has_relationship(entity1, entity2 [, relType])}. Example: ${Structr.has_relationship(Structr.get('me'), user, 'FOLLOWS')} (ignores direction of the relationship)";
	public static final String ERROR_MESSAGE_HAS_OUTGOING_RELATIONSHIP = "Usage: ${has_outgoing_relationship(from, to [, relType])}. Example: ${has_outgoing_relationship(me, user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_HAS_OUTGOING_RELATIONSHIP_JS = "Usage: ${Structr.has_outgoing_relationship(from, to [, relType])}. Example: ${Structr.has_outgoing_relationship(Structr.get('me'), user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_HAS_INCOMING_RELATIONSHIP = "Usage: ${has_incoming_relationship(from, to [, relType])}. Example: ${has_incoming_relationship(me, user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_HAS_INCOMING_RELATIONSHIP_JS = "Usage: ${Structr.has_incoming_relationship(from, to [, relType])}. Example: ${Structr.has_incoming_relationship(Structr.get('me'), user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_GET_RELATIONSHIPS = "Usage: ${get_relationships(entity1, entity2 [, relType])}. Example: ${get_relationships(me, user, 'FOLLOWS')}  (ignores direction of the relationship)";
	public static final String ERROR_MESSAGE_GET_RELATIONSHIPS_JS = "Usage: ${Structr.get_relationships(entity1, entity2 [, relType])}. Example: ${Structr.get_relationships(Structr.get('me'), user, 'FOLLOWS')}  (ignores direction of the relationship)";
	public static final String ERROR_MESSAGE_GET_OUTGOING_RELATIONSHIPS = "Usage: ${get_outgoing_relationships(from, to [, relType])}. Example: ${get_outgoing_relationships(me, user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_GET_OUTGOING_RELATIONSHIPS_JS = "Usage: ${Structr.get_outgoing_relationships(from, to [, relType])}. Example: ${Structr.get_outgoing_relationships(Structr.get('me'), user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS = "Usage: ${get_incoming_relationships(from, to [, relType])}. Example: ${get_incoming_relationships(me, user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS_JS = "Usage: ${Structr.get_incoming_relationships(from, to [, relType])}. Example: ${Structr.get_incoming_relationships(Structr.get('me'), user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_CREATE_RELATIONSHIP = "Usage: ${create_relationship(from, to, relType)}. Example: ${create_relationship(me, user, 'FOLLOWS')} (Relationshiptype has to exist)";
	public static final String ERROR_MESSAGE_CREATE_RELATIONSHIP_JS = "Usage: ${Structr.create_relationship(from, to, relType)}. Example: ${Structr.create_relationship(Structr.get('me'), user, 'FOLLOWS')} (Relationshiptype has to exist)";

	public static Function<Object, Object> get(final String name) {
		return functions.get(name);
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String expression) throws FrameworkException {

		final String expressionWithoutNewlines = expression.replace('\n', ' ');
		final StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(expressionWithoutNewlines));
		tokenizer.eolIsSignificant(true);
		tokenizer.ordinaryChar('.');
		tokenizer.wordChars('_', '_');
		tokenizer.wordChars('.', '.');
		tokenizer.wordChars('!', '!');

		Expression root = new RootExpression();
		Expression current = root;
		Expression next = null;
		String lastToken = null;
		int token = 0;
		int level = 0;

		while (token != StreamTokenizer.TT_EOF) {

			token = nextToken(tokenizer);

			switch (token) {

				case StreamTokenizer.TT_EOF:
					break;

				case StreamTokenizer.TT_EOL:
					break;

				case StreamTokenizer.TT_NUMBER:
					if (current == null) {
						throw new FrameworkException(422, "Invalid expression: mismatched opening bracket before NUMBER");
					}
					next = new ConstantExpression(tokenizer.nval);
					current.add(next);
					lastToken += "NUMBER";
					break;

				case StreamTokenizer.TT_WORD:
					if (current == null) {
						throw new FrameworkException(422, "Invalid expression: mismatched opening bracket before " + tokenizer.sval);
					}
					next = checkReservedWords(tokenizer.sval);
					current.add(next);
					lastToken = tokenizer.sval;
					break;

				case '(':
					if (((current == null || current instanceof RootExpression) && next == null) || current == next) {

						// an additional bracket without a new function,
						// this can only be an execution group.
						next = new GroupExpression();
						current.add(next);
					}

					current = next;
					lastToken += "(";
					level++;
					break;

				case ')':
					if (current == null) {
						throw new FrameworkException(422, "Invalid expression: mismatched opening bracket before " + lastToken);
					}
					current = current.getParent();
					if (current == null) {
						throw new FrameworkException(422, "Invalid expression: mismatched closing bracket after " + lastToken);
					}
					lastToken += ")";
					level--;
					break;

				case '[':
					// bind directly to the previous expression
					next = new ArrayExpression();
					current.add(next);
					current = next;
					lastToken += "[";
					level++;
					break;

				case ']':

					if (current == null) {
						throw new FrameworkException(422, "Invalid expression: mismatched closing bracket before " + lastToken);
					}
					current = current.getParent();
					if (current == null) {
						throw new FrameworkException(422, "Invalid expression: mismatched closing bracket after " + lastToken);
					}
					lastToken += "]";
					level--;
					break;

				case ';':
					next = null;
					lastToken += ";";
					break;

				case ',':
					next = current;
					lastToken += ",";
					break;

				default:
					if (current == null) {
						throw new FrameworkException(422, "Invalid expression: mismatched opening bracket before " + tokenizer.sval);
					}
					current.add(new ConstantExpression(tokenizer.sval));
					lastToken = tokenizer.sval;

			}
		}

		if (level > 0) {
			throw new FrameworkException(422, "Invalid expression: mismatched closing bracket after " + lastToken);
		}

		return root.evaluate(actionContext, entity);
	}

	private static Expression checkReservedWords(final String word) throws FrameworkException {

		if (word == null) {
			return new NullExpression();
		}

		switch (word) {

			case "cache":
				return new CacheExpression();

			case "true":
				return new ConstantExpression(true);

			case "false":
				return new ConstantExpression(false);

			case "if":
				return new IfExpression();

			case "each":
				return new EachExpression();

			case "filter":
				return new FilterExpression();

			case "data":
				return new ValueExpression("data");

			case "null":
				return new ConstantExpression(NULL_STRING);
		}

		// no match, try functions
		final Function<Object, Object> function = Functions.get(word);
		if (function != null) {

			return new FunctionExpression(word, function);

		} else {

			return new ValueExpression(word);
		}
	}

	public static int nextToken(final StreamTokenizer tokenizer) {

		try {

			return tokenizer.nextToken();

		} catch (IOException ioex) {
		}

		return StreamTokenizer.TT_EOF;
	}

	static {

		functions.put("error", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final Class entityType;
				final String type;

				if (entity != null) {

					entityType = entity.getClass();
					type       = entity.getType();

				} else {

					entityType = GraphObject.class;
					type       = "Base";

				}

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, sources[0].toString());
					ctx.raiseError(type, new ErrorToken(422, key) {

						@Override
						public JsonElement getContent() {
							return new JsonPrimitive(getErrorToken());
						}

						@Override
						public String getErrorToken() {
							return sources[1].toString();
						}
					});

				} else if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

					final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, sources[0].toString());
					ctx.raiseError(type, new SemanticErrorToken(key) {

						@Override
						public JsonElement getContent() {

							JsonObject obj = new JsonObject();

							if (sources[2] instanceof Number) {

								obj.add(getErrorToken(), new JsonPrimitive((Number) sources[2]));

							} else if (sources[2] instanceof Boolean) {

								obj.add(getErrorToken(), new JsonPrimitive((Boolean) sources[2]));

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_ERROR;
			}
		});
		functions.put("md5", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? DigestUtils.md5Hex(sources[0].toString())
					: "";

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_MD5;
			}
		});
		functions.put("upper", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? sources[0].toString().toUpperCase()
					: "";

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_UPPER;
			}

		});
		functions.put("lower", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? sources[0].toString().toLowerCase()
					: "";

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_LOWER;
			}

		});
		functions.put("join", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof Collection) {

						return StringUtils.join((Collection) sources[0], sources[1].toString());
					}

					if (sources[0].getClass().isArray()) {

						return StringUtils.join((Object[]) sources[0], sources[1].toString());
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_JOIN;
			}

		});
		functions.put("concat", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final List list = new ArrayList();
				for (final Object source : sources) {

					// collection can contain nulls..
					if (source != null) {

						if (source instanceof Collection) {

							list.addAll((Collection) source);

						} else if (source.getClass().isArray()) {

							list.addAll(Arrays.asList((Object[]) source));

						} else {

							list.add(source);
						}
					}
				}

				return StringUtils.join(list, "");
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_CONCAT;
			}

		});
		functions.put("split", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final String toSplit = sources[0].toString();
					String splitExpr = "[,;]+";

					if (sources.length >= 2) {
						splitExpr = sources[1].toString();
					}

					return Arrays.asList(toSplit.split(splitExpr));
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SPLIT;
			}

		});
		functions.put("abbr", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_ABBR;
			}

		});
		functions.put("capitalize", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? StringUtils.capitalize(sources[0].toString())
					: "";

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_CAPITALIZE;
			}
		});
		functions.put("titleize", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources[0] == null) {
					return null;
				}

				if (StringUtils.isBlank(sources[0].toString())) {
					return "";
				}

				final String separator;
				if (sources.length < 2) {
					separator = " ";
				} else {
					separator = sources[1].toString();
				}

				String[] in = StringUtils.split(sources[0].toString(), separator);
				String[] out = new String[in.length];
				for (int i = 0; i < in.length; i++) {
					out[i] = StringUtils.capitalize(in[i]);
				}
				return StringUtils.join(out, " ");

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_TITLEIZE;
			}

		});
		functions.put("num", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						return getDoubleOrNull(sources[0]);

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_NUM;
			}
		});
		functions.put("int", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof Number) {
						return ((Number) sources[0]).intValue();
					}

					try {
						return getDoubleOrNull(sources[0]).intValue();

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_INT;
			}
		});
		functions.put("random", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof Number) {

					try {
						return RandomStringUtils.randomAlphanumeric(((Number) sources[0]).intValue());

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_RANDOM;
			}
		});
		functions.put("rint", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof Number) {

					try {
						return new Random(System.currentTimeMillis()).nextInt(((Number) sources[0]).intValue());

					} catch (Throwable t) {
						// ignore
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_RINT;
			}
		});
		functions.put("index_of", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					final String source = sources[0].toString();
					final String part = sources[1].toString();

					return source.indexOf(part);
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_INDEX_OF;
			}
		});
		functions.put("contains", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof String && sources[1] instanceof String) {

						final String source = sources[0].toString();
						final String part = sources[1].toString();

						return source.contains(part);

					} else if (sources[0] instanceof Collection && sources[1] instanceof GraphObject) {

						final Collection collection = (Collection) sources[0];
						final GraphObject obj = (GraphObject) sources[1];

						return collection.contains(obj);

					} else if (sources[0].getClass().isArray()) {

						return ArrayUtils.contains((Object[]) sources[0], sources[1]);
					}
				}

				return false;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_CONTAINS;
			}
		});
		functions.put("substring", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final String source = sources[0].toString();
					final int sourceLength = source.length();
					final int start = parseInt(sources[1]);
					final int length = sources.length >= 3 ? parseInt(sources[2]) : sourceLength - start;
					final int end = start + length;

					if (start >= 0 && start < sourceLength && end >= 0 && end <= sourceLength && start <= end) {

						return source.substring(start, end);
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SUBSTRING;
			}
		});
		functions.put("length", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					return sources[0].toString().length();
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SUBSTRING;
			}
		});
		functions.put("replace", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final String template = sources[0].toString();
					AbstractNode node = null;

					if (sources[1] instanceof AbstractNode) {
						node = (AbstractNode) sources[1];
					}

					if (sources[1] instanceof List) {

						final List list = (List) sources[1];
						if (list.size() == 1 && list.get(0) instanceof AbstractNode) {

							node = (AbstractNode) list.get(0);
						}
					}

					if (node != null) {

						// recursive replacement call, be careful here
						return Scripting.replaceVariables(ctx, node, template);
					}

					return "";
				}

				return usage(ctx.isJavaScriptContext());

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_REPLACE;
			}
		});
		functions.put("clean", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					if (StringUtils.isBlank(sources[0].toString())) {
						return "";
					}

					return cleanString(sources[0]);
				}

				return null;

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_CLEAN;
			}

		});
		functions.put("urlencode", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? encodeURL(sources[0].toString())
					: "";

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_URLENCODE;
			}

		});
		functions.put("escape_javascript", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? StringEscapeUtils.escapeEcmaScript(sources[0].toString())
					: "";

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_ESCAPE_JS;
			}

		});
		functions.put("escape_json", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
					? StringEscapeUtils.escapeJson(sources[0].toString())
					: "";

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_ESCAPE_JSON;
			}

		});
		functions.put("if", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_IF;
			}

		});
		functions.put("empty", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources.length == 0 || sources[0] == null || StringUtils.isEmpty(sources[0].toString())) {

					return true;

				} else {
					return false;
				}

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_EMPTY;
			}

		});
		functions.put("equal", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_EQUAL;
			}

		});
		functions.put("eq", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				return functions.get("equal").apply(ctx, entity, sources);
			}

			@Override
			public String usage(boolean inJavaScriptContext) {

				return functions.get("equal").usage(inJavaScriptContext);
			}

		});
		functions.put("add", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_ADD;
			}

		});
		functions.put("double_sum", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				double result = 0.0;

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof Collection) {

						for (final Number num : (Collection<Number>) sources[0]) {

							result += num.doubleValue();
						}
					}
				}

				return result;

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_DOUBLE_SUM;
			}

		});
		functions.put("int_sum", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				int result = 0;

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof Collection) {

						for (final Number num : (Collection<Number>) sources[0]) {

							result += num.intValue();
						}
					}
				}

				return result;

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_INT_SUM;
			}

		});
		functions.put("is_collection", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {
					return (sources[0] instanceof Collection);
				} else {
					return false;
				}

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_IS_COLLECTION;
			}

		});
		functions.put("is_entity", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {
					return (sources[0] instanceof GraphObject);
				} else {
					return false;
				}

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_IS_ENTITY;
			}

		});
		functions.put("extract", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					// no property key given, maybe we should extract a list of lists?
					if (sources[0] instanceof Collection) {

						final List extraction = new LinkedList();

						for (final Object obj : (Collection) sources[0]) {

							if (obj instanceof Collection) {

								extraction.addAll((Collection) obj);
							}
						}

						return extraction;
					}

				} else if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof Collection && sources[1] instanceof String) {

						final ConfigurationProvider config = StructrApp.getConfiguration();
						final List extraction = new LinkedList();
						final String keyName = (String) sources[1];

						for (final Object obj : (Collection) sources[0]) {

							if (obj instanceof GraphObject) {

								final PropertyKey key = config.getPropertyKeyForJSONName(obj.getClass(), keyName);
								final Object value = ((GraphObject) obj).getProperty(key);
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_EXTRACT;
			}

		});
		functions.put("merge", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final List list = new ArrayList();
				for (final Object source : sources) {

					if (source instanceof Collection) {

						// filter null objects
						for (Object obj : (Collection) source) {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_MERGE;
			}

		});
		functions.put("complement", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final Set sourceSet = new HashSet();

				if (sources[0] instanceof Collection) {

					sourceSet.addAll((Collection) sources[0]);

					for (int cnt = 1; cnt < sources.length; cnt++) {

						final Object source = sources[cnt];

						if (source instanceof Collection) {

							sourceSet.removeAll((Collection) source);

						} else if (source != null) {

							sourceSet.remove(source);
						}
					}

				} else {

					return "Argument 1 for complement must be a Collection";
				}

				return sourceSet;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_COMPLEMENT;
			}

		});
		functions.put("unwind", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final List list = new ArrayList();
				for (final Object source : sources) {

					if (source instanceof Collection) {

						// filter null objects
						for (Object obj : (Collection) source) {
							if (obj != null) {

								if (obj instanceof Collection) {

									for (final Object elem : (Collection) obj) {

										if (elem != null) {

											list.add(elem);
										}
									}

								} else {

									list.add(obj);
								}
							}
						}

					} else if (source != null) {

						list.add(source);
					}
				}

				return list;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_UNWIND;
			}

		});
		functions.put("sort", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof List && sources[1] instanceof String) {

						final List list = (List) sources[0];
						final String sortKey = sources[1].toString();
						final Iterator iterator = list.iterator();

						if (iterator.hasNext()) {

							final Object firstElement = iterator.next();
							if (firstElement instanceof GraphObject) {

								final Class type = firstElement.getClass();
								final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sortKey);
								final boolean descending = sources.length == 3 && sources[2] != null && "true".equals(sources[2].toString());

								if (key != null) {

									List<GraphObject> sortCollection = (List<GraphObject>) list;
									Collections.sort(sortCollection, new GraphObjectComparator(key, descending));

									return sortCollection;
								}
							}

						}
					}
				}

				return sources[0];
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SORT;
			}

		});
		functions.put("lt", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					return lt(sources[0], sources[1]);
				}

				return result;

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_LT;
			}
		});
		functions.put("gt", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					return gt(sources[0], sources[1]);
				}

				return result;

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_GT;
			}
		});
		functions.put("lte", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					return lte(sources[0], sources[1]);
				}

				return result;

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_LTE;
			}
		});
		functions.put("gte", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					return gte(sources[0], sources[1]);

				}

				return result;

			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_GTE;
			}
		});
		functions.put("subt", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SUBT;
			}
		});
		functions.put("mult", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_MULT;
			}
		});
		functions.put("quot", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_QUOT;
			}
		});
		functions.put("round", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_ROUND;
			}
		});
		functions.put("max", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				Object result = "";
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_MAX;
			}
		});
		functions.put("min", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				Object result = "";
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_MIN;
			}
		});
		functions.put("config", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final String configKey = sources[0].toString();
					final String defaultValue = sources.length >= 2 ? sources[1].toString() : "";

					return StructrApp.getConfigurationValue(configKey, defaultValue);
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_CONFIG_JS : ERROR_MESSAGE_CONFIG);
			}
		});
		functions.put("date_format", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 2) {
					return usage(ctx.isJavaScriptContext());
				}

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					Date date = null;

					if (sources[0] instanceof Date) {

						date = (Date) sources[0];

					} else if (sources[0] instanceof Number) {

						date = new Date(((Number) sources[0]).longValue());

					} else {

						try {

							// parse with format from IS
							date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(sources[0].toString());

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
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_DATE_FORMAT_JS : ERROR_MESSAGE_DATE_FORMAT);
			}
		});
		functions.put("parse_date", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 2) {
					return usage(ctx.isJavaScriptContext());
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
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_PARSE_DATE_JS : ERROR_MESSAGE_PARSE_DATE);
			}
		});
		functions.put("number_format", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 3) {
					return usage(ctx.isJavaScriptContext());
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

					} catch (Throwable t) {
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_NUMBER_FORMAT_JS : ERROR_MESSAGE_NUMBER_FORMAT);
			}
		});
		functions.put("template", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources == null || sources != null && sources.length != 3) {
					return usage(ctx.isJavaScriptContext());
				}

				if (arrayHasLengthAndAllElementsNotNull(sources, 3) && sources[2] instanceof AbstractNode) {

					final App app = StructrApp.getInstance(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
					final String name = sources[0].toString();
					final String locale = sources[1].toString();
					final MailTemplate template = app.nodeQuery(MailTemplate.class).andName(name).and(MailTemplate.locale, locale).getFirst();
					final AbstractNode templateInstance = (AbstractNode) sources[2];

					if (template != null) {

						final String text = template.getProperty(MailTemplate.text);
						if (text != null) {

							// recursive replacement call, be careful here
							return Scripting.replaceVariables(ctx, templateInstance, text);
						}
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_TEMPLATE_JS : ERROR_MESSAGE_TEMPLATE);
			}
		});
		functions.put("not", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					return !("true".equals(sources[0].toString()) || Boolean.TRUE.equals(sources[0]));

				}

				return true;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_NOT;
			}

		});
		functions.put("and", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				boolean result = true;

				if (sources != null) {

					if (sources.length < 2) {
						return usage(ctx.isJavaScriptContext());
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_AND;
			}

		});
		functions.put("or", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				boolean result = false;

				if (sources != null) {

					if (sources.length < 2) {
						return usage(ctx.isJavaScriptContext());
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_OR;
			}
		});
		functions.put("get", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					GraphObject dataObject = null;

					if (sources[0] instanceof GraphObject) {
						dataObject = (GraphObject) sources[0];
					}

					if (sources[0] instanceof List) {

						final List list = (List) sources[0];
						if (list.size() == 1 && list.get(0) instanceof GraphObject) {

							dataObject = (GraphObject) list.get(0);
						}
					}

					if (dataObject != null) {

						final String keyName = sources[1].toString();
						final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(dataObject.getClass(), keyName);

						if (key != null) {

							final PropertyConverter inputConverter = key.inputConverter(securityContext);
							Object value = dataObject.getProperty(key);

							if (inputConverter != null) {
								return inputConverter.revert(value);
							}

							return dataObject.getProperty(key);
						}

						return "";

					} else {

						return ERROR_MESSAGE_GET_ENTITY;
					}
				}

				return usage(ctx.isJavaScriptContext());
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_GET;
			}
		});
		functions.put("size", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final List list = new ArrayList();
				for (final Object source : sources) {

					if (source != null) {

						if (source instanceof Collection) {

							// filter null objects
							for (Object obj : (Collection) source) {
								if (obj != null && !NULL_STRING.equals(obj)) {

									list.add(obj);
								}
							}

						} else if (source.getClass().isArray()) {

							list.addAll(Arrays.asList((Object[]) source));

						} else if (source != null && !NULL_STRING.equals(source)) {

							list.add(source);
						}

						return list.size();
					}
				}

				return 0;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SIZE;
			}
		});
		functions.put("first", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof List && !((List) sources[0]).isEmpty()) {
						return ((List) sources[0]).get(0);
					}

					if (sources[0].getClass().isArray()) {

						final Object[] arr = (Object[]) sources[0];
						if (arr.length > 0) {

							return arr[0];
						}
					}
				}

				return null;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_FIRST;
			}
		});
		functions.put("last", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof List && !((List) sources[0]).isEmpty()) {

						final List list = (List) sources[0];
						return list.get(list.size() - 1);
					}

					if (sources[0].getClass().isArray()) {

						final Object[] arr = (Object[]) sources[0];
						if (arr.length > 0) {

							return arr[arr.length - 1];
						}
					}

				}

				return null;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_LAST;
			}
		});
		functions.put("nth", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					final int pos = Double.valueOf(sources[1].toString()).intValue();

					if (sources[0] instanceof List && !((List) sources[0]).isEmpty()) {

						final List list = (List) sources[0];
						final int size = list.size();

						if (pos >= size) {

							return null;

						}

						return list.get(Math.min(Math.max(0, pos), size - 1));
					}

					if (sources[0].getClass().isArray()) {

						final Object[] arr = (Object[]) sources[0];
						if (pos <= arr.length) {

							return arr[pos];
						}
					}
				}

				return null;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_NTH;
			}
		});
		functions.put("get_counter", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					return ctx.getCounter(parseInt(sources[0]));
				}

				return 0;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_GET_COUNTER;
			}
		});
		functions.put("inc_counter", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final int level = parseInt(sources[0]);

					ctx.incrementCounter(level);

					// reset lower levels?
					if (sources.length == 2 && "true".equals(sources[1].toString())) {

						// reset lower levels
						for (int i = level + 1; i < 10; i++) {
							ctx.resetCounter(i);
						}
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_INC_COUNTER;
			}
		});
		functions.put("reset_counter", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					ctx.resetCounter(parseInt(sources[0]));
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_RESET_COUNTER;
			}
		});
		functions.put("merge_properties", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof GraphObject && sources[1] instanceof GraphObject) {

					final ConfigurationProvider config = StructrApp.getConfiguration();
					final Set<PropertyKey> mergeKeys = new LinkedHashSet<>();
					final GraphObject source = (GraphObject) sources[0];
					final GraphObject target = (GraphObject) sources[1];
					final int paramCount = sources.length;

					for (int i = 2; i < paramCount; i++) {

						final String keyName = sources[i].toString();
						final PropertyKey key = config.getPropertyKeyForJSONName(target.getClass(), keyName);

						mergeKeys.add(key);
					}

					for (final PropertyKey key : mergeKeys) {

						final Object sourceValue = source.getProperty(key);
						if (sourceValue != null) {

							target.setProperty(key, sourceValue);
						}

					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_MERGE_PROPERTIES;
			}
		});
		functions.put("keys", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof GraphObject) {

					final Set<String> keys = new LinkedHashSet<>();
					final GraphObject source = (GraphObject) sources[0];

					for (final PropertyKey key : source.getPropertyKeys(sources[1].toString())) {
						keys.add(key.jsonName());
					}

					return new LinkedList<>(keys);

				} else if (arrayHasMinLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof GraphObjectMap) {

					return new LinkedList<>(((GraphObjectMap) sources[0]).keySet());

				} else if (arrayHasMinLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof Map) {

					return new LinkedList<>(((Map) sources[0]).keySet());

				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_KEYS;
			}
		});

		// ----- BEGIN functions with side effects -----
		functions.put("retrieve", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof String) {

					return ctx.retrieve(sources[0].toString());

				} else {

					return usage(ctx.isJavaScriptContext());
				}
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_RETRIEVE_JS : ERROR_MESSAGE_RETRIEVE);
			}
		});
		functions.put("store", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof String) {

					ctx.store(sources[0].toString(), sources[1]);

				} else {

					return usage(ctx.isJavaScriptContext());
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_STORE_JS : ERROR_MESSAGE_STORE);
			}
		});
		functions.put("print", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					for (Object i : sources) {

						System.out.print(i);
					}

					System.out.println();
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_PRINT;
			}
		});
		functions.put("read", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = getSandboxFileName(sources[0].toString());
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_READ;
			}
		});
		functions.put("write", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = getSandboxFileName(sources[0].toString());
						if (sandboxFilename != null) {

							final File file = new File(sandboxFilename);
							if (!file.exists()) {

								try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file, false))) {

									for (int i = 1; i < sources.length; i++) {
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_WRITE;
			}
		});
		functions.put("append", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = getSandboxFileName(sources[0].toString());
						if (sandboxFilename != null) {

							final File file = new File(sandboxFilename);

							try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file, true))) {

								for (int i = 1; i < sources.length; i++) {
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_APPEND;
			}
		});
		functions.put("xml", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof String) {

					try {

						final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						if (builder != null) {

							final String xml = (String) sources[0];
							final StringReader reader = new StringReader(xml);
							final InputSource src = new InputSource(reader);

							return builder.parse(src);
						}

					} catch (IOException | SAXException | ParserConfigurationException ex) {
						ex.printStackTrace();
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_XML;
			}
		});
		functions.put("xpath", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_XPATH;
			}
		});
		functions.put("set", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					if (sources[0] instanceof GraphObject) {

						final GraphObject source = (GraphObject) sources[0];
						final Map<String, Object> properties = new LinkedHashMap<>();
						final SecurityContext securityContext = source.getSecurityContext();
						final Gson gson = new GsonBuilder().create();
						final Class type = source.getClass();
						final int sourceCount = sources.length;

						if (sources.length == 3 && sources[2] != null && sources[1].toString().matches("[a-zA-Z0-9_]+")) {

							properties.put(sources[1].toString(), sources[2]);

						} else {

							// we either have and odd number of items, or two multi-value items.
							for (int i = 1; i < sourceCount; i++) {

								final Map<String, Object> values = deserialize(gson, sources[i].toString());
								if (values != null) {

									properties.putAll(values);
								}
							}
						}

						// store values in entity
						final PropertyMap map = PropertyMap.inputTypeToJavaType(securityContext, type, properties);
						for (final Map.Entry<PropertyKey, Object> entry : map.entrySet()) {

							source.setProperty(entry.getKey(), entry.getValue());
						}

					} else {

						throw new FrameworkException(422, "Invalid use of builtin method set, usage: set(entity, params..)");
					}

				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SET;
			}
		});
		functions.put("send_plaintext_mail", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 6)) {

					final String from = sources[0].toString();
					final String fromName = sources[1].toString();
					final String to = sources[2].toString();
					final String toName = sources[3].toString();
					final String subject = sources[4].toString();
					final String textContent = sources[5].toString();

					try {
						return MailHelper.sendSimpleMail(from, fromName, to, toName, null, null, from, subject, textContent);

					} catch (EmailException eex) {
						eex.printStackTrace();
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SEND_PLAINTEXT_MAIL;
			}
		});
		functions.put("send_html_mail", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 6)) {

					final String from = sources[0].toString();
					final String fromName = sources[1].toString();
					final String to = sources[2].toString();
					final String toName = sources[3].toString();
					final String subject = sources[4].toString();
					final String htmlContent = sources[5].toString();
					String textContent = "";

					if (sources.length == 7) {
						textContent = sources[6].toString();
					}

					try {
						return MailHelper.sendHtmlMail(from, fromName, to, toName, null, null, from, subject, htmlContent, textContent);

					} catch (EmailException eex) {
						eex.printStackTrace();
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SEND_HTML_MAIL;
			}
		});
		functions.put("geocode", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

					final Gson gson = new GsonBuilder().create();
					final String street = sources[0].toString();
					final String city = sources[1].toString();
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
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_GEOCODE;
			}
		});
		functions.put("find", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					final SecurityContext securityContext = ctx.getSecurityContext();
					final ConfigurationProvider config = StructrApp.getConfiguration();
					final Query query = StructrApp.getInstance(securityContext).nodeQuery().sort(GraphObject.createdDate).order(false);

					// the type to query for
					Class type = null;

					if (sources.length >= 1 && sources[0] != null) {

						type = config.getNodeEntityClass(sources[0].toString());

						if (type != null) {

							query.andTypes(type);
						}
					}

					// extension for native javascript objects
					if (sources.length == 2 && sources[1] instanceof Map) {

						query.and(PropertyMap.inputTypeToJavaType(securityContext, type, (Map) sources[1]));

					} else {

						final Integer parameter_count = sources.length;

						if (parameter_count % 2 == 0) {

							throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + ERROR_MESSAGE_FIND);
						}

						for (Integer c = 1; c < parameter_count; c += 2) {

							final PropertyKey key = config.getPropertyKeyForJSONName(type, sources[c].toString());

							if (key != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key.isSearchable()) {

									throw new FrameworkException(400, "Search key " + key.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key.inputConverter(securityContext);
								Object value = sources[c + 1];

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								query.and(key, value);
							}
						}
					}

					return query.getAsList();
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_FIND;
			}
		});
		functions.put("search", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
					final ConfigurationProvider config = StructrApp.getConfiguration();
					final Query query = StructrApp.getInstance(securityContext).nodeQuery();
					Class type = null;

					if (sources.length >= 1 && sources[0] != null) {

						type = config.getNodeEntityClass(sources[0].toString());

						if (type != null) {

							query.andTypes(type);
						}
					}

					// extension for native javascript objects
					if (sources.length == 2 && sources[1] instanceof Map) {

						final PropertyMap map = PropertyMap.inputTypeToJavaType(securityContext, type, (Map) sources[1]);
						for (final Entry<PropertyKey, Object> entry : map.entrySet()) {

							query.and(entry.getKey(), entry.getValue(), false);
						}

					} else {

						final Integer parameter_count = sources.length;

						if (parameter_count % 2 == 0) {

							throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + ERROR_MESSAGE_FIND);
						}

						for (Integer c = 1; c < parameter_count; c += 2) {

							final PropertyKey key = config.getPropertyKeyForJSONName(type, sources[c].toString());

							if (key != null) {

								// throw exception if key is not indexed (otherwise the user will never know)
								if (!key.isSearchable()) {

									throw new FrameworkException(400, "Search key " + key.jsonName() + " is not indexed.");
								}

								final PropertyConverter inputConverter = key.inputConverter(securityContext);
								Object value = sources[c + 1];

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								query.and(key, value, false);
							}

						}
					}

					final Object x = query.getAsList();

					// return search results
					return x;
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_SEARCH;
			}
		});
		functions.put("create", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
					final App app = StructrApp.getInstance(securityContext);
					final ConfigurationProvider config = StructrApp.getConfiguration();
					PropertyMap propertyMap = null;
					Class type = null;

					if (sources.length >= 1 && sources[0] != null) {

						type = config.getNodeEntityClass(sources[0].toString());

						if (entity != null && type.equals(entity.getClass())) {

							throw new FrameworkException(422, "Cannot create() entity of the same type in save action.");
						}
					}

					// extension for native javascript objects
					if (sources.length == 2 && sources[1] instanceof Map) {

						propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type, (Map) sources[1]);

					} else {

						propertyMap = new PropertyMap();
						final Integer parameter_count = sources.length;

						if (parameter_count % 2 == 0) {

							throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + ERROR_MESSAGE_CREATE);
						}

						for (Integer c = 1; c < parameter_count; c += 2) {

							final PropertyKey key = config.getPropertyKeyForJSONName(type, sources[c].toString());

							if (key != null) {

								final PropertyConverter inputConverter = key.inputConverter(securityContext);
								Object value = sources[c + 1];

								if (inputConverter != null) {

									value = inputConverter.convert(value);
								}

								propertyMap.put(key, value);

							}

						}
					}

					if (type != null) {

						return app.create(type, propertyMap);

					} else {

						throw new FrameworkException(422, "Unknown type in create() save action.");
					}

				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_CREATE;
			}
		});
		functions.put("delete", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources != null) {

					final App app = StructrApp.getInstance(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
					for (final Object obj : sources) {

						if (obj instanceof NodeInterface) {

							app.delete((NodeInterface) obj);
							continue;
						}

						if (obj instanceof RelationshipInterface) {

							app.delete((RelationshipInterface) obj);
							continue;
						}
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return ERROR_MESSAGE_DELETE;
			}
		});
		functions.put("incoming", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final RelationshipFactory factory = new RelationshipFactory(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
					final Object source = sources[0];

					if (source instanceof NodeInterface) {

						final NodeInterface node = (NodeInterface) source;
						if (sources.length > 1) {

							final Object relType = sources[1];
							if (relType != null && relType instanceof String) {

								final String relTypeName = (String) relType;
								return factory.instantiate(node.getNode().getRelationships(Direction.INCOMING, DynamicRelationshipType.withName(relTypeName)));
							}

						} else {

							return factory.instantiate(node.getNode().getRelationships(Direction.INCOMING));
						}

					} else {

						return "Error: entity is not a node.";
					}
				}
				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_INCOMING_JS : ERROR_MESSAGE_INCOMING);
			}
		});
		functions.put("outgoing", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final RelationshipFactory factory = new RelationshipFactory(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
					final Object source = sources[0];

					if (source instanceof NodeInterface) {

						final NodeInterface node = (NodeInterface) source;
						if (sources.length > 1) {

							final Object relType = sources[1];
							if (relType != null && relType instanceof String) {

								final String relTypeName = (String) relType;
								return factory.instantiate(node.getNode().getRelationships(Direction.OUTGOING, DynamicRelationshipType.withName(relTypeName)));
							}

						} else {

							return factory.instantiate(node.getNode().getRelationships(Direction.OUTGOING));
						}

					} else {

						return "Error: entity is not a node.";
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_OUTGOING_JS : ERROR_MESSAGE_OUTGOING);
			}
		});
		functions.put("has_relationship", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final Object source = sources[0];
					final Object target = sources[1];

					AbstractNode sourceNode = null;
					AbstractNode targetNode = null;

					if (source instanceof AbstractNode && target instanceof AbstractNode) {

						sourceNode = (AbstractNode) source;
						targetNode = (AbstractNode) target;

					} else {

						return "Error: entities are not nodes.";
					}

					if (sources.length == 2) {

						for (final AbstractRelationship rel : sourceNode.getRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null & t != null
								&& ((s.equals(sourceNode) && t.equals(targetNode)) || (s.equals(targetNode) && t.equals(sourceNode)))) {
								return true;
							}
						}

					} else if (sources.length == 3) {

						// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
						final String relType = (String) sources[2];

						for (final AbstractRelationship rel : sourceNode.getRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null & t != null
								&& rel.getRelType().name().equals(relType)
								&& ((s.equals(sourceNode) && t.equals(targetNode)) || (s.equals(targetNode) && t.equals(sourceNode)))) {
								return true;
							}
						}

					}

				}

				return false;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_HAS_RELATIONSHIP_JS : ERROR_MESSAGE_HAS_RELATIONSHIP);
			}
		});
		functions.put("has_outgoing_relationship", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final Object source = sources[0];
					final Object target = sources[1];

					AbstractNode sourceNode = null;
					AbstractNode targetNode = null;

					if (source instanceof AbstractNode && target instanceof AbstractNode) {

						sourceNode = (AbstractNode) source;
						targetNode = (AbstractNode) target;

					} else {

						return "Error: entities are not nodes.";
					}

					if (sources.length == 2) {

						for (final AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null & t != null
								&& s.equals(sourceNode) && t.equals(targetNode)) {
								return true;
							}
						}

					} else if (sources.length == 3) {

						// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
						final String relType = (String) sources[2];

						for (final AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null & t != null
								&& rel.getRelType().name().equals(relType)
								&& s.equals(sourceNode) && t.equals(targetNode)) {
								return true;
							}
						}

					}

				}

				return false;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_HAS_OUTGOING_RELATIONSHIP_JS : ERROR_MESSAGE_HAS_OUTGOING_RELATIONSHIP);
			}
		});
		functions.put("has_incoming_relationship", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final Object source = sources[0];
					final Object target = sources[1];

					AbstractNode sourceNode = null;
					AbstractNode targetNode = null;

					if (source instanceof AbstractNode && target instanceof AbstractNode) {

						sourceNode = (AbstractNode) source;
						targetNode = (AbstractNode) target;

					} else {

						return "Error: entities are not nodes.";
					}

					if (sources.length == 2) {

						for (final AbstractRelationship rel : sourceNode.getIncomingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null & t != null
								&& s.equals(targetNode) && t.equals(sourceNode)) {
								return true;
							}
						}

					} else if (sources.length == 3) {

						// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
						final String relType = (String) sources[2];

						for (final AbstractRelationship rel : sourceNode.getIncomingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null & t != null
								&& rel.getRelType().name().equals(relType)
								&& s.equals(targetNode) && t.equals(sourceNode)) {
								return true;
							}
						}

					}

				}

				return false;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_HAS_INCOMING_RELATIONSHIP_JS : ERROR_MESSAGE_HAS_INCOMING_RELATIONSHIP);
			}
		});
		functions.put("get_relationships", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final List<AbstractRelationship> list = new ArrayList<>();

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final Object source = sources[0];
					final Object target = sources[1];

					NodeInterface sourceNode = null;
					NodeInterface targetNode = null;

					if (source instanceof NodeInterface && target instanceof NodeInterface) {

						sourceNode = (NodeInterface) source;
						targetNode = (NodeInterface) target;

					} else {

						return "Error: Entities are not nodes.";
					}

					if (sources.length == 2) {

						for (final AbstractRelationship rel : sourceNode.getRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null && t != null
								&& ((s.equals(sourceNode) && t.equals(targetNode)) || (s.equals(targetNode) && t.equals(sourceNode)))) {
								list.add(rel);
							}
						}

					} else if (sources.length == 3) {

						// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
						final String relType = (String) sources[2];

						for (final AbstractRelationship rel : sourceNode.getRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null && t != null
								&& rel.getRelType().name().equals(relType)
								&& ((s.equals(sourceNode) && t.equals(targetNode)) || (s.equals(targetNode) && t.equals(sourceNode)))) {
								list.add(rel);
							}
						}

					}
				}

				return list;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_GET_RELATIONSHIPS_JS : ERROR_MESSAGE_GET_RELATIONSHIPS);
			}
		});
		functions.put("get_outgoing_relationships", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final List<AbstractRelationship> list = new ArrayList<>();

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final Object source = sources[0];
					final Object target = sources[1];

					AbstractNode sourceNode = null;
					AbstractNode targetNode = null;

					if (source instanceof AbstractNode && target instanceof AbstractNode) {

						sourceNode = (AbstractNode) source;
						targetNode = (AbstractNode) target;

					} else {

						return "Error: entities are not nodes.";
					}

					if (sources.length == 2) {

						for (final AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null && t != null
								&& s.equals(sourceNode) && t.equals(targetNode)) {
								list.add(rel);
							}
						}

					} else if (sources.length == 3) {

						// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
						final String relType = (String) sources[2];

						for (final AbstractRelationship rel : sourceNode.getOutgoingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null && t != null
								&& rel.getRelType().name().equals(relType)
								&& s.equals(sourceNode) && t.equals(targetNode)) {
								list.add(rel);
							}
						}

					}
				}

				return list;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_GET_OUTGOING_RELATIONSHIPS_JS : ERROR_MESSAGE_GET_OUTGOING_RELATIONSHIPS);
			}
		});
		functions.put("get_incoming_relationships", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final List<AbstractRelationship> list = new ArrayList<>();

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

					final Object source = sources[0];
					final Object target = sources[1];

					AbstractNode sourceNode = null;
					AbstractNode targetNode = null;

					if (source instanceof AbstractNode && target instanceof AbstractNode) {

						sourceNode = (AbstractNode) source;
						targetNode = (AbstractNode) target;

					} else {

						return "Error: entities are not nodes.";
					}

					if (sources.length == 2) {

						for (final AbstractRelationship rel : sourceNode.getIncomingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null && t != null
								&& s.equals(targetNode) && t.equals(sourceNode)) {
								list.add(rel);
							}
						}

					} else if (sources.length == 3) {

						// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
						final String relType = (String) sources[2];

						for (final AbstractRelationship rel : sourceNode.getIncomingRelationships()) {

							final NodeInterface s = rel.getSourceNode();
							final NodeInterface t = rel.getTargetNode();

							// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
							if (s != null && t != null
								&& rel.getRelType().name().equals(relType)
								&& s.equals(targetNode) && t.equals(sourceNode)) {
								list.add(rel);
							}
						}

					}
				}

				return list;
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS_JS : ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS);
			}
		});
		functions.put("create_relationship", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

					final Object source = sources[0];
					final Object target = sources[1];
					final String relType = (String) sources[2];

					AbstractNode sourceNode = null;
					AbstractNode targetNode = null;

					if (source instanceof AbstractNode && target instanceof AbstractNode) {

						sourceNode = (AbstractNode) source;
						targetNode = (AbstractNode) target;

					} else {

						return "Error: entities are not nodes.";
					}

					final Class relClass = StructrApp.getConfiguration().getRelationClassForCombinedType(sourceNode.getType(), relType, targetNode.getType());

					if (relClass != null) {

						StructrApp.getInstance(sourceNode.getSecurityContext()).create(sourceNode, targetNode, relClass);

					} else {

						return "Error: Unknown relationship type";
					}

				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_CREATE_RELATIONSHIP_JS : ERROR_MESSAGE_CREATE_RELATIONSHIP);
			}
		});
		functions.put("grant", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 3)) {

					if (sources[0] instanceof Principal) {

						final Principal principal = (Principal) sources[0];

						if (sources[1] instanceof AbstractNode) {

							final AbstractNode node = (AbstractNode) sources[1];

							if (sources[2] instanceof String) {

								final String[] parts = ((String) sources[2]).split("[,]+");
								for (final String part : parts) {

									final String trimmedPart = part.trim();
									if (trimmedPart.length() > 0) {

										final Permission permission = Permissions.valueOf(trimmedPart);
										if (permission != null) {

											node.grant(permission, principal);

										} else {

											return "Error: unknown permission " + trimmedPart;
										}
									}
								}

								return "";

							} else {

								return "Error: third argument is not a string.";
							}

						} else {

							return "Error: second argument is not a node.";
						}

					} else {

						return "Error: first argument is not of type Principal.";
					}

				} else {

					return usage(ctx.isJavaScriptContext());
				}
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_GRANT_JS : ERROR_MESSAGE_GRANT);
			}
		});
		functions.put("revoke", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 3)) {

					if (sources[0] instanceof Principal) {

						final Principal principal = (Principal) sources[0];

						if (sources[1] instanceof AbstractNode) {

							final AbstractNode node = (AbstractNode) sources[1];

							if (sources[2] instanceof String) {

								final String[] parts = ((String) sources[2]).split("[,]+");
								for (final String part : parts) {

									final String trimmedPart = part.trim();
									if (trimmedPart.length() > 0) {

										final Permission permission = Permissions.valueOf(trimmedPart);
										if (permission != null) {

											node.revoke(permission, principal);

										} else {

											return "Error: unknown permission " + trimmedPart;
										}
									}
								}

								return "";

							} else {

								return "Error: third argument is not a string.";
							}

						} else {

							return "Error: second argument is not a node.";
						}

					} else {

						return "Error: first argument is not of type Principal.";
					}

				} else {

					return usage(ctx.isJavaScriptContext());
				}
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_REVOKE_JS : ERROR_MESSAGE_REVOKE);
			}
		});
		functions.put("unlock_readonly_properties_once", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					if (sources[0] instanceof AbstractNode) {

						((AbstractNode) sources[0]).unlockReadOnlyPropertiesOnce();

					} else {

						return usage(ctx.isJavaScriptContext());

					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE_JS : ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE);
			}
		});
		functions.put("call", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final String key = sources[0].toString();

					if (sources.length > 1) {

						Actions.call(key, Arrays.copyOfRange(sources, 1, sources.length));

					} else {

						Actions.call(key);
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE_JS : ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE);
			}
		});
		functions.put("set_privileged", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				Function<Object, Object> set = functions.get("set");
				if (set != null) {

					synchronized (entity) {

						final SecurityContext previousSecurityContext = entity.getSecurityContext();
						entity.setSecurityContext(SecurityContext.getSuperUserInstance());

						try {

							set.apply(ctx, entity, sources);

						} finally {

							entity.setSecurityContext(previousSecurityContext);
						}
					}
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_SET_PRIVILEGED_JS : ERROR_MESSAGE_SET_PRIVILEGED);
			}
		});
		functions.put("cypher", new Function<Object, Object>() {

			@Override
			public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					final Map<String, Object> params = new LinkedHashMap<>();
					final String query               = sources[0].toString();

					// parameters?
					if (sources.length > 1 && sources[1] != null && sources[1] instanceof Map) {
						params.putAll((Map)sources[1]);
					}

					return StructrApp.getInstance(ctx.getSecurityContext()).cypher(query, params);
				}

				return "";
			}

			@Override
			public String usage(boolean inJavaScriptContext) {
				return (inJavaScriptContext ? ERROR_MESSAGE_SET_PRIVILEGED_JS : ERROR_MESSAGE_SET_PRIVILEGED);
			}
		});
	}

	/**
	 * Test if the given object array has a minimum length and all its
	 * elements are not null.
	 *
	 * @param array
	 * @param minLength If null, don't do length check
	 * @return true if array has min length and all elements are not null
	 */
	public static boolean arrayHasMinLengthAndAllElementsNotNull(final Object[] array, final Integer minLength) {

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
	 * Test if the given object array has exact the given length and all its
	 * elements are not null.
	 *
	 * @param array
	 * @param length If null, don't do length check
	 * @return true if array has exact length and all elements are not null
	 */
	public static boolean arrayHasLengthAndAllElementsNotNull(final Object[] array, final Integer length) {

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

	protected static String serialize(final Gson gson, final Map<String, Object> map) {
		return gson.toJson(map, new TypeToken<Map<String, String>>() {
		}.getType());
	}

	protected static Map<String, Object> deserialize(final Gson gson, final String source) {
		return gson.fromJson(source, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	protected static Integer parseInt(final Object source) {

		if (source instanceof Integer) {

			return ((Integer) source);
		}

		if (source instanceof Number) {

			return ((Number) source).intValue();
		}

		if (source instanceof String) {

			return Integer.parseInt((String) source);
		}

		return null;
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

	protected static double getDoubleForComparison(final Object obj) {

		if (obj instanceof Number) {

			return ((Number) obj).doubleValue();

		} else {

			try {
				return Double.valueOf(obj.toString());

			} catch (Throwable t) {

				t.printStackTrace();
			}
		}

		return 0.0;
	}

	protected static Double getDoubleOrNull(final Object obj) {

		try {

			if (obj instanceof Date) {

				return (double) ((Date) obj).getTime();

			} else if (obj instanceof Number) {

				return ((Number) obj).doubleValue();

			} else {

				Date date = DatePropertyParser.parseISO8601DateString(obj.toString());

				if (date != null) {

					return (double) (date).getTime();
				}

				return Double.parseDouble(obj.toString());

			}

		} catch (Throwable t) {

			t.printStackTrace();
		}

		return null;
	}

	protected static boolean valueEquals(final Object obj1, final Object obj2) {

		if (obj1 instanceof Enum || obj2 instanceof Enum) {

			return obj1.toString().equals(obj2.toString());

		}

		return eq(obj1, obj2);
	}

	protected static String getSandboxFileName(final String source) throws IOException {

		final File sandboxFile = new File(source);
		final String fileName = sandboxFile.getName();
		final String basePath = StructrApp.getConfigurationValue(Services.BASE_PATH);

		if (!basePath.isEmpty()) {

			final String defaultExchangePath = basePath.endsWith("/") ? basePath.concat("exchange") : basePath.concat("/exchange");
			String exchangeDir = StructrApp.getConfigurationValue(Services.DATA_EXCHANGE_PATH, defaultExchangePath);

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

	public static String cleanString(final Object input) {

		if (input == null) {

			return "";

		}

		String normalized = Normalizer.normalize(input.toString(), Normalizer.Form.NFD)
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

		String result = normalized.replaceAll("-", " ");
		result = StringUtils.normalizeSpace(result.toLowerCase());
		result = result.replaceAll("[^\\p{ASCII}]", "").replaceAll("\\p{P}", "-").replaceAll("\\-(\\s+\\-)+", "-");
		result = result.replaceAll(" ", "-");

		return result;

	}

	public static void recursivelyConvertMapToGraphObjectMap(final GraphObjectMap destination, final Map<String, Object> source, final int depth) {

		if (depth > 20) {
			return;
		}

		for (final Map.Entry<String, Object> entry : source.entrySet()) {

			final String key = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof Map) {

				final Map<String, Object> map = (Map<String, Object>) value;
				final GraphObjectMap obj = new GraphObjectMap();

				destination.put(new StringProperty(key), obj);

				recursivelyConvertMapToGraphObjectMap(obj, map, depth + 1);

			} else if (value instanceof Collection) {

				final List list = new LinkedList();
				final Collection collection = (Collection) value;

				for (final Object obj : collection) {

					if (obj instanceof Map) {

						final GraphObjectMap container = new GraphObjectMap();
						list.add(container);

						recursivelyConvertMapToGraphObjectMap(container, (Map<String, Object>) obj, depth + 1);

					} else {

						list.add(obj);
					}
				}

				destination.put(new StringProperty(key), list);

			} else {

				destination.put(new StringProperty(key), value);
			}
		}
	}

	public static Object numberOrString(final String value) {

		if (value != null) {

			if ("true".equals(value.toLowerCase())) {
				return true;
			}

			if ("false".equals(value.toLowerCase())) {
				return false;
			}

			if (NumberUtils.isNumber(value)) {
				return NumberUtils.createNumber(value);
			}
		}

		return value;
	}

	private static int compareBooleanBoolean(final Object o1, final Object o2) {

		final Boolean value1 = (Boolean) o1;
		final Boolean value2 = (Boolean) o2;

		return value1.compareTo(value2);
	}

	private static int compareNumberNumber(final Object o1, final Object o2) {

		final Double value1 = getDoubleForComparison(o1);
		final Double value2 = getDoubleForComparison(o2);

		return value1.compareTo(value2);
	}

	private static int compareStringString(final Object o1, final Object o2) {

		final String value1 = (String) o1;
		final String value2 = (String) o2;

		return value1.compareTo(value2);
	}

	private static int compareDateDate(final Object o1, final Object o2) {

		final Date value1 = (Date) o1;
		final Date value2 = (Date) o2;

		return value1.compareTo(value2);
	}

	private static int compareDateString(final Object o1, final Object o2) {

		final String value1 = DatePropertyParser.format((Date) o1, DateProperty.DEFAULT_FORMAT);
		final String value2 = (String) o2;

		return value1.compareTo(value2);
	}

	private static int compareStringDate(final Object o1, final Object o2) {

		final String value1 = (String) o1;
		final String value2 = DatePropertyParser.format((Date) o2, DateProperty.DEFAULT_FORMAT);

		return value1.compareTo(value2);
	}

	private static int compareBooleanString(final Object o1, final Object o2) {

		return -1;
	}

	private static int compareStringBoolean(final Object o1, final Object o2) {

		return -1;
	}

	private static int compareNumberString(final Object o1, final Object o2) {

		final Double value1 = getDoubleForComparison(o1);
		final Double value2 = Double.parseDouble((String) o2);

		return (value1.compareTo(value2) == 0 ? -1 : value1.compareTo(value2));
	}

	private static int compareStringNumber(final Object o1, final Object o2) {

		final Double value1 = Double.parseDouble((String) o1);
		final Double value2 = getDoubleForComparison(o2);

		return (value1.compareTo(value2) == 0 ? -1 : value1.compareTo(value2));
	}

	private static boolean gt(final Object o1, final Object o2) {

		if (o1 instanceof Number && o2 instanceof Number) {

			return compareNumberNumber(o1, o2) > 0;

		} else if (o1 instanceof String && o2 instanceof String) {

			return compareStringString(o1, o2) > 0;

		} else if (o1 instanceof Date && o2 instanceof Date) {

			return compareDateDate(o1, o2) > 0;

		} else if (o1 instanceof Date && o2 instanceof String) {

			return compareDateString(o1, o2) > 0;

		} else if (o1 instanceof String && o2 instanceof Date) {

			return compareStringDate(o1, o2) > 0;

		} else if (o1 instanceof Boolean && o2 instanceof String) {

			return compareBooleanString(o1, o2) > 0;

		} else if (o1 instanceof String && o2 instanceof Boolean) {

			return compareStringBoolean(o1, o2) > 0;

		} else if (o1 instanceof Number && o2 instanceof String) {

			return compareNumberString(o1, o2) > 0;

		} else if (o1 instanceof String && o2 instanceof Number) {

			return compareStringNumber(o1, o2) > 0;

		} else {

			return compareStringString(o1.toString(), o2.toString()) > 0;

		}
	}

	private static boolean lt(final Object o1, final Object o2) {

		if (o1 instanceof Number && o2 instanceof Number) {

			return compareNumberNumber(o1, o2) < 0;

		} else if (o1 instanceof String && o2 instanceof String) {

			return compareStringString(o1, o2) < 0;

		} else if (o1 instanceof Date && o2 instanceof Date) {

			return compareDateDate(o1, o2) < 0;

		} else if (o1 instanceof Date && o2 instanceof String) {

			return compareDateString(o1, o2) < 0;

		} else if (o1 instanceof String && o2 instanceof Date) {

			return compareStringDate(o1, o2) < 0;

		} else if (o1 instanceof Boolean && o2 instanceof String) {

			return compareBooleanString(o1, o2) < 0;

		} else if (o1 instanceof String && o2 instanceof Boolean) {

			return compareStringBoolean(o1, o2) < 0;

		} else if (o1 instanceof Number && o2 instanceof String) {

			return compareNumberString(o1, o2) < 0;

		} else if (o1 instanceof String && o2 instanceof Number) {

			return compareStringNumber(o1, o2) < 0;

		} else {

			return compareStringString(o1.toString(), o2.toString()) < 0;

		}
	}

	private static boolean eq(final Object o1, final Object o2) {

		if (o1 instanceof Number && o2 instanceof Number) {

			return compareNumberNumber(o1, o2) == 0;

		} else if (o1 instanceof String && o2 instanceof String) {

			return compareStringString(o1, o2) == 0;

		} else if (o1 instanceof Date && o2 instanceof Date) {

			return compareDateDate(o1, o2) == 0;

		} else if (o1 instanceof Date && o2 instanceof String) {

			return compareDateString(o1, o2) == 0;

		} else if (o1 instanceof String && o2 instanceof Date) {

			return compareStringDate(o1, o2) == 0;

		} else if (o1 instanceof Boolean && o2 instanceof String) {

			return compareBooleanString(o1, o2) == 0;

		} else if (o1 instanceof String && o2 instanceof Boolean) {

			return compareStringBoolean(o1, o2) == 0;

		} else if (o1 instanceof Number && o2 instanceof String) {

			return compareNumberString(o1, o2) == 0;

		} else if (o1 instanceof String && o2 instanceof Number) {

			return compareStringNumber(o1, o2) == 0;

		} else {

			return compareStringString(o1.toString(), o2.toString()) == 0;

		}
	}

	private static boolean gte(final Object o1, final Object o2) {
		return eq(o1, o2) || gt(o1, o2);
	}

	private static boolean lte(final Object o1, final Object o2) {
		return eq(o1, o2) || lt(o1, o2);
	}

}
