/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.parser;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.parser.function.AbbrFunction;
import org.structr.core.parser.function.AddFunction;
import org.structr.core.parser.function.AndFunction;
import org.structr.core.parser.function.AppendFunction;
import org.structr.core.parser.function.CallFunction;
import org.structr.core.parser.function.CapitalizeFunction;
import org.structr.core.parser.function.CeilFunction;
import org.structr.core.parser.function.CleanFunction;
import org.structr.core.parser.function.ComplementFunction;
import org.structr.core.parser.function.ConcatFunction;
import org.structr.core.parser.function.ConfigFunction;
import org.structr.core.parser.function.ContainsFunction;
import org.structr.core.parser.function.CreateFunction;
import org.structr.core.parser.function.CreateRelationshipFunction;
import org.structr.core.parser.function.CypherFunction;
import org.structr.core.parser.function.DateFormatFunction;
import org.structr.core.parser.function.DeleteFunction;
import org.structr.core.parser.function.DisableNotificationsFunction;
import org.structr.core.parser.function.DoubleSumFunction;
import org.structr.core.parser.function.EmptyFunction;
import org.structr.core.parser.function.EnableNotificationsFunction;
import org.structr.core.parser.function.EqualFunction;
import org.structr.core.parser.function.ErrorFunction;
import org.structr.core.parser.function.EscapeJavascriptFunction;
import org.structr.core.parser.function.EscapeJsonFunction;
import org.structr.core.parser.function.ExecFunction;
import org.structr.core.parser.function.ExtractFunction;
import org.structr.core.parser.function.FindFunction;
import org.structr.core.parser.function.FirstFunction;
import org.structr.core.parser.function.FloorFunction;
import org.structr.core.parser.function.GeocodeFunction;
import org.structr.core.parser.function.GetCounterFunction;
import org.structr.core.parser.function.GetFunction;
import org.structr.core.parser.function.GetIncomingRelationshipsFunction;
import org.structr.core.parser.function.GetOrNullFunction;
import org.structr.core.parser.function.GetOutgoingRelationshipsFunction;
import org.structr.core.parser.function.GetRelationshipsFunction;
import org.structr.core.parser.function.GrantFunction;
import org.structr.core.parser.function.GtFunction;
import org.structr.core.parser.function.GteFunction;
import org.structr.core.parser.function.HasIncomingRelationshipFunction;
import org.structr.core.parser.function.HasOutgoingRelationshipFunction;
import org.structr.core.parser.function.HasRelationshipFunction;
import org.structr.core.parser.function.IncCounterFunction;
import org.structr.core.parser.function.IncomingFunction;
import org.structr.core.parser.function.IndexOfFunction;
import org.structr.core.parser.function.InstantiateFunction;
import org.structr.core.parser.function.IntFunction;
import org.structr.core.parser.function.IntSumFunction;
import org.structr.core.parser.function.IsAllowedFunction;
import org.structr.core.parser.function.IsCollectionFunction;
import org.structr.core.parser.function.IsEntityFunction;
import org.structr.core.parser.function.JoinFunction;
import org.structr.core.parser.function.KeysFunction;
import org.structr.core.parser.function.LastFunction;
import org.structr.core.parser.function.LengthFunction;
import org.structr.core.parser.function.LocalizeFunction;
import org.structr.core.parser.function.LogFunction;
import org.structr.core.parser.function.LowerFunction;
import org.structr.core.parser.function.LtFunction;
import org.structr.core.parser.function.LteFunction;
import org.structr.core.parser.function.MD5Function;
import org.structr.core.parser.function.MaxFunction;
import org.structr.core.parser.function.MergeFunction;
import org.structr.core.parser.function.MergePropertiesFunction;
import org.structr.core.parser.function.MergeUniqueFunction;
import org.structr.core.parser.function.MinFunction;
import org.structr.core.parser.function.ModFunction;
import org.structr.core.parser.function.MultFunction;
import org.structr.core.parser.function.NotFunction;
import org.structr.core.parser.function.NthFunction;
import org.structr.core.parser.function.NumFunction;
import org.structr.core.parser.function.NumberFormatFunction;
import org.structr.core.parser.function.OrFunction;
import org.structr.core.parser.function.OutgoingFunction;
import org.structr.core.parser.function.ParseDateFunction;
import org.structr.core.parser.function.PrintFunction;
import org.structr.core.parser.function.PropertyInfoFunction;
import org.structr.core.parser.function.QuotFunction;
import org.structr.core.parser.function.RandomFunction;
import org.structr.core.parser.function.ReadFunction;
import org.structr.core.parser.function.ReplaceFunction;
import org.structr.core.parser.function.ResetCounterFunction;
import org.structr.core.parser.function.RetrieveFunction;
import org.structr.core.parser.function.RevokeFunction;
import org.structr.core.parser.function.RintFunction;
import org.structr.core.parser.function.RoundFunction;
import org.structr.core.parser.function.SearchFunction;
import org.structr.core.parser.function.SendHtmlMailFunction;
import org.structr.core.parser.function.SendPlaintextMailFunction;
import org.structr.core.parser.function.SetFunction;
import org.structr.core.parser.function.SetPrivilegedFunction;
import org.structr.core.parser.function.SizeFunction;
import org.structr.core.parser.function.SortFunction;
import org.structr.core.parser.function.SplitFunction;
import org.structr.core.parser.function.StoreFunction;
import org.structr.core.parser.function.SubstringFunction;
import org.structr.core.parser.function.SubtFunction;
import org.structr.core.parser.function.TemplateFunction;
import org.structr.core.parser.function.TitleizeFunction;
import org.structr.core.parser.function.UnlockReadonlyPropertiesFunction;
import org.structr.core.parser.function.UnwindFunction;
import org.structr.core.parser.function.UpperFunction;
import org.structr.core.parser.function.UrlEncodeFunction;
import org.structr.core.parser.function.WriteFunction;
import org.structr.core.parser.function.XPathFunction;
import org.structr.core.parser.function.XmlFunction;
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
		functions.put("send_plaintext_mail", new SendPlaintextMailFunction());
		functions.put("send_html_mail", new SendHtmlMailFunction());
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
		functions.put("call", new CallFunction());
		functions.put("exec", new ExecFunction());
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
