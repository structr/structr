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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.Collection;
import java.util.List;

public class RenderFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_RENDER    = "Usage: ${render(node)} or ${render(nodes)}. Example: ${render(get(this, \"children\"))}";
	public static final String ERROR_MESSAGE_RENDER_JS = "Usage: ${{Structr.render(node)}} or ${{Structr.render(nodes)}}. Example: ${{Structr.render(Structr.get('this').children)}}";

	@Override
	public String getName() {
		return "render";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("list");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length == 1) {

			if (sources[0] == null) {

				return "";
			}

			boolean useBuffer      = false;
			RenderContext innerCtx = null;

			if (ctx.isRenderContext()) {

				innerCtx  = new RenderContext((RenderContext)ctx);
				useBuffer = true;

			} else {

				innerCtx  = new RenderContext(ctx.getSecurityContext());
				useBuffer = false;
			}

			if (RenderContext.EditMode.PREVIEW.equals(innerCtx.getEditMode(ctx.getSecurityContext().getCachedUser()))) {
				innerCtx.getBuffer().append("<structr:render data-caller-id=\"").append(caller.toString()).append("\">");
			}

			if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.DOM_NODE)) {

				n.as(DOMNode.class).render(innerCtx, 0);

			} else if (sources[0] instanceof Collection collection) {

				for (final Object obj : collection) {

					if (obj instanceof NodeInterface n && n.is(StructrTraits.DOM_NODE)) {
						n.as(DOMNode.class).render(innerCtx, 0);
					}
				}
			} else if (sources[0] instanceof Iterable iterable) {

				for (final Object obj : iterable) {

					if (obj instanceof NodeInterface n && n.is(StructrTraits.DOM_NODE)) {

						n.as(DOMNode.class).render(innerCtx, 0);

					} else {
						// TODO: Render data object with its attached template
					}
				}
			} else {

				logger.warn("Error: Parameter 1 is neither node nor collection. Parameters: {}", getParametersAsString(sources));
			}

			if (RenderContext.EditMode.PREVIEW.equals(innerCtx.getEditMode(ctx.getSecurityContext().getCachedUser()))) {
				innerCtx.getBuffer().append("</structr:render>");
			}

			if (useBuffer) {

				// output was written to RenderContext async buffer
				return null;

			} else {

				// output needs to be returned as a function result
				return StringUtils.join(innerCtx.getBuffer().getQueue(), "");
			}

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
	public String getShortDescription() {
		return "Renders the children of the current node";
	}
}
