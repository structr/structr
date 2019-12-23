/**
 * Copyright (C) 2010-2019 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.autocomplete;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.CaseHelper;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.Function;
import org.structr.schema.action.Hint;

/**
 *
 *
 */
public class JavascriptHintProvider extends AbstractHintProvider {

	private static final Logger logger = LoggerFactory.getLogger(JavascriptHintProvider.class);

	@Override
	protected List<Hint> getAllHints(final SecurityContext securityContext, final GraphObject currentNode, final String editorText, final ParseResult result) {

		final List<String> tokens = result.getTokens();
		final String[] lines      = editorText.split("\n");
		final String lastLine     = lines[lines.length - 1];
		final String trimmed      = lastLine.trim();
		final int length          = trimmed.length();
		final StringBuilder buf   = new StringBuilder();

		for (int i=length-1; i>=0; i--) {

			final char c = trimmed.charAt(i);
			switch (c) {

				case '[':
				case ']':
				case '{':
				case '}':
				case ' ':
				case ',':
				case ';':
				case ':':
				case '=':
				case '\'':
				case '"':
				case ')':
				case '(':
				case '?':
				case '&':
				case '|':
				case '\\':
				case '/':
				case '+':
				case '-':
				case '*':
				case '!':
				case '#':
				case '~':
				case '.':
					addNonempty(tokens, buf.toString());
					buf.setLength(0);
					buf.append(c);
					break;

				default:
					buf.insert(0, c);
					break;
			}
		}

		addNonempty(tokens, buf.toString());

		Collections.reverse(tokens);

		final List<Hint> hints  = new LinkedList<>();
		final int tokenCount    = tokens.size();
		int startTokenIndex     = -1;

		// try to find a starting point ($. or Structr.) for the expression
		for (int i=tokenCount-1; i>=0; i--) {

			final String token = tokens.get(i);

			if ("$.".equals(token) || "Structr.".equals(token)) {
				startTokenIndex = i;
				break;
			}
		}

		// did we find ($. or Structr.) at all?
		if (startTokenIndex >= 0) {

			final String expression = StringUtils.join(tokens.subList(startTokenIndex, tokenCount), "");

			result.setExpression(expression);

			final String[] expressionParts = expression.split("[\\.'\"\\(]+");

			// just a single $. or Structr.
			if (expressionParts.length == 1) {

				// add functions
				for (final Function<Object, Object> func : Functions.getFunctions()) {
					hints.add(func);
				}

				// sort hints
				Collections.sort(hints, comparator);

				// add keywords
				hints.add(0, createHint("this",     "", "The current object",         "this"));
				hints.add(0, createHint("response", "", "The current response",       "response"));
				hints.add(0, createHint("request",  "", "The current request",        "request"));
				hints.add(0, createHint("page",     "", "The current page",           "page"));
				hints.add(0, createHint("me",       "", "The current user",           "me"));
				hints.add(0, createHint("locale",   "", "The current locale",         "locale"));
				hints.add(0, createHint("current",  "", "The current details object", "current"));

				result.setUnrestricted(true);

			}

			// $. with incomplete or complete selection of a
			if (expressionParts.length == 2) {

				if (expression.endsWith(".")) {

					final String token = expressionParts[1];

					if (handleToken(securityContext, token, currentNode, hints, result)) {

						result.setUnrestricted(true);
					}

				} else if ("retrieve".equals(expressionParts[1]) || "retrieve".equals(expressionParts[1])) {

					addHintsForRetrieve(securityContext, hints, result);

					result.setUnrestricted(true);

				} else {

					// part is not complete, use full list and filter in postprocessing
					// add functions
					for (final Function<Object, Object> func : Functions.getFunctions()) {
						hints.add(func);
					}

					// sort hints
					Collections.sort(hints, comparator);

					// add keywords
					hints.add(0, createHint("this",     "", "The current object",         "this"));
					hints.add(0, createHint("response", "", "The current response",       "response"));
					hints.add(0, createHint("request",  "", "The current request",        "request"));
					hints.add(0, createHint("page",     "", "The current page",           "page"));
					hints.add(0, createHint("me",       "", "The current user",           "me"));
					hints.add(0, createHint("locale",   "", "The current locale",         "locale"));
					hints.add(0, createHint("current",  "", "The current details object", "current"));
				}

			}

			if (expressionParts.length == 3) {

				// third token is incomplete, we're interested in the second token only
				final String token = expressionParts[1];

				handleToken(securityContext, token, currentNode, hints, result);
			}
		}

		return hints;
	}

	@Override
	protected String getFunctionName(final String source) {

		if (source.contains("_")) {
			return CaseHelper.toLowerCamelCase(source);
		}

		return source;
	}

	@Override
	protected String visitReplacement(final String replacement) {
		return replacement;
	}

	// ----- private methods -----
	private void addHintsForType(final SecurityContext securityContext, final Class type, final List<Hint> hints, final ParseResult result) {

		try {

			final List<GraphObjectMap> typeInfo = SchemaHelper.getSchemaTypeInfo(securityContext, type.getSimpleName(), type, PropertyView.All);

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
				if (!type.getSimpleName().equals(declaringClass) && !"name".equals(name)) {
					continue;
				}

				hints.add(createHint(name, className, propertyType));
			}

		} catch (FrameworkException ex) {
			java.util.logging.Logger.getLogger(JavascriptHintProvider.class.getName()).log(Level.SEVERE, null, ex);
		}

		Collections.sort(hints, comparator);
	}

	private void addHintsForCurrentObject(final SecurityContext securityContext, final GraphObject currentNode, final List<Hint> hints, final ParseResult result) {

		final SchemaMethod method     = (SchemaMethod)currentNode;
		final AbstractSchemaNode node = method.getProperty(SchemaMethod.schemaNode);

		if (node != null) {

			final Class type = StructrApp.getConfiguration().getNodeEntityClass(node.getClassName());

			try {

				final List<GraphObjectMap> typeInfo = SchemaHelper.getSchemaTypeInfo(securityContext, type.getSimpleName(), type, PropertyView.All);

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

					hints.add(createHint(name, className, propertyType));
				}

			} catch (FrameworkException ex) {
				java.util.logging.Logger.getLogger(JavascriptHintProvider.class.getName()).log(Level.SEVERE, null, ex);
			}

			Collections.sort(hints, comparator);
		}
	}

	private void addHintsForRetrieve(final SecurityContext securityContext, final List<Hint> hints, final ParseResult result) {

		// find keys currently stored in context
		for (final String key : securityContext.getContextStore().getConstantKeys()) {

			hints.add(createHint(key, "", "key from store()"));
		}

		Collections.sort(hints, comparator);
	}

	private boolean handleToken(final SecurityContext securityContext, final String token, final GraphObject currentNode, final List<Hint> hints, final ParseResult result) {

		if ("this".equals(token) && currentNode instanceof SchemaMethod) {

			addHintsForCurrentObject(securityContext, currentNode, hints, result);

			return true;
		}

		if ("retrieve".equals(token)) {

			addHintsForRetrieve(securityContext, hints, result);

			return true;
		}

		if ("page".equals(token)) {

			addHintsForType(securityContext, StructrApp.getConfiguration().getNodeEntityClass("Page"), hints, result);

			return true;
		}

		if ("me".equals(token)) {

			// fixme: not the right property set for users... :(
			addHintsForType(securityContext, StructrApp.getConfiguration().getNodeEntityClass("User"), hints, result);

			return true;
		}

		return false;
	}
}
