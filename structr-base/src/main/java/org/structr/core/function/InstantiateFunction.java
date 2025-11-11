/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.api.graph.Node;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeFactory;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class InstantiateFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "instantiate";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("node");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof Node)) {

				throw new IllegalArgumentException();
			}

			return new NodeFactory(ctx.getSecurityContext()).instantiate((Node)sources[0]);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${{$.instantiate(node)}}. Example: ${{$.instantiate(result.node)}}"),
			Usage.structrScript("Usage: ${instantiate(node)}. Example: ${instantiate(result.node)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Instantiates the given Neo4j node into a Structr node.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
