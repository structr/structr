/**
 * Copyright (C) 2010-2017 Structr GmbH
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
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractUser;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ConfigFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CONFIG    = "Usage: ${config(keyFromStructrConf[, \"default\"])}. Example: ${config(\"base.path\")}";
	public static final String ERROR_MESSAGE_CONFIG_JS = "Usage: ${{Structr.config(keyFromStructrConf[, \"default\"])}}. Example: ${{Structr.config(\"base.path\")}}";

	@Override
	public String getName() {
		return "config()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (!arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {
				
				return null;
			}

			final String configKey = sources[0].toString();

			if (Services.SUPERUSER_PASSWORD.equals(configKey)) {

				return AbstractUser.HIDDEN;

			}

			final String defaultValue = sources.length >= 2 ? sources[1].toString() : "";

			return StructrApp.getConfigurationValue(configKey, defaultValue);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		}

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CONFIG_JS : ERROR_MESSAGE_CONFIG);
	}

	@Override
	public String shortDescription() {
		return "Returns the structr.conf value with the given key";
	}

}
