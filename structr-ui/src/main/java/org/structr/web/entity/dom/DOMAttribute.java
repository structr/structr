package org.structr.web.entity.dom;

import org.structr.web.common.HtmlProperty;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/**
 *
 * @author Christian Morgner
 */
public class DOMAttribute implements Node {

	private DOMElement parent = null;
	private HtmlProperty key  = null;
	private String value      = null;
	
	public DOMAttribute(DOMElement parent, HtmlProperty key, String value) {
		
		this.parent = parent;
		this.value = value;
		this.key = key;
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
		return (getNodeName().hashCode() * 31) + value.hashCode();
	}
	
	@Override
	public String getNodeName() {
		return key.getOriginalName();
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
		return null;
	}

	@Override
	public Node getNextSibling() {
		return null;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public Document getOwnerDocument() {
		return parent.getOwnerDocument();
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
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setTextContent(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isSameNode(Node node) {
		return getNodeName().equals(node.getNodeName());
	}

	@Override
	public String lookupPrefix(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isDefaultNamespace(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String lookupNamespaceURI(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isEqualNode(Node node) {
		return equals(node);
	}

	@Override
	public Object getFeature(String string, String string1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object setUserData(String string, Object o, UserDataHandler udh) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getUserData(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
