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

import org.structr.common.error.FrameworkException;
import org.structr.schema.NonIndexed;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.Renderable;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 */

public interface DocumentFragment extends DOMNode, NonIndexed {
	/*

	@Override
	public String getContextName() {
		return "DocumentFragment";
	}

	@Override
	public String getNodeName() {
		return "#document-fragment";
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeValue() {
		return null;
	}

	@Override
	public void setNodeValue(final String value) {
	}

	@Override
	public short getNodeType() {
		return DOCUMENT_FRAGMENT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	public boolean hasAttributes() {
		return false;
	}

	@Override
	public boolean isSynced() {
		return false;
	}

	@Override
	public boolean contentEquals(final Node node) {
		return false;
	}

	public void updateFromNode(final DOMNode otherNode) throws FrameworkException {
	}

	public void renderContent(RenderContext renderContext, int depth) throws FrameworkException {
	}

	@Override
	public DOMNode doAdopt(final Page page) throws DOMException {
		return null;
	}

	@Override
	public Node doImport(Page page) throws DOMException {
		return page.createDocumentFragment();
	}

	@Override
	public void render(final RenderContext renderContext, final int depth) throws FrameworkException {

		NodeList _children = getChildNodes();
		int len            = _children.getLength();

		for (int i=0; i<len; i++) {

			Node child = _children.item(i);

			if (child != null && child instanceof Renderable) {

				((Renderable)child).render(renderContext, depth);
			}
		}

	}

	 */
}
