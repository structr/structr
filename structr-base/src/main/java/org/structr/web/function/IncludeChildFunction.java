/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;

import java.util.List;

/**
 * Convenience method to render named child nodes.
 */
public class IncludeChildFunction extends IncludeFunction {

	@Override
	public String getName() {
		return "includeChild";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name [, collection, dataKey]");
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

				if (RenderContext.EditMode.PREVIEW.equals(innerCtx.getEditMode(ctx.getSecurityContext().getCachedUser()))) {
					innerCtx.getBuffer().append("<structr:include-child data-caller-id=\"").append(caller.toString()).append("\">");
				}

				final Template templateNode                = n.as(Template.class);
				final List<NodeInterface> childrenWithName = templateNode.treeGetChildren().stream().filter(ni -> (sources[0]).equals(ni.as(DOMNode.class).getName())).toList();
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${includeChild(name)}. Example: ${includeChild('Child Node')}"),
			Usage.javaScript("Usage: ${{Structr.includeChild(name)}}. Example: ${{Structr.includeChild('Child Node')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Loads a template's child element with the given name and renders its HTML representation into the output buffer.";
	}

	@Override
	public String getLongDescription() {
		return """
		Nodes can be included via their `name` property. When used with an optional collection and data key argument, the included HTML element will be rendered as a Repeater Element.
		
		See also `include()` and `render()`.
		""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${includeChild('Child1')}", "Render the contents of the child node named \"Child1\" into the output buffer"),
			Example.structrScript("${includeChild('Item Template', find('Item'), 'item')}", "Render the contents of the child node named \"Item Template\" once for every Item node in the database")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Works only during page rendering in Template nodes.",
			"Child nodes must be direct children of the template node.",
			"Underneath the template node, child node names MUST be unique in order for `includeChild()` to work."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
	}
}