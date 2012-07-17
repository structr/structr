/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.converter;

import java.util.List;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;

/**
 * A property converter that can convert Lists to Arrays and back.
 *
 * @author Christian Morgner
 */
public class ListArrayConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {

		if(source != null && source instanceof List) {
			return ((List)source).toArray(new String[0]);
		}

		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		return source;
	}
}
