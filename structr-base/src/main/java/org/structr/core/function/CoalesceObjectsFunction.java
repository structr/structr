/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;


public class CoalesceObjectsFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_COALESCE = "Usage: ${coalesce_objects(obj1, obj2...)}. Example: ${coalesce(node1, node2, node3)}";
	public static final String ERROR_MESSAGE_COALESCE_JS = "Usage: ${{Structr.coalesceObjects(obj1, obj2...)}}. Example: ${{Structr.coalesceObjects(node1, node2, node3)}}";

	@Override
	public String getName() {
		return "coalesce_objects";
	}

	@Override
	public String getSignature() {
		return "obj1, obj2, obj3, ...";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final int length = sources.length;

			for (int i = 0; i < length; i++) {

				if (sources[i] != null) {
					return sources[i];
				}

			}

			// no non-null value was supplied
			return null;

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_COALESCE_JS : ERROR_MESSAGE_COALESCE);
	}

	@Override
	public String shortDescription() {
		return "Returns the first non-null value in the list of expressions passed to it. In case all arguments are null, null will be returned";
	}

}
