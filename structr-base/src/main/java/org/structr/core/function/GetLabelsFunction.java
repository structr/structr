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
package org.structr.core.function;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GetLabelsFunction extends CoreFunction {

	@Override
	public String getName() {

		return "getLabels";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("node");
	}

	@Override
	public String getShortDescription() {

		return "Returns a collection of all labels of the given node.";
	}

	@Override
	public String getLongDescription() {

		return "";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasLengthAndAllElementsNotNull(sources, 1);

		if (!(sources[0] instanceof NodeInterface)) {

			logParameterError(caller, sources, "Expected node as first argument!", ctx.isJavaScriptContext());
		}

		final NodeInterface node = (NodeInterface)sources[0];

		return Iterables.toList(node.getNode().getLabels());
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.javaScript("getLabels(node)"), Usage.structrScript("getLabels(node)"));
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Database;
	}
}
