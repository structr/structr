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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class SetLogLevelFunction extends CoreFunction {

	private static final Logger logger = LoggerFactory.getLogger(SetLogLevelFunction.class.getName());

	@Override
	public String getName() {
		return "setLogLevel";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("string");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${setLogLevel(string)}. Example ${setLogLevel('WARN')}"),
			Usage.javaScript("Usage: ${{Structr.setLogLevel(string)}}. Example ${{Structr.setLogLevel('WARN')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the application log level to the given level, if supported.";
	}

	@Override
	public String getLongDescription() {
		return """
		Supported values are: %s. The log level can also be set via the configuration setting "%s". Using this function overrides the base configuration.

		Change takes effect immediately until another call is made or the application is restarted. On system start, the configuration value is used.
		""".formatted(String.join(", ", Settings.getAvailableLogLevels().keySet()), Settings.LogLevel.getKey());
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
