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
import org.structr.core.datasources.Channel;
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
public class RenderFieldsFunction extends ApplyTemplatesFunction {

	@Override
	public String getName() {
		return "renderFields";
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

			final String tag = getStringOrNull(sources, 0);

			// Are we are in a DOMNode?
			if (caller instanceof NodeInterface n && n.is(StructrTraits.DOM_NODE)) {

				final DOMNode domNode               = n.as(DOMNode.class);
				final DOMNode component             = domNode.getClosestComponent();
				final ComponentConfiguration config = component.getComponentConfiguration();
				final DataAdapter dataAdapter       = config.getDataAdapter();

				applyTemplates(ctx, dataAdapter, domNode, tag, false);

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
			Usage.structrScript("Usage: ${renderFields(tag, slot)}. Example: ${renderFields('th'))}"),
			Usage.javaScript("Usage: ${{ $.renderFields(tag, slot)}}. Example: ${{ $.renderFields('th')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Renders values for one or all of the fields *of one element* from the enclosing component's data source, wrapped in the given tag.";
	}

	@Override
	public String getLongDescription() {
		return "This function evaluates the `value` expression(s) *for one element* and renders the result, wrapped in the given tag. If the `slot` argument is present, only the value for the given slot is rendered. If no slot is given, this function renders all fields for one value, wrapped in the HTML element given in the `tag` argument.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${renderFields('li', 'label)}", "Render the value of the `label` slot of the current element")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Works only during page rendering in Template nodes.",
			"This function can only be used inside a component that iterates over the values of a data source."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
	}
}