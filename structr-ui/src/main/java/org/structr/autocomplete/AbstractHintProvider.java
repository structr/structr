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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.Hint;



public abstract class AbstractHintProvider {

	private static final Set<String> startChars = new HashSet<>(Arrays.asList(new String[] { ".", ",", "(", "((", "(((", "((((", "(((((", "((((((", "${" } ));

	private enum QueryType {
		REST, Cypher, XPath, Function
	}

	public static final Property<String> displayText = new StringProperty("displayText");
	public static final Property<String> text        = new StringProperty("text");
	public static final Property<GraphObject> from   = new GenericProperty("from");
	public static final Property<GraphObject> to     = new GenericProperty("to");
	public static final Property<Integer> line       = new IntProperty("line");
	public static final Property<Integer> ch         = new IntProperty("ch");

	protected abstract List<Hint> getAllHints(final GraphObject currentNode, final String currentToken, final String previousToken, final String thirdToken);
	protected abstract String getFunctionName(final String sourceName);

	protected final Comparator comparator = new HintComparator();

	public List<GraphObject> getHints(final GraphObject currentEntity, final String type, final String currentToken, final String previousToken, final String thirdToken, final int cursorLine, final int cursorPosition) {

		final List<Hint> allHints     = getAllHints(currentEntity, currentToken, previousToken, thirdToken);
		final List<GraphObject> hints = new LinkedList<>();
		int maxNameLength             = 0;

		if (StringUtils.isBlank(currentToken) || startChars.contains(currentToken)) {

			// display all possible hints
			for (final Hint hint : allHints) {

				final GraphObjectMap item = new GraphObjectMap();
				final String functionName = getFunctionName(hint.getReplacement());

				if (hint.mayModify()) {

					item.put(text, visitReplacement(functionName));

				} else {

					item.put(text, functionName);
				}

				item.put(displayText, functionName + " - " + textOrPlaceholder(hint.shortDescription()));
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

				if (functionName.startsWith(currentToken)) {

					final GraphObjectMap item = new GraphObjectMap();

					if (hint.mayModify()) {

						item.put(text, visitReplacement(functionName));

					} else {

						item.put(text, functionName);
					}

					item.put(displayText, functionName + " - " + textOrPlaceholder(hint.shortDescription()));

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
}
