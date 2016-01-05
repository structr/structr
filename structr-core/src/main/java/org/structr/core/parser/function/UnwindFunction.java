package org.structr.core.parser.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class UnwindFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_UNWIND = "Usage: ${unwind(list1, ...)}. Example: ${unwind(this.children)}";

	@Override
	public String getName() {
		return "unwind()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final List list = new ArrayList();
		for (final Object source : sources) {

			if (source instanceof Collection) {

				// filter null objects
				for (Object obj : (Collection)source) {
					if (obj != null) {

						if (obj instanceof Collection) {

							for (final Object elem : (Collection)obj) {

								if (elem != null) {

									list.add(elem);
								}
							}

						} else {

							list.add(obj);
						}
					}
				}

			} else if (source != null) {

				list.add(source);
			}
		}

		return list;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_UNWIND;
	}

	@Override
	public String shortDescription() {
		return "";
	}
}
