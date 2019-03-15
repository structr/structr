/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.function;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 */
public class RenderFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_RENDER    = "Usage: ${render(node)} or ${render(nodes)}. Example: ${render(get(this, \"children\"))}";
	public static final String ERROR_MESSAGE_RENDER_JS = "Usage: ${{Structr.render(node)}} or ${{Structr.render(nodes)}}. Example: ${{Structr.render(Structr.get('this').children)}}";

	@Override
	public String getName() {
		return "render";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length == 1) {

			RenderContext innerCtx = new RenderContext((RenderContext)ctx);

			if (sources[0] == null) {
				
				return "";
			}
			
			if (sources[0] instanceof DOMNode) {

				((DOMNode)sources[0]).render(innerCtx, 0);

			} else if (sources[0] instanceof Collection) {

				for (final Object obj : (Collection)sources[0]) {

					if (obj instanceof DOMNode) {
						((DOMNode)obj).render(innerCtx, 0);
					}

				}

			} else {

				logger.warn("Error: Parameter 1 is neither node nor collection. Parameters: {}", getParametersAsString(sources));

			}

			return StringUtils.join(innerCtx.getBuffer().getQueue(), "");

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_RENDER_JS : ERROR_MESSAGE_RENDER);
	}

	@Override
	public String shortDescription() {
		return "Renders the children of the current node";
	}

}
