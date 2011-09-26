/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.resource.wrapper;

import java.util.LinkedList;
import java.util.List;
import org.structr.core.node.NodeAttribute;

/**
 *
 * @author Christian Morgner
 */
public class NodeAttributeWrapper {

	private List<NodeAttribute> attributes = null;

	public NodeAttributeWrapper() {
		this.attributes = new LinkedList<NodeAttribute>();
	}

	public void add(String key, Object value) {

		add(key, value, "String");
	}

	public void add(String key, Object value, String type) {

		attributes.add(new NodeAttribute(key, value));
	}

	public List<NodeAttribute> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		for(NodeAttribute attr : attributes) {

			builder.append(attr.getKey()).append(" = '").append(attr.getValue()).append("', ");
		}

		return builder.toString();
	}
}
