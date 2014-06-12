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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.structr.common.GraphObjectComparator;
import org.structr.common.MailHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.MailTemplate;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
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

	public static final String NULL_STRING                       = "___NULL___";

	public static final String ERROR_MESSAGE_MD5                 = "Usage: ${md5(string)}. Example: ${md5(this.email)}";
	public static final String ERROR_MESSAGE_ERROR               = "Usage: ${error(...)}. Example: ${error(\"base\", \"must_equal\", int(5))}";
	public static final String ERROR_MESSAGE_UPPER               = "Usage: ${upper(string)}. Example: ${upper(this.nickName)}";
	public static final String ERROR_MESSAGE_LOWER               = "Usage: ${lower(string)}. Example: ${lower(this.email)}";
	public static final String ERROR_MESSAGE_JOIN                = "Usage: ${join(collection, separator)}. Example: ${join(this.names, \",\")}";
	public static final String ERROR_MESSAGE_CONCAT              = "Usage: ${concat(values...)}. Example: ${concat(this.firstName, this.lastName)}";
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
	public static final String ERROR_MESSAGE_KEYS                = "Usage: ${keys(entity, viewName)}. Example: ${keys(this, \"ui\")}";
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

	public static Function<Object, Object> get(final String name) {
		return functions.get(name);
	}

	public static Object evaluate(final SecurityContext securityContext, final ActionContext actionContext, final GraphObject entity, final String expression) throws FrameworkException {

		final String expressionWithoutNewlines = expression.replace('\n', ' ');
		final StreamTokenizer tokenizer        = new StreamTokenizer(new StringReader(expressionWithoutNewlines));
		tokenizer.eolIsSignificant(true);
		tokenizer.wordChars('_', '_');
		tokenizer.wordChars('.', '.');
		tokenizer.wordChars('!', '!');

		Expression root    = new RootExpression();
		Expression current = root;
		Expression next    = null;
		String lastToken   = null;
		int token          = 0;
		int level          = 0;

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

		return root.evaluate(securityContext, actionContext, entity);
	}

	private static Expression checkReservedWords(final String word) throws FrameworkException {

		if (word == null) {
			return new NullExpression();
		}

		switch (word) {

			case "true":
				return new ConstantExpression(true);

			case "false":
				return new ConstantExpression(false);

			case "if":
				return new IfExpression();

			case "each":
				return new EachExpression();

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

	private static int nextToken(final StreamTokenizer tokenizer) {

		try {

			return tokenizer.nextToken();

		} catch (IOException ioex) { }

		return StreamTokenizer.TT_EOF;
	}

	static {

		functions.put("error", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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


				} else if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

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
				return ERROR_MESSAGE_ERROR;
			}
		});
		functions.put("md5", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof Collection) {

					return StringUtils.join((Collection)sources[0], sources[1].toString());
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_JOIN;
			}

		});
		functions.put("concat", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
				return ERROR_MESSAGE_CONCAT;
			}

		});
		functions.put("split", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
		functions.put("empty", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (sources.length == 0 || sources[0] == null || StringUtils.isEmpty(sources[0].toString())) {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = getDoubleForComparison(sources[0]);
					double value2 = getDoubleForComparison(sources[1]);

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = getDoubleForComparison(sources[0]);
					double value2 = getDoubleForComparison(sources[1]);

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = getDoubleForComparison(sources[0]);
					double value2 = getDoubleForComparison(sources[1]);

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				String result = "";

				if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

					double value1 = getDoubleForComparison(sources[0]);
					double value2 = getDoubleForComparison(sources[1]);

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				final SecurityContext securityContext = entity.getSecurityContext();
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

							final PropertyConverter inputConverter = key.inputConverter(securityContext);
							Object value = node.getProperty(key);

							if (inputConverter != null) {
								return inputConverter.revert(value);
							}

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
		functions.put("merge_properties", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
						if (sourceValue != null) {

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
		functions.put("keys", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof GraphObject) {

					final Set<String> keys   = new LinkedHashSet<>();
					final GraphObject source = (GraphObject)sources[0];

					for (final PropertyKey key : source.getPropertyKeys(sources[1].toString())) {
						keys.add(key.jsonName());
					}

					return new LinkedList<>(keys);
				}

				return "";
			}

			@Override
			public String usage() {
				return ERROR_MESSAGE_KEYS;
			}
		});

		// ----- BEGIN functions with side effects -----
		functions.put("print", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage() {
				return ERROR_MESSAGE_READ;
			}
		});
		functions.put("write", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = getSandboxFileName(sources[0].toString());
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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

				if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

					try {
						final String sandboxFilename = getSandboxFileName(sources[0].toString());
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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public String usage() {
				return ERROR_MESSAGE_SET;
			}
		});
		functions.put("send_plaintext_mail", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

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

	protected static String serialize(final Gson gson, final Map<String, Object> map) {
		return gson.toJson(map, new TypeToken<Map<String, String>>() { }.getType());
	}

	protected static Map<String, Object> deserialize(final Gson gson, final String source) {
		return gson.fromJson(source, new TypeToken<Map<String, Object>>() { }.getType());
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
}
