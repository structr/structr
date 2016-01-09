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
package org.structr.function;

import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class AddHeaderFunction extends UiFunction {

	public static final String ERROR_MESSAGE_ADD_HEADER    = "Usage: ${add_header(field, value)}. Example: ${add_header('X-User', 'johndoe')}";
	public static final String ERROR_MESSAGE_ADD_HEADER_JS = "Usage: ${{Structr.add_header(field, value)}}. Example: ${{Structr.add_header('X-User', 'johndoe')}}";

	@Override
	public String getName() {
		return "add_header()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length == 2) {

			final String name = sources[0].toString();
			final String value = sources[1].toString();

			ctx.addHeader(name, value);

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ADD_HEADER_JS : ERROR_MESSAGE_ADD_HEADER);
	}

	@Override
	public String shortDescription() {
		return "Adds the given header field and value to the next request";
	}

}
