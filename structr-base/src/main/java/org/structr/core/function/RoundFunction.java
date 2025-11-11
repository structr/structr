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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Language;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class RoundFunction extends AdvancedScriptingFunction {


	@Override
	public String getName() {
		return "round";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("value [, decimalPlaces ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		String decimalPlaces = "0";

		if (sources == null) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		if (sources.length == 1) {

			if (sources[0] == null) {
				return null;
			}

			if (StringUtils.isBlank(sources[0].toString())) {
				return "";
			}

			final double f1 = Double.parseDouble(sources[0].toString());
			return Math.round(f1);

		}

		if (sources.length == 2) {

			if (sources[0] == null) {
				return null;
			}

			if (sources[1] == null) {
				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return usage(ctx.isJavaScriptContext());
			}

			decimalPlaces = sources[1].toString();

			try {

				final double f1 = Double.parseDouble(sources[0].toString());
				final double f2 = Math.pow(10, (Double.parseDouble(decimalPlaces)));
				long r = Math.round(f1 * f2);

				return (double) r / f2;

			} catch (Throwable t) {

				logException(caller, t, sources);
				return t.getMessage();

			}
		}

		logParameterError(caller, sources, ctx.isJavaScriptContext());
		return usage(ctx.isJavaScriptContext());

	}


	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${round(value1 [, decimalPlaces])}. Example: ${round(2.345678, 2)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Rounds the given argument to the nearest integer.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
