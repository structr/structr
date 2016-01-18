/**
 * Copyright (C) 2010-2016 Structr GmbH
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
