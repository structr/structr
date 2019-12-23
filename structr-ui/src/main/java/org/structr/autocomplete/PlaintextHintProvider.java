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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.Function;
import org.structr.schema.action.Hint;

/**
 *
 *
 */
public class PlaintextHintProvider extends AbstractHintProvider {

	private static final Logger logger = LoggerFactory.getLogger(PlaintextHintProvider.class);

	@Override
	protected List<Hint> getAllHints(final SecurityContext securityContext, final GraphObject currentNode, final String editorText, final ParseResult parseResult) {

		final List<String> tokens = parseResult.getTokens();
		final StringBuilder buf   = new StringBuilder();
		final List<Hint> hints    = new LinkedList<>();
		final String[] lines      = editorText.split("\n");
		final String lastLine     = lines[lines.length - 1];
		final String trimmed      = lastLine.trim();
		final int length          = trimmed.length();

		for (int i=length-1; i>=0; i--) {

			final char c = trimmed.charAt(i);
			switch (c) {

				case '\'':
				case '"':
				case ')':
				case '(':
				case '$':
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

		// add functions
		for (final Function<Object, Object> func : Functions.getFunctions()) {
			hints.add(func);
		}

		// sort hints before prepending keywords
		Collections.sort(hints, comparator);

		// add keywords
		hints.add(0, createHint("this",     "", "The current object",         "this"));
		hints.add(0, createHint("response", "", "The current response",       "response"));
		hints.add(0, createHint("request",  "", "The current request",        "request"));
		hints.add(0, createHint("page",     "", "The current page",           "page"));
		hints.add(0, createHint("me",       "", "The current user",           "me"));
		hints.add(0, createHint("locale",   "", "The current locale",         "locale"));
		hints.add(0, createHint("current",  "", "The current details object", "current"));

		return hints;
	}

	@Override
	protected String getFunctionName(String sourceName) {
		return sourceName;
	}
}
