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
public class HeadFunction extends UiFunction {

	public static final String ERROR_MESSAGE_HEAD    = "Usage: ${HEAD(URL[, username, password])}. Example: ${HEAD('http://structr.org', 'foo', 'bar')}";
	public static final String ERROR_MESSAGE_HEAD_JS = "Usage: ${{Structr.HEAD(URL[, username, password]])}}. Example: ${{Structr.HEAD('http://structr.org', 'foo', 'bar')}}";

	@Override
	public String getName() {
		return "HEAD()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length > 0) {

			try {

				String address = sources[0].toString();
				String username = null;
				String password = null;

				if (sources.length > 1) {
					username = sources[1].toString();
				}

				if (sources.length > 2) {
					password = sources[2].toString();
				}

				return headFromUrl(ctx, address, username, password);

			} catch (Throwable t) {
				t.printStackTrace();
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_HEAD_JS : ERROR_MESSAGE_HEAD);
	}

	@Override
	public String shortDescription() {
		return "Sends an HTTP HEAD request to the given URL and returns the response headers";
	}

}
