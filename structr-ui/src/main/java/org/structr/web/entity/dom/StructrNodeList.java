package org.structr.web.entity.dom;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Christian Morgner
 */
public class StructrNodeList<T extends Node> extends ArrayList<T> implements NodeList {

	public StructrNodeList() {
		super();
	}

	public StructrNodeList(List<T> children) {
		super(children);
	}

	@Override
	public Node item(int i) {

		return get(i);
	}

	@Override
	public int getLength() {

		return size();
	}
	
	public void addAll(NodeList nodeList) {
		
		int len = nodeList.getLength();
		
		for (int i=0; i<len; i++) {
			add((T)nodeList.item(i));
		}
	}
}	
