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
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class SleepFunction extends CoreFunction {

	private static final Logger logger             = LoggerFactory.getLogger(SleepFunction.class);
	public static final String ERROR_MESSAGE_SLEEP = "Usage: ${sleep(milliseconds)}. Example: ${sleep(1000)}";

	@Override
	public String getName() {
		return "sleep";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("milliseconds");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			// parseInt should be safe here, we probably don't want to wait for more than 49 days..
			final long milliseconds = parseInt(sources[0]);

			if (milliseconds == 0 || milliseconds > 3_600_000) {

				logger.warn("Unusual wait time in sleep() function, do you really want to wait for {} milliseconds?", milliseconds);
			}

			try {

				Thread.sleep(milliseconds);

			} catch (InterruptedException iex) {

				logger.warn("Interrupted while waiting for {} milliseconds: {}", milliseconds, iex.getMessage());
				Thread.currentThread().interrupt();
			}

			return sources[0].toString().toUpperCase();

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
		return ERROR_MESSAGE_SLEEP;
	}

	@Override
	public String getShortDescription() {
		return "Pauses the execution of the current thread for the given number of milliseconds.";
	}
}
