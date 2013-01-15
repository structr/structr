package org.structr.web.entity.dom;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Christian Morgner
 */
public class StructrNodeList extends ArrayList<DOMNode> implements NodeList {

	public StructrNodeList() {
		super();
	}

	public StructrNodeList(List<DOMNode> children) {
		super(children);
	}

	//~--- methods ------------------------------------------------

	@Override
	public Node item(int i) {

		return get(i);
	}

	//~--- get methods --------------------------------------------

	@Override
	public int getLength() {

		return size();
	}
}	
