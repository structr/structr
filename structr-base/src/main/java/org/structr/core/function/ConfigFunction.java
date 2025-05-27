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

import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.schema.action.ActionContext;

public class ConfigFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_CONFIG    = "Usage: ${config(keyFromStructrConf[, \"default\"])}. Example: ${config(\"base.path\")}";
	public static final String ERROR_MESSAGE_CONFIG_JS = "Usage: ${{Structr.config(keyFromStructrConf[, \"default\"])}}. Example: ${{Structr.config(\"base.path\")}}";

	@Override
	public String getName() {
		return "config";
	}

	@Override
	public String getSignature() {
		return "configKey [, defaultValue ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String configKey    = sources[0].toString();
			final String defaultValue = sources.length >= 2 ? sources[1].toString() : "";
			Setting setting           = Settings.getSetting(configKey);

			if (setting == null) {

				setting = Settings.getCaseSensitiveSetting(configKey);
			}

			if (setting != null) {

				if (setting.isProtected()) {

					return Principal.HIDDEN;

				} else {

					return setting.getValue();
				}

			} else {

				return defaultValue;
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CONFIG_JS : ERROR_MESSAGE_CONFIG);
	}

	@Override
	public String shortDescription() {
		return "Returns the structr.conf value for the given key";
	}
}
