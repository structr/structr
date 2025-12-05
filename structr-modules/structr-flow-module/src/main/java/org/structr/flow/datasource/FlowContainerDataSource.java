/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.flow.datasource;

import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.flow.impl.FlowContainer;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.RenderContext;
import org.structr.web.function.UiFunction;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders dynamic markup for the results of a {@link FlowContainer}.
 *
 * The flow query is assembled stored in a DOMNode attribute (flowQuery) and assembled 
 */
public class FlowContainerDataSource implements GraphDataSource<Iterable<GraphObject>> {

	@Override
	public Iterable<GraphObject> getData(final ActionContext actionContext, final NodeInterface referenceNode) throws FrameworkException {

		final RenderContext renderContext = (RenderContext) actionContext;
		final Traits traits               = Traits.of(StructrTraits.DOM_NODE);

		if (traits.hasKey(DOMNodeTraitDefinition.FLOW_PROPERTY)) {

			final PropertyKey<NodeInterface> flowKey = Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FLOW_PROPERTY);
			final NodeInterface flowNode             = referenceNode.getProperty(flowKey);

			if (flowNode != null) {

				try {

					final FlowContainer flow = flowNode.as(FlowContainer.class);
					final Object result = Scripting.evaluate(renderContext, referenceNode, "${flow('" + flow.getEffectiveName() + "')}", "flow query");

					if (result instanceof Iterable) {

						return FlowContainerDataSource.map((Iterable) result);

					} else if (result instanceof Object[]) {

						return (List<GraphObject>) UiFunction.toGraphObject(result, 1);

					} else if (result != null) {

						// Handle single result.
						List<GraphObject> results = new ArrayList<>();
						results.add((GraphObject) UiFunction.toGraphObject(result, 1));

						return results;
					}

				} catch (UnlicensedScriptException ex) {
					ex.log(LoggerFactory.getLogger(FlowContainerDataSource.class));
				}
			}
		}

		return null;
	}

	// ----- public static methods -----
	public static Iterable<GraphObject> map(final Iterable<Object> src) {

		return Iterables.map((Object t) -> {

			if (t instanceof GraphObject) {
				return (GraphObject)t;
			} else if (t instanceof Map) {
				return Function.toGraphObjectMap((Map)t);
			} else {

				return (GraphObject)UiFunction.toGraphObject(t, 1);
			}

		}, src);

	}
}
