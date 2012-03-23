package org.structr.core.converter;

import java.util.Iterator;
import java.util.List;
import org.structr.common.PropertyKey;
import org.structr.core.IterableAdapter;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;

/**
 *
 * @author Axel Morgner
 */

public class ResultCountConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {
		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		
		Object result = this.currentObject.getProperty((((PropertyKey)value.get()).name()));
		
		if (result == null) return 0;
		
		if (result instanceof List) {
			return ((List) result).size();
		}
		
		if (result instanceof IterableAdapter) {
			
			int count = 0;
			Iterator it = ((IterableAdapter) result).iterator();
			
			while (it.hasNext()) {
				it.next();
				count++;
			}
			
			return count;
		}
		
		return 1;
	}
}
