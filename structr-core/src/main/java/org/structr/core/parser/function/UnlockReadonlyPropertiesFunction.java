package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class UnlockReadonlyPropertiesFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE    = "Usage: ${unlock_readonly_properties_once(node)}. Example ${unlock_readonly_properties_once, this}";
	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE_JS = "Usage: ${{Structr.unlock_readonly_properties_once(node)}}. Example ${{Structr.unlock_readonly_properties_once, Structr.get('this')}}";

	@Override
	public String getName() {
		return "unlock_readonly_properties_once()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof AbstractNode) {

				((AbstractNode)sources[0]).unlockReadOnlyPropertiesOnce();

			} else {

				return usage(ctx.isJavaScriptContext());

			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE_JS : ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE);
	}

	@Override
	public String shortDescription() {
		return "Unlocks any read-only property for a single access";
	}

}
