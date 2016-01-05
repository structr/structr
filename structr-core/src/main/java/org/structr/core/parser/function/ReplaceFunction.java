package org.structr.core.parser.function;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ReplaceFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_REPLACE = "Usage: ${replace(template, source)}. Example: ${replace(\"${this.id}\", this)}";

	@Override
	public String getName() {
		return "replace()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			final String template = sources[0].toString();
			GraphObject node = null;

			if (sources[1] instanceof GraphObject) {
				node = (GraphObject)sources[1];
			}

			if (sources[1] instanceof List) {

				final List list = (List)sources[1];
				if (list.size() == 1 && list.get(0) instanceof GraphObject) {

					node = (GraphObject)list.get(0);
				}
			}

			if (node != null) {

				// recursive replacement call, be careful here
				return Scripting.replaceVariables(ctx, node, template);
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_REPLACE;
	}

	@Override
	public String shortDescription() {
		return "";
	}

}
