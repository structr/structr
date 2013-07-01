package org.structr.core.graph.search;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.NumericUtils;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class DoubleSearchAttribute extends PropertySearchAttribute<Double> {

	public DoubleSearchAttribute(PropertyKey<Double> key, Double value, Occur occur, boolean isExactMatch) {
		super(key, value, occur, isExactMatch);
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
