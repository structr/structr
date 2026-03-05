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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.*;

public class RenderEachFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "renderEach";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("dataSource, tag [, slot]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndTypes(sources, 2, DataSource.class, String.class);

		if (!ctx.isRenderContext()) {

			return null;
		}

		final RenderFieldsFunction func       = new RenderFieldsFunction();
		final RenderContext renderContext     = (RenderContext) ctx;
		final SecurityContext securityContext = ctx.getSecurityContext();
		final DataSource dataSource           = (DataSource) sources[0];
		final List<String> wrapperElements    = splitAndTrim(getStringOrNull(sources, 1), " ");
		final String slot                     = getStringOrNull(sources, 2);
		final TagWithCSSInfo outerWrapper     = getWrapperElement(getOrNull(wrapperElements, 0));
		final String innerWrapper             = getOrNull(wrapperElements, 1);

		if (caller instanceof NodeInterface n && n.is(StructrTraits.DOM_NODE)) {

			final DOMNode domNode    = n.as(DOMNode.class);
			final AsyncBuffer buffer = renderContext.getBuffer();
			final String dataKey     = dataSource.getDataKey();
			final String channel     = dataSource.getDataKey();
			final String role        = domNode.getRoleForComponent();
			String selectedValue     = null;

			// save previous value
			final GraphObject previousValue = renderContext.getDataNode(dataKey);

			// selected value from channel for highlighting
			if (channel != null) {

				selectedValue = renderContext.getRequestParameter(channel);
			}

			for (final GraphObject item : dataSource.getValues(securityContext)) {

				renderContext.putDataObject(dataKey, item);

				if (outerWrapper != null) {

					final Map<String, String> data  = new LinkedHashMap<>();
					final Set<String> additionalCss = new LinkedHashSet<>();
					final String uuid               = item.getUuid();

					if ("controller".equals(role)) {

						data.put("data-structr-success-target", "[data-source='" + channel + "']");
						data.put("data-structr-events", "click");
						data.put("data-structr-target", channel);
						data.put("data-" + channel, uuid);

						additionalCss.add("controller");

						if (uuid.equals(selectedValue)) {
							additionalCss.add("selected");
						}
					}

					outerWrapper.formatStartTag(buffer, data, additionalCss);
				}

				func.applyTemplates(renderContext, dataSource, domNode, innerWrapper, slot, true);

				if (outerWrapper != null) {
					outerWrapper.formatEndTag(buffer);
				}
			}

			// restore previous value
			renderContext.putDataObject(dataKey, previousValue);

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${renderEach(dataSource, tag)} or ${renderEach(dataSource, tag, slot)}."),
			Usage.javaScript("Usage: ${{ $.renderEach(datasource, tag)}} or ${{ $.renderEach(dataSource, tag, slot)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Renders the filtered contents of the given datasource according to the datasource configuration.";
	}

	@Override
	public String getLongDescription() {
		return "This function iterates over the paginated and filtered elements from the given data source, evaluates the `value` expression(s) and renders the result, wrapped in the given tag. If the `slot` argument is present, only the value for the given slot is rendered. If no slot is given, this function renders all fields for all values, wrapped in the HTML element given in the `tag` argument.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${renderEach(currentDataSource, 'li', 'label')}", "Render list items"),
			Example.structrScript("${renderEach(currentDataSource, 'th td')}", "Render table rows and cells"),
			Example.javaScript("${{ $.renderEach(currentDataSource, 'li', 'label') }}", "Render list items"),
			Example.javaScript("${{ $.renderEach(currentDataSource, 'th td') }}", "Render table rows and cells")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("dataSource", "data source to render"),
			Parameter.optional("tag", "tag to wrap the content in"),
			Parameter.optional("slot", "slot to fetch content from")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Works only during page rendering in Template nodes.",
			"This function implements the core logic of data-driven components.",
			"The data source specification controls the rendering of data-driven components."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
	}
}
