/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.flow;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.FlowEngine;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class FlowFunction extends Function<Object, Object> {

	public static final String USAGE    = "Usage: ${flow(name)}";
	public static final String USAGE_JS = "Usage: ${Structr.flow(name)}";

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof String) {

				final String name   = (String)sources[0];
				final FlowNode node = StructrApp.getInstance(ctx.getSecurityContext()).nodeQuery(FlowNode.class).and(FlowNode.name, name).getFirst();

				if (node != null) {

					final FlowEngine engine = new FlowEngine();
					final FlowResult result = engine.execute(node);

					return result.getResult();
				}
			}
		}

		return usage(false);

	}

	@Override
	public String usage(final boolean inJavaScriptContext) {


		if (inJavaScriptContext) {
			return USAGE_JS;
		}

		return USAGE;
	}

	@Override
	public String shortDescription() {
		return "Returns the evaluation result of the Flow with the given name.";
	}

	@Override
	public String getName() {
		return "flow";
	}

}
