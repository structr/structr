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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.core.function.ParseResult;
import org.structr.core.parser.*;
import org.structr.core.script.Snippet;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public class PlaintextHintProvider extends AbstractHintProvider {

	@Override
	protected List<AbstractHint> getAllHints(final ActionContext securityContext, final GraphObject currentNode, final String editorText, final ParseResult result) {

		// don't interpret invalid strings
		if (editorText != null && (editorText.endsWith("''") || editorText.endsWith("\"\""))) {
			return Collections.EMPTY_LIST;
		}

		final List<AbstractHint> hints = new LinkedList<>();
		final ActionContext ctx        = new ActionContext(securityContext);

		try {

			// parse function but ignore exceptions, we're only interested in the expression structure
			Functions.parse(ctx, currentNode, new Snippet("hint", editorText), result);

		} catch (FrameworkException ignore) { }

		final Expression root = result.getRootExpression();
		Expression last       = root;
		boolean found         = true;

		while (found) {

			found = false;

			final List<Expression> children = last.getChildren();
			if (children != null && !children.isEmpty()) {

				final Expression child = children.get(children.size() - 1);
				if (!(child instanceof ConstantExpression)) {

					// ignore constants
					last = children.get(children.size() - 1);
					found = true;
				}
			}
		}

		if (last instanceof RootExpression) {

			addAllHints(ctx, hints);
		}

		if (last instanceof ValueExpression) {

			handleValueExpression(securityContext, (ValueExpression)last, currentNode, hints, result);
		}

		if (last instanceof FunctionExpression) {

			final FunctionExpression fe           = (FunctionExpression)last;
			final List<AbstractHint> contextHints = fe.getContextHints();

			if (contextHints != null) {

				hints.addAll(contextHints);

				Collections.sort(hints, comparator);

			} else {

				addAllHints(ctx, hints);
			}
		}

		return hints;
	}

	@Override
	protected String getFunctionName(String sourceName) {
		return sourceName;
	}

	// ----- private methods -----
	private void handleValueExpression(final ActionContext actionContext, final ValueExpression expression, final GraphObject currentNode, final List<AbstractHint> hints, final ParseResult result) {

		final String keyword = expression.getKeyword();

		// keyword can consist of multiple keywords, separated by ".", so we need to
		// split the list and resolve the path
		final String[] parts = keyword.split("[\\.]+", -1);
		final int length     = parts.length;

		if (length > 1) {

			final String joined       = StringUtils.join(result.getTokens(), "");
			final String[] split      = StringUtils.splitPreserveAllTokens(joined, ".");
			final List<String> tokens = Arrays.asList(split);

			// replace tokens in result (must be split by ".")
			result.getTokens().clear();
			result.getTokens().addAll(Arrays.asList(parts));

			// evaluate first part only for now..
			handleTokens(actionContext, tokens, currentNode, hints, result);

		} else {

			addAllHints(actionContext, hints);
		}
	}

}
