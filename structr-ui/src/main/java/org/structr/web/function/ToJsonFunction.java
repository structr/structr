/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import org.mozilla.javascript.Wrapper;
import org.structr.api.config.Settings;
import org.structr.api.util.PagingIterable;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.StaticValue;
import org.structr.core.Value;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;

public class ToJsonFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_TO_JSON    = "Usage: ${to_json(obj [, view[, depth = 3]])}. Example: ${to_json(this, 'public', 4)}";
	public static final String ERROR_MESSAGE_TO_JSON_JS = "Usage: ${{Structr.to_json(obj [, view[, depth = 3]])}}. Example: ${{Structr.to_json(Structr.get('this'), 'public', 4)}}";

	@Override
	public String getName() {
		return "to_json";
	}

	@Override
	public String getSignature() {
		return "obj [, view, depth = 3 ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length >= 1 && sources.length <= 3) {

			try {

				final SecurityContext securityContext = ctx.getSecurityContext();
				final StringWriter writer             = new StringWriter();

				final Value<String> view = new StaticValue<>("public");
				if (sources.length > 1) {

					view.set(securityContext, sources[1].toString());
				}

				int outputDepth = Settings.RestOutputDepth.getValue();
				if (sources.length > 2 && sources[2] instanceof Number) {
					outputDepth = ((Number)sources[2]).intValue();
				}

				final Object obj = (sources[0] instanceof Wrapper) ? ((Wrapper)sources[0]).unwrap() : sources[0];

				if (obj instanceof GraphObject) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, true, outputDepth, false);

					jsonStreamer.streamSingle(securityContext, writer, (GraphObject)obj);

				} else if (obj instanceof Iterable) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, true, outputDepth, true);
					final Iterable list                    = (Iterable)obj;

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>(list), null, false);

				} else if (obj instanceof Map) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, true, outputDepth, false);
					final GraphObjectMap map               = new GraphObjectMap();

					UiFunction.recursivelyConvertMapToGraphObjectMap(map, (Map)obj, outputDepth);

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>(Arrays.asList(map)), null, false);
				}

				return writer.getBuffer().toString();


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
		return (inJavaScriptContext ? ERROR_MESSAGE_TO_JSON_JS : ERROR_MESSAGE_TO_JSON);
	}

	@Override
	public String shortDescription() {
		return "Serializes the given entity to JSON";
	}

}
