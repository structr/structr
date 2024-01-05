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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 */
public class DOMNodeList<T extends Node> extends ArrayList<T> implements NodeList {

	public DOMNodeList() {
		super();
	}

	public DOMNodeList(List<T> children) {
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
