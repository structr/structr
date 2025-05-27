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

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class SetLogLevelFunction extends CoreFunction {

	private static final Logger logger = LoggerFactory.getLogger(SetLogLevelFunction.class.getName());

	public static final String ERROR_MESSAGE_SET_LOG_LEVEL    = "Usage: ${set_log_level(string)}. Example ${set_log_level('WARN')}";
	public static final String ERROR_MESSAGE_SET_LOG_LEVEL_JS = "Usage: ${{Structr.setLogLevel(string)}}. Example ${{Structr.setLogLevel('WARN')}}";

	@Override
	public String getName() {
		return "set_log_level";
	}

	@Override
	public String getSignature() {
		return "str";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String requestedLevel = sources[0].toString();
			final boolean success = SetLogLevelFunction.setLogLevel(requestedLevel);

			if (!success) {

				logger.error("{}: Unsupported log level: {}", getName(), requestedLevel);
			}

			return "";

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SET_LOG_LEVEL_JS : ERROR_MESSAGE_SET_LOG_LEVEL);
	}

	@Override
	public String shortDescription() {
		return "Sets the application log level to the given level, if supported. Change takes effect immediately until another call is made or the application is restarted. On system start, the configuration value is used.";
	}

	public static boolean setLogLevel(final String level) {

		final ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.structr");

		if (Settings.getAvailableLogLevels().containsKey(level)) {

			logger.setLevel(Level.toLevel(level));

			return true;

		} else {

			return false;
		}
	}
}
