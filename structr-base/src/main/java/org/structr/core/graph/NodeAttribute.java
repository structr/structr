/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.graph;

import org.structr.core.property.PropertyKey;

/**
 * A parameterized node attribute to identify a node.
 *
 *
 */
public class NodeAttribute<T> {

	private PropertyKey<T> key = null;
	private T value            = null;

	public NodeAttribute() {
	}

	public NodeAttribute(final PropertyKey<T> key, final T value) {
		this.key   = key;
		this.value = value;
	}

	/**
	 * @return the key
	 */
	public PropertyKey<T> getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(final PropertyKey<T> key) {
		this.key = key;
	}

	/**
	 * @return the value
	 */
	public T getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(final T value) {
		this.value = value;
	}

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		buf.append(key != null ? key.dbName() : "[null]");
		buf.append("=");
		buf.append(value);

		return buf.toString();
	}

	@Override
	public int hashCode() {

		if (key != null) {
			return key.hashCode();
		}

		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if(obj instanceof NodeAttribute) {
			return ((NodeAttribute)obj).hashCode() == hashCode();
		}

		return false;
	}
}
