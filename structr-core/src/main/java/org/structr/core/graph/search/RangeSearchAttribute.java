/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph.search;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TermRangeQuery;

import org.structr.core.property.PropertyKey;

/**
 * Search attribute for range queries
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class RangeSearchAttribute extends SearchAttribute {
	
	private static final Logger logger                    = Logger.getLogger(RangeSearchAttribute.class.getName());

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

	@Override
	public PropertyKey getKey() {
		return searchKey;
	}

	@Override
	public String getValue() {

		Query q;
		if (rangeStart instanceof Date && rangeEnd instanceof Date) {
			
			q = NumericRangeQuery.newLongRange(searchKey.dbName(), ((Date) rangeStart).getTime(), ((Date) rangeEnd).getTime(), true, true);
			
		} else if (rangeStart instanceof Long && rangeEnd instanceof Long) {
			
			q = NumericRangeQuery.newLongRange(searchKey.dbName(), (Long) rangeStart, (Long) rangeEnd, true, true);
			
		} else if (rangeStart instanceof Float && rangeEnd instanceof Float) {
			
			q = NumericRangeQuery.newFloatRange(searchKey.dbName(), (Float) rangeStart, (Float) rangeEnd, true, true);
			
		} else if (rangeStart instanceof Double && rangeEnd instanceof Double) {
			
			q = NumericRangeQuery.newDoubleRange(searchKey.dbName(), (Double) rangeStart, (Double) rangeEnd, true, true);
			
		} else if (rangeStart instanceof Integer && rangeEnd instanceof Integer) {
			
			q = NumericRangeQuery.newIntRange(searchKey.dbName(), (Integer) rangeStart, (Integer) rangeEnd, true, true);
			
		} else {
			
			q = new TermRangeQuery(searchKey.dbName(), rangeStart.toString(), rangeEnd.toString(), true, true);
			
		}
		logger.log(Level.INFO, "Range query: {0}", q);
		
		return q.toString();
		
	}
}
