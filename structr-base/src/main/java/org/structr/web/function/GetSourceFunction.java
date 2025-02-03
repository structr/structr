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
package org.structr.web.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.dom.DOMNode;

public class GetSourceFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_GET_SOURCE    = "Usage: ${get_source(element, editMode)}. Example: ${get_source(this, 1)}";
	public static final String ERROR_MESSAGE_GET_SOURCE_JS = "Usage: ${{Structr.getSource(element, editMode)}}. Example: ${{Structr.getSource(this, 1)}}";

	@Override
	public String getName() {
		return "get_source";
	}

	@Override
	public String getSignature() {
		return "element, editMode";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof NodeInterface n && n.is("DOMNode") && sources[1] instanceof Number) {

				final DOMNode node    = n.as(DOMNode.class);
				final Number editMode = (Number)sources[1];
				final EditMode mode   = RenderContext.editMode(Integer.toString(editMode.intValue()));
				final String content  = node.getContent(mode);

				return content;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_SOURCE_JS : ERROR_MESSAGE_GET_SOURCE);
	}

	@Override
	public String shortDescription() {
		return "Returns the rendered HTML content for the given element.";
	}
}
