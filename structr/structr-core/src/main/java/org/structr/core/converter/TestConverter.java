package org.structr.core.converter;

import org.structr.core.PropertyConverter;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */


public class TestConverter implements PropertyConverter {

	@Override
	public Object convertFrom(Object source, Value value) {
		return source;
	}

	@Override
	public Object convertTo(Object source, Value value) {
		return source;
	}
}
