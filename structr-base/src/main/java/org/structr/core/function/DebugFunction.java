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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class DebugFunction extends LogFunction {

	private static final Logger logger = LoggerFactory.getLogger(LogFunction.class.getName());

	public static final String ERROR_MESSAGE_DEBUG    = "Usage: ${debug(string)}. Example ${debug('Hello World!')}";
	public static final String ERROR_MESSAGE_DEBUG_JS = "Usage: ${{Structr.debug(string)}}. Example ${{Structr.debug('Hello World!')}}";

	@Override
	public String getName() {
		return "debug";
	}

	@Override
	public String getSignature() {
		return "str";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (Settings.DebugLogging.getValue()) {

			return super.apply(ctx, caller, sources);

		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DEBUG : ERROR_MESSAGE_DEBUG_JS);
	}

	@Override
	public String shortDescription() {
		return "Logs the given string to the logfile if the debug mode is enabled in the configuration";
	}
}
