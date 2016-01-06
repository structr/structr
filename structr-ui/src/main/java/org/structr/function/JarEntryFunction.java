package org.structr.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class JarEntryFunction extends UiFunction {

	@Override
	public String getName() {
		return "jar_entry";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			return new NameAndContent(sources[0].toString(), sources[1].toString());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return "jar_entry()";
	}

	@Override
	public String shortDescription() {
		return "";
	}

}
