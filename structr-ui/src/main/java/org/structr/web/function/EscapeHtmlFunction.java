/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 */
public class EscapeHtmlFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ESCAPE_HTML    = "Usage: ${escape_html(text)}. Example: ${escape_html(\"test & test\")}";
	public static final String ERROR_MESSAGE_ESCAPE_HTML_JS = "Usage: ${{Structr.escape_html(text)}}. Example: ${{Structr.escape_html(\"test & test\")}}";

	@Override
	public String getName() {
		return "escape_html()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		try {
			if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {
				
				return null;
			}

			return DOMNode.escapeForHtmlAttributes(sources[0].toString());

		} catch (final IllegalArgumentException e) {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ESCAPE_HTML_JS : ERROR_MESSAGE_ESCAPE_HTML);
	}

	@Override
	public String shortDescription() {
		return "";
	}

}
