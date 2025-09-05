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

import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;

public class SetDetailsObjectFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_SET_DETAILS_OBJECT    = "Usage: ${set_details_object(obj)}. Example: ${set_details_object(this)}";
	public static final String ERROR_MESSAGE_SET_DETAILS_OBJECT_JS = "Usage: ${{Structr.setDetailsObject(obj)}}. Example: ${{Structr.setDetailsObject(Structr.this)}}";

	@Override
	public String getName() {
		return "set_details_object";
	}

	@Override
	public String getSignature() {
		return "obj";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length == 1) {

			if (ctx.isRenderContext()) {

				if (sources[0] instanceof GraphObject) {

					((RenderContext) ctx).setDetailsDataObject((GraphObject)sources[0]);

				} else {

					logger.warn("{}(): First parameter is not a graph object. Parameters: {}", getName(), getParametersAsString(sources));
				}

			} else {

				logger.warn("{}(): This function can only be called in a page-rendering context! Parameters: {}", getName(), getParametersAsString(sources));
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SET_DETAILS_OBJECT_JS : ERROR_MESSAGE_SET_DETAILS_OBJECT);
	}

	@Override
	public String shortDescription() {
		return "Sets the given object as the detail object";
	}
}
