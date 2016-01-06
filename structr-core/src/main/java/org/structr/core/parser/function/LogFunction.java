package org.structr.core.parser.function;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LogFunction extends Function<Object, Object> {

	private static final Logger logger = Logger.getLogger(LogFunction.class.getName());

	public static final String ERROR_MESSAGE_LOG    = "Usage: ${log(string)}. Example ${log('Hello World!')}";
	public static final String ERROR_MESSAGE_LOG_JS = "Usage: ${{Structr.log(string)}}. Example ${{Structr.log('Hello World!')}}";

	@Override
	public String getName() {
		return "log()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final StringBuilder buf = new StringBuilder();
			for (final Object obj : sources) {

				buf.append(obj);
			}

			logger.log(Level.INFO, buf.toString());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_LOG_JS : ERROR_MESSAGE_LOG);
	}

	@Override
	public String shortDescription() {
		return "Logs the given string to the logfile";
	}

}
