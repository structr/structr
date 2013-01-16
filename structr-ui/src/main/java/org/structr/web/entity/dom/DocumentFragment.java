package org.structr.web.entity.dom;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
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
	
}
