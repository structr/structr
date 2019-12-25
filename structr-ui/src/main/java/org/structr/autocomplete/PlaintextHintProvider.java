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

import org.structr.core.function.ParseResult;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.core.parser.Expression;
import org.structr.core.parser.FunctionExpression;
import org.structr.core.parser.RootExpression;
import org.structr.core.parser.ValueExpression;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Hint;

/**
 *
 *
 */
public class PlaintextHintProvider extends AbstractHintProvider {

	private static final Logger logger = LoggerFactory.getLogger(PlaintextHintProvider.class);

	@Override
	protected List<Hint> getAllHints(final SecurityContext securityContext, final GraphObject currentNode, final String editorText, final ParseResult result) {

		final List<Hint> hints    = new LinkedList<>();
		final ActionContext ctx   = new ActionContext(securityContext);

		try {

			// parse function but ignore exceptions, we're only interested in the expression structure
			Functions.parse(ctx, currentNode, editorText, result);

		} catch (FrameworkException ignore) { }

		final Expression root = result.getRootExpression();
		Expression last       = root;
		boolean found         = true;

		while (found) {

			found = false;

			final List<Expression> children = last.getChildren();
			if (children != null && !children.isEmpty()) {

				last = children.get(children.size() - 1);
				found = true;
			}
		}

		if (last instanceof RootExpression) {

			addAllHints(hints);
			result.setUnrestricted(true);
		}

		if (last instanceof ValueExpression) {

			addAllHints(hints);
			result.setUnrestricted(false);
		}

		if (last instanceof FunctionExpression) {

			// what to do?
			addAllHints(hints);
			result.setUnrestricted(true);
		}

		return hints;
	}

	/*
	@Override
	protected List<Hint> getAllHints(final SecurityContext securityContext, final GraphObject currentNode, final String editorText, final ParseResult result) {

		final List<String> tokens = result.getTokens();
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

		final int tokenCount    = tokens.size();
		int startTokenIndex     = 0;

		for (int i=tokenCount-1; i>=0; i--) {

			final String token = tokens.get(i);

			if (".".equals(token) || "(".equals(token)) {
				startTokenIndex = i;
				break;
			}
		}

		if (startTokenIndex >= 0) {

			final String expression = StringUtils.join(tokens.subList(startTokenIndex, tokenCount), "");

			System.out.println("##### expression: " + expression);
			System.out.println("##### tokens:     '" + StringUtils.join(tokens, "', '") + "'");

			handleSSExpression(securityContext, currentNode, expression, hints, result);
		}

		return hints;
	}
	*/

	@Override
	protected String getFunctionName(String sourceName) {
		return sourceName;
	}
}
