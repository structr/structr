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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.core.GraphObjectMap;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.microformat.MicroformatParser;

/**
 *
 */
public class ParseFunction extends UiFunction {

	public static final String ERROR_MESSAGE_PARSE    = "Usage: ${parse(URL, selector)}. Example: ${parse('http://structr.org', 'li.data')}";
	public static final String ERROR_MESSAGE_PARSE_JS = "Usage: ${{Structr.parse(URL, selector)}}. Example: ${{Structr.parse('http://structr.org', 'li.data')}}";

	@Override
	public String getName() {
		return "parse";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length == 2) {

			try {

				final String source = sources[0].toString();
				final String selector = sources[1].toString();
				final List<Map<String, Object>> objects = new MicroformatParser().parse(source, selector);
				final List<GraphObjectMap> elements = new LinkedList<>();

				for (final Map<String, Object> map : objects) {

					final GraphObjectMap obj = new GraphObjectMap();
					elements.add(obj);

					recursivelyConvertMapToGraphObjectMap(obj, map, 0);
				}

				return elements;

			} catch (Throwable t) {

				logException(caller, t, sources);

			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PARSE_JS : ERROR_MESSAGE_PARSE);
	}

	@Override
	public String shortDescription() {
		return "Parses the given string and returns an object";
	}

}
