/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ChronologicalOrderToken;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.MatchToken;
import org.structr.common.error.RangeToken;
import org.structr.common.error.TooShortToken;
import org.structr.common.error.UniqueToken;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;

/**
 * Defines helper methods for property validation.
 *
 *
 */
public class ValidationHelper {

	private static final Logger logger = Logger.getLogger(ValidationHelper.class.getName());

	private static final PropertyKey UnknownType = new GenericProperty("unknown type");

	// ----- public static methods -----
	/**
	 * Checks whether the value for the given property key of the given node
	 * has at least the given length.
	 *
	 * @param node the node
	 * @param key the property key whose value should be checked
	 * @param minLength the min length
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkStringMinLength(final GraphObject node, final PropertyKey<String> key, final int minLength, final ErrorBuffer errorBuffer) {

		String value = node.getProperty(key);
		String type  = node.getType();

		if (StringUtils.isNotBlank(value)) {

			if (value.length() >= minLength) {

				return false;

			}

			errorBuffer.add(new TooShortToken(type, key, minLength));

			return true;

		}

		errorBuffer.add(new EmptyPropertyToken(type, key));

		return true;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is a non-empty string.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkStringNotBlank(final GraphObject node, final PropertyKey<String> key, final ErrorBuffer errorBuffer) {

		String type  = node.getType();

		if (StringUtils.isNotBlank(node.getProperty(key))) {

			return false;

		}

		errorBuffer.add(new EmptyPropertyToken(type, key));

		return true;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is a non-empty string.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkPropertyNotNull(final GraphObject node, final PropertyKey key, final ErrorBuffer errorBuffer) {

		String type  = node.getType();

		if (key == null) {
			errorBuffer.add(new EmptyPropertyToken(type, UnknownType));
			return true;
		}

		Object value = node.getProperty(key);

		if (value != null) {

			if (value instanceof Iterable) {

				if (((Iterable) value).iterator().hasNext()) {

					return false;

				}

			} else {

				return false;

			}

		}

		errorBuffer.add(new EmptyPropertyToken(type, key));

		return true;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is non null and of type Date.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkDate(final GraphObject node, final PropertyKey<Date> key, final ErrorBuffer errorBuffer) {

		Date date     = node.getProperty(key);
		String type   = node.getType();
		boolean error = false;

		if ((date == null) || ((date != null) && (date.getTime() == 0))) {

			errorBuffer.add(new EmptyPropertyToken(type, key));
			error = true;

		}

		return error;
	}


	/**
	 * Checks whether the Date values for the two given property keys are
	 * in chronological order, i.e. the Date of key1 lies before the one of
	 * key2.
	 *
	 * @param node the node
	 * @param key1 the first Date key
	 * @param key2 the second Date key
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkDatesChronological(final GraphObject node, final PropertyKey<Date> key1, final PropertyKey<Date> key2, final ErrorBuffer errorBuffer) {

		Date date1    = node.getProperty(key1);
		Date date2    = node.getProperty(key2);
		String type   = node.getType();
		boolean error = false;

		error |= checkDate(node, key1, errorBuffer);
		error |= checkDate(node, key2, errorBuffer);

		if ((date1 != null) && (date2 != null) &&!date1.before(date2)) {

			errorBuffer.add(new ChronologicalOrderToken(type, key1, key2));

			error = true;

		}

		return error;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is one of the values array.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param values the values to check against
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkStringInArray(final GraphObject node, final PropertyKey<String> key, final String[] values, final ErrorBuffer errorBuffer) {

		String type  = node.getType();

		if (StringUtils.isNotBlank(node.getProperty(key))) {

			if (Arrays.asList(values).contains(node.getProperty(key))) {

				return false;

			}

		}

		errorBuffer.add(new ValueToken(type, key, values));

		return true;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is a valid enum value of the given type.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param enumType the enum type to check against
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkStringInEnum(final GraphObject node, final PropertyKey<? extends Enum> key, Class<? extends Enum> enumType, final ErrorBuffer errorBuffer) {

		return checkStringInEnum(node.getType(), node, key, enumType, errorBuffer);
	}

	/**
	 * Checks whether the value of the given property key of the given node
	 * if not null and matches the given regular expression.
	 *
	 * @param node
	 * @param key
	 * @param expression
	 * @param errorBuffer
	 * @return true if string matches expression
	 */
	public static boolean checkStringMatchesRegex(final GraphObject node, final PropertyKey<String> key, final String expression, final ErrorBuffer errorBuffer) {

		String value = node.getProperty(key);
		boolean matches = value != null && value.matches(expression);

		if (!matches) {
			errorBuffer.add(new MatchToken(node.getType(), key, expression));
		}

		return matches;

	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is a valid enum value of the given type. In case of an error, the
	 * type identifiery in typeString is used for the error message.
	 *
	 * @param typeString
	 * @param node the node
	 * @param key the property key
	 * @param enumType the enum type to check against
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkStringInEnum(final String typeString, final GraphObject node, final PropertyKey<? extends Enum> key, Class<? extends Enum> enumType, final ErrorBuffer errorBuffer) {

		Enum value = node.getProperty(key);
		Enum[] values = enumType.getEnumConstants();

		for (Enum v : values) {

			if (v.equals(value)) {
				return false;
			}

		}

		errorBuffer.add(new ValueToken(typeString, key, values));

		return true;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is null OR one of the values given in the values array.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param values the values array
	 * @param errorBuffer the error buffer
	 *
	 * @return true if there is an error checking the given node
	 */
	public static boolean checkNullOrStringInArray(final GraphObject node, final PropertyKey<String> key, String[] values, final ErrorBuffer errorBuffer) {

		String value = node.getProperty(key);
		String type  = node.getType();

		if(value == null) {
			return false;
		}

		if (StringUtils.isNotBlank(node.getProperty(key))) {

			if (Arrays.asList(values).contains(node.getProperty(key))) {

				return false;

			}

		}

		errorBuffer.add(new ValueToken(type, key, values));

		return true;
	}

	public static boolean checkIntegerInRangeError(final GraphObject node, final PropertyKey<Integer> key, final String range, final ErrorBuffer errorBuffer) {

		// we expect expression to have the following format:
		// - "[" or "]" followed by a number (including negative values
		// - a comma (must exist)
		// - a number (including negative values followed by "[" or "]"

		final int length        = range.length();
		final String leftBound  = range.substring(0, 1);
		final String rightBound = range.substring(length-1, length);
		final String[] parts    = range.substring(1, length-1).split(",+");
		final String type       = node.getType();

		if (parts.length == 2) {

			final String leftPart   = parts[0].trim();
			final String rightPart  = parts[1].trim();
			final int left          = Integer.parseInt(leftPart);
			final int right         = Integer.parseInt(rightPart);
			final Integer value     = node.getProperty(key);

			// do not check for non-null values, ignore (silently succeed)
			if (value != null) {

				// result
				boolean inRange         = true;

				if ("[".equals(leftBound)) {
					inRange &= (value >= left);
				} else {
					inRange &= (value > left);
				}

				if ("]".equals(rightBound)) {
					inRange &= (value <= right);
				} else {
					inRange &= (value < right);
				}

				if (!inRange) {

					errorBuffer.add(new RangeToken(type, key, range));
				}

				return !inRange;
			}

		}

		// no error
		return false;
	}

	public static boolean checkLongInRangeError(final GraphObject node, final PropertyKey<Long> key, final String range, final ErrorBuffer errorBuffer) {

		// we expect expression to have the following format:
		// - "[" or "]" followed by a number (including negative values
		// - a comma (must exist)
		// - a number (including negative values followed by "[" or "]"

		final int length        = range.length();
		final String leftBound  = range.substring(0, 1);
		final String rightBound = range.substring(length-1, length);
		final String[] parts    = range.substring(1, length-1).split(",+");
		final String type       = node.getType();

		if (parts.length == 2) {

			final String leftPart   = parts[0].trim();
			final String rightPart  = parts[1].trim();
			final long left         = Long.parseLong(leftPart);
			final long right        = Long.parseLong(rightPart);
			final Long value        = node.getProperty(key);

			// do not check for non-null values, ignore (silently succeed)
			if (value != null) {

				// result
				boolean inRange         = true;

				if ("[".equals(leftBound)) {
					inRange &= (value >= left);
				} else {
					inRange &= (value > left);
				}

				if ("]".equals(rightBound)) {
					inRange &= (value <= right);
				} else {
					inRange &= (value < right);
				}

				if (!inRange) {

					errorBuffer.add(new RangeToken(type, key, range));
				}

				return !inRange;
			}

		}

		// no error
		return false;
	}

	public static boolean checkDoubleInRangeError(final GraphObject node, final PropertyKey<Double> key, final String range, final ErrorBuffer errorBuffer) {

		// we expect expression to have the following format:
		// - "[" or "]" followed by a number (including negative values
		// - a comma (must exist)
		// - a number (including negative values followed by "[" or "]"

		final int length        = range.length();
		final String leftBound  = range.substring(0, 1);
		final String rightBound = range.substring(length-1, length);
		final String[] parts    = range.substring(1, length-1).split(",+");
		final String type       = node.getType();

		if (parts.length == 2) {

			final String leftPart  = parts[0].trim();
			final String rightPart = parts[1].trim();
			final double left      = Double.parseDouble(leftPart);
			final double right     = Double.parseDouble(rightPart);
			final Double value     = node.getProperty(key);

			// do not check for non-null values, ignore (silently succeed)
			if (value != null) {

				// result
				boolean inRange         = true;

				if ("[".equals(leftBound)) {
					inRange &= (value >= left);
				} else {
					inRange &= (value > left);
				}

				if ("]".equals(rightBound)) {
					inRange &= (value <= right);
				} else {
					inRange &= (value < right);
				}

				if (!inRange) {

					errorBuffer.add(new RangeToken(type, key, range));
				}

				return !inRange;
			}

		}

		// no error
		return false;
	}

	public static synchronized boolean checkPropertyUniquenessError(final GraphObject object, final PropertyKey key, final ErrorBuffer errorBuffer) {

		if (key != null) {

			final Object value         = object.getProperty(key);
			if (value != null) {
				
				Result<GraphObject> result = null;
				boolean exists             = false;
				String id                  = null;

				try {

					if (object instanceof NodeInterface) {

						result = StructrApp.getInstance().nodeQuery(((NodeInterface)object).getClass()).and(key, value).getResult();

					} else {

						result = StructrApp.getInstance().relationshipQuery(((RelationshipInterface)object).getClass()).and(key, value).getResult();

					}

					exists = !result.isEmpty();

				} catch (FrameworkException fex) {

					logger.log(Level.WARNING, "", fex);

				}

				if (exists) {

					GraphObject foundNode = result.get(0);

					if (foundNode.getId() != object.getId()) {

						id = ((AbstractNode) result.get(0)).getUuid();

						errorBuffer.add(new UniqueToken(object.getType(), key, id));

						return true;
					}
				}
			}
		}

		// no error
		return false;
	}
}
