package org.structr.core.converter;

import org.structr.core.PropertyConverter;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */

public class TestConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {
		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		return source;
	}
}
