/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.Renderable;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Christian Morgner
 */

public class DocumentFragment extends DOMNode implements org.w3c.dom.DocumentFragment {

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeName() {
		return "#document-fragment";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String string) throws DOMException {
	}

	@Override
	public short getNodeType() {
		return DOCUMENT_FRAGMENT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	@Override
	public boolean contentEquals(final DOMNode otherNode) {
		return false;
	}

	@Override
	public void updateFromNode(final DOMNode newNode) throws FrameworkException {
		// do nothing
	}

	@Override
	public void updateFromPropertyMap(final PropertyMap properties) throws FrameworkException {
		// do nothing
	}

	@Override
	public boolean isSynced() {
		return false;
	}

	// ----- interface Renderable -----
	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		NodeList _children = getChildNodes();
		int len            = _children.getLength();

		for (int i=0; i<len; i++) {

			Node child = _children.item(i);

			if (child != null && child instanceof Renderable) {

				((Renderable)child).render(securityContext, renderContext, depth);
			}
		}

	}

	@Override
	public Node doAdopt(Page newPage) throws DOMException {

		// do nothing, only children of DocumentFragments are
		// adopted
		return null;
	}

	@Override
	public Node doImport(Page newPage) throws DOMException {
		// simply return an empty DocumentFragment, as the importing
		// will be done by the Page method if deep importing is enabled.
		return newPage.createDocumentFragment();
	}
}
