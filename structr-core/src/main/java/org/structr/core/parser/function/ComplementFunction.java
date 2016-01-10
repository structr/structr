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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ComplementFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_COMPLEMENT = "Usage: ${complement(list1, list2, list3, ...)}. (The resulting list contains no duplicates) Example: ${complement(allUsers, me)} => List of all users except myself";

	@Override
	public String getName() {
		return "complement()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final Set sourceSet = new HashSet();

		if (sources[0] instanceof Collection) {

			sourceSet.addAll((Collection)sources[0]);

			for (int cnt = 1; cnt < sources.length; cnt++) {

				final Object source = sources[cnt];

				if (source instanceof Collection) {

					sourceSet.removeAll((Collection)source);

				} else if (source != null) {

					sourceSet.remove(source);
				}
			}

		} else {

			return "Argument 1 for complement must be a Collection";
		}

		return sourceSet;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_COMPLEMENT;
	}

	@Override
	public String shortDescription() {
		return "";
	}


}
