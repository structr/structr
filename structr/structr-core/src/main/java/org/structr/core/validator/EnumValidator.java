
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.core.validator;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class EnumValidator<T> extends PropertyValidator<T> {

	private Set<T> values = new LinkedHashSet<T>();

	public EnumValidator(T[] values) {
		for(T t : values) {
			this.values.add(t);
		}
	}

	@Override
	public boolean isValid(GraphObject object, String key, T value, ErrorBuffer errorBuffer) {

		if (value == null) {

			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));

		} else {

			if(values.contains(value)) {

				return true;

			} else {
				
				errorBuffer.add(object.getType(), new ValueToken(key, values.toArray()));
			}
		}

		return false;
	}
}
