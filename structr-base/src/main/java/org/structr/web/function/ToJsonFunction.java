/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.function;

import org.structr.api.config.Settings;
import org.structr.api.util.PagingIterable;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

public class ToJsonFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_TO_JSON    = "Usage: ${to_json(obj [, view[, depth = 3[, serializeNulls = true ]]])}. Example: ${to_json(this, 'public', 4)}";
	public static final String ERROR_MESSAGE_TO_JSON_JS = "Usage: ${{Structr.to_json(obj [, view[, depth = 3[, serializeNulls = true ]]])}}. Example: ${{Structr.to_json(Structr.get('this'), 'public', 4)}}";

	@Override
	public String getName() {
		return "to_json";
	}

	@Override
	public String getSignature() {
		return "obj [, view, depth = 3, serializeNulls = true ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length >= 1 && sources.length <= 4) {

			try {

				final SecurityContext securityContext = ctx.getSecurityContext();
				final StringWriter writer             = new StringWriter();

				final String view            = (sources.length > 1) ? sources[1].toString() : PropertyView.Public;
				final int outputDepth        = (sources.length > 2 && sources[2] instanceof Number) ? ((Number)sources[2]).intValue() : Settings.RestOutputDepth.getValue();
				final boolean serializeNulls = (sources.length > 3 && sources[3] instanceof Boolean) ? ((Boolean)sources[3]) : true;

				final boolean returnRawResultWasEnabled = securityContext.returnRawResult();

				// prevent "result" wrapper from being introduced
				securityContext.enableReturnRawResult();

				final Object obj = sources[0];

				if (obj instanceof GraphObject) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, false, serializeNulls);

					jsonStreamer.streamSingle(securityContext, writer, (GraphObject)obj);

				} else if (obj instanceof Iterable) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, true, serializeNulls);
					final Iterable list                    = (Iterable)obj;

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>("toJson()", list), null, false);

				} else if (obj instanceof Map) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, false, serializeNulls);
					final GraphObjectMap map               = new GraphObjectMap();

					UiFunction.recursivelyConvertMapToGraphObjectMap(map, (Map)obj, outputDepth);

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>("toJson()", Arrays.asList(map)), null, false);

				} else if (obj instanceof String) {

					return "\"" + ((String) obj).replaceAll("\"", "\\\\\"") + "\"";

				} else if (obj instanceof Number) {

					return obj;

				}

				if (Boolean.FALSE.equals(returnRawResultWasEnabled)) {
					securityContext.disableReturnRawResult();
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
		return "Serializes the given object to JSON";
	}
}
