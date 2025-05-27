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
import org.structr.schema.action.ActionContext;


public class TitleizeFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_TITLEIZE = "Usage: ${titleize(string, separator)}. (Default separator is \" \") Example: ${titleize(this.lowerCamelCaseString, \"_\")}";

	@Override
	public String getName() {
		return "titleize";
	}

	@Override
	public String getSignature() {
		return "str";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length == 0 || (sources.length > 0 && sources[0] == null) || (sources.length > 0 && sources[0] != null && StringUtils.isBlank(sources[0].toString()))) {
			return "";
		}

		if (sources.length > 2) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		final String separator;
		if (sources.length == 1) {
			separator = " ";
		} else {
			separator = sources[1].toString();
		}

		String[] in = StringUtils.split(sources[0].toString(), separator);
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++) {
			out[i] = StringUtils.capitalize(in[i]);
		}
		return StringUtils.join(out, " ");

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_TITLEIZE;
	}

	@Override
	public String shortDescription() {
		return "Titleizes the given string";
	}

}
