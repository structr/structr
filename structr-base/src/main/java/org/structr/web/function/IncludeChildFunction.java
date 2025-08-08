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
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;

import java.util.List;

/**
 * Convenience method to render named child nodes.
 */
public class IncludeChildFunction extends IncludeFunction {

	public static final String ERROR_MESSAGE_INCLUDE    = "Usage: ${include_child(name)}. Example: ${include_child(\"Child Node\")}";
	public static final String ERROR_MESSAGE_INCLUDE_JS = "Usage: ${{Structr.includeChild(name)}}. Example: ${{Structr.includeChild(\"Child Node\")}}";

	@Override
	public String getName() {
		return "include_child";
	}

	@Override
	public String getSignature() {
		return "name [, collection, dataKey]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof String)) {

				return null;
			}

			if (!ctx.isRenderContext()) {

				return null;
			}

			final SecurityContext securityContext    = ctx.getSecurityContext();
			final App app                            = StructrApp.getInstance(securityContext);
			final RenderContext innerCtx             = new RenderContext((RenderContext)ctx);

			// Are we are in a Template node?
			if (caller instanceof NodeInterface n && n.is(StructrTraits.TEMPLATE)) {

				final Template templateNode                = n.as(Template.class);
				final List<NodeInterface> childrenWithName = templateNode.treeGetChildren().stream().filter(ni -> ni.as(DOMNode.class).getName().equals(sources[0])).toList();
				final int childrenWithNameCount            = childrenWithName.size();

				if (childrenWithNameCount == 1) {

					// Exactly one child found => use this node
					return renderNode(securityContext, ctx, innerCtx, sources, app, childrenWithName.getFirst().as(DOMNode.class), true);

				} else if (childrenWithNameCount > 1) {

					// More than one child node found => error
					logger.warn(getName() + "(): Ambiguous child node name '" + sources[0] + "' (" + StringUtils.join(childrenWithName, ", ") + ")");
				}

			} else {

				logger.warn(getName() + "(): Can only be used in a template context.");
			}

			return "";

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
		return (inJavaScriptContext ? ERROR_MESSAGE_INCLUDE_JS : ERROR_MESSAGE_INCLUDE);
	}

	@Override
	public String shortDescription() {
		return "Includes the content of the child node with the given name (optionally as a repeater element)";
	}
}