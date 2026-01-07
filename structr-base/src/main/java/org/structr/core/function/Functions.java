/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.function.tokenizer.*;
import org.structr.core.parser.*;
import org.structr.core.script.Snippet;
import org.structr.core.script.StructrScriptException;
import org.structr.core.script.polyglot.function.DoAsFunction;
import org.structr.core.script.polyglot.function.DoInNewTransactionFunction;
import org.structr.core.script.polyglot.function.DoPrivilegedFunction;
import org.structr.docs.Documentable;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.text.Normalizer;
import java.util.*;

/**
 *
 *
 */
public class Functions {

	protected static final Logger logger                                 = LoggerFactory.getLogger(Functions.class.getName());
	private static final Set<Function<Object, Object>> allFunctions      = new LinkedHashSet<>();
	private static final Map<String, Function<Object, Object>> functions = new LinkedHashMap<>();

	public static void put(final LicenseManager licenseManager, final Function<Object, Object> function) {
		Functions.put(licenseManager, function, true);
	}

	public static void put(final LicenseManager licenseManager, final Function<Object, Object> function, final boolean warnUnregistered) {

		allFunctions.add(function);

		functions.put(function.getName(), function);

		function.aliases().forEach(alias -> {
			functions.put(alias, function);
		});
	}

	public static Set<String> getNames() {
		return new LinkedHashSet<>(functions.keySet());
	}

	public static Function<Object, Object> get(final String name) {
		return functions.get(name);
	}

	public static Function<Object, Object> getByClass(final Class clazz) {

		for (Map.Entry<String, Function<Object, Object>> entry : functions.entrySet()) {

			if (entry.getValue().getClass().isAssignableFrom(clazz)) {

				return entry.getValue();
			}
		}

		return null;
	}

	public static Collection<Function<Object, Object>> getFunctions() {
		return new LinkedList<>(functions.values());
	}

	public static void refresh(final LicenseManager manager) {

		for (final Function<Object, Object> function : allFunctions) {

			Functions.put(manager, function, false);
		}
	}

	public static List<String> tokenize(final String input, final boolean includeQuotesInTokens) {

		final StructrScriptTokenizer tokenizer = new StructrScriptTokenizer(includeQuotesInTokens);
		final List<String> tokens              = new LinkedList<>();

		tokenizer.setIsSilent(true);

		for (final Token token : tokenizer.tokenize(input)) {

			if (StringUtils.isNotBlank(token.getContent())) {

				tokens.add(token.getContent());
			}
		}

		return tokens;
	}

	public static Expression parse(final Snippet snippet, final ParseResult result, final boolean silenceTokenizer) throws FrameworkException, UnlicensedScriptException {

		final Map<Integer, String> namespaceMap = new TreeMap<>();
		final String expression                 = snippet.getSource();
		final List<String> tokens               = result.getTokens();
		final StructrScriptTokenizer tokenizer  = new StructrScriptTokenizer(false);
		tokenizer.setIsSilent(silenceTokenizer);

		Expression root = new RootExpression();
		Expression current = root;
		Expression next = null;
		Token lastToken = null;
		int level = 0;

		// store root result for access even when an exception occurs while parsing
		result.setRootExpression(root);

		for (final Token token : tokenizer.tokenize(expression)) {

			switch (token.getType()) {

				case "number":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before NUMBER", token.getRow(), token.getColumn());
					}
					final String stringToken = String.valueOf(token.getContent());
					tokens.add(stringToken);
					next = new ConstantExpression(Double.valueOf(token.getContent()), token.getRow(), token.getColumn());
					current.add(next);
					break;

				case "identifier":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before " + token.getContent(), token.getRow(), token.getColumn());
					}
					next = checkReservedWords(token.getContent(), level, namespaceMap, token.getRow(), token.getColumn());
					Expression previousExpression = current.getPrevious();
					if (token.getContent().startsWith(".") && previousExpression != null && previousExpression instanceof FunctionExpression && next instanceof ValueExpression) {

						final FunctionExpression previousFunctionExpression = (FunctionExpression) previousExpression;
						final ValueExpression    valueExpression            = (ValueExpression) next;

						current.replacePrevious(new FunctionValueExpression(previousFunctionExpression, valueExpression, token.getRow(), token.getColumn()));
					} else {
						current.add(next);
					}
					tokens.add(token.getContent());
					break;

				case "(":
					if (((current == null || current instanceof RootExpression) && next == null) || current == next) {

						// an additional bracket without a new function, this can only be an execution group.
						next = new GroupExpression(token.getRow(), token.getColumn());
						current.add(next);
					}

					// ValueExpression plus "(" => unknown function
					if (next instanceof ValueExpression || next instanceof ConstantExpression) {

						// check if the token is something like myEntity.methodToBeCalled() and don't throw an exception then
						if (!lastToken.getContent().contains(".")) {

							throw new StructrScriptException(422, "Unknown function: " + lastToken.getContent(), token.getRow(), token.getColumn());
						}
					}

					current = next;
					tokens.add("(");
					level++;
					break;

				case ")":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before " + token.getContent(), token.getRow(), token.getColumn());
					}
					current = current.getParent();
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket after " + token.getContent(), token.getRow(), token.getColumn());
					}
					tokens.add(")");
					level--;
					namespaceMap.remove(level);
					break;

				case "[":
					// bind directly to the previous expression
					next = new ArrayExpression(token.getRow(), token.getColumn());
					current.add(next);
					current = next;
					tokens.add("[");
					level++;
					break;

				case "]":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket before " + token.getContent(), token.getRow(), token.getColumn());
					}
					current = current.getParent();
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket after " + token.getContent(), token.getRow(), token.getColumn());
					}
					tokens.add("]");
					level--;
					break;

				case ";":
					next = null;
					tokens.add(";");
					break;

				case ",":
					tokens.add(",");
					next = current;
					break;

				case "string":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before " + token.getContent(), token.getRow(), token.getColumn());
					}
					final ConstantExpression constantExpression = new ConstantExpression(token.getContent(), token.getRow(), token.getColumn());
					final String quoteChar                      = token.getQuote();
					current.add(constantExpression);
					constantExpression.setQuoteChar(quoteChar);
					if (StringUtils.isEmpty(token.getContent())) {
						tokens.add(quoteChar);
					} else {
						tokens.add(quoteChar + token.getContent() + quoteChar);
					}
					break;
			}

			lastToken = token;
		}

		if (level > 0) {
			throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket after " + lastToken.getContent(), lastToken.getRow(), lastToken.getColumn());
		}

		return root;
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final Snippet snippet, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		final Expression root = parse(snippet, new ParseResult(), false);

		return root.evaluate(actionContext, entity, hints);
	}

	public static String cleanString(final Object input) {

		if (input == null) {

			return "";
		}

		String normalized = input.toString()
			.replaceAll("ü", "ue")
			.replaceAll("ö", "oe")
			.replaceAll("ä", "ae")
			.replaceAll("ß", "ss")
			.replaceAll("Ü(?=[a-zäöüß ])", "Ue")
			.replaceAll("Ö(?=[a-zäöüß ])", "Oe")
			.replaceAll("Ä(?=[a-zäöüß ])", "Ae")
			.replaceAll("Ü", "UE")
			.replaceAll("Ö", "OE")
			.replaceAll("Ä", "AE")
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
			.replaceAll("_", " ").replaceAll("_", " ")
			.replaceAll("`", "-");

		normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);

		String result = normalized.replaceAll("-", " ");
		result = StringUtils.normalizeSpace(result.toLowerCase());
		result = result.replaceAll("[^\\p{ASCII}]", "").replaceAll("\\p{P}", "-").replaceAll("\\-(\\s+\\-)+", "-");
		result = result.replaceAll(" ", "-");

		return result;
	}

	public static void addExpressions(final List<Documentable> functions) {

		functions.add(new DoInNewTransactionFunction(null, null));
		functions.add(new DoPrivilegedFunction(null));
		functions.add(new DoAsFunction(null));
		functions.add(new CacheExpression(0, 0));
		functions.add(new IfExpression(0, 0));
		functions.add(new IsExpression(0, 0));
		functions.add(new EachExpression(0, 0));
		functions.add(new FilterExpression(0, 0));
		functions.add(new MapExpression(0, 0));
		functions.add(new ReduceExpression(0, 0));
		functions.add(new AnyExpression(0, 0));
		functions.add(new AllExpression(0, 0));
		functions.add(new NoneExpression(0, 0));
	}

	public static void addFunctionsAndExpressions(final List<Documentable> documentables) {

		documentables.addAll(Functions.getFunctions());
		Functions.addExpressions(documentables);
	}

	// ----- private methods -----
	private static Expression checkReservedWords(final String word, final int level, final Map<Integer, String> namespace, final int row, final int column) throws FrameworkException {

		if (word == null) {
			return new NullExpression(row, column);
		}

		switch (word) {

			case "cache":
				return new CacheExpression(row, column);

			case "true":
				return new ConstantExpression(true, row, column);

			case "false":
				return new ConstantExpression(false, row, column);

			case "if":
				return new IfExpression(row, column);

			case "is":
				return new IsExpression(row, column);

			case "each":
				return new EachExpression(row, column);

			case "filter":
				return new FilterExpression(row, column);

			case "map":
				return new MapExpression(row, column);

			case "reduce":
				return new ReduceExpression(row, column);

			case "slice":
				throw new FrameworkException(422, "The slice() function is not supported any more, please use the page() predicate which does exactly the same, but with database support.");

			case "data":
				return new ValueExpression("data", row, column);

			case "any":
				return new AnyExpression(row, column);

			case "all":
				return new AllExpression(row, column);

			case "none":
				return new NoneExpression(row, column);

			case "null":
				return new ConstantExpression(null, row, column);

		}

		// no match, try functions
		final Function<Object, Object> function = getNamespacedFunction(CaseHelper.toCamelCase(word), namespace.values());
		if (function != null) {

			final String namespaceIdentifier = function.getNamespaceIdentifier();
			if (namespaceIdentifier != null) {

				namespace.put(level, namespaceIdentifier);
			}

			return new FunctionExpression(word, function, row, column);

		} else {

			return new ValueExpression(word, row, column);
		}
	}

	private static String getNamespacedKeyword(final String keyword, final Collection<String> namespace) {

		final StringBuilder buf = new StringBuilder(StringUtils.join(namespace, "."));

		if (buf.length() > 0) {
			buf.append(".");
		}

		buf.append(keyword);

		return buf.toString();
	}

	private static Function<Object, Object> getNamespacedFunction(final String word, final Collection<String> namespace) {

		final Function<Object, Object> function = Functions.get(getNamespacedKeyword(word, namespace));
		if (function != null) {

			return function;
		}

		return Functions.get(word);
	}

}
