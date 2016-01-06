package org.structr.core.parser.function;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class CypherFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CYPHER    = "Usage: ${cypher('MATCH (n) RETURN n')}";
	public static final String ERROR_MESSAGE_CYPHER_JS = "Usage: ${{Structr.cypher(query)}}. Example ${{Structr.cypher('MATCH (n) RETURN n')}}";

	@Override
	public String getName() {
		return "cypher()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final Map<String, Object> params = new LinkedHashMap<>();
			final String query = sources[0].toString();

			// parameters?
			if (sources.length > 1 && sources[1] != null && sources[1] instanceof Map) {
				params.putAll((Map)sources[1]);
			}

			return StructrApp.getInstance(ctx.getSecurityContext()).cypher(query, params);
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CYPHER_JS : ERROR_MESSAGE_CYPHER);
	}

	@Override
	public String shortDescription() {
		return "Returns the result of the given Cypher query";
	}

}
