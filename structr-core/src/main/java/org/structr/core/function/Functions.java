/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.function;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.parser.ArrayExpression;
import org.structr.core.parser.CacheExpression;
import org.structr.core.parser.ConstantExpression;
import org.structr.core.parser.EachExpression;
import org.structr.core.parser.Expression;
import org.structr.core.parser.FilterExpression;
import org.structr.core.parser.FunctionExpression;
import org.structr.core.parser.FunctionValueExpression;
import org.structr.core.parser.GroupExpression;
import org.structr.core.parser.IfExpression;
import org.structr.core.parser.NullExpression;
import org.structr.core.parser.RootExpression;
import org.structr.core.parser.ValueExpression;
import org.structr.core.parser.function.PrivilegedFindFunction;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 *
 */
public class Functions {

	public static final Map<String, Function<Object, Object>> functions = new LinkedHashMap<>();
	public static final String NULL_STRING                              = "___NULL___";

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
					Expression previousExpression = current.getPrevious();
					if (tokenizer.sval.startsWith(".") && previousExpression != null && previousExpression instanceof FunctionExpression && next instanceof ValueExpression) {

						final FunctionExpression previousFunctionExpression = (FunctionExpression) previousExpression;
						final ValueExpression    valueExpression            = (ValueExpression) next;

						current.replacePrevious(new FunctionValueExpression(previousFunctionExpression, valueExpression));
					} else {
						current.add(next);
					}
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

		functions.put("error", new ErrorFunction());
		functions.put("md5", new MD5Function());
		functions.put("upper", new UpperFunction());
		functions.put("lower", new LowerFunction());
		functions.put("join", new JoinFunction());
		functions.put("concat", new ConcatFunction());
		functions.put("split", new SplitFunction());
		functions.put("split_regex", new SplitRegexFunction());
		functions.put("abbr", new AbbrFunction());
		functions.put("capitalize", new CapitalizeFunction());
		functions.put("titleize", new TitleizeFunction());
		functions.put("num", new NumFunction());
		functions.put("int", new IntFunction());
		functions.put("random", new RandomFunction());
		functions.put("rint", new RintFunction());
		functions.put("index_of", new IndexOfFunction());
		functions.put("contains", new ContainsFunction());
		functions.put("substring", new SubstringFunction());
		functions.put("length", new LengthFunction());
		functions.put("replace", new ReplaceFunction());
		functions.put("clean", new CleanFunction());
		functions.put("urlencode", new UrlEncodeFunction());
		functions.put("escape_javascript", new EscapeJavascriptFunction());
		functions.put("escape_json", new EscapeJsonFunction());
		functions.put("empty", new EmptyFunction());
		functions.put("equal", new EqualFunction());
		functions.put("eq", new EqualFunction());
		functions.put("add", new AddFunction());
		functions.put("double_sum", new DoubleSumFunction());
		functions.put("int_sum", new IntSumFunction());
		functions.put("is_collection", new IsCollectionFunction());
		functions.put("is_entity", new IsEntityFunction());
		functions.put("extract", new ExtractFunction());
		functions.put("merge", new MergeFunction());
		functions.put("merge_unique", new MergeUniqueFunction());
		functions.put("complement", new ComplementFunction());
		functions.put("unwind", new UnwindFunction());
		functions.put("sort", new SortFunction());
		functions.put("lt", new LtFunction());
		functions.put("gt", new GtFunction());
		functions.put("lte", new LteFunction());
		functions.put("gte", new GteFunction());
		functions.put("subt", new SubtFunction());
		functions.put("mult", new MultFunction());
		functions.put("quot", new QuotFunction());
		functions.put("mod", new ModFunction());
		functions.put("floor", new FloorFunction());
		functions.put("ceil", new CeilFunction());
		functions.put("round", new RoundFunction());
		functions.put("max", new MaxFunction());
		functions.put("min", new MinFunction());
		functions.put("config", new ConfigFunction());
		functions.put("date_format", new DateFormatFunction());
		functions.put("parse_date", new ParseDateFunction());
		functions.put("to_date", new ToDateFunction());
		functions.put("number_format", new NumberFormatFunction());
		functions.put("template", new TemplateFunction());
		functions.put("not", new NotFunction());
		functions.put("and", new AndFunction());
		functions.put("or", new OrFunction());
		functions.put("get", new GetFunction());
		functions.put("get_or_null", new GetOrNullFunction());
		functions.put("size", new SizeFunction());
		functions.put("first", new FirstFunction());
		functions.put("last", new LastFunction());
		functions.put("nth", new NthFunction());
		functions.put("get_counter", new GetCounterFunction());
		functions.put("inc_counter", new IncCounterFunction());
		functions.put("reset_counter", new ResetCounterFunction());
		functions.put("merge_properties", new MergePropertiesFunction());
		functions.put("keys", new KeysFunction());
		functions.put("values", new ValuesFunction());
		functions.put("changelog", new ChangelogFunction());
		functions.put("timer", new TimerFunction());
		functions.put("str_replace", new StrReplaceFunction());
                functions.put("find_privileged", new PrivilegedFindFunction());

		// ----- BEGIN functions with side effects -----
		functions.put("retrieve", new RetrieveFunction());
		functions.put("store", new StoreFunction());
		functions.put("print", new PrintFunction());
		functions.put("log", new LogFunction());
		functions.put("read", new ReadFunction());
		functions.put("write", new WriteFunction());
		functions.put("append", new AppendFunction());
		functions.put("xml", new XmlFunction());
		functions.put("xpath", new XPathFunction());
		functions.put("set", new SetFunction());
		functions.put("geocode", new GeocodeFunction());
		functions.put("find", new FindFunction());
		functions.put("search", new SearchFunction());
		functions.put("create", new CreateFunction());
		functions.put("delete", new DeleteFunction());
		functions.put("incoming", new IncomingFunction());
		functions.put("instantiate", new InstantiateFunction());
		functions.put("outgoing", new OutgoingFunction());
		functions.put("has_relationship", new HasRelationshipFunction());
		functions.put("has_outgoing_relationship", new HasOutgoingRelationshipFunction());
		functions.put("has_incoming_relationship", new HasIncomingRelationshipFunction());
		functions.put("get_relationships", new GetRelationshipsFunction());
		functions.put("get_outgoing_relationships", new GetOutgoingRelationshipsFunction());
		functions.put("get_incoming_relationships", new GetIncomingRelationshipsFunction());
		functions.put("create_relationship", new CreateRelationshipFunction());
		functions.put("grant", new GrantFunction());
		functions.put("revoke", new RevokeFunction());
		functions.put("is_allowed", new IsAllowedFunction());
		functions.put("unlock_readonly_properties_once", new UnlockReadonlyPropertiesFunction());
		functions.put("unlock_system_properties_once", new UnlockSystemPropertiesFunction());
		functions.put("call", new CallFunction());
		functions.put("exec", new ExecFunction());
		functions.put("exec_binary", new ExecBinaryFunction());
		functions.put("set_privileged", new SetPrivilegedFunction());
		functions.put("cypher", new CypherFunction());
		functions.put("localize", new LocalizeFunction());
		functions.put("property_info", new PropertyInfoFunction());
		functions.put("disable_notifications", new DisableNotificationsFunction());
		functions.put("enable_notifications", new EnableNotificationsFunction());
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
}
