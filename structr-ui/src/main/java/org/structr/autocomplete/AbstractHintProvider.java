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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.Hint;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Content.ContentHandler;



public abstract class AbstractHintProvider {

	private enum QueryType {
		REST, Cypher, XPath, Function
	}

	public static final Property<String> displayText = new StringProperty("displayText");
	public static final Property<String> text        = new StringProperty("text");
	public static final Property<GraphObject> from   = new GenericProperty("from");
	public static final Property<GraphObject> to     = new GenericProperty("to");
	public static final Property<Integer> line       = new IntProperty("line");
	public static final Property<Integer> ch         = new IntProperty("ch");

	protected abstract List<Hint> getAllHints(final SecurityContext securityContext, final GraphObject currentNode, final String editorText, final ParseResult parseResult);
	protected abstract String getFunctionName(final String sourceName);

	protected final Comparator comparator     = new HintComparator();

	public static List<GraphObject> getHints(final SecurityContext securityContext, final GraphObject currentEntity, final String type, final String textBefore, final String textAfter, final int cursorLine, final int cursorPosition) {

		String text = null;

		if (currentEntity instanceof SchemaMethod) {

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
				t.printStackTrace();
			}
		}

		// handle text for autocompletion
		if (text != null) {

			if (text.startsWith("${{")) {

				final JavascriptHintProvider provider = new JavascriptHintProvider();
				final String source                   = text.substring(3);

				return provider.getHints(securityContext, currentEntity, type, source, cursorLine, cursorPosition);

			} else if (text.startsWith("${")) {

				final PlaintextHintProvider provider = new PlaintextHintProvider();
				final String source                   = text.substring(2);

				return provider.getHints(securityContext, currentEntity, type, source, cursorLine, cursorPosition);
			}
		}

		return Collections.EMPTY_LIST;
	}

	public List<GraphObject> getHints(final SecurityContext securityContext, final GraphObject currentEntity, final String type, final String script, final int cursorLine, final int cursorPosition) {

		final ParseResult parseResult = new ParseResult();
		final List<Hint> allHints     = getAllHints(securityContext, currentEntity, script, parseResult);
		final List<GraphObject> hints = new LinkedList<>();
		final String currentToken     = parseResult.getLastToken();
		int maxNameLength             = 0;

		if (parseResult.isUnrestricted()) {

			// display all possible hints
			for (final Hint hint : allHints) {

				final GraphObjectMap item = new GraphObjectMap();
				final String displayName  = getFunctionName(hint.getDisplayName());
				final String functionName = getFunctionName(hint.getReplacement());

				if (hint.mayModify()) {

					item.put(text, visitReplacement(functionName));

				} else {

					item.put(text, functionName);
				}

				item.put(displayText, displayName + " - " + textOrPlaceholder(hint.shortDescription()));
				addPosition(item, hint, cursorLine, cursorPosition, cursorPosition);

				if (functionName.length() > maxNameLength) {
					maxNameLength = functionName.length();
				}

				hints.add(item);
			}

		} else {

			final int currentTokenLength = currentToken.length();

			for (final Hint hint : allHints) {

				final String functionName = getFunctionName(hint.getReplacement());
				final String displayName  = getFunctionName(hint.getDisplayName());

				if (functionName.startsWith(currentToken)) {

					final GraphObjectMap item = new GraphObjectMap();

					if (hint.mayModify()) {

						item.put(text, visitReplacement(functionName));

					} else {

						item.put(text, functionName);
					}

					item.put(displayText, displayName + " - " + textOrPlaceholder(hint.shortDescription()));

					addPosition(item, hint, cursorLine, cursorPosition - currentTokenLength, cursorPosition);

					if (functionName.length() > maxNameLength) {
						maxNameLength = functionName.length();
					}

					hints.add(item);
				}
			}
		}

		alignHintDescriptions(hints, maxNameLength);

		return hints;
	}

	protected String visitReplacement(final String replacement) {
		return replacement;
	}

	protected Hint createHint(final String name, final String signature, final String description) {
		return createHint(name, signature, description, null);
	}

	protected Hint createHint(final String name, final String signature, final String description, final String replacement) {

		final NonFunctionHint hint = new NonFunctionHint() {

			@Override
			public String shortDescription() {
				return description;
			}

			@Override
			public String getName() {
				return name;
			}
		};

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

			if (pos < maxNameLength) {

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

	protected void addNonempty(final List<String> list, final String string) {

		if (StringUtils.isNotBlank(string)) {
			list.add(string);
		}
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

	protected static class ParseResult {

		private List<String> tokens  = new ArrayList<>();
		private String expression    = null;
		private boolean unrestricted = false;

		public List<String> getTokens() {
			return tokens;
		}

		public void setExpression(final String expression) {
			this.expression = expression;
		}

		public String getExpression() {
			return expression;
		}

		public String getLastToken() {

			if (tokens.isEmpty()) {
				return "";
			}

			return tokens.get(tokens.size() - 1);
		}

		public void setUnrestricted(final boolean unrestricted) {
			this.unrestricted = unrestricted;
		}

		public boolean isUnrestricted() {
			return unrestricted;
		}
	}

	protected static class AutocompleteContentHandler implements ContentHandler {

		private boolean inScript  = false;
		private String scriptText = null;

		@Override
		public void handleScript(String script) throws FrameworkException, IOException {
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
