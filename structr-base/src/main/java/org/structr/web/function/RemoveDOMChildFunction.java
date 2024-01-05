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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.command.RemoveCommand;

public class RemoveDOMChildFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_REMOVE_DOM_CHILD    = "Usage: ${remove_dom_child(parent, child)}. Example: ${remove_dom_child(this, child)}";
	public static final String ERROR_MESSAGE_REMOVE_DOM_CHILD_JS = "Usage: ${{Structr.removeDomChild(parent, child)}}. Example: ${{Structr.removeDomChild(this, child)}}";

	@Override
	public String getName() {
		return "remove_dom_child";
	}

	@Override
	public String getSignature() {
		return "parent, child";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

		if (sources[0] instanceof DOMElement) {

			final DOMElement parent = (DOMElement) sources[0];

			if (sources[1] instanceof DOMElement) {

				final DOMNode child = (DOMNode) sources[1];

				RemoveDOMChildFunction.apply(ctx.getSecurityContext(), parent, child);
			}
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_REMOVE_DOM_CHILD_JS : ERROR_MESSAGE_REMOVE_DOM_CHILD);
	}

	@Override
	public String shortDescription() {
		return "Removes a node from the DOM";
	}

	public static void apply(final SecurityContext securityContext, final DOMElement parent, final DOMNode child) throws FrameworkException {

		parent.removeChild(child);

		RemoveCommand.recursivelyRemoveNodesFromPage(child, securityContext);
	}
}
