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
package org.structr.web.entity.dom;

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.Renderable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;

/**
 *
 *
 */

public interface DocumentFragment extends DOMNode, org.w3c.dom.DocumentFragment, NonIndexed {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("DocumentFragment");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DocumentFragment"));
		type.setExtends(URI.create("#/definitions/DOMNode"));
		type.setCategory("html");

		// ----- interface org.w3c.dom.Node -----
		type.overrideMethod("getNodeName",   false, "return \"#document-fragment\";");
		type.overrideMethod("getLocalName",  false, "return null;");
		type.overrideMethod("getNodeValue",  false, "return null;");
		type.overrideMethod("hasAttributes", false, "return false;");
		type.overrideMethod("getAttributes", false, "return null;");
		type.overrideMethod("getNodeType",   false, "return DOCUMENT_FRAGMENT_NODE;");
		type.overrideMethod("setNodeValue",  false, "");

		// ----- interface DOMNode -----
		type.overrideMethod("getContextName", false, "return \"DocumentFragment\";");
		type.overrideMethod("isSynced",       false, "return false;");
		type.overrideMethod("contentEquals",  false, "return false;");
		type.overrideMethod("updateFromNode", false, "");
		type.overrideMethod("render",         false, DocumentFragment.class.getName() + ".render(this, arg0, arg1);");
		type.overrideMethod("renderContent",  false, "");
		type.overrideMethod("updateFromNode", false, "");
		type.overrideMethod("doAdopt",        false, "return null;");
		type.overrideMethod("doImport",       false, "return arg0.createDocumentFragment();");

		// ----- interface org.w3c.dom.Node -----
		type.overrideMethod("getLocalName",          false, "return null;");
		type.overrideMethod("getNodeType",           false, "return DOCUMENT_FRAGMENT_NODE;");
		type.overrideMethod("getNodeName",           false, "return \"#document-fragment\";");
		type.overrideMethod("getNodeValue",          false, "return null;");
		type.overrideMethod("setNodeValue",          false, "");
		type.overrideMethod("getAttributes",         false, "return null;");
		type.overrideMethod("hasAttributes",         false, "return false;");
	}}

	public static void render(final DocumentFragment thisNode, final RenderContext renderContext, final int depth) throws FrameworkException {

		NodeList _children = thisNode.getChildNodes();
		int len            = _children.getLength();

		for (int i=0; i<len; i++) {

			Node child = _children.item(i);

			if (child != null && child instanceof Renderable) {

				((Renderable)child).render(renderContext, depth);
			}
		}

	}
}
