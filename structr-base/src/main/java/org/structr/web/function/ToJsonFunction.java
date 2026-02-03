/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class ToJsonFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "toJson";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("obj [, view, depth = 3, serializeNulls = true ]");
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

				} else if (obj instanceof Iterable list) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, true, serializeNulls);

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>("toJson()", list), null, false);

				} else if (obj instanceof Map) {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, false, serializeNulls);
					final GraphObjectMap map               = new GraphObjectMap();

					UiFunction.recursivelyConvertMapToGraphObjectMap(map, (Map)obj, outputDepth);

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>("toJson()", List.of(map)), null, false);

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
	public List<Usage> getUsages() {
		return List.of(
				Usage.structrScript("Usage: ${toJson(obj [, view[, depth = 3[, serializeNulls = true ]]])}."),
				Usage.javaScript("Usage: ${{$.toJson(obj [, view[, depth = 3[, serializeNulls = true ]]])}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Serializes the given object to JSON.";
	}

	@Override
	public String getLongDescription() {
		return """
		Returns a JSON string representation of the given object very similar to `JSON.stringify()` in JavaScript.
		The output of this method will be very similar to the output of the REST server except for the response 
		headers and the result container. The optional `view` parameter can be used to select the view representation 
		of the entity. If no view is given, the `public` view is used. The optional `depth` parameter defines 
		at which depth the JSON serialization stops. If no depth is given, the default value of 3 is used.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${ toJson(find('MyData'), 'public', 4) }"),
				Example.javaScript("${{$.toJson($.this, 'public', 4)}}")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("source", "object or collection"),
				Parameter.optional("view", "view (default: `public`)"),
				Parameter.optional("depth", "conversion depth (default: 3)"),
				Parameter.optional("serializeNulls", "nulled keep properties (default: true)")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"For database objects this method is preferrable to `JSON.stringify()` because a view can be chosen. `JSON.stringify()` will only return the `id` and `type` property for nodes."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}
}
