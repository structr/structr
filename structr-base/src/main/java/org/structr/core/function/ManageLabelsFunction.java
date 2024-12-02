/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;

import java.util.List;

public abstract class ManageLabelsFunction extends CoreFunction {

	@Override
	public String getSignature() {
		return "node, labels";
	}

	// ----- protected methods -----
	public void validateArguments(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasLengthAndAllElementsNotNull(sources, 2);

		if (!(sources[0] instanceof NodeInterface)) {

			logParameterError(caller, sources, "Expected node as first argument!", ctx.isJavaScriptContext());
		}

		if (!(sources[1] instanceof List)) {

			logParameterError(caller, sources, "Expected list as second argument!", ctx.isJavaScriptContext());
		}
	}
}
