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
package org.structr.autocomplete;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.function.ParseResult;
import org.structr.docs.Documentable;
import org.structr.schema.action.ActionContext;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public class JavascriptHintProvider extends AbstractHintProvider {

	protected static final Logger logger = LoggerFactory.getLogger(JavascriptHintProvider.class);

	@Override
	protected List<Documentable> getAllHints(final ActionContext actionContext, final GraphObject currentNode, final String editorText, final ParseResult result) {

		final List<String> tokens = result.getTokens();
		final String[] lines      = editorText.split("\n");
		final String lastLine     = lines[lines.length - 1];
		final String trimmed      = lastLine.trim();
		final int length          = trimmed.length();
		final StringBuilder buf   = new StringBuilder();

		// This loop splits the editor text into text- and non-text tokens while
		// preserving the separator chars (which String.split() doesn't provide).
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

		// The resulting token list needs to be reversed since we go backwards through the text.
		Collections.reverse(tokens);

		final List<Documentable> hints = new LinkedList<>();
		final int tokenCount           = tokens.size();
		int startTokenIndex            = -1;

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

			handleJSExpression(actionContext, currentNode, expression, hints, result);
		}

		return hints;
	}

	// TODO: move to appropriate place (BuiltinFunctionHint !?)
	@Override
	protected String getFunctionName(final String source) {

		if (source.contains("_")) {
			return CaseHelper.toLowerCamelCase(source);
		}

		return source;
	}
}
