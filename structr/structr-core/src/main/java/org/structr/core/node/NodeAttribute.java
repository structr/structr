/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
package org.structr.core.node;

import org.structr.common.PropertyKey;

/**
 * A parameterized node attribute to identify a node attribute
 * in {@see FindNodeCommand}.
 *
 * @author cmorgner
 */
public class NodeAttribute {

	private String key = null;
	private Object value = null;

	public NodeAttribute() {
	}

	public NodeAttribute(final String key, final Object value) {
		this.key = key;
		this.value = value;
	}
        
	public NodeAttribute(final PropertyKey key, final Object value) {
		this.key = key.name();
		this.value = value;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(final String key) {
		this.key = key;
	}
        
        /**
	 * @param key the key to set
	 */
	public void setKey(final PropertyKey key) {
		this.key = key.name();
	}
	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(final Object value) {
		this.value = value;
	}

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		buf.append("NodeAttribute('");
		buf.append(key);
		buf.append("', '");
		buf.append(value);
		buf.append("')");

		return buf.toString();
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if(obj instanceof NodeAttribute) {
			return ((NodeAttribute)obj).hashCode() == hashCode();
		}

		return false;
	}
}
