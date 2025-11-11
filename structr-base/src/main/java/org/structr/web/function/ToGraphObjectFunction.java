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
package org.structr.web.function;

import org.structr.common.SecurityContext;
import org.structr.core.StaticValue;
import org.structr.core.Value;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class ToGraphObjectFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "to_graph_object";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("obj");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length >= 1 && sources.length <= 3) {

			try {

				final SecurityContext securityContext = ctx.getSecurityContext();

				final Value<String> view = new StaticValue<>("public");
				if (sources.length > 1) {
					view.set(securityContext, sources[1].toString());
				}

				int outputDepth = 3;
				if (sources.length > 2 && sources[2] instanceof Number) {
					outputDepth = ((Number)sources[2]).intValue();
				}

				final Object converted = UiFunction.toGraphObject(sources[0], outputDepth);

				if (converted != null) {
					return converted;
				}

			} catch (Throwable t) {

				logException(caller, t, sources);
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${to_graph_object(obj)}"),
			Usage.javaScript("Usage: ${{ $.toGraphObject(obj) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Converts the given entity to GraphObjectMap";
	}
}
