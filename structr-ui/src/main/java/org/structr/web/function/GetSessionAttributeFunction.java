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

/**
 *
 */
public class GetSessionAttributeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET_SESSION_ATTRIBUTE    = "Usage: ${get_session_attribute(key, object)}. Example: ${get_session_attribute(\"do_no_track\")}";
	public static final String ERROR_MESSAGE_GET_SESSION_ATTRIBUTE_JS = "Usage: ${{Structr.get_session_attribute(key, object)}}. Example: ${{Structr.get_session_attribute(\"do_not_track\")}}";

	@Override
	public String getName() {
		return "get_session_attribute()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			return ctx.getSecurityContext().getSession().getAttribute(sources[0].toString());

		} else {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

		}

		return "";

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_SESSION_ATTRIBUTE_JS : ERROR_MESSAGE_GET_SESSION_ATTRIBUTE);
	}

	@Override
	public String shortDescription() {
		return "";
	}

}