package org.structr.core.graph.search;

import java.util.Date;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.NumericUtils;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class DateSearchAttribute extends PropertySearchAttribute<Date> {

	public DateSearchAttribute(PropertyKey<Date> key, Date value, Occur occur, boolean isExactMatch) {
		super(key, value, occur, isExactMatch);
	}
	
	@Override
	public String getStringValue() {
		
		Date value = getValue();
		if (value != null) {
			
			return NumericUtils.longToPrefixCoded(value.getTime());
		}
		
		return null;
	}
}
