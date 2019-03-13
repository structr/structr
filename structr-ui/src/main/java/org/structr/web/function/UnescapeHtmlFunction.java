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
package org.structr.web.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.dom.DOMNode;

public class UnescapeHtmlFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_UNESCAPE_HTML    = "Usage: ${unescape_html(text)}. Example: ${unescape_html(\"test &amp; test\")}";
	public static final String ERROR_MESSAGE_UNESCAPE_HTML_JS = "Usage: ${{Structr.unescape_html(text)}}. Example: ${{Structr.unescape_html(\"test &amp; test\")}}";

	@Override
	public String getName() {
		return "unescape_html";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return DOMNode.unescapeForHtmlAttributes(sources[0].toString());

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_UNESCAPE_HTML_JS : ERROR_MESSAGE_UNESCAPE_HTML);
	}

	@Override
	public String shortDescription() {
		return "";
	}
}
