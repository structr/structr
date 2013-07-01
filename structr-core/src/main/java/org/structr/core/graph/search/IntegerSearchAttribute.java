package org.structr.core.graph.search;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.NumericUtils;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class IntegerSearchAttribute extends PropertySearchAttribute<Integer> {

	public IntegerSearchAttribute(PropertyKey<Integer> key, Integer value, Occur occur, boolean isExactMatch) {
		super(key, value, occur, isExactMatch);
	}
	
	@Override
	public String getStringValue() {
		
		Integer value = getValue();
		if (value != null) {
			
			return NumericUtils.intToPrefixCoded(value);
		}
		
		return null;
	}

}
