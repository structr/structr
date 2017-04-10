/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.StaticValue;
import org.structr.core.Value;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionContext;

public class ToGraphObjectFunction extends UiFunction {

	public static final String ERROR_MESSAGE_TO_GRAPH_OBJECT    = "Usage: ${to_graph_object(obj [, view[, depth = 3]])}. Example: ${to_graph_object(this, 'public', 4)}";
	public static final String ERROR_MESSAGE_TO_GRAPH_OBJECT_JS = "Usage: ${{Structr.to_graph_object(obj [, view[, depth = 3]])}}. Example: ${{Structr.to_graph_object(Structr.get('this'), 'public', 4)}}";

	@Override
	public String getName() {
		return "to_graph_object()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length >= 1 && sources.length <= 3) {

			try {

				final SecurityContext securityContext = ctx.getSecurityContext();

				final Value<String> view = new StaticValue<>("public");
				if (sources.length > 1) {

					view.set(securityContext, sources[1].toString());
				}

				int outputDepth = 3;
				if (sources.length > 2 && sources[2] instanceof Number) {
					outputDepth = ((Number)sources[2]).intValue();
				}

				if (sources[0] instanceof GraphObject) {

					return sources[0];

				} else if (sources[0] instanceof List) {

					final List list = (List)sources[0];
					List<GraphObject> res = new ArrayList<>();

					for(Object o : list){

						if (o instanceof Map) {

							GraphObjectMap newObj = new GraphObjectMap();

							this.recursivelyConvertMapToGraphObjectMap(newObj, (Map)o, outputDepth);

							res.add(newObj);

						} else if (o instanceof GraphObjectMap) {

							res.add((GraphObjectMap)o);

						} else if (o instanceof String) {

							final GraphObjectMap stringWrapperObject = new GraphObjectMap();

							stringWrapperObject.put(new StringProperty("value"), o);

							res.add(stringWrapperObject);

						}

					}

					return res;


				} else if (sources[0] instanceof Map) {

					final GraphObjectMap map  = new GraphObjectMap();

					this.recursivelyConvertMapToGraphObjectMap(map, (Map)sources[0], outputDepth);

					return map;

				}

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
		return (inJavaScriptContext ? ERROR_MESSAGE_TO_GRAPH_OBJECT_JS : ERROR_MESSAGE_TO_GRAPH_OBJECT);
	}

	@Override
	public String shortDescription() {
		return "Converts the given entity to GraphObjectMap";
	}

}
