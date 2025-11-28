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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class PropertyInfoFunction extends AdvancedScriptingFunction {


	@Override
	public String getName() {
		return "propertyInfo";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type, propertyName");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final String typeName = sources[0].toString();
			final String keyName = sources[1].toString();

			Traits traits = Traits.of(typeName);

			if (traits != null) {

				final PropertyKey key = traits.key(keyName);
				if (key != null) {

					return SchemaHelper.getPropertyInfo(ctx.getSecurityContext(), key);

				} else {

					logger.warn("Error: Unknown property \"{}.{}\". Parameters: {}", typeName, keyName, getParametersAsString(sources));
					return "Unknown property " + typeName + "." + keyName;
				}

			} else {

				logger.warn("Error: Unknown type \"{}\". Parameters: {}", typeName, getParametersAsString(sources));
				return "Unknown type " + typeName;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${propertyInfo(type, name)}. Example ${propertyInfo('User', 'name')}"),
			Usage.javaScript("Usage: ${Structr.propertyInfo(type, name)}. Example ${Structr.propertyInfo('User', 'name')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the schema information for the given property.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
