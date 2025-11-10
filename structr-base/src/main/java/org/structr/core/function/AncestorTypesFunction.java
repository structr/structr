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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AncestorTypesFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_ANCESTOR_TYPES    = "Usage: ${ancestor_types(type[, blacklist])}. Example ${ancestor_types('User', ['Principal'])}";
	public static final String ERROR_MESSAGE_ANCESTOR_TYPES_JS = "Usage: ${Structr.ancestor_types(type[, blacklist])}. Example ${Structr.ancestor_types('User', ['Principal'])}";

	@Override
	public String getName() {
		return "ancestor_types";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("type [, blacklist ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String typeName = sources[0].toString();
			if (!Traits.exists(typeName)) {

				throw new FrameworkException(422, new StringBuilder(getName()).append("(): Type '").append(typeName).append("' not found.").toString());
			}

			final Traits type                     = Traits.of(typeName);
			final ArrayList<String> ancestorTypes = new ArrayList();
			final List<String> blackList          = new ArrayList(Arrays.asList(typeName, StructrTraits.NODE_INTERFACE, StructrTraits.PROPERTY_CONTAINER, StructrTraits.GRAPH_OBJECT, StructrTraits.ACCESS_CONTROLLABLE));

			if (sources.length == 2) {

				if (sources[1] instanceof List) {

					blackList.addAll((List)sources[1]);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Expected 'blacklist' parameter to be of type List.").toString());
				}
			}

			if (type != null) {

				if (type.isServiceClass() == false) {

					ancestorTypes.addAll(type.getAllTraits());
					ancestorTypes.removeAll(blackList);

				} else {

					throw new FrameworkException(422, new StringBuilder(getName()).append("(): Not applicable to service class '").append(type.getName()).append("'.").toString());
				}

			} else {

				logger.warn("{}(): Type not found: {}" + (caller != null ? " (source of call: " + caller.toString() + ")" : ""), getName(), sources[0]);
			}

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
	public String getShortDescription() {
		return "Returns the names of the parent types of the given type and filters out all entries of the blacklist collection.";
	}
}
