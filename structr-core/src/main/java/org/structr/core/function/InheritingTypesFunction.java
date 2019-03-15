/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.ArrayList;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.search.SearchCommand;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class InheritingTypesFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INHERITING_TYPES    = "Usage: ${inheriting_types(type[, blacklist])}. Example ${inheriting_types('User')}";
	public static final String ERROR_MESSAGE_INHERITING_TYPES_JS = "Usage: ${Structr.inheriting_types(type[, blacklist])}. Example ${Structr.inheriting_types('User')}";

	@Override
	public String getName() {
		return "inheriting_types";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String typeName = sources[0].toString();
			final Class type = SchemaHelper.getEntityClassForRawType(typeName);
			final ArrayList inheritants = new ArrayList();

			inheritants.addAll(SearchCommand.getAllSubtypesAsStringSet(type.getSimpleName()));

			if (sources.length == 2) {
				inheritants.removeAll((List)sources[1]);
			}

			return inheritants;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_INHERITING_TYPES_JS : ERROR_MESSAGE_INHERITING_TYPES);
	}

	@Override
	public String shortDescription() {
		return "Returns the names of the child classes of the given type";
	}
}
