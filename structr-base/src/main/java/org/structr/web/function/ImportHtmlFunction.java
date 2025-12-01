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
package org.structr.web.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.importer.Importer;

import java.util.List;

public class ImportHtmlFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "importHtml";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("parent, html");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

		if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.DOM_ELEMENT)) {

			final DOMElement parent = n.as(DOMElement.class);

			if (sources[1] instanceof String source) {

				final Importer importer = new Importer(ctx.getSecurityContext(), source, null, null, false, false, false, false);

				/*
				if (processDeploymentInfo) {
					importer.setIsDeployment(true);
					importer.setCommentHandler(new DeploymentCommentHandler());
				}

				// test: insert widget into Page object directly
				if (parent.equals(page)) {
					importer.setIsDeployment(true);
				}
				*/

				importer.parse(true);
				importer.createChildNodes(parent, parent.getOwnerDocument(), true);
			}
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${importHtml(parent, html)}. Example: ${importHtml(this, '<div></div>')}"),
			Usage.javaScript("Usage: ${{Structr.importHtml(parent, html)}}. Example: ${{Structr.importHtml(this, '<div></div>')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Imports HTML source code into an element.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
