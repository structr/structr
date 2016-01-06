package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class PrintFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_PRINT    = "Usage: ${print(objects...)}. Example: ${print(this.name, \"test\")}";
	public static final String ERROR_MESSAGE_PRINT_JS = "Usage: ${{Structr.print(objects...)}}. Example: ${{Structr.print(Structr.get('this').name, \"test\")}}";

	@Override
	public String getName() {
		return "print()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			for (Object i : sources) {

				ctx.print(i);
			}
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PRINT_JS : ERROR_MESSAGE_PRINT);
	}

	@Override
	public String shortDescription() {
		return "Prints the given string to the output buffer";
	}

}
