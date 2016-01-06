package org.structr.function;

import java.io.StringWriter;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.StaticValue;
import org.structr.core.Value;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class ToJsonFunction extends UiFunction {

	public static final String ERROR_MESSAGE_TO_JSON    = "Usage: ${to_json(obj [, view])}. Example: ${to_json(this)}";
	public static final String ERROR_MESSAGE_TO_JSON_JS = "Usage: ${{Structr.to_json(obj [, view])}}. Example: ${{Structr.to_json(Structr.get('this'))}}";

	@Override
	public String getName() {
		return "to_json()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length > 0) {

			final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();

			if (sources[0] instanceof GraphObject) {

				try {

					final Value<String> view = new StaticValue<>("public");
					if (sources.length > 1) {

						view.set(securityContext, sources[1].toString());
					}

					int outputDepth = 3;
					if (sources.length > 2) {

						if (sources[2] instanceof Number) {
							outputDepth = ((Number)sources[2]).intValue();
						}
					}

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, true, outputDepth);
					final StringWriter writer = new StringWriter();

					jsonStreamer.streamSingle(securityContext, writer, (GraphObject)sources[0]);

					return writer.getBuffer().toString();

				} catch (Throwable t) {
					t.printStackTrace();
				}

			} else if (sources[0] instanceof List) {

				try {

					final Value<String> view = new StaticValue<>("public");
					if (sources.length > 1) {

						view.set(securityContext, sources[1].toString());
					}

					int outputDepth = 3;
					if (sources.length > 2) {

						if (sources[2] instanceof Number) {
							outputDepth = ((Number)sources[2]).intValue();
						}
					}

					final StreamingJsonWriter jsonStreamer = new StreamingJsonWriter(view, true, outputDepth);
					final StringWriter writer = new StringWriter();
					final List list = (List)sources[0];

					jsonStreamer.stream(securityContext, writer, new Result(list, list.size(), true, false), null);

					return writer.getBuffer().toString();

				} catch (Throwable t) {
					t.printStackTrace();
				}

			}

			return "";
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
