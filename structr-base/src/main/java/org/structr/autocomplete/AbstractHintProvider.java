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
import org.structr.core.function.ParseResult;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.polyglot.function.DoAsFunction;
import org.structr.core.script.polyglot.function.DoInNewTransactionFunction;
import org.structr.core.script.polyglot.function.DoPrivilegedFunction;
import org.structr.core.script.polyglot.function.IncludeJSFunction;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Content.ContentHandler;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.graph.Tx;



public abstract class AbstractHintProvider {

	private static final Logger logger = LoggerFactory.getLogger(AbstractHintProvider.class);

	protected abstract List<AbstractHint> getAllHints(final ActionContext ionContext, final GraphObject currentNode, final String editorText, final ParseResult parseResult);
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

		final ParseResult parseResult     = new ParseResult();
		final List<AbstractHint> allHints = getAllHints(actionContext, currentEntity, script, parseResult);
		final List<GraphObject> hints     = new LinkedList<>();
		final String lastToken            = parseResult.getLastToken();
		final boolean unrestricted        = lastToken.endsWith("(") || lastToken.endsWith(("."));

		for (final AbstractHint hint : allHints) {

			if (!hint.isHidden()) {

				final String functionName = getFunctionName(hint.getReplacement());
				final String displayName  = getFunctionName(hint.getDisplayName());

				if ((unrestricted || displayName.startsWith(lastToken)) && ( (!script.endsWith(functionName)) || (script.endsWith(functionName) && hint instanceof Function) )) {

					hints.add(hint.toGraphObject());
				}
			}
		}

		return hints;
	}

	protected AbstractHint createKeywordHint(final String name, final String description, final String replacement) {

		return new KeywordHint(name, description, replacement);
	}

	protected void handleJSExpression(final ActionContext actionContext, final GraphObject currentNode, final String expression, final List<AbstractHint> hints, final ParseResult result) {

		final String[] expressionParts = StringUtils.splitPreserveAllTokens(expression, ".(");
		final List<String> tokens      = Arrays.asList(expressionParts);
		final int length               = expressionParts.length;

		if (length > 1) {

			handleTokens(actionContext, tokens, currentNode, hints, result);

		} else {

			addAllHints(actionContext, hints);
		}

		result.setExpression(expression);
	}

	protected void addAllHints(final ActionContext actionContext, final List<AbstractHint> hints) {

		for (final Function<Object, Object> func : Functions.getFunctions()) {
			hints.add(func);
		}

		hints.add(new IncludeJSFunction(actionContext));
		hints.add(new DoInNewTransactionFunction(actionContext, null));
		hints.add(new DoAsFunction(actionContext));
		hints.add(new DoPrivilegedFunction(actionContext));

		// sort current hints = only built-in functions
		// sort case-insensitive so POST and GET etc do not show up at the top
		Collections.sort(hints, comparator);

		// Important: Order is reverse alphanumeric, so add(0, ...) means insert at the beginning.
		// If you change something here, make sure to change AutocompleteTest.java accordingly.

		// add keywords, keep in sync and include everything from StructrBinding.getMemberKeys()
		hints.add(0, createKeywordHint("this",                "The current object", "this"));
		hints.add(0, createKeywordHint("session",             "The current session", "session"));
		hints.add(0, createKeywordHint("response",            "The current response",       "response"));
		hints.add(0, createKeywordHint("request",             "The current request", "request"));
		hints.add(0, createKeywordHint("predicate",           "Search predicate", "predicate"));
		hints.add(0, createKeywordHint("page",                "The current page",           "page"));
		hints.add(0, createKeywordHint("methodParameters",    "Access method parameters", "methodParameters"));
		hints.add(0, createKeywordHint("me",                  "The current user", "me"));
		hints.add(0, createKeywordHint("locale",              "The current locale",         "locale"));
		hints.add(0, createKeywordHint("current",             "The current details object", "current"));
		hints.add(0, createKeywordHint("cache",               "Time-based cache object", "cache"));
		hints.add(0, createKeywordHint("batch",               "Open a batch transaction context", "batch"));
		hints.add(0, createKeywordHint("applicationStore",    "The application store", "applicationStore"));

		// add global schema methods to show at the start of the list
		try (final Tx tx = StructrApp.getInstance().tx()) {

			final List<SchemaMethod> methods = StructrApp.getInstance().nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null).sort(SchemaMethod.name, true).getAsList();

			for (final SchemaMethod method : methods) {
				hints.add(0, new UserDefinedFunctionHint(method.getName(), method.getProperty(SchemaMethod.summary), method.getProperty(SchemaMethod.description)));
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	protected void addNonempty(final List<String> list, final String string) {

		if (StringUtils.isNotBlank(string)) {
			list.add(string);
		}
	}

	protected void addHintsForType(final ActionContext actionContext, final Class type, final List<AbstractHint> hints, final ParseResult result) {

		final List<AbstractHint> methodHints = new LinkedList<>();

		try (final Tx tx = StructrApp.getInstance().tx()) {

			// entity properties
			final List<GraphObjectMap> typeInfo = SchemaHelper.getSchemaTypeInfo(actionContext.getSecurityContext(), type.getSimpleName(), type, PropertyView.All);
			for (final GraphObjectMap property : typeInfo) {

				final Map<String, Object> map = property.toMap();
				final String name             = (String)map.get("jsonName");
				final String propertyType     = (String)map.get("uiType");
				final String declaringClass   = (String)map.get("declaringClass");

				// skip properties defined in NodeInterface class, except for name
				if (NodeInterface.class.getSimpleName().equals(declaringClass) && !"name".equals(name)) {
					continue;
				}

				// filter inherited properties (except name)
				//if (!type.getSimpleName().equals(declaringClass) && !"name".equals(name)) {
				//	continue;
				//}

				hints.add(new PropertyHint(name, propertyType));
			}

			// entity methods
			// go into their own collection, are sorted and the appended to the list
			final Collection<AbstractMethod> methods = Methods.getAllMethods(type).values();
			for (final AbstractMethod method : methods) {

				final String name              = method.getName();
				final String methodSummary     = method.getSummary();
				final String methodDescription = method.getDescription();

				methodHints.add(new MethodHint(name, methodSummary, methodDescription));
			}

			Collections.sort(methodHints, comparator);

			tx.success();

		} catch (FrameworkException ex) {
			JavascriptHintProvider.logger.error(ExceptionUtils.getStackTrace(ex));
		}

		Collections.sort(hints, comparator);

		// add sorted method hints after the other hints
		hints.addAll(methodHints);
	}

	protected boolean handleTokens(final ActionContext actionContext, final List<String> tokens, final GraphObject currentNode, final List<AbstractHint> hints, final ParseResult result) {

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
						addAllHints(actionContext, hints);
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

						final List<AbstractHint> contextHints = func.getContextHints(result.getLastToken());
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

	// ----- nested classes -----
	protected static class HintComparator implements Comparator<AbstractHint> {

		@Override
		public int compare(final AbstractHint o1, final AbstractHint o2) {

			final boolean firstIsDynamic  = o1.isDynamic();
			final boolean secindIsDynamic = o2.isDynamic();

			if (firstIsDynamic && !secindIsDynamic) {
				return -1;
			}

			if (!firstIsDynamic && secindIsDynamic) {
				return 1;
			}

			return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
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
