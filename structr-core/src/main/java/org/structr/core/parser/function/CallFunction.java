package org.structr.core.parser.function;

import java.util.Arrays;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.Function;

/**
 *
 */
public class CallFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CALL    = "Usage: ${call(key [, payloads...]}. Example ${call('myEvent')}";
	public static final String ERROR_MESSAGE_CALL_JS = "Usage: ${{Structr.call(key [, payloads...]}}. Example ${{Structr.call('myEvent')}}";

	@Override
	public String getName() {
		return "call()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final String key = sources[0].toString();

			if (sources.length > 1) {

				return Actions.call(key, Arrays.copyOfRange(sources, 1, sources.length));

			} else {

				return Actions.call(key);
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CALL_JS : ERROR_MESSAGE_CALL);
	}

	@Override
	public String shortDescription() {
		return "Calls the given exported / dynamic method on the given entity";
	}

}
