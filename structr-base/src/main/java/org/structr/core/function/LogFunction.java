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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.script.Scripting;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class LogFunction extends CoreFunction {

	private static final Logger logger = LoggerFactory.getLogger(LogFunction.class.getName());

	public static final String ERROR_MESSAGE_LOG    = "Usage: ${log(string)}. Example ${log('Hello World!')}";
	public static final String ERROR_MESSAGE_LOG_JS = "Usage: ${{Structr.log(string)}}. Example ${{Structr.log('Hello World!')}}";

	@Override
	public String getName() {
		return "log";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("str");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (sources == null) {
				throw new IllegalArgumentException();
			}

			final StringBuilder buf = new StringBuilder();
			for (final Object obj : sources) {

				if (obj != null) {
					buf.append(Scripting.formatForLogging(obj));
				}
			}

			logger.info(buf.toString());
			return "";

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_LOG_JS : ERROR_MESSAGE_LOG);
	}

	@Override
	public String getShortDescription() {
		return "Logs the given string to the logfile";
	}
}
