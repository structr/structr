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
import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.datasources.Channel;
import org.structr.core.datasources.ChannelResult;
import org.structr.core.entity.DataAdapter;
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
import org.structr.web.datasource.TagWithCSSInfo;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

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

		if (!ctx.isRenderContext()) {

			return null;
		}

		final RenderFieldsFunction func       = new RenderFieldsFunction();
		final RenderContext renderContext     = (RenderContext) ctx;
		final List<String> wrapperElements    = splitAndTrim(getStringOrNull(sources, 0), " ");
		final String slot                     = getStringOrNull(sources, 1);
		final TagWithCSSInfo outerWrapper     = getWrapperElement(getOrNull(wrapperElements, 0));
		final String innerWrapper             = getOrNull(wrapperElements, 1);

		if (caller instanceof NodeInterface n && n.is(StructrTraits.DOM_NODE)) {

			final DOMNode domNode                    = n.as(DOMNode.class);
			final DOMNode component                  = domNode.getClosestComponent();
			final ComponentConfiguration config      = component.getComponentConfiguration();
			final DataAdapter dataAdapter            = config.getDataAdapter();
			final Channel<GraphObject> sourceChannel = config.getDataSource();
			final String dataKey                     = dataAdapter.getDataKey();
			final GraphObject previousValue          = renderContext.getDataNode(dataKey);

			if (sourceChannel != null) {

				final String reloadBehaviour  = domNode.getReloadBehaviourForComponent();
				final String selectionChannel = config.getSelectionChannel();
				final ChannelInput input      = config.getChannelInput(renderContext);
				final AsyncBuffer buffer      = renderContext.getBuffer();
				final String role             = domNode.getRoleForComponent();
				final String resets           = getChannelDependencies(domNode, selectionChannel);

				final ChannelResult<GraphObject> result = sourceChannel.getResult(renderContext, input);

				for (final GraphObject item : result.getData()) {

					renderContext.putDataObject(dataKey, item);

					if (outerWrapper != null) {

						final Map<String, String> data = new LinkedHashMap<>();
						final Set<String> additionalCss = new LinkedHashSet<>();
						final String uuid = item.getUuid();

						if ("controller".equals(role)) {

							if (selectionChannel != null) {

								switch (reloadBehaviour) {

									case "partial":
									case "others":

										// partial reload is triggered via pagination mechanism
										data.put("data-structr-success-target", "[data-channel]");
										break;

									case "page":

										data.put("data-structr-success-target", "url:");
										break;

									default:
										data.put("data-structr-success-target", reloadBehaviour);
										break;

								}

								data.put("data-structr-events", "click");
								data.put("data-structr-target", selectionChannel);
								data.put("data-" + selectionChannel, uuid);

								additionalCss.add("controller");

								final String selectedId = renderContext.getChannelValue(selectionChannel);
								if (selectedId != null && uuid != null && uuid.equals(selectedId)) {

									additionalCss.add("selected");
								}

								if (StringUtils.isNotEmpty(resets)) {

									data.put("data-resets", resets);
								}
							}
						}

						outerWrapper.formatStartTag(buffer, data, additionalCss);
					}

					func.applyTemplates(renderContext, dataAdapter, domNode, innerWrapper, slot, true);

					if (outerWrapper != null) {
						outerWrapper.formatEndTag(buffer);
					}
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
			Usage.structrScript("Usage: ${renderEach(tag)} or ${renderEach(tag, slot)}."),
			Usage.javaScript("Usage: ${{ $.renderEach(tag)}} or ${{ $.renderEach(tag, slot)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Renders the filtered contents of a component's enclosing data source according to the data adapter configuration.";
	}

	@Override
	public String getLongDescription() {
		return "This function iterates over the paginated and filtered elements from the component's data source, evaluates the `value` expression(s) and renders the result, wrapped in the given tag. If the `slot` argument is present, only the value for the given slot is rendered. If no slot is given, this function renders all fields for all values, wrapped in the HTML element given in the `tag` argument.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${renderEach('li', 'label')}", "Render list items"),
			Example.structrScript("${renderEach('th td')}", "Render table rows and cells"),
			Example.javaScript("${{ $.renderEach('li', 'label') }}", "Render list items"),
			Example.javaScript("${{ $.renderEach('th td') }}", "Render table rows and cells")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.optional("tag", "tag to wrap the content in"),
			Parameter.optional("slot", "slot to fetch content from")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Works only during page rendering in Template nodes.",
			"This function implements the core logic of data-driven components."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
	}

	// ----- private methods -----
	private String getChannelDependencies(DOMNode node, final String channel) throws FrameworkException {

		if (channel == null) {
			return null;
		}

		final Page page          = node.getOwnerDocument();
		final Set<String> result = new LinkedHashSet<>();

		if (page != null) {

			final Map<String, Set<String>> dependencies = new LinkedHashMap<>();
			final Deque<String> queue                   = new LinkedList<>();

			for (final NodeInterface n : page.getAllChildNodes()) {

				final ComponentConfiguration config = n.as(DOMNode.class).getComponentConfiguration();
				if (config != null) {

					final String selectionChannel = config.getSelectionChannel();
					if (selectionChannel != null) {

						final Channel sourceChannel = config.getDataSource();
						if (sourceChannel != null) {

							final String source = sourceChannel.getName();
							final String target = selectionChannel;

							dependencies.computeIfAbsent(source, key -> new LinkedHashSet<>()).add(target);
						}
					}
				}
			}


			queue.push(channel);

			while (!queue.isEmpty()) {

				final String current       = queue.pop();
				final Set<String> mappings = dependencies.get(current);

				if (mappings != null) {
					for (final String dependency : mappings) {

						if (result.add(dependency)) {

							queue.push(dependency);
						}
					}
				}
			}

		}

		return StringUtils.join(result, " ");
	}
}
