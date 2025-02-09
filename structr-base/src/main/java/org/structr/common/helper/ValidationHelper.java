/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.common.helper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;
import org.structr.common.error.*;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Defines helper methods for property validation.
 *
 *
 */
public class ValidationHelper {

	private static final Logger logger = LoggerFactory.getLogger(ValidationHelper.class.getName());

	private static final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

	/**
	 * Checks whether the value for the given property key of the given node
	 * has at least the given length.
	 *
	 * @param node the node
	 * @param key the property key whose value should be checked
	 * @param minLength the min length
	 * @param errorBuffer the error buffer
	 *
	 * @return true if the condition is valid
	 */
	public static boolean isValidStringMinLength(final GraphObject node, final PropertyKey<String> key, final int minLength, final ErrorBuffer errorBuffer) {

		String value = node.getProperty(key);
		String type  = node.getType();

		if (StringUtils.isNotBlank(value)) {

			if (value.length() >= minLength) {

				return true;

			}

			errorBuffer.add(new TooShortToken(type, key.jsonName(), minLength));
			return false;

		}

		errorBuffer.add(new EmptyPropertyToken(type, key.jsonName()));
		return false;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is a non-empty string.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param errorBuffer the error buffer
	 *
	 * @return true if the condition is valid
	 */
	public static boolean isValidStringNotBlank(final GraphObject node, final PropertyKey<String> key, final ErrorBuffer errorBuffer) {

		if (StringUtils.isNotBlank(node.getProperty(key))) {

			return true;
		}

		errorBuffer.add(new EmptyPropertyToken(node.getType(), key.jsonName()));

		return false;
	}

	/**
	 * Checks whether the value for the given property key of the given node
	 * is a non-empty string.
	 *
	 * @param node the node
	 * @param key the property key
	 * @param errorBuffer the error buffer
	 *
	 * @return true if the condition is valid
	 */
	public static boolean isValidPropertyNotNull(final GraphObject node, final PropertyKey key, final ErrorBuffer errorBuffer) {

		final String type  = node.getType();
		if (key == null) {

			errorBuffer.add(new EmptyPropertyToken(type, "unknown type"));
			return false;
		}

		final Object value = node.getProperty(key);
		if (value != null) {

			if (value instanceof Iterable) {

				if (((Iterable) value).iterator().hasNext()) {

					return true;
				}

			} else {

				return true;

			}
		}

		final EmptyPropertyToken ept = new EmptyPropertyToken(type, key.jsonName());

		// for nodes, that were not created in the current tx, add the detail UUID
		if (!TransactionCommand.getCurrentTransaction().isNodeCreated(node.getPropertyContainer().getId().getId())) {
			ept.withDetail(node.getUuid());
		}

		errorBuffer.add(ept);

		return false;
	}

	/**
	 * Checks whether the value of the given property key of the given node
	 * is not null and matches the given regular expression.
	 *
	 * @param node
	 * @param key
	 * @param expression
	 * @param errorBuffer
	 * @return true if string matches expression
	 */
	public static boolean isValidStringMatchingRegex(final GraphObject node, final PropertyKey<String> key, final String expression, final ErrorBuffer errorBuffer) {

		final String value = node.getProperty(key);

		if (isValidStringMatchingRegex(value, expression)) {
			return true;
		}

		// no match
		errorBuffer.add(new MatchToken(node.getType(), key.jsonName(), expression, value));
		return false;
	}

	/**
	 * Checks whether the value of the given property key of the given node
	 * is not null and matches the given regular expression.
	 *
	 * @param value
	 * @param expression
	 * @return true if string matches expression
	 */
	public static boolean isValidStringMatchingRegex(final String value, final String expression) {

		Pattern pattern = patterns.get(expression);

		if (pattern == null) {

			pattern = Pattern.compile(expression);
			patterns.put(expression, pattern);
		}

		return (value != null && pattern.matcher(value).matches());
	}

	/**
	 * Checks whether the value of the given property key of the given node
	 * is not null and matches the given regular expression.
	 *
	 * @param node
	 * @param key
	 * @param errorBuffer
	 * @return true if string matches expression
	 */
	public static boolean isValidUuid(final GraphObject node, final PropertyKey<String> key, final ErrorBuffer errorBuffer) {

		final String value = node.getProperty(key);

		if (Settings.isValidUuid(value)) {
			return true;
		}

		// no match
		errorBuffer.add(new MatchToken(node.getType(), key.jsonName(), Settings.getValidUUIDRegexString(), value));
		return false;
	}

	public static boolean isValidIntegerInRange(final GraphObject node, final PropertyKey<Integer> key, final String range, final ErrorBuffer errorBuffer) {

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
				boolean inRange = true;

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

					errorBuffer.add(new RangeToken(type, key.jsonName(), range));
				}

				return inRange;
			}
		}

		// no error
		return true;
	}

	public static boolean isValidIntegerArrayInRange(final GraphObject node, final PropertyKey<Integer[]> key, final String range, final ErrorBuffer errorBuffer) {

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
			final Integer[] values  = node.getProperty(key);

			// do not check for non-null values, ignore (silently succeed)
			if (values != null) {

				// result
				boolean inRange = true;

				for (final Integer value : values) {

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
				}

				if (!inRange) {

					errorBuffer.add(new RangeToken(type, key.jsonName(), range));
				}

				return inRange;
			}
		}

		// no error
		return true;
	}

	public static boolean isValidLongInRange(final GraphObject node, final PropertyKey<Long> key, final String range, final ErrorBuffer errorBuffer) {

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

					errorBuffer.add(new RangeToken(type, key.jsonName(), range));
				}

				return inRange;
			}

		}

		// no error
		return true;
	}

	public static boolean isValidLongArrayInRange(final GraphObject node, final PropertyKey<Long[]> key, final String range, final ErrorBuffer errorBuffer) {

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
			final Long[] values     = node.getProperty(key);

			// do not check for non-null values, ignore (silently succeed)
			if (values != null) {

				// result
				boolean inRange  = true;

				for (final Long value : values) {

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
				}

				if (!inRange) {

					errorBuffer.add(new RangeToken(type, key.jsonName(), range));
				}

				return inRange;
			}

		}

		// no error
		return true;
	}

	public static boolean isValidDoubleInRange(final GraphObject node, final PropertyKey<Double> key, final String range, final ErrorBuffer errorBuffer) {

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

					errorBuffer.add(new RangeToken(type, key.jsonName(), range));
				}

				return inRange;
			}

		}

		// no error
		return true;
	}

	public static boolean isValidDoubleArrayInRange(final GraphObject node, final PropertyKey<Double[]> key, final String range, final ErrorBuffer errorBuffer) {

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
			final Double[] values  = node.getProperty(key);

			// do not check for non-null values, ignore (silently succeed)
			if (values != null) {

				// result
				boolean inRange = true;

				for (final Double value : values) {

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
				}

				if (!inRange) {

					errorBuffer.add(new RangeToken(type, key.jsonName(), range));
				}

				return inRange;
			}

		}

		// no error
		return true;
	}

	public static synchronized boolean isValidUniqueProperty(final GraphObject object, final PropertyKey key, final ErrorBuffer errorBuffer) {

		if (key != null) {

			// validation will only be executed for non-null values
			final Object value = object.getProperty(key);
			if (value != null) {

				final Traits traits      = object.getTraits();
				String type              = traits.getName();
				List<GraphObject> result = null;

				// use declaring class for inheritance-aware uniqueness
				final Trait trait = key.getDeclaringTrait();
				if (trait == null || "NodeInterface".equals(trait.getName())) {

					// fallback: object type
					type = object.getTraits().getName();

				} else {

					// use declaring trait for inheritance-aware queries
					type = trait.getName();
				}

				try {

					if (object instanceof NodeInterface) {

						result = StructrApp.getInstance()
								.nodeQuery(type)
								.and(key, value)
								.sort(traits.key("createdDate"))
								.getAsList();

					} else {

						result = StructrApp.getInstance()
								.relationshipQuery(type)
								.and(key, value)
								.sort(traits.key("createdDate"))
								.getAsList();

					}

				} catch (FrameworkException fex) {

					logger.warn("", fex);

				}

				/* This validation code runs at the end of a transaction, so if there
				 * is a constraint violation, there are at least two different nodes
				 * with the same value for the unique key. At this point, we don't
				 * know which node we are currently examining, so we sort by creation
				 * date (ascending order) and look at the first node of the result
				 * list. We want the validation code to fail for all constraint
				 * violating nodes that are older than the first node.
				 */

				if (result != null) {

					final Identity identity = object.getPropertyContainer().getId();

					for (final GraphObject foundNode : result) {

						if (!identity.equals(foundNode.getPropertyContainer().getId())) {

							// validation is aborted when the first validation failure occurs, so
							// we can assume that the object currently examined is the first
							// existing object, hence all others get the error message with the
							// UUID of the first one.
							errorBuffer.add(new UniqueToken(object.getType(), key.jsonName(), object.getUuid(), foundNode.getUuid(), value));

							// error!
							return false;
						}
					}
				}
			}
		}

		// no error
		return true;
	}

	public static synchronized boolean areValidCompoundUniqueProperties(final GraphObject object, final ErrorBuffer errorBuffer, final Set<PropertyKey> keys) {

		if (keys != null && !keys.isEmpty()) {

			final Traits traits                = object.getTraits();
			final PropertyMap properties       = new PropertyMap();
			List<? extends GraphObject> result = null;
			String type                        = null;

			for (final PropertyKey key : keys) {

				properties.put(key, object.getProperty(key));

				if (type == null) {

					// set type on first iteration
					type = key.getDeclaringTrait().getName();
				}
			}

			if (type == null) {

				// fallback: object type
				type = traits.getName();
			}

			try {

				if (object instanceof NodeInterface) {

					result = StructrApp.getInstance()
							.nodeQuery(type)
							.and(properties)
							.sort(traits.key("createdDate"))
							.getAsList();

				} else {

					result = StructrApp.getInstance()
							.relationshipQuery(type)
							.and(properties)
							.sort(traits.key("createdDate"))
							.getAsList();

				}

			} catch (FrameworkException fex) {

				logger.warn("", fex);

			}

			/* This validation code runs at the end of a transaction, so if there
			 * is a constraint violation, there are at least two different nodes
			 * with the same value for the unique key. At this point, we don't
			 * know which node we are currently examining, so we sort by creation
			 * date (ascending order) and look at the first node of the result
			 * list. We want the validation code to fail for all constraint
			 * violating nodes that are older than the first node.
			 */

			if (result != null) {

				final Identity identity = object.getPropertyContainer().getId();

				for (final GraphObject foundNode : result) {

					if (!identity.equals(foundNode.getPropertyContainer().getId())) {

						// validation is aborted when the first validation failure occurs, so
						// we can assume that the object currently examined is the first
						// existing object, hence all others get the error message with the
						// UUID of the first one.
						errorBuffer.add(new CompoundToken(object.getType(), keys, object.getUuid()));

						// error!
						return false;
					}
				}
			}
		}

		// no error
		return true;
	}

	public static synchronized boolean isValidGloballyUniqueProperty(final GraphObject object, final PropertyKey key, final ErrorBuffer errorBuffer) {

		if (key != null) {

			final Traits traits                = object.getTraits();
			final Object value                 = object.getProperty(key);
			List<? extends GraphObject> result = null;

			try {

				if (object instanceof NodeInterface) {

					result = StructrApp.getInstance()
							.nodeQuery()
							.and(key, value)
							.sort(traits.key("createdDate"))
							.getAsList();

				} else if (object instanceof RelationshipInterface) {

					result = StructrApp.getInstance()
							.relationshipQuery()
							.and(key, value)
							.sort(traits.key("createdDate"))
							.getAsList();

				} else {

					logger.error("GraphObject is neither NodeInterface nor RelationshipInterface");

					return false;
				}

			} catch (FrameworkException fex) {

				logger.warn("Unable to fetch list of nodes for uniqueness check", fex);
				// handle error
			}

			if (result != null) {

				final Identity identity = object.getPropertyContainer().getId();

				for (final GraphObject foundNode : result) {

					if (!identity.equals(foundNode.getPropertyContainer().getId())) {

						// validation is aborted when the first validation failure occurs, so
						// we can assume that the object currently examined is the first
						// existing object, hence all others get the error message with the
						// UUID of the first one.
						errorBuffer.add(new UniqueToken(object.getType(), key.jsonName(), object.getUuid(), foundNode.getUuid(), value));

						// error!
						return false;
					}
				}
			}
		}

		// no error
		return true;

	}
}
