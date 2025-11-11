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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GetenvFunction extends CoreFunction {

	@Override
	public String getName() {
		return "getenv";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("[variable]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 0, 1);

			if (sources.length == 1) {

				return System.getenv(sources[0].toString());

			} else {

				return System.getenv();
			}

		} catch (SecurityException se) {

			logException(se, "SecurityException encountered for parameters: {}", sources);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${getenv()}. Example: ${getenv('JAVA_HOME')}"),
			Usage.javaScript("Usage: ${{ $.getenv() }. Example: ${{ $.getenv('JAVA_HOME'); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the value of the specified environment variable. If no value is specified, all environment variables are returned as a map. An environment variable is a system-dependent external named value.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
