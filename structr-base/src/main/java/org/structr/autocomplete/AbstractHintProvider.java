/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.autocomplete;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.function.Functions;
import org.structr.core.function.KeywordHint;
import org.structr.core.function.ParseResult;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.schema.action.Hint;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Content.ContentHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;



public abstract class AbstractHintProvider {

	private static final Logger logger = LoggerFactory.getLogger(AbstractHintProvider.class);

	private enum QueryType {
		REST, Cypher, XPath, Function
	}

	public static final Property<String> displayText = new StringProperty("displayText");
	public static final Property<String> className   = new StringProperty("className");
	public static final Property<String> text        = new StringProperty("text");
	public static final Property<GraphObject> from   = new GenericProperty("from");
	public static final Property<GraphObject> to     = new GenericProperty("to");
	public static final Property<Integer> line       = new IntProperty("line");
	public static final Property<Integer> ch         = new IntProperty("ch");

	protected abstract List<Hint> getAllHints(final ActionContext ionContext, final GraphObject currentNode, final String editorText, final ParseResult parseResult);
	protected abstract String getFunctionName(final String sourceName);

	protected final Comparator comparator     = new HintComparator();

	public static List<GraphObject> getHints(final ActionContext actionContext, final boolean isAutoscriptEnv, final GraphObject currentEntity, final String textBefore, final String textAfter, final int cursorLine, final int cursorPosition) {

		String text = null;

		// don't interpret invalid strings
		if (textBefore != null && (textBefore.endsWith("''") || textBefore.endsWith("\"\""))) {
			return Collections.EMPTY_LIST;
		}

		if (StringUtils.isBlank(textAfter) || textAfter.startsWith(" ") || textAfter.startsWith("\t") || textAfter.startsWith("\n") || textAfter.startsWith(";") || textAfter.startsWith(")")) {

			if (isAutoscriptEnv || currentEntity instanceof SchemaMethod) {

				// we can use the whole text here, the method will always contain script code and nothing else
				// add ${ to be able to reuse code below
				text = "${" + textBefore;

			} else {

				try {

					final AutocompleteContentHandler handler = new AutocompleteContentHandler();
					Content.renderContentWithScripts(textBefore, handler);

					if (handler.inScript()) {

						// we are inside of a scripting context
						text = handler.getScript();
					}

				} catch (Throwable t) {
					logger.error(ExceptionUtils.getStackTrace(t));
				}
			}

			// handle text for autocompletion
			if (text != null) {

				if (text.startsWith("${{")) {

					final JavascriptHintProvider provider = new JavascriptHintProvider();
					final String source                   = text.substring(3);

					return provider.getHints(actionContext, currentEntity, source, cursorLine, cursorPosition);

				} else if (text.startsWith("${")) {

					final PlaintextHintProvider provider = new PlaintextHintProvider();
					final String source                  = text.substring(2);

					return provider.getHints(actionContext, currentEntity, source, cursorLine, cursorPosition);
				}
			}
		}

		return Collections.EMPTY_LIST;
	}

	public List<GraphObject> getHints(final ActionContext actionContext, final GraphObject currentEntity, final String script, final int cursorLine, final int cursorPosition) {

		final ParseResult parseResult = new ParseResult();
		final List<Hint> allHints     = getAllHints(actionContext, currentEntity, script, parseResult);
		final List<GraphObject> hints = new LinkedList<>();
		final String lastToken        = parseResult.getLastToken();
		final boolean unrestricted    = lastToken.endsWith("(") || lastToken.endsWith(("."));
		final int lastTokenLength     = unrestricted ? 0 : lastToken.length();
		int maxNameLength             = 0;

		for (final Hint hint : allHints) {

			if (hint instanceof SeparatorHint) {

				final GraphObjectMap item = new GraphObjectMap();

				item.put(displayText, "");
				item.put(className  , "separator");
				item.put(text,        "");

				hints.add(item);

			} else if (!hint.isHidden()) {

				final String functionName = getFunctionName(hint.getReplacement());
				final String displayName  = getFunctionName(hint.getDisplayName());

				if ((unrestricted || displayName.startsWith(lastToken)) && ( (!script.endsWith(functionName)) || (script.endsWith(functionName) && hint instanceof Function) )) {

					final GraphObjectMap item = new GraphObjectMap();

					if (hint.mayModify()) {

						item.put(text, displayName);

					} else {

						item.put(text, displayName);
					}

					item.put(displayText, displayName + " - " + textOrPlaceholder(hint.shortDescription()));

					addPosition(item, hint, cursorLine, cursorPosition - lastTokenLength, cursorPosition);

					if (displayName.length() > maxNameLength) {
						maxNameLength = displayName.length();
					}

					hints.add(item);
				}
			}
		}

		alignHintDescriptions(hints, maxNameLength + 1);

		return hints;
	}

	protected String visitReplacement(final String replacement) {
		return replacement;
	}

	protected Hint createHint(final String name, final String description, final String replacement) {

		final KeywordHint hint = new KeywordHint(name, description);

		if (replacement != null) {
			hint.setReplacement(replacement);
		}

		return hint;
	}

	protected void alignHintDescriptions(final List<GraphObject> hints, final int maxNameLength) {

		// insert appropriate number of spaces into description to align function names
		for (final GraphObject item : hints) {

			final String text = item.getProperty(displayText);
			final int pos     = text.indexOf(" - ");

			if (pos >= 0 && pos < maxNameLength) {

				final StringBuilder buf = new StringBuilder(text);
				buf.insert(pos, StringUtils.leftPad("", maxNameLength - pos));

				// ignore exception, won't happen on a GraphObjectMap anyway
				try { item.setProperty(displayText, buf.toString()); } catch (FrameworkException fex) {}
			}
		}
	}

	protected String textOrPlaceholder(final String source) {

		if (StringUtils.isBlank(source)) {

			return "   (no description available yet)";
		}

		return source;
	}

	protected void handleJSExpression(final ActionContext actionContext, final GraphObject currentNode, final String expression, final List<Hint> hints, final ParseResult result) {

		final String[] expressionParts = StringUtils.splitPreserveAllTokens(expression, ".(");
		final List<String> tokens      = Arrays.asList(expressionParts);
		final int length               = expressionParts.length;

		if (length > 1) {

			handleTokens(actionContext, tokens, currentNode, hints, result);

		} else {

			addAllHints(hints);
		}

		result.setExpression(expression);
	}

	protected void addAllHints(final List<Hint> hints) {

		for (final Function<Object, Object> func : Functions.getFunctions()) {
			hints.add(func);
		}

		// sort hints
		Collections.sort(hints, comparator);

		// Important: Order is reverse alphanumeric, so add(0, ...) means insert at the beginning.
		// If you change something here, make sure to change AutocompleteTest.java accordingly.

		// add keywords, keep in sync and include everything from StructrBinding.getMemberKeys()
		hints.add(0, createHint("this",                "The current object", "this"));
		hints.add(0, createHint("session",             "The current session", "session"));
		hints.add(0, createHint("response",            "The current response",       "response"));
		hints.add(0, createHint("request",             "The current request", "request"));
		hints.add(0, createHint("predicate",           "Search predicate", "predicate"));
		hints.add(0, createHint("page",                "The current page",           "page"));
		hints.add(0, createHint("methodParameters",    "Access method parameters", "methodParameters"));
		hints.add(0, createHint("me",                  "The current user", "me"));
		hints.add(0, createHint("locale",              "The current locale",         "locale"));
		hints.add(0, createHint("includeJs",           "Include JavaScript files", "includeJs"));
		hints.add(0, createHint("doPrivileged",        "Open a privileged context", "doPrivileged"));
		hints.add(0, createHint("doInNewTransaction",  "Open a new transaction context", "doInNewTransaction"));
		hints.add(0, createHint("current",             "The current details object", "current"));
		hints.add(0, createHint("cache",               "Time-based cache object", "cache"));
		hints.add(0, createHint("batch",               "Open a batch transaction context", "batch"));
		hints.add(0, createHint("applicationStore",    "The application store", "applicationStore"));

	}

	protected void addNonempty(final List<String> list, final String string) {

		if (StringUtils.isNotBlank(string)) {
			list.add(string);
		}
	}

	protected void addHintsForType(final ActionContext actionContext, final Class type, final List<Hint> hints, final ParseResult result) {

		final List<Hint> methodHints = new LinkedList<>();

		try {

			// properties
			final List<GraphObjectMap> typeInfo = SchemaHelper.getSchemaTypeInfo(actionContext.getSecurityContext(), type.getSimpleName(), type, PropertyView.All);
			for (final GraphObjectMap property : typeInfo) {

				final Map<String, Object> map = property.toMap();
				final String name             = (String)map.get("jsonName");
				final String propertyType     = (String)map.get("uiType");
				final String className        = (String)map.get("className");
				final String declaringClass   = (String)map.get("declaringClass");

				// skip properties defined in NodeInterface class, except for name
				if (NodeInterface.class.getSimpleName().equals(declaringClass) && !"name".equals(name)) {
					continue;
				}

				// filter inherited properties (except name)
				//if (!type.getSimpleName().equals(declaringClass) && !"name".equals(name)) {
				//	continue;
				//}

				hints.add(createHint(name, propertyType, name));
			}

			// methods go into their own collection, are sorted and the appended to the list
			final Map<String, Method> methods = StructrApp.getConfiguration().getExportedMethodsForType(type);
			for (final Entry<String, Method> entry : methods.entrySet()) {

				final String name = entry.getKey();

				methodHints.add(createHint(name + "()", "custom method", name));
			}

			Collections.sort(methodHints, comparator);

		} catch (FrameworkException ex) {
			java.util.logging.Logger.getLogger(JavascriptHintProvider.class.getName()).log(Level.SEVERE, null, ex);
		}

		Collections.sort(hints, comparator);

		// add sorted method hints after the other hints
		hints.addAll(methodHints);
	}

	protected boolean handleTokens(final ActionContext actionContext, final List<String> tokens, final GraphObject currentNode, final List<Hint> hints, final ParseResult result) {

		List<String> tokenTypes = new LinkedList<>();
		Class type              = null;

		for (final String token : tokens) {

			switch (token) {

				case "":
					// last character in the expression is "."
					tokenTypes.add("dot");
					break;

				case "$":
				case "Structr":
					tokenTypes.add("root");
					break;

				case "page":
					tokenTypes.add("keyword");
					type      = StructrApp.getConfiguration().getNodeEntityClass("Page");
					break;

				case "me":
					tokenTypes.add("keyword");
					type      = StructrApp.getConfiguration().getNodeEntityClass("User");
					break;

				case "current":
					tokenTypes.add("keyword");
					type      = StructrApp.getConfiguration().getNodeEntityClass("AbstractNode");
					break;

				case "this":
					if(currentNode instanceof SchemaMethod) {

						final AbstractSchemaNode node = currentNode.getProperty(SchemaMethod.schemaNode);
						if (node != null) {

							tokenTypes.add("keyword");
							type = StructrApp.getConfiguration().getNodeEntityClass(node.getClassName());
						}

					} else if (currentNode instanceof SchemaProperty && Type.Function.equals(((SchemaProperty)currentNode).getPropertyType())) {


						final AbstractSchemaNode node = currentNode.getProperty(SchemaMethod.schemaNode);
						if (node != null) {

							tokenTypes.add("keyword");
							type = StructrApp.getConfiguration().getNodeEntityClass(node.getClassName());
						}

					} else if (currentNode != null) {

						tokenTypes.add("keyword");
						type      = StructrApp.getConfiguration().getNodeEntityClass(currentNode.getType());
					}
					break;

				default:
					// skip numbers
					if (!StringUtils.isNumeric(token)) {

						if (type != null) {

							final String cleaned  = token.replaceAll("[\\W\\d]+", "");
							final PropertyKey key = StructrApp.key(type, cleaned, false);

							if (key != null && key.relatedType() != null) {

								tokenTypes.add("keyword");
								type = key.relatedType();

							} else {

								tokenTypes.add(token);
							}

						} else {

							tokenTypes.add(token);
						}
					}
					break;
			}
		}

		Collections.reverse(tokenTypes);

		boolean requireValidPredecessor = false;

		for (final String tokenType : tokenTypes) {

			switch (tokenType) {

				case "dot":
					// if the last token of an expression is a dot, e.g. "this.owner.", the preceding keyword must be valid
					requireValidPredecessor = true;
					break;

				case "root":
					if (tokenTypes.size() < 3) {
						addAllHints(hints);
						return true;
					}
					break;

				case "keyword":
					if (type != null) {
						addHintsForType(actionContext, type, hints, result);
						return true;
					}
					break;

				default:
					final Function func = Functions.get(tokenType);
					if (func != null) {

						final List<Hint> contextHints = func.getContextHints(result.getLastToken());
						if (contextHints != null) {

							hints.addAll(contextHints);

							Collections.sort(hints, comparator);
						}

						return true;

					} else if (requireValidPredecessor) {

						return false;
					}
					break;
			}
		}

		return false;
	}

	// ----- private methods -----
	private void addPosition(final GraphObjectMap item, final Hint hint, final int cursorLine, final int replaceFrom, final int replaceTo) {

		final GraphObjectMap fromObject = new GraphObjectMap();
		final GraphObjectMap toObject   = new GraphObjectMap();

		fromObject.put(line, cursorLine);
		fromObject.put(ch, replaceFrom);

		toObject.put(line, cursorLine);
		toObject.put(ch, replaceTo);

		item.put(from, fromObject);
		item.put(to, toObject);
	}

	// ----- nested classes -----
	protected static class HintComparator implements Comparator<Hint> {

		@Override
		public int compare(final Hint o1, final Hint o2) {

			final boolean firstIsDynamic  = o1.isDynamic();
			final boolean secindIsDynamic = o2.isDynamic();

			if (firstIsDynamic && !secindIsDynamic) {
				return -1;
			}

			if (!firstIsDynamic && secindIsDynamic) {
				return 1;
			}

			return o1.getName().compareTo(o2.getName());
		}
	}

	protected static class AutocompleteContentHandler implements ContentHandler {

		private boolean inScript  = false;
		private String scriptText = null;

		@Override
		public void handleScript(String script, final int row, final int column) throws FrameworkException, IOException {
			inScript = false;
		}

		@Override
		public void handleText(String text) throws FrameworkException {
		}

		@Override
		public void handleIncompleteScript(String script) throws FrameworkException, IOException {
			scriptText = script;
		}

		@Override
		public void possibleStartOfScript(int row, int column) {
			inScript = true;
		}

		public boolean inScript() {
			return inScript;
		}

		public String getScript() {
			return scriptText;
		}
	}
}
