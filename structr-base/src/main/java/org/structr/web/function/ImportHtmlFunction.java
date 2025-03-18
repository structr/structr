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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.importer.Importer;

public class ImportHtmlFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_IMPORT_HTML    = "Usage: ${import_html(parent, html)}. Example: ${import_html(this, '<div></div>')}";
	public static final String ERROR_MESSAGE_IMPORT_HTML_JS = "Usage: ${{Structr.importHtml(parent, html)}}. Example: ${{Structr.importHtml(this, '<div></div>')}}";

	@Override
	public String getName() {
		return "import_html";
	}

	@Override
	public String getSignature() {
		return "parent, html";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

		if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.DOM_ELEMENT)) {

			final DOMElement parent = n.as(DOMElement.class);

			if (sources[1] instanceof String) {

				final String source     = (String) sources[1];
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_IMPORT_HTML_JS : ERROR_MESSAGE_IMPORT_HTML);
	}

	@Override
	public String shortDescription() {
		return "Imports HTML source code into an element";
	}
}
