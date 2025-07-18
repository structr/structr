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
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InheritingTypesFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_INHERITING_TYPES    = "Usage: ${inheriting_types(type[, blacklist])}. Example ${inheriting_types('User')}";
	public static final String ERROR_MESSAGE_INHERITING_TYPES_JS = "Usage: ${Structr.inheriting_types(type[, blacklist])}. Example ${Structr.inheriting_types('User')}";

	@Override
	public String getName() {
		return "inheriting_types";
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
			if (!Traits.exists(typeName)) {

				throw new FrameworkException(422, new StringBuilder(getName()).append("(): Type '").append(typeName).append("' not found.").toString());
			}

			final Traits type               = Traits.of(typeName);
			final ArrayList inheritingTypes = new ArrayList();
			final List<String> blackList    = new ArrayList(Arrays.asList(typeName));

			if (sources.length == 2) {

				if (sources[1] instanceof List) {

					blackList.addAll((List)sources[1]);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Expected 'blacklist' parameter to be of type List.").toString());
				}
			}

			if (type != null) {

				if (type.isServiceClass() == false) {

					inheritingTypes.addAll(Traits.getAllTypes(value -> value.getAllTraits().contains(typeName)));
					inheritingTypes.removeAll(blackList);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Not applicable to service class '").append(type.getName()).append("'.").toString());
				}

			} else {

				logger.warn("{}(): Type not found: {}" + (caller != null ? " (source of call: " + caller.toString() + ")" : ""), getName(), sources[0]);
			}

			return inheritingTypes;

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
		return "Returns the names of the child types of the given type and filters out all entries of the blacklist collection.";
	}
}
