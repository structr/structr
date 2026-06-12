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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.DataAdapter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;

import java.util.List;

/**
 * Convenience method to render named child nodes.
 */
public class RenderLabelsFunction extends ApplyTemplatesFunction {

	@Override
	public String getName() {
		return "renderLabels";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("tag [, slot]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (!ctx.isRenderContext()) {

				return null;
			}

			final String templateWrapper = getStringOrNull(sources, 0);
			final String slot            = getStringOrNull(sources, 1);

			// Are we are in a DOMNode?
			if (caller instanceof NodeInterface n && n.is(StructrTraits.DOM_NODE)) {

				final DOMNode domNode               = n.as(DOMNode.class);
				final DOMNode component             = domNode.getClosestComponent();
				final ComponentConfiguration config = component.getComponentConfiguration();
				final DataAdapter dataAdapter       = config.getDataAdapter();

				applyLabels(ctx, dataAdapter, domNode, templateWrapper, slot);

			} else {

				logger.warn(getName() + "(): Can only be used in a rendering context.");
			}

			return null;

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
			Usage.structrScript("Usage: ${renderLabels(tag, slot)}. Example: ${renderLabels('li', 'label')}"),
			Usage.javaScript("Usage: ${{ $.renderLabels(tag, slot)}}. Example: ${{ $.renderLabels('li', 'label')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Renders labels for one or all of the fields *of one element* from the enclosing component's data source, wrapped in the given tag.";
	}

	@Override
	public String getLongDescription() {
		return "This function renders the `label` property of the data source specification *for one element*, wrapped in the given tag. If the `slot` argument is present, only the label for the given slot is rendered. If no slot is given, this function renders all labels for one value, wrapped in the HTML element given in the `tag` argument.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${renderLabels('li', 'label')}", "Render the label of the `label` field"),
			Example.structrScript("${renderLabels('th')}", "Render the labels of all fields of the current data source, wrapped in a th element")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Works only during page rendering in Template nodes.",
			"This function will most likely be used to render the header row of a dynamic table."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
	}
}