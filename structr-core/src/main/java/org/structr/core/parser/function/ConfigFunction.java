package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ConfigFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CONFIG    = "Usage: ${config(keyFromStructrConf)}. Example: ${config(\"base.path\")}";
	public static final String ERROR_MESSAGE_CONFIG_JS = "Usage: ${{Structr.config(keyFromStructrConf)}}. Example: ${{Structr.config(\"base.path\")}}";

	@Override
	public String getName() {
		return "config()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final String configKey = sources[0].toString();
			final String defaultValue = sources.length >= 2 ? sources[1].toString() : "";

			return StructrApp.getConfigurationValue(configKey, defaultValue);
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CONFIG_JS : ERROR_MESSAGE_CONFIG);
	}

	@Override
	public String shortDescription() {
		return "Returns the structr.conf value with the given key";
	}

}
