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
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class CopyPermissionsFunction extends CoreFunction {

	public static final String ERROR_MESSAGE    = "Usage: ${copy_permissions(source, target[, overwrite])}. Example: ${copy_permissions(this, this.child)}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{ Structr.copy_permissions(source, target[, overwrite]); }}. Example: ${{ Structr.copy_permissions(Structr.this, Structr.this.child); }}";

	@Override
	public String getName() {
		return "copy_permissions";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("source, target [, overwrite ]");
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
	public String usage(final boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String getShortDescription() {
		return "Copies the security configuration of an entity to another entity.";
	}
}
