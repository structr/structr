/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.parser.*;
import org.structr.core.script.Snippet;
import org.structr.core.script.StructrScriptException;
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

	protected static final Logger logger = LoggerFactory.getLogger(Functions.class.getName());
	private static final Set<Function<Object, Object>> allFunctions      = new LinkedHashSet<>();
	private static final Map<String, Function<Object, Object>> functions = new LinkedHashMap<>();

	public static void put(final LicenseManager licenseManager, final Function<Object, Object> function) {
		Functions.put(licenseManager, function, true);
	}

	public static void put(final LicenseManager licenseManager, final Function<Object, Object> function, final boolean warnUnregistered) {

		final boolean licensed = (licenseManager == null || licenseManager.isModuleLicensed(function.getRequiredModule()));

		allFunctions.add(function);

		registerFunction(licensed, function.getName(), function, warnUnregistered);

		function.aliases().forEach(alias -> {
			registerFunction(licensed, alias, function, warnUnregistered);
		});
	}

	private static void registerFunction(final boolean licensed, final String name, final Function<Object, Object> function, final boolean warnUnregistered) {

		if (warnUnregistered && functions.containsKey(name)) {
			logger.warn("A function named '{}' is already registered! The previous function will be overwritten with this one.", name);
		}

		if (licensed) {

			functions.put(name, function);

		} else {

			functions.put(name, new UnlicensedFunction(name, function.getRequiredModule()));
		}
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

	public static Expression parse(final ActionContext actionContext, final GraphObject entity, final Snippet snippet, final ParseResult result) throws FrameworkException, UnlicensedScriptException {

		final Map<Integer, String> namespaceMap = new TreeMap<>();
		final String expression                 = snippet.getSource();
		final List<String> tokens               = result.getTokens();
		final StructrScriptTokenizer tokenizer  = new StructrScriptTokenizer();

		Expression root = new RootExpression();
		Expression current = root;
		Expression next = null;
		Token lastToken = null;
		int level = 0;

		// store root result for access even when an exception occurs while parsing
		result.setRootExpression(root);

		for (final Token token : tokenizer.tokenize(expression)) {

			switch (token.type) {

				case "number":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before NUMBER", token.row, token.column);
					}
					final String stringToken = String.valueOf(token.content);
					tokens.add(stringToken);
					next = new ConstantExpression(Double.valueOf(token.content), token.row, token.column);
					current.add(next);
					break;

				case "identifier":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before " + token.content, token.row, token.column);
					}
					next = checkReservedWords(token.content, level, namespaceMap, token.row, token.column);
					Expression previousExpression = current.getPrevious();
					if (token.content.startsWith(".") && previousExpression != null && previousExpression instanceof FunctionExpression && next instanceof ValueExpression) {

						final FunctionExpression previousFunctionExpression = (FunctionExpression) previousExpression;
						final ValueExpression    valueExpression            = (ValueExpression) next;

						current.replacePrevious(new FunctionValueExpression(previousFunctionExpression, valueExpression, token.row, token.column));
					} else {
						current.add(next);
					}
					tokens.add(token.content);
					break;

				case "(":
					if (((current == null || current instanceof RootExpression) && next == null) || current == next) {

						// an additional bracket without a new function, this can only be an execution group.
						next = new GroupExpression(token.row, token.column);
						current.add(next);
					}

					// ValueExpression plus "(" => unknown function
					if (next instanceof ValueExpression || next instanceof ConstantExpression) {

						// check if the token is something like myEntity.methodToBeCalled() and don't throw an exception then
						if (!lastToken.content.contains(".")) {

							throw new StructrScriptException(422, "Unknown function: " + lastToken.content, token.row, token.column);
						}
					}

					current = next;
					tokens.add("(");
					level++;
					break;

				case ")":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before " + token.content, token.row, token.column);
					}
					current = current.getParent();
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket after " + token.content, token.row, token.column);
					}
					tokens.add(")");
					level--;
					namespaceMap.remove(level);
					break;

				case "[":
					// bind directly to the previous expression
					next = new ArrayExpression(token.row, token.column);
					current.add(next);
					current = next;
					tokens.add("[");
					level++;
					break;

				case "]":
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket before " + token.content, token.row, token.column);
					}
					current = current.getParent();
					if (current == null) {
						throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket after " + token.content, token.row, token.column);
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
						throw new StructrScriptException(422, "Invalid expression: mismatched opening bracket before " + token.content, token.row, token.column);
					}
					final ConstantExpression constantExpression = new ConstantExpression(token.content, token.row, token.column);
					final String quoteChar                      = token.quote;
					current.add(constantExpression);
					constantExpression.setQuoteChar(quoteChar);
					if (StringUtils.isEmpty(token.content)) {
						tokens.add(quoteChar);
					} else {
						tokens.add(quoteChar + token.content + quoteChar);
					}
					break;
			}

			lastToken = token;
		}

		if (level > 0) {
			throw new StructrScriptException(422, "Invalid expression: mismatched closing bracket after " + lastToken.content, lastToken.row, lastToken.column);
		}

		return root;
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final Snippet snippet, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		final Expression root = parse(actionContext, entity, snippet, new ParseResult());

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
				return new SliceExpression(row, column);

			case "batch":
				return new BatchExpression(row, column);

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
		final Function<Object, Object> function = getNamespacedFunction(word, namespace.values());
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

	private static class StructrScriptTokenizer {

		private static final List<Tokenizer> candidates = new LinkedList<>();
		private List<Token> tokens         = new LinkedList<>();
		private Tokenizer currentToken     = null;
		private int column                 = 1;
		private int row                    = 1;

		static {

			candidates.add(new Identifier());
			candidates.add(new SingleCharacter((char)9));  // tab
			candidates.add(new SingleCharacter((char)10)); // newline
			candidates.add(new SingleCharacter((char)13)); // carriage-return
			candidates.add(new SingleCharacter((char)32)); // space
			candidates.add(new SingleCharacter(','));
			candidates.add(new SingleCharacter(';'));
			candidates.add(new SingleCharacter('('));
			candidates.add(new SingleCharacter(')'));
			candidates.add(new SingleCharacter('['));
			candidates.add(new SingleCharacter(']'));
			candidates.add(new QuotedString('\''));
			candidates.add(new QuotedString('\"'));
			candidates.add(new Number());
		}

		public List<Token> tokenize(final String expression) {

			final char[] chars = expression.toCharArray();
			final int length   = chars.length;
			int count          = 0;
			int i              = 0;

			// FIXME: does this mean StructrScript expressions can only be 1000 characters long?!
			while (i < length && count++ < 1000) {

				while (i < length && currentToken != null && currentToken.accept(chars[i])) {

					currentToken.add(chars[i]);

					if (chars[i] == '\n') {
						column = 1;
						row++;
					} else {
						column++;
					}

					i++;
				}

				// find token for character
				if (i < length) {

					final Tokenizer nextToken = findToken(chars[i]);
					if (nextToken != null) {

						if (currentToken != null) {

							tokens.add(currentToken.getToken());
						}

						currentToken = nextToken;

					} else {

						logger.warn("Unexpected character {} ({}) in string \"{}\". Tokens: {}", (int)chars[i], Character.toString(chars[i]), expression, tokens);

						// no token, stop parsing
						break;
					}
				}
			}

			if (currentToken != null) {

				tokens.add(currentToken.getToken());
			}

			return tokens;
		}

		private Tokenizer findToken(final char c) {

			for (final Tokenizer t : candidates) {

				final Tokenizer instance = t.newInstance();
				if (instance.accept(c)) {

					instance.init(row, column);

					return instance;
				}
			}

			return null;
		}
	}

	private static class Token {

		private String type    = null;
		private String content = null;
		private String quote   = null;
		private int row        = 1;
		private int column     = 1;

		public Token(final String type, final String content, final String quote, final int row, final int column) {

			this.type    = type;
			this.content = content;
			this.row     = row;
			this.column  = column;
			this.quote   = quote;
		}

		@Override
		public String toString() {
			return content;
		}

	}

	private static abstract class Tokenizer {

		private StringBuilder buf = new StringBuilder();
		private int row           = 0;
		private int column        = 0;

		abstract boolean accept(final char character);
		abstract Tokenizer newInstance();
		abstract String getQuoteChar();
		abstract String getType();

		@Override
		public String toString() {
			return getType();
		}


		public void add(final char character) {
			buf.append(character);
		}

		public String getContent() {
			return buf.toString();
		}

		public Token getToken() {
			return new Token(getType(), getContent(), getQuoteChar(), row, column);
		}

		public void init(final int row, final int column) {
			this.column = column;
			this.row    = row;
		}

		public void reset() {
			buf.setLength(0);
		}
	}

	private static class Identifier extends Tokenizer {

		private int index = 0;

		@Override
		public boolean accept(final char character) {

			if (index == 0) {

				return Character.isAlphabetic(character) || character == '_' || character == '.';

			} else {

				return Character.isAlphabetic(character) || Character.isDigit(character) || character == '_' || character == '.' || character == '!' || character == '-';
			}
		}

		@Override
		public void add(final char character) {
			super.add(character);
			index++;
		}

		@Override
		public String getType() {
			return "identifier";
		}

		@Override
		public String getQuoteChar() {
			return null;
		}

		@Override
		Tokenizer newInstance() {
			return new Identifier();
		}
	}

	private static class Number extends Tokenizer {

		private int index = 0;

		@Override
		public boolean accept(final char character) {

			if (index == 0) {

				return Character.isDigit(character) || character == '-';

			} else {

				return Character.isDigit(character) || character == '.';
			}
		}

		@Override
		public void add(final char character) {
			super.add(character);
			index++;
		}

		@Override
		public String getType() {
			return "number";
		}

		@Override
		public String getQuoteChar() {
			return null;
		}

		@Override
		Tokenizer newInstance() {
			return new Number();
		}
	}

	private static class SingleCharacter extends Tokenizer {

		private boolean first = true;
		private char key      = 0;

		public SingleCharacter(final char key) {
			this.key = key;
		}

		@Override
		public boolean accept(final char character) {
			return first && key == character;
		}

		@Override
		public String getType() {
			return Character.toString(key);
		}

		@Override
		public String getQuoteChar() {
			return null;
		}

		@Override
		public void add(final char character) {
			super.add(character);
			first = false;
		}

		@Override
		public void reset() {
			first = true;
		}

		@Override
		Tokenizer newInstance() {
			return new SingleCharacter(key);
		}
	}

	private static class QuotedString extends Tokenizer {

		private boolean esc     = false;
		private char quoteChar  = 0;
		private int state       = 0;

		public QuotedString(final char quoteChar) {
			this.quoteChar = quoteChar;
		}

		@Override
		public boolean accept(final char character) {

			switch (state) {

				// not started
				case 0:
					if (character == quoteChar) {
						return true;
					}
					break;

				// in group (accept all)
				case 1:
					return true;
			}

			return false;
		}

		@Override
		public void add(final char character) {

			switch (state) {

				case 0:
					if (character == quoteChar && !esc) {
						state = 1;
					}
					break;

				case 1:
					if (character == quoteChar && !esc) {
						state = 2;
					}
					break;
			}

			if (esc || (character != quoteChar && (character != '\\'))) {

				if (esc) {

					// convert back to escape code
					super.add(StringEscapeUtils.unescapeJava("\\" + Character.toString(character)).charAt(0));

				} else {

					super.add(character);
				}
			}

			if (character == '\\' && !esc) {

				esc = true;

			} else {

				esc = false;
			}
		}

		@Override
		public void reset() {
			state = 0;
		}

		@Override
		public String getType() {
			return "string";
		}

		@Override
		public String getQuoteChar() {
			return Character.toString(quoteChar);
		}

		@Override
		Tokenizer newInstance() {
			return new QuotedString(quoteChar);
		}
	}
}
