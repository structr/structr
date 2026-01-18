/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.flow.impl;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.traits.definitions.FlowContainerTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.List;
import java.util.Map;

public class FlowFunction extends Function<Object, Object> {

	public static final String USAGE    = "Usage: ${flow(name)}";
	public static final String USAGE_JS = "Usage: ${{ Structr.flow(name) }}";

	public FlowFunction() {
	}

	@Override
	public String getName() {
		return "flow";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name");
	}

	@Override
	public String getRequiredModule() {
		return "api-builder";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				final String name                 = (String)sources[0];
				final PropertyKey<String> nameKey = Traits.of(StructrTraits.FLOW_CONTAINER).key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY);
				Map<String, Object> parameters    = null;

				final NodeInterface containerNode = StructrApp.getInstance(ctx.getSecurityContext()).nodeQuery(StructrTraits.FLOW_CONTAINER).key(nameKey, name).getFirst();

				if (sources.length > 1 && sources[1] instanceof Map) {
					parameters = (Map)sources[1];
				}

				if (containerNode != null) {

					final FlowContainer container = containerNode.as(FlowContainer.class);
					final FlowNode node           = container.getStartNode();

					if (node != null) {

						final Context context = new Context(caller instanceof GraphObject ? (GraphObject)caller : null);

						// Inject given parameter object into context
						if (parameters != null) {

							for (Map.Entry<String, Object> entry : parameters.entrySet()) {
								context.setParameter(entry.getKey(), entry.getValue());
							}

						} else {

							// If parameters are given in key,value format e.g. from StructrScript
							if (sources.length >= 3 && sources.length % 2 != 0) {

								for (int c = 1; c < sources.length; c += 2) {
									context.setParameter(sources[c].toString(), sources[c + 1]);
								}
							}
						}

						final FlowEngine engine = new FlowEngine(context);
						final FlowResult result = engine.execute(node);

						return result.getResult();

					} else {

						logger.warn("FlowContainer {} does not have a start node set", container.getUuid());
					}

				} else {

					logger.error("FlowContainer {} does not exist", name);
					throw new FrameworkException(422, "FlowContainer " + name + " does not exist");
				}
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
		);
	}

	@Override
	public String getShortDescription() {
		return "Executes a given Flow and returns the evaluation result.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("name", "effective name of the Flow"),
			Parameter.optional("parameterMap", "parameters")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			${{
				let result = $.flow('package1.flow1', {
				    parameter1 : 42,
				    parameter2 : 3.14
				});
			}}
			""", "Execute the Flow \"package1.flow1\"")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The effective name is the combined name of the Flow plus all its parent packages.",
			"In a StructrScript environment, parameters can be passed as pairs of 'key1', 'value1'."
		);
	}
}
