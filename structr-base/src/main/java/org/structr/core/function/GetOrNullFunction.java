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

import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GetOrNullFunction extends CoreFunction {

	@Override
	public String getName() {
		return "get_or_null";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("entity, propertyName");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			GraphObject dataObject = null;

			if (sources[0] instanceof GraphObject) {
				dataObject = (GraphObject)sources[0];
			}

			if (sources[0] instanceof List) {

				final List list = (List)sources[0];
				if (list.size() == 1 && list.get(0) instanceof GraphObject) {

					dataObject = (GraphObject)list.get(0);
				}
			}

			if (dataObject != null) {

				final String keyName = sources[1].toString();
				final PropertyKey key = dataObject.getTraits().key(keyName);

				if (key != null) {

					final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
					Object value = dataObject.getProperty(key);

					if (inputConverter != null) {
						return inputConverter.revert(value);
					}

					return dataObject.getProperty(key);
				}

				return "";
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${get_or_null(entity, propertyKey)}. Example: ${get_or_null(this, \"children\")}"),
			Usage.javaScript("Usage: ${{Structr.getOrNull(entity, propertyKey)}}. Example: ${{Structr.getOrNull(this, \"children\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the value with the given name of the given entity, or null";
	}
}
