/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.parser.AllExpression;
import org.structr.core.parser.AnyExpression;
import org.structr.core.parser.ArrayExpression;
import org.structr.core.parser.BatchExpression;
import org.structr.core.parser.CacheExpression;
import org.structr.core.parser.ConstantExpression;
import org.structr.core.parser.EachExpression;
import org.structr.core.parser.Expression;
import org.structr.core.parser.FilterExpression;
import org.structr.core.parser.FunctionExpression;
import org.structr.core.parser.FunctionValueExpression;
import org.structr.core.parser.GroupExpression;
import org.structr.core.parser.IfExpression;
import org.structr.core.parser.IsExpression;
import org.structr.core.parser.NoneExpression;
import org.structr.core.parser.NullExpression;
import org.structr.core.parser.RootExpression;
import org.structr.core.parser.SliceExpression;
import org.structr.core.parser.ValueExpression;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 *
 */
public class Functions {

	protected static final Logger logger = LoggerFactory.getLogger(Functions.class.getName());
	private static final Map<String, Function<Object, Object>> functions = new LinkedHashMap<>();

	public static void put(final boolean licensed, final int edition, final Function<Object, Object> function) {

		registerFunction(licensed, edition, function.getName(), function);

		function.aliases().forEach(alias -> {
			registerFunction(licensed, edition, alias, function);
		});
	}

	private static void registerFunction(final boolean licensed, final int edition, final String name, final Function<Object, Object> function) {

		if (functions.containsKey(name)) {
			logger.warn("A Function named '{}' is already registered, previous function will be overwritten with this one.", name);
		}

		if (licensed) {

			functions.put(name, function);

		} else {

			functions.put(name, new UnlicensedFunction(name, edition));
		}
	}

	public static Set<String> getNames() {
		return new LinkedHashSet<>(functions.keySet());
	}

	public static Function<Object, Object> get(final String name) {
		return functions.get(name);
	}

	public static Collection<Function<Object, Object>> getFunctions() {
		return new LinkedList<>(functions.values());
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String expression) throws FrameworkException, UnlicensedScriptException {

		final String expressionWithoutNewlines = expression.replace('\n', ' ').replace('\r', ' ');
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

	// ----- private methods -----
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

			case "is":
				return new IsExpression();

			case "each":
				return new EachExpression();

			case "filter":
				return new FilterExpression();

			case "slice":
				return new SliceExpression();

			case "batch":
				return new BatchExpression();

			case "data":
				return new ValueExpression("data");

			case "any":
				return new AnyExpression();

			case "all":
				return new AllExpression();

			case "none":
				return new NoneExpression();

			case "null":
				return new ConstantExpression(null);

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

		} catch (IOException ioex) {
		}

		return StreamTokenizer.TT_EOF;
	}
}
