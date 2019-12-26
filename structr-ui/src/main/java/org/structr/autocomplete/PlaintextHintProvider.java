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

			handleValueExpression(securityContext, (ValueExpression)last, currentNode, hints, result);
		}

		if (last instanceof FunctionExpression) {

			// what to do?
			addAllHints(hints);
			result.setUnrestricted(true);
		}

		return hints;
	}

	@Override
	protected String getFunctionName(String sourceName) {
		return sourceName;
	}

	// ----- private methods -----
	private void handleValueExpression(final SecurityContext securityContext, final ValueExpression expression, final GraphObject currentNode, final List<Hint> hints, final ParseResult result) {

		final String keyword = expression.getKeyword();

		// keyword can consist of multiple keywords, separated by ".", so we need to
		// split the list and resolve the path
		final String[] parts = keyword.split("[\\.]+", -1);
		final int length     = parts.length;

		if (length > 1) {

			// replace tokens in result (must be split by ".")
			result.getTokens().clear();
			result.getTokens().addAll(Arrays.asList(parts));

			// evaluate first part only for now..
			if (handleToken(securityContext, parts[0], currentNode, hints, result)) {

				// odd number of parts?
				result.setUnrestricted(length % 2 == 1);
			}

		} else {

			addAllHints(hints);
			result.setUnrestricted(keyword.endsWith("."));
		}
	}

}
