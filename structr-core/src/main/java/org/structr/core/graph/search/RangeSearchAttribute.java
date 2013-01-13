/*
 *  Copyright (C) 2010-2013 Axel Morgner
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
package org.structr.core.graph.search;

import org.structr.core.property.PropertyKey;

/**
 * An experimental search attribute to support range queries in structr. This
 * is not used yet.
 *
 * @author Christian Morgner
 */
public class RangeSearchAttribute extends SearchAttribute {

	private PropertyKey searchKey = null;
	private Object rangeStart = null;
	private Object rangeEnd = null;

	public RangeSearchAttribute(final PropertyKey searchKey, final Object rangeStart, final Object rangeEnd, final SearchOperator searchOp) {

		setSearchOperator(searchOp);

		this.searchKey  = searchKey;
		this.rangeStart = rangeStart;
		this.rangeEnd   = rangeEnd;
	}

	@Override
	public Object getAttribute() {
		return null;
	}

	@Override
	public void setAttribute(Object attribute) {
	}

	public PropertyKey getKey() {
		return searchKey;
	}

	public String getValue() {
		
		StringBuilder buf = new StringBuilder();
	
		buf.append(searchKey.dbName());
		buf.append(":[\"");
		buf.append(rangeStart.toString());
		buf.append("\" TO \"");
		buf.append(rangeEnd.toString());
		buf.append("\"]");
		
		return buf.toString();
	}
}
