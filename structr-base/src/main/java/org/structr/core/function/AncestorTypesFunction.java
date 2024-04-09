/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AncestorTypesFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_ANCESTOR_TYPES    = "Usage: ${ancestor_types(type[, blacklist])}. Example ${ancestor_types('User')}";
	public static final String ERROR_MESSAGE_ANCESTOR_TYPES_JS = "Usage: ${Structr.ancestor_types(type[, blacklist])}. Example ${Structr.ancestor_types('User')}";

	@Override
	public String getName() {
		return "ancestor_types";
	}

	@Override
	public String getSignature() {
		return "type [, blacklist ]";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String typeName = sources[0].toString();
			final ArrayList<String> ancestorTypes = new ArrayList();

			Class type = SchemaHelper.getEntityClassForRawType(typeName);

			if (type != null) {

				while (type != null && !type.equals(Object.class)) {

					ancestorTypes.add(type.getSimpleName());
					type = type.getSuperclass();
				}

			} else {

				logger.warn("{}(): Type not found: {}" + (caller != null ? " (source of call: " + caller.toString() + ")" : ""), getName(), sources[0]);
			}

			final List<String> blackList = (sources.length == 2) ? (List)sources[1] : Arrays.asList("AbstractNode");
			ancestorTypes.removeAll(blackList);

			return ancestorTypes;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ANCESTOR_TYPES_JS : ERROR_MESSAGE_ANCESTOR_TYPES);
	}

	@Override
	public String shortDescription() {
		return "Returns the names of the parent classes of the given type";
	}
}
