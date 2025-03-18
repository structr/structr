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
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;

public class InsertHtmlFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_INSERT_HTML    = "Usage: ${insert_html(parent, html)}. Example: ${insert_html(this, html)}";
	public static final String ERROR_MESSAGE_INSERT_HTML_JS = "Usage: ${{Structr.insertHtml(parent, html)}}. Example: ${{Structr.insertHtml(this, html)}}";

	@Override
	public String getName() {
		return "insert_html";
	}

	@Override
	public String getSignature() {
		return "parent, html";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndTypes(sources, 2, NodeInterface.class, String.class);

		final NodeInterface parent = (NodeInterface)sources[0];
		final String html          = (String) sources[1];

		return InsertHtmlFunction.apply(ctx.getSecurityContext(), parent, html);
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_INSERT_HTML_JS : ERROR_MESSAGE_INSERT_HTML);
	}

	@Override
	public String shortDescription() {
		return "Inserts a new HTML subtree into the DOM";
	}

	public static DOMNode apply(final SecurityContext securityContext, final NodeInterface parent, final String htmlSource) throws FrameworkException {

		final Importer importer = new Importer(securityContext, htmlSource, null, null, false, false, false, false);

		importer.setIsDeployment(true);
		importer.setCommentHandler(new DeploymentCommentHandler());

		importer.parse(true);

		final DOMNode domNodeParent = parent.as(DOMNode.class);

		return importer.createChildNodes(domNodeParent, domNodeParent.getOwnerDocument(), true);
	}
}
