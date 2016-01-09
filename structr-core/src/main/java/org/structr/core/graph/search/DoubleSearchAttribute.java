/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.graph.search;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.NumericUtils;
import org.structr.core.property.PropertyKey;

/**
 *
 *
 */
public class DoubleSearchAttribute extends PropertySearchAttribute<Double> {

	public DoubleSearchAttribute(PropertyKey<Double> key, Double value, Occur occur, boolean isExactMatch) {
		super(key, value, occur, isExactMatch);
	}

	@Override
	public String toString() {
		return "DoubleSearchAttribute()";
	}

	@Override
	public String getStringValue() {

		Double value = getValue();
		if (value != null) {
			return NumericUtils.doubleToPrefixCoded(value);
		}

		return null;
	}
}
