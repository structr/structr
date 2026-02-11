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
package org.structr.web.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;

import java.util.List;

public class InsertHtmlFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "insertHtml";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("parent, html");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndTypes(sources, 2, NodeInterface.class, String.class);

		final NodeInterface parent = (NodeInterface)sources[0];
		final String html          = (String) sources[1];

		return InsertHtmlFunction.apply(ctx.getSecurityContext(), parent, html);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${insertHtml(parent, html)}. Example: ${insertHtml(this, html)}"),
			Usage.javaScript("Usage: ${{Structr.insertHtml(parent, html)}}. Example: ${{Structr.insertHtml(this, html)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Inserts a new HTML subtree into the DOM.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
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
