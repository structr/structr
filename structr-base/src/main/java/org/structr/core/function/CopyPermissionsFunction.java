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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class CopyPermissionsFunction extends CoreFunction {

	@Override
	public String getName() {
		return "copyPermissions";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("source, target [, overwrite ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3);

			final Object source = sources[0];
			final Object target = sources[1];
			final Boolean overwrite = (sources.length == 3 && Boolean.TRUE.equals(sources[2]));

			if (source instanceof NodeInterface && target instanceof NodeInterface) {

				final NodeInterface sourceNode = (NodeInterface)source;
				final NodeInterface targetNode = (NodeInterface)target;

				sourceNode.copyPermissionsTo(ctx.getSecurityContext(), targetNode, overwrite);

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
			}

			return null;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${copyPermissions(source, target[, syncPermissions])}. Example: ${copyPermissions(this, this.child)}"),
			Usage.javaScript("Usage: ${{ $..copyPermissions(source, target[, syncPermissions]); }}. Example: ${{ $.copyPermissions($.this, $.this.child); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Copies the security configuration of an entity to another entity.";
	}

	@Override
	public String getLongDescription() {
		return "If the `syncPermissions` parameter is set to `true`, the permissions of existing security relationships are aligned between source and target nodes. If it is not set (or omitted) the function just adds the permissions to the existing permissions.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("sourceNode", "source node to copy permissions from"),
			Parameter.mandatory("targetNode",  "target node to copy permissions to"),
			Parameter.optional("syncPermissions", "synchronize permissions between source and target nodes")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function **only** changes target node permissions that are also present on the source node."
		);
	}
}
