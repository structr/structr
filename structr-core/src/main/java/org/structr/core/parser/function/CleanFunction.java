package org.structr.core.parser.function;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import static org.structr.core.parser.Functions.cleanString;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class CleanFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CLEAN = "Usage: ${clean(string)}. Example: ${clean(this.stringWithNonWordChars)}";

	@Override
	public String getName() {
		return "clean()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof Collection) {

				final List<String> cleanList = new LinkedList<>();

				for (final Object obj : (Collection)sources[0]) {

					if (StringUtils.isBlank(obj.toString())) {

						cleanList.add("");

					} else {

						cleanList.add(cleanString(obj));

					}
				}

				return cleanList;
			}

			if (StringUtils.isBlank(sources[0].toString())) {
				return "";
			}

			return cleanString(sources[0]);
		}

		return null;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_CLEAN;
	}

	@Override
	public String shortDescription() {
		return "Cleans the given string";
	}

}
