package org.structr.core.graph.search;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.NumericUtils;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class LongSearchAttribute extends PropertySearchAttribute<Long> {

	public LongSearchAttribute(PropertyKey<Long> key, Long value, Occur occur, boolean isExactMatch) {
		super(key, value, occur, isExactMatch);
	}
	
	@Override
	public String getStringValue() {
		
		Long value = getValue();
		if (value != null) {
			
			return NumericUtils.longToPrefixCoded(value);
		}
		
		return null;
	}
}
