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

import org.structr.web.entity.html.Object;
import org.w3c.dom.*;

import java.util.LinkedHashMap;

/**
 *
 *
 */
public class Html5DocumentType implements DocumentType {

	private Page parent = null;
	
	public Html5DocumentType(Page parent) {
		this.parent = parent;
	}
	
	@Override
	public String getName() {
		return "html";
	}

	@Override
	public NamedNodeMap getEntities() {
		return new StructrNamedNodeMap();
	}

	@Override
	public NamedNodeMap getNotations() {
		return new StructrNamedNodeMap();
	}

	@Override
	public String getPublicId() {
		return null;
	}

	@Override
	public String getSystemId() {
		return null;
	}

	@Override
	public String getInternalSubset() {
		return null;
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getNodeName() {
		return getName();
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
	}

	@Override
	public short getNodeType() {
		return DOCUMENT_TYPE_NODE;
	}

	@Override
	public Node getParentNode() {
		return parent;
	}

	@Override
	public NodeList getChildNodes() {
		return null;
	}

	@Override
	public Node getFirstChild() {
		return null;
	}

	@Override
	public Node getLastChild() {
		return null;
	}

	@Override
	public Node getPreviousSibling() {
		// document type has no prev. sibling
		return null;
	}

	@Override
	public Node getNextSibling() {
		// next sibling is always the document element
		return getOwnerDocument().getDocumentElement();
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public Document getOwnerDocument() {
		return parent;
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		return null;
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		return null;
	}

	@Override
	public Node removeChild(Node oldChild) throws DOMException {
		return null;
	}

	@Override
	public Node appendChild(Node newChild) throws DOMException {
		return null;
	}

	@Override
	public boolean hasChildNodes() {
		return false;
	}

	@Override
	public Node cloneNode(boolean deep) {
		return null;
	}

	@Override
	public void normalize() {
	}

	@Override
	public boolean isSupported(String feature, String version) {
		return false;
	}

	@Override
	public String getNamespaceURI() {
		return parent.getNamespaceURI();
	}

	@Override
	public String getPrefix() {
		return null;
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	@Override
	public String getBaseURI() {
		return null;
	}

	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		return -1;
	}

	@Override
	public String getTextContent() throws DOMException {
		return null;
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
	}

	@Override
	public boolean isSameNode(Node other) {
		return equals(other);
	}

	@Override
	public String lookupPrefix(String namespaceURI) {
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		return true;
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		return null;
	}

	@Override
	public boolean isEqualNode(Node arg) {
		return equals(arg);
	}

	@Override
	public Object getFeature(String feature, String version) {
		return null;
	}

	@Override
	public java.lang.Object getUserData(String key) {
		return null;
	}

	@Override
	public java.lang.Object setUserData(String key, java.lang.Object data, UserDataHandler handler) {
		return null;
	}
	
	// ----- nested classes -----
	private static class StructrNamedNodeMap extends LinkedHashMap<String, Node> implements NamedNodeMap {

		@Override
		public Node getNamedItem(String name) {
			return get(name);
		}

		@Override
		public Node setNamedItem(Node arg) throws DOMException {
			return put(arg.getNodeName(), arg);
		}

		@Override
		public Node removeNamedItem(String name) throws DOMException {
			return remove(name);
		}

		@Override
		public Node item(int index) {
			
			int pos = 0;
			
			for (Node node : this.values()) {
				if (pos++ == index) {
					return node;
				}
			}
			
			return null;
		}

		@Override
		public int getLength() {
			return size();
		}

		@Override
		public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
			return getNamedItem(localName);
		}

		@Override
		public Node setNamedItemNS(Node arg) throws DOMException {
			return setNamedItem(arg);
		}

		@Override
		public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
			return removeNamedItem(localName);
		}
		
	}
}
