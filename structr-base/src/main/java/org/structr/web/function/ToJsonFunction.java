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

import com.google.gson.GsonBuilder;
import org.structr.api.config.Settings;
import org.structr.api.util.PagingIterable;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;
import org.structr.schema.parser.ZonedDateTimePropertyGenerator;

import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.Date;
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
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException{

		try {

			assertArrayHasMinLengthAndMaxLength(sources, 1, 4);

			final SecurityContext securityContext = ctx.getSecurityContext();
			final StringWriter writer = new StringWriter();
			final String view = (sources.length > 1) ? sources[1].toString() : PropertyView.Public;
			final int outputDepth = (sources.length > 2 && sources[2] instanceof Number) ? ((Number) sources[2]).intValue() : Settings.RestOutputDepth.getValue();
			final boolean serializeNulls = (sources.length > 3 && sources[3] instanceof Boolean) ? ((Boolean) sources[3]) : true;
			final boolean returnRawResultWasEnabled = securityContext.returnRawResult();

			// prevent "result" wrapper from being introduced when we are using StreamingJsonWriter
			securityContext.enableReturnRawResult();

			final Object obj = sources[0];

			switch (obj) {

				case GraphObject graphObject -> {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, false, serializeNulls);

					jsonStreamer.streamSingle(securityContext, writer, graphObject);
				}

				case Iterable list -> {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, true, serializeNulls);

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>("toJson()", list), null, false);
				}

				case Map map -> {

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, Settings.JsonIndentation.getValue(), outputDepth, false, serializeNulls);
					final GraphObjectMap graphObjectMap = new GraphObjectMap();

					UiFunction.recursivelyConvertMapToGraphObjectMap(graphObjectMap, map, outputDepth);

					jsonStreamer.stream(securityContext, writer, new PagingIterable<>("toJson()", List.of(graphObjectMap)), null, false);
				}

				case Date d -> {

					// even for raw dates, keep "our" date format instead of the default JSON format
					new GsonBuilder().setPrettyPrinting().setDateFormat(Settings.DefaultDateFormat.getValue()).create().toJson(d, writer);
				}

				case ZonedDateTime zdt -> {

					// for raw ZonedDateTime objects, keep the default formatting (or the configured override)
					new GsonBuilder().setPrettyPrinting().create().toJson(ZonedDateTimePropertyGenerator.format(zdt, null), writer);
				}

				case null, default -> {

					// for everything that we do not have a special representation in structr - or special functionality attached (views), use the native JSON.stringify

					final ScriptConfig scriptConfig = ScriptConfig.builder().wrapJsInMain(false).build();
					final GraphObjectMap tmpGraphObject = new GraphObjectMap();

					tmpGraphObject.setProperty(new GenericProperty("tmp"), obj);

					final Object stringifyResult = Scripting.evaluate(new ActionContext(securityContext), tmpGraphObject, "${{ JSON.stringify($.this.tmp); }}", "internal_JSON_stringify", null, scriptConfig);

					writer.write((String) stringifyResult);
				}
			}

			if (Boolean.FALSE.equals(returnRawResultWasEnabled)) {

				securityContext.disableReturnRawResult();
			}

			return writer.getBuffer().toString();

		} catch (ArgumentCountException ace) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, "%s: %s".formatted(getName(), ace.getMessage()), ace);

		} catch (Throwable t) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, "%s: %s".formatted(getName(), t.getMessage()), t);
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
				Usage.structrScript("Usage: ${toJson(obj [, view [, depth = 3 [, serializeNulls = true ]]])}."),
				Usage.javaScript("Usage: ${{ $.toJson(obj [, view [, depth = 3 [, serializeNulls = true ]]]) }}.")
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
			headers and the result container.

			For database objects, the optional `view` parameter can be used to select the view representation
			of the entity. If no view is given, the `public` view is used. The optional `depth` parameter defines
			at which depth the JSON serialization stops. If no depth is given, the default value of 3 is used.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ toJson(find('MyData'), 'public', 4) }"), Example.javaScript("${{ $.toJson($.this, 'public', 4) }}"));
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("source", "object or collection"),
				Parameter.optional("view", "view (default: `public`)"),
				Parameter.optional("depth", "conversion depth (default: 3)"),
				Parameter.optional("serializeNulls", "keep null properties (default: true)")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
				"For database objects this method is preferable to `JSON.stringify()` because a view can be chosen. `JSON.stringify()` will only return the `id` and `type` property for nodes.",
				"For native JavaScript objects in a JavaScript context, it is highly encouraged to use the native JSON.stringify() function.",
				"Due to the fact that toJson() runs in Java, `undefined` is not available and will be serialized as `null`.",
				"In contrast to JSON.stringify, a native JavaScript `Set` will be serialized as a JSON array.",
				"`Date` properties of nodes will be serialized using their custom format (if defined), otherwise `dateproperty.defaultformat` will be used.",
				"JavaScript `Date` objects will be serialized using the default date format configured in `dateproperty.defaultformat` to keep date formatting aligned to when stringifying a node.",
				"`ZonedDateTime` properties of nodes will be serialized using their custom format (if defined), otherwise `zoneddatetimeproperty.format.override` (if defined) will be used. If none of those are defined the default serialization (ISO 8601 + RFC 9557) will be used.",
				"JavaScript `ZonedDateTime` objects will be serialized using the override format configured in `zoneddatetimeproperty.format.override` (or the default ISO 8601 + RFC 9557) to keep date formatting aligned to when stringifying a node."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}
}
