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

import org.w3c.dom.*;

/**
 *
 *
 */
public class DOMAttribute implements Attr, DOMImportable, DOMAdoptable {

	private DOMElement parent = null;
	private boolean specified = false;
	private boolean isId      = false;
	private String name       = null;
	private String value      = null;
	private Page page         = null;
	
	public DOMAttribute(Page page, DOMElement parent, String name, String value) {
		this(page, parent, name, value, true, false);
	}
	
	public DOMAttribute(Page page, DOMElement parent, String name, String value, boolean specified, boolean isId) {
		
		this.page      = page;
		this.specified = specified;
		this.isId      = isId;
		this.parent    = parent;
		this.name      = name;
		this.value     = value;
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o instanceof DOMAttribute) {
			
			return ((DOMAttribute)o).hashCode() == hashCode();
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return (name.hashCode() * 31) + value.hashCode();
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	@Override
	public String getNodeName() {
		return name;
	}

	@Override
	public String getNodeValue() throws DOMException {
		return value;
	}

	@Override
	public void setNodeValue(String value) throws DOMException {
		this.value = value;
	}

	@Override
	public short getNodeType() {
		return ATTRIBUTE_NODE;
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
		
		if (parent != null) {
			
			String previousAttributeName = parent.getOffsetAttributeName(name, -1);
			if (previousAttributeName != null) {

				return parent.getAttributeNode(previousAttributeName);
			}
		}
		
		return null;
	}

	@Override
	public Node getNextSibling() {

		if (parent != null) {
			
			String nextAttributeName = parent.getOffsetAttributeName(name, 1);
			if (nextAttributeName != null) {

				return parent.getAttributeNode(nextAttributeName);
			}
		}
		
		return null;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public Document getOwnerDocument() {
		return page;
	}

	@Override
	public Node insertBefore(Node node, Node node1) throws DOMException {
		return null;
	}

	@Override
	public Node replaceChild(Node node, Node node1) throws DOMException {
		return null;
	}

	@Override
	public Node removeChild(Node node) throws DOMException {
		return null;
	}

	@Override
	public Node appendChild(Node node) throws DOMException {
		return null;
	}

	@Override
	public boolean hasChildNodes() {
		return false;
	}

	@Override
	public Node cloneNode(boolean bln) {
		return null;
	}

	@Override
	public void normalize() {
	}

	@Override
	public boolean isSupported(String string, String string1) {
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
		return getNodeName();
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
	public short compareDocumentPosition(Node node) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getTextContent() throws DOMException {
		return value;
	}

	@Override
	public void setTextContent(String text) throws DOMException {
		this.value = text;
	}

	@Override
	public boolean isSameNode(Node node) {
		return getNodeName().equals(node.getNodeName());
	}

	@Override
	public String lookupPrefix(String string) {
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String string) {
		return true;
	}

	@Override
	public String lookupNamespaceURI(String string) {
		return null;
	}

	@Override
	public boolean isEqualNode(Node node) {
		return equals(node);
	}

	@Override
	public Object getFeature(String string, String string1) {
		return null;
	}

	@Override
	public Object setUserData(String string, Object o, UserDataHandler udh) {
		return null;
	}

	@Override
	public Object getUserData(String string) {
		return null;
	}

	@Override
	public String getName() {
		return getNodeName();
	}

	@Override
	public boolean getSpecified() {
		return specified;
	}

	@Override
	public String getValue() {
		return getNodeValue();
	}

	@Override
	public void setValue(String value) throws DOMException {
		setNodeValue(value);
	}

	@Override
	public Element getOwnerElement() {
		return parent;
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		return null;
	}

	@Override
	public boolean isId() {
		return isId;
	}
	
	public void setParent(DOMElement parent) {
		this.parent = parent;
	}

	// ----- interface DOMAdoptable -----
	@Override
	public Node doAdopt(final Page _page) throws DOMException {
		
		this.page = _page;
		
		return this;
	}

	// ----- interface DOMImportable -----
	@Override
	public Node doImport(Page newPage) throws DOMException {
		
		Attr newAttr = newPage.createAttribute(name);
		newAttr.setValue(value);
		
		return newAttr;
	}
}
