package org.structr.core.converter;

import java.util.Collection;
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
		
		int count = 0;
		
		if(currentObject != null && value != null) {
			
			Object val = value.get();
			
			if(val != null) {
				
				Object toCount = currentObject.getProperty(val.toString());
				if(toCount != null) {

					if (toCount instanceof Collection) {

						count = ((Collection)toCount).size();

					} else if (toCount instanceof Iterable) {

						for(Object o : ((Iterable)toCount)) {
							count++;
						}

					} else {

						// a single object
						count = 1;
					}
				}
			}
		}
		
		return count;
	}
}
