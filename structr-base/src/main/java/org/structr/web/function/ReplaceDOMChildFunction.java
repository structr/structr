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
import org.structr.web.common.DOMNodeContent;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;
import org.structr.websocket.command.RemoveCommand;

public class ReplaceDOMChildFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_REPLACE_DOM_CHILD    = "Usage: ${replace_dom_child(parent, child, html)}. Example: ${replace_dom_child(this, child, html)}";
	public static final String ERROR_MESSAGE_REPLACE_DOM_CHILD_JS = "Usage: ${{Structr.replaceDomChild(parent, child, html)}}. Example: ${{Structr.replaceDomChild(this, child, html)}}";

	@Override
	public String getName() {
		return "replace_dom_child";
	}

	@Override
	public String getSignature() {
		return "parent, child, html";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndTypes(sources, 3, DOMElement.class, DOMNode.class, String.class);

		final DOMElement parent = (DOMElement) sources[0];
		final DOMNode child     = (DOMNode) sources[1];
		final String html       = (String)sources[2];

		return ReplaceDOMChildFunction.apply(ctx.getSecurityContext(), parent, child, html);
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_REPLACE_DOM_CHILD_JS : ERROR_MESSAGE_REPLACE_DOM_CHILD);
	}

	@Override
	public String shortDescription() {
		return "Replaces a node from the DOM with new HTML";
	}

	public static DOMNode apply(final SecurityContext securityContext, final DOMElement parent, final DOMNode child, final String htmlSource) throws FrameworkException {

		final Importer importer = new Importer(securityContext, htmlSource, null, null, false, false, false, false);

		importer.setIsDeployment(true);
		importer.setCommentHandler(new DeploymentCommentHandler());

		if (importer.parse(true)) {

			final DOMNodeContent content = new DOMNodeContent();

			// read content
			content.loadFrom(child);

			// remove child
			parent.removeChild(child);

			final DOMNode newChild = importer.createChildNodes(parent, parent.getOwnerDocument(), true).as(DOMNode.class);

			// store existing content
			content.moveTo(newChild);

			// move the rest to trash
			RemoveCommand.recursivelyRemoveNodesFromPage(child, securityContext);

			return newChild;
		}

		return null;
	}
}
