/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

package org.structr.core;

import java.util.LinkedList;
import java.util.List;
import org.structr.core.node.NodeAttribute;

/**
 * Wrapper class for JSON input via {@see JsonRestServlet}.
 *
 * @author Christian Morgner
 */
public class PropertySet {

	public enum PropertyFormat {
		NestedKeyValue,			// "properties" : [ { "key" : "name", "value" : "Test" }, ... ]
		NestedKeyValueType,		// "properties" : [ { "key" : "name", "value" : "Test", "type" : "String" }, ... ]
		FlatNameValue			// { "name" : "Test" }
	}

	private List<NodeAttribute> attributes = null;

	public PropertySet() {
		this.attributes = new LinkedList<NodeAttribute>();
	}

	/**
	 * Add a key-value-pair of type String to this property set.
	 *
	 * @param key the key
	 * @param value the value
	 */
	public void add(String key, Object value) {

		attributes.add(new NodeAttribute(key, value));
	}

//	/**
//	 * Add a key-value-pair of given type to this property set.
//	 *
//	 * @param key the key
//	 * @param value the value
//	 * @param type the type
//	 */
//	public void add(String key, Object value, String type) {
//
//		attributes.put(key, new NodeAttribute(key, value));
//	}

	/**
	 * Return the list of attributes in this property set.
	 *
	 * @return the list of attributes
	 */
	public List<NodeAttribute> getAttributes() {
		return new LinkedList<NodeAttribute>(attributes);
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
