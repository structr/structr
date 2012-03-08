/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.common;

import java.util.Arrays;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.structr.common.PropertyKey;
import org.structr.common.error.ChronologicalOrderToken;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.TooShortToken;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;

/**
 *
 * @author Christian Morgner
 */
public class ValidationHelper {

	// ----- public static methods -----
	public static boolean checkStringMinLength(GraphObject node, PropertyKey key, int minLength, ErrorBuffer errorBuffer) {

		String value = node.getStringProperty(key.name());
		String type  = node.getType();

		if (StringUtils.isNotBlank(value)) {

			if (value.length() >= minLength) {

				return false;

			}

			errorBuffer.add(type, new TooShortToken(key.name(), minLength));

			return true;

		}

		errorBuffer.add(type, new EmptyPropertyToken(key.name()));

		return true;
	}

	public static boolean checkStringNotBlank(GraphObject node, PropertyKey key, ErrorBuffer errorBuffer) {

		String type  = node.getType();

		if (StringUtils.isNotBlank((String)node.getProperty(key.name()))) {

			return false;

		}

		errorBuffer.add(type, new EmptyPropertyToken(key.name()));

		return true;
	}

	public static boolean checkPropertyNotNull(GraphObject node, PropertyKey key, ErrorBuffer errorBuffer) {

		Object value = node.getProperty(key.name());
		String type  = node.getType();

		if (value != null) {

			if (value instanceof IterableAdapter) {

				if (((IterableAdapter) value).iterator().hasNext()) {

					return false;

				}

			} else {

				return false;

			}

		}

		errorBuffer.add(type, new EmptyPropertyToken(key.name()));

		return true;
	}

	public static boolean checkDate(GraphObject node, PropertyKey key, ErrorBuffer errorBuffer) {

		Date date     = node.getDateProperty(key.name());
		String type   = node.getType();
		boolean error = false;

		if ((date == null) || ((date != null) && (date.getTime() == 0))) {

			errorBuffer.add(type, new EmptyPropertyToken(key.name()));
			error = true;

		}

		return error;
	}

	public static boolean checkDatesChronological(GraphObject node, PropertyKey key1, PropertyKey key2, ErrorBuffer errorBuffer) {

		Date date1    = node.getDateProperty(key1.name());
		Date date2    = node.getDateProperty(key2.name());
		String type   = node.getType();
		boolean error = false;

		error |= checkDate(node, key1, errorBuffer);
		error |= checkDate(node, key2, errorBuffer);

		if ((date1 != null) && (date2 != null) &&!date1.before(date2)) {

			errorBuffer.add(type, new ChronologicalOrderToken(key1.name(), key2.name()));

			error = true;

		}

		return error;
	}

	public static boolean checkStringInArray(GraphObject node, PropertyKey key, String[] values, ErrorBuffer errorBuffer) {

		String type  = node.getType();

		if (StringUtils.isNotBlank(node.getStringProperty(key.name()))) {

			if (Arrays.asList(values).contains(node.getStringProperty(key.name()))) {

				return false;

			}

		}

		errorBuffer.add(type, new ValueToken(key.name(), values));

		return true;
	}

	public static boolean checkNullOrStringInArray(GraphObject node, PropertyKey key, String[] values, ErrorBuffer errorBuffer) {

		String value = node.getStringProperty(key.name());
		String type  = node.getType();

		if(value == null) {
			return false;
		}

		if (StringUtils.isNotBlank(node.getStringProperty(key.name()))) {

			if (Arrays.asList(values).contains(node.getStringProperty(key.name()))) {

				return false;

			}

		}

		errorBuffer.add(type, new ValueToken(key.name(), values));

		return true;
	}
}
