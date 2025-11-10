/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.schema.action;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.autocomplete.BuiltinFunctionHint;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.ArgumentTypeException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.Functions;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.docs.DocumentableType;
import org.structr.docs.Example;
import org.structr.docs.Language;
import org.structr.docs.Parameter;
import org.structr.schema.parser.DatePropertyGenerator;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 *
 *
 */
public abstract class Function<S, T> extends BuiltinFunctionHint {

	protected static final Logger logger = LoggerFactory.getLogger(Functions.class.getName());

	public abstract T apply(ActionContext ctx, Object caller, S[] sources) throws FrameworkException;
	public abstract String usage(boolean inJavaScriptContext);
	public abstract String getRequiredModule();

	public List<String> aliases() {
		return Collections.EMPTY_LIST;
	}

	public String getNamespaceIdentifier() {
		return null;
	}

	@Override
	public DocumentableType getType() {
		return DocumentableType.BuiltInFunction;
	}

	@Override
	public List<Parameter> getParameters() {
		// override this method to modify default behaviour
		return null;
	}

	@Override
	public List<Example> getExamples() {
		// override this method to modify default behaviour
		return null;
	}

	@Override
	public List<String> getNotes() {
		// override this method to modify default behaviour
		return null;
	}

	@Override
	public List<Language> getLanguages() {
		// override this method to modify default behaviour
		return Language.all();
	}

	public int hashCode() {
		return getName().hashCode();
	}

	public boolean equals(final Object obj) {

		if (obj instanceof Function) {
			return obj.hashCode() == hashCode();
		}

		return false;
	}

	/**
	 * Basic logging for functions called with wrong parameter combination/count
	 *
	 * @param caller The element that caused the error
	 * @param parameters The function parameters
	 * @param inJavaScriptContext Has the function been called from a JavaScript context?
	 */
	protected void logParameterError(final Object caller, final Object[] parameters, final boolean inJavaScriptContext) {
		logParameterError(caller, parameters, "Unsupported parameter combination/count in", inJavaScriptContext);
	}

	/**
	 * Basic logging for functions called with wrong parameter count
	 *
	 * @param caller The element that caused the error
	 * @param parameters The function parameters
	 * @param message The message to be printed
	 * @param inJavaScriptContext Has the function been called from a JavaScript context?
	 */
	protected void logParameterError(final Object caller, final Object[] parameters, final String message, final boolean inJavaScriptContext) {
		logger.warn("{}: {} '{}'. Parameters: {}. {}", new Object[] { getReplacement(), message, caller, getParametersAsString(parameters), usage(inJavaScriptContext) });
	}

	/**
	 * Logging of an Exception in a function with a simple message outputting the name and call parameters of the function
	 *
	 * @param caller The element that caused the error
	 * @param t The exception thrown
	 * @param parameters The method parameters
	 */
	protected void logException (final Object caller, final Throwable t, final Object[] parameters) {
		logException(t, "{}: Exception in '{}' for parameters: {}", new Object[] { getReplacement(), caller, getParametersAsString(parameters) });
	}

	/**
	 * Logging of an Exception in a function with custom message and message parameters.
	 *
	 * @param t The exception thrown, only logged if log.functions.stacktrace setting is true
	 * @param msg The message to be printed
	 * @param messageParams The parameters for the message
	 */
	protected void logException (final Throwable t, final String msg, final Object[] messageParams) {
		logException(logger, t, msg, messageParams);
	}

	public static void logException (final Logger l, final Throwable t, final String msg, final Object[] messageParams) {
		if (Settings.LogFunctionsStackTrace.getValue()) {
			l.error(msg, ArrayUtils.add(messageParams, t));
		} else {
			l.error(msg + "\n(Stacktrace suppressed - see setting " + Settings.LogFunctionsStackTrace.getKey() + ")", messageParams);
		}
	}

	protected static String getParametersAsString (final Object[] sources) {
		return Arrays.toString(sources);
	}

	/**
	 * Test if the given object array has a minimum length and all its elements are not null.
	 *
	 * @param array
	 * @param minLength
	 * @throws IllegalArgumentException in case of wrong number of parameters
	 */
	protected void assertArrayHasMinLengthAndAllElementsNotNull(final Object[] array, final Integer minLength) throws ArgumentCountException, ArgumentNullException  {

		if (array.length < minLength) {
			throw ArgumentCountException.tooFew(array.length, minLength);
		}

		for (final Object element : array) {

			if (element == null) {
				throw new ArgumentNullException();
			}
		}
	}

	/**
	 * Test if the given object array has a minimum length and all its elements are not null.
	 *
	 * @param array
	 * @param minLength
	 * @param maxLength
	 * @throws ArgumentCountException in case of wrong number of parameters
	 * @throws ArgumentNullException in case of a null parameter
	 */
	protected void assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(final Object[] array, final Integer minLength, final Integer maxLength) throws ArgumentCountException, ArgumentNullException {

		if (array.length < minLength || array.length > maxLength) {
			throw ArgumentCountException.notBetween(array.length, minLength, maxLength);
		}

		for (final Object element : array) {

			if (element == null) {
				throw new ArgumentNullException();
			}
		}
	}

	/**
	 * Test if the given object array has exact the given length and all its elements are not null.
	 *
	 * @param array
	 * @param length
	 * @throws ArgumentCountException in case of wrong number of parameters
	 * @throws ArgumentNullException in case of a null parameter
	 */
	protected void assertArrayHasLengthAndAllElementsNotNull(final Object[] array, final Integer length) throws ArgumentCountException, ArgumentNullException {

		if (array.length != length) {
			throw ArgumentCountException.notEqual(array.length, length);
		}

		for (final Object element : array) {

			if (element == null) {
				throw new ArgumentNullException();
			}
		}
	}

	protected void assertArrayHasMinLengthAndTypes(final Object[] array, final int minimum, final Class... types) throws ArgumentCountException, ArgumentNullException {

		if (array.length < minimum) {
			throw ArgumentTypeException.wrongTypes(array, minimum, types);
		}

		for (int i=0; (i<array.length && i < types.length); i++) {

			final Object element = array[i];
			final Class type     = types[i];

			if (element != null) {

				if (GraphObject.class.isAssignableFrom(type)) {

					if (element instanceof GraphObject g) {

						if (!g.is(type.getSimpleName())) {

							throw ArgumentTypeException.wrongTypes(array, minimum, types);
						}

					} else {

						throw ArgumentTypeException.wrongTypes(array, minimum, types);
					}

				} else {

					if (!type.isAssignableFrom(element.getClass())) {
						throw ArgumentTypeException.wrongTypes(array, minimum, types);
					}
				}

			} else {

				throw ArgumentTypeException.wrongTypes(array, minimum, types);
			}
		}
	}

	protected void assertArrayHasLengthAndTypes(final Object[] array, final int length, final Class... types) throws ArgumentCountException, ArgumentNullException {

		if (array.length != length) {
			throw ArgumentTypeException.wrongTypes(array, length, types);
		}

		for (int i=0; (i<array.length && i < types.length); i++) {

			final Object element = array[i];
			final Class type     = types[i];

			if (element != null) {

				if (GraphObject.class.isAssignableFrom(type)) {

					if (element instanceof GraphObject g) {

						if (!g.is(type.getSimpleName())) {

							throw ArgumentTypeException.wrongTypes(array, length, types);
						}

					} else {

						throw ArgumentTypeException.wrongTypes(array, length, types);
					}

				} else {

					if (!type.isAssignableFrom(element.getClass())) {
						throw ArgumentTypeException.wrongTypes(array, length, types);
					}
				}

			} else {

				throw ArgumentTypeException.wrongTypes(array, length, types);
			}
		}
	}

	protected Double getDoubleOrNull(final Object obj) {

		try {

			if (obj instanceof Date) {

				return (double)((Date)obj).getTime();

			} else if (obj instanceof Number) {

				return ((Number)obj).doubleValue();

			} else {

				Date date = DatePropertyGenerator.parseISO8601DateString(obj.toString());

				if (date != null) {

					return (double)(date).getTime();
				}

				return Double.parseDouble(obj.toString());

			}

		} catch (NumberFormatException nfe) {

			logger.error("{}: Exception parsing '{}'", new Object[] { getReplacement(), obj });

		} catch (Throwable t) {

			logException(t, "{}: Exception parsing '{}'", new Object[] { getReplacement(), obj });
		}

		return null;
	}

	public static Integer parseInt(final Object source) {

		if (source instanceof Integer) {

			return ((Integer)source);
		}

		if (source instanceof Number) {

			return ((Number)source).intValue();
		}

		if (source instanceof String) {

			return Integer.parseInt((String)source);
		}

		return null;
	}

	protected int parseInt(final Object source, final int defaultValue) {

		if (source instanceof Integer) {

			return ((Integer)source);
		}

		if (source instanceof Number) {

			return ((Number)source).intValue();
		}

		if (source instanceof String) {

			return Integer.parseInt((String)source);
		}

		return defaultValue;
	}

	protected double parseDouble(final Object source, final double defaultValue) {

		if (source instanceof Double) {

			return ((Double)source);
		}

		if (source instanceof Number) {

			return ((Number)source).doubleValue();
		}

		if (source instanceof String) {

			return Double.parseDouble((String)source);
		}

		return defaultValue;
	}

	protected String encodeURL(final String source) {

		try {
			return URLEncoder.encode(source, "UTF-8");

		} catch (UnsupportedEncodingException ex) {
		}

		// fallback, unencoded
		return source;
	}

	public boolean valueEquals(final Object obj1, final Object obj2) {

		if (obj1 instanceof Enum || obj2 instanceof Enum) {

			return obj1.toString().equals(obj2.toString());

		}

		return eq(obj1, obj2);
	}

	protected double getDoubleForComparison(final Object obj) {

		if (obj instanceof Number) {

			return ((Number)obj).doubleValue();

		} else {

			try {
				return Double.valueOf(obj.toString());

			} catch (Throwable t) {

				logException(t, "{}: Exception parsing '{}'", new Object[] { getReplacement(), obj });
			}
		}

		return 0.0;
	}

	protected boolean gt(final Object o1, final Object o2) {

		if (o1 != null && o2 == null) {
			return true;
		}

		if ((o1 == null && o2 != null) || (o1 == null && o2 == null)) {
			return false;
		}

		if (o1 instanceof Number && o2 instanceof Number) {

			return compareNumberNumber(o1, o2) > 0;

		} else if (o1 instanceof String && o2 instanceof String) {

			return compareStringString(o1, o2) > 0;

		} else if (o1 instanceof Date && o2 instanceof Date) {

			return compareDateDate(o1, o2) > 0;

		} else if (o1 instanceof Date && o2 instanceof String) {

			return compareDateString(o1, o2) > 0;

		} else if (o1 instanceof String && o2 instanceof Date) {

			return compareStringDate(o1, o2) > 0;

		} else if (o1 instanceof Boolean && o2 instanceof String) {

			return compareBooleanString((Boolean)o1, (String)o2) > 0;

		} else if (o1 instanceof String && o2 instanceof Boolean) {

			return compareStringBoolean((String)o1, (Boolean)o2) > 0;

		} else if (o1 instanceof Number && o2 instanceof String) {

			return compareNumberString((Number)o1, (String)o2) > 0;

		} else if (o1 instanceof String && o2 instanceof Number) {

			return compareStringNumber((String)o1, (Number)o2) > 0;

		} else {

			return compareStringString(o1.toString(), o2.toString()) > 0;

		}
	}

	protected boolean lt(final Object o1, final Object o2) {

		if (o1 == null && o2 != null) {
			return true;
		}

		if ((o1 != null && o2 == null) || (o1 == null && o2 == null)) {
			return false;
		}

		if (o1 instanceof Number && o2 instanceof Number) {

			return compareNumberNumber(o1, o2) < 0;

		} else if (o1 instanceof String && o2 instanceof String) {

			return compareStringString(o1, o2) < 0;

		} else if (o1 instanceof Date && o2 instanceof Date) {

			return compareDateDate(o1, o2) < 0;

		} else if (o1 instanceof Date && o2 instanceof String) {

			return compareDateString(o1, o2) < 0;

		} else if (o1 instanceof String && o2 instanceof Date) {

			return compareStringDate(o1, o2) < 0;

		} else if (o1 instanceof Boolean && o2 instanceof String) {

			return compareBooleanString((Boolean)o1, (String)o2) < 0;

		} else if (o1 instanceof String && o2 instanceof Boolean) {

			return compareStringBoolean((String)o1, (Boolean)o2) < 0;

		} else if (o1 instanceof Number && o2 instanceof String) {

			return compareNumberString((Number)o1, (String)o2) < 0;

		} else if (o1 instanceof String && o2 instanceof Number) {

			return compareStringNumber((String)o1, (Number)o2) < 0;

		} else {

			return compareStringString(o1.toString(), o2.toString()) < 0;

		}
	}

	protected boolean gte(final Object o1, final Object o2) {
		return eq(o1, o2) || gt(o1, o2);
	}

	protected boolean lte(final Object o1, final Object o2) {
		return eq(o1, o2) || lt(o1, o2);
	}

	protected String getSandboxFileName(final String source) throws IOException {

		final String basePath = Settings.getBasePath();
		if (!basePath.isEmpty()) {

			final String exchangeDir = Settings.getFullSettingPath(Settings.DataExchangePath);

			// create exchange directory
			final File dir = new File(exchangeDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			final String finalFilePath = dir.getCanonicalPath().concat(File.separator).concat(source);
			final File sourceFile = new File(finalFilePath);

			if (finalFilePath.equals(sourceFile.getCanonicalPath())) {

				return finalFilePath;

			} else {

				logger.warn("File path might contain directory traversal attack, aborting. Path: '{}'", source);

			}

		} else {

			logger.warn("Unable to determine base.path from structr.conf, no data input/output possible.");
		}

		return null;
	}

	protected static void recursivelyConvertMapToGraphObjectMap(final GraphObjectMap destination, final Map<String, Object> source, final int depth) {

		if (depth > 20) {
			return;
		}

		for (final Map.Entry entry : source.entrySet()) {

			final String key   = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof Map) {

				final Map<String, Object> map = (Map<String, Object>) value;
				final GraphObjectMap obj = GraphObjectMap.fromMap(source);

				final Traits traits           = obj.getTraits();
				final PropertyKey propertyKey = traits.key(key);

				if (propertyKey != null) {
					destination.put(propertyKey, obj);
				} else {
					logger.warn("PropertyKey is null for key '{}' in map {}", key, map);
				}

				recursivelyConvertMapToGraphObjectMap(obj, map, depth + 1);

			} else if (value instanceof Iterable) {

				final List list           = new LinkedList();
				final Iterable collection = (Iterable)value;

				for (final Object obj : collection) {

					if (obj instanceof Map) {

						final GraphObjectMap container = new GraphObjectMap();
						list.add(container);

						recursivelyConvertMapToGraphObjectMap(container, (Map<String, Object>)obj, depth + 1);

					} else {

						list.add(obj);
					}
				}

				destination.put(new GenericProperty(key), list);

			} else {

				destination.put(new GenericProperty(key), value);
			}
		}
	}

	public static GraphObjectMap recursivelyWrapIterableInMap (final Iterable list, final Integer outputDepth) {

		final GraphObjectMap listWrapperObject = new GraphObjectMap();

		if (outputDepth <= 20) {
			listWrapperObject.put(new GenericProperty("values"), Function.toGraphObject(list, outputDepth + 1));
		}

		return listWrapperObject;

	}

	public static GraphObjectMap toGraphObjectMap(final Map<String, Object> src) {

		final GraphObjectMap dest = new GraphObjectMap();

		recursivelyConvertMapToGraphObjectMap(dest, src, 0);

		return dest;
	}

	public static Object toGraphObject (final Object sourceObject, final Integer outputDepth) {

		if (sourceObject instanceof GraphObject) {

			return sourceObject;

		} else if (sourceObject instanceof Iterable) {

			final List<GraphObject> res = new ArrayList<>();

			for(final Object o : (Iterable)sourceObject) {

				if (o instanceof Map) {

					final GraphObjectMap newObj = new GraphObjectMap();

					Function.recursivelyConvertMapToGraphObjectMap(newObj, (Map)o, outputDepth);

					res.add(newObj);

				} else if (o instanceof GraphObject) {

					res.add((GraphObject)o);

				} else if (o instanceof CharSequence) {

					res.add(Function.wrapStringInGraphObjectMap(o.toString()));

				} else if (o instanceof Number) {

					res.add(Function.wrapNumberInGraphObjectMap((Number)o));

				} else if (o instanceof Boolean) {

					res.add(Function.wrapBooleanInGraphObjectMap((Boolean)o));

				} else if (o instanceof Iterable) {

					res.add(Function.recursivelyWrapIterableInMap((Iterable)o, outputDepth));

				}
			}

			return res;

		} else if (sourceObject instanceof Map) {

			final GraphObjectMap map  = new GraphObjectMap();

			Function.recursivelyConvertMapToGraphObjectMap(map, (Map)sourceObject, outputDepth);

			return map;

		} else if (sourceObject instanceof String[]) {

			final List<GraphObject> res = new ArrayList<>();

			for (final String s : (String[]) sourceObject) {

				res.add(Function.wrapStringInGraphObjectMap(s));
			}

			return res;

		} else if (sourceObject instanceof CharSequence) {

			return Function.wrapStringInGraphObjectMap(sourceObject.toString());

		} else if (sourceObject instanceof Number) {

			return Function.wrapNumberInGraphObjectMap((Number)sourceObject);

		} else if (sourceObject instanceof Boolean) {

			return Function.wrapBooleanInGraphObjectMap((Boolean)sourceObject);

		} else if (sourceObject instanceof Date) {

			return Function.wrapDateInGraphObjectMap((Date)sourceObject);
		}

		return null;

	}

	public static GraphObjectMap wrapDateInGraphObjectMap (final Date date) {

		final GraphObjectMap dateWrapperObject = new GraphObjectMap();
		dateWrapperObject.put(new DateProperty("value"), date);
		return dateWrapperObject;
	}

	public static GraphObjectMap wrapStringInGraphObjectMap (final String str) {

		final GraphObjectMap stringWrapperObject = new GraphObjectMap();
		stringWrapperObject.put(new StringProperty("value"), str);
		return stringWrapperObject;
	}

	public static GraphObjectMap wrapNumberInGraphObjectMap (final Number num) {

		final GraphObjectMap numberWrapperObject = new GraphObjectMap();

		if (num instanceof Integer) {
			numberWrapperObject.put(new IntProperty("value"), num);
		} else if (num instanceof Double) {
			numberWrapperObject.put(new DoubleProperty("value"), num);
		} else if (num instanceof Long) {
			numberWrapperObject.put(new LongProperty("value"), num);
		} else if (num instanceof Float) {
			numberWrapperObject.put(new DoubleProperty("value"), num);
		}

		return numberWrapperObject;
	}

	public static GraphObjectMap wrapBooleanInGraphObjectMap (final Boolean bool) {

		final GraphObjectMap wrapperObject = new GraphObjectMap();

		wrapperObject.put(new BooleanProperty("value"), bool);

		return wrapperObject;
	}

	public static Object numberOrString(final String value) {

		if (value != null) {

			if ("true".equals(value.toLowerCase())) {
				return true;
			}

			if ("false".equals(value.toLowerCase())) {
				return false;
			}

			if (NumberUtils.isCreatable(value)) {
				return NumberUtils.createNumber(value);
			}
		}

		return value;
	}

	// ----- private methods -----
	private boolean eq(final Object o1, final Object o2) {

		if (o1 == null && o2 == null) {
			return true;
		}

		if ((o1 == null && o2 != null) || (o1 != null && o2 == null)) {
			return false;
		}

		try {

			if (o1 instanceof Number && o2 instanceof Number) {

				return compareNumberNumber(o1, o2) == 0;

			} else if (o1 instanceof String && o2 instanceof String) {

				return compareStringString(o1, o2) == 0;

			} else if (o1 instanceof Date && o2 instanceof Date) {

				return compareDateDate(o1, o2) == 0;

			} else if (o1 instanceof Date && o2 instanceof String) {

				return compareDateString(o1, o2) == 0;

			} else if (o1 instanceof String && o2 instanceof Date) {

				return compareStringDate(o1, o2) == 0;

			} else if (o1 instanceof Boolean && o2 instanceof String) {

				return compareBooleanStringEqual((Boolean)o1, (String)o2);

			} else if (o1 instanceof String && o2 instanceof Boolean) {

				return compareBooleanStringEqual((String)o1, (Boolean)o2);

			} else if (o1 instanceof Number && o2 instanceof String) {

				return compareNumberString((Number)o1, (String)o2) == 0;

			} else if (o1 instanceof String && o2 instanceof Number) {

				return compareStringNumber((String)o1, (Number)o2) == 0;

			} else {

				return compareStringString(o1.toString(), o2.toString()) == 0;

			}

		} catch (NumberFormatException nfe) {

			return false;

		}
	}

	private int compareBooleanBoolean(final Object o1, final Object o2) {

		final Boolean value1 = (Boolean)o1;
		final Boolean value2 = (Boolean)o2;

		return value1.compareTo(value2);
	}

	private int compareNumberNumber(final Object o1, final Object o2) {

		final Double value1 = getDoubleForComparison(o1);
		final Double value2 = getDoubleForComparison(o2);

		return value1.compareTo(value2);
	}

	private int compareStringString(final Object o1, final Object o2) {

		final String value1 = (String)o1;
		final String value2 = (String)o2;

		return value1.compareTo(value2);
	}

	private int compareDateDate(final Object o1, final Object o2) {

		final Date value1 = (Date)o1;
		final Date value2 = (Date)o2;

		return value1.compareTo(value2);
	}

	private int compareDateString(final Object o1, final Object o2) {

		final String value1 = DatePropertyGenerator.format((Date)o1, Settings.DefaultDateFormat.getValue());
		final String value2 = (String)o2;

		return value1.compareTo(value2);
	}

	private int compareStringDate(final Object o1, final Object o2) {

		final String value1 = (String)o1;
		final String value2 = DatePropertyGenerator.format((Date)o2, Settings.DefaultDateFormat.getValue());

		return value1.compareTo(value2);
	}

	private int compareBooleanString(final Boolean o1, final String o2) {
		return o1.compareTo(Boolean.valueOf(o2));
	}

	private int compareStringBoolean(final String o1, final Boolean o2) {
		return Boolean.valueOf(o1).compareTo(o2);
	}

	private boolean compareBooleanStringEqual(final Boolean o1, final String o2) {
		return o2.equals(o1.toString());
	}

	private boolean compareBooleanStringEqual(final String o1, final Boolean o2) {
		return o1.equals(o2.toString());
	}

	private int compareNumberString(final Number o1, final String o2) {


		final Double value1 = getDoubleForComparison(o1);
		Double value2;
		try {
			value2 = Double.parseDouble(o2);

		} catch (NumberFormatException nfe) {
			value2 = Double.NEGATIVE_INFINITY;
		}

		return value1.compareTo(value2);


	}

	private int compareStringNumber(final String o1, final Number o2) {

		Double value1;
		try {
			value1 = Double.parseDouble(o1);
		} catch (NumberFormatException nfe) {
			value1 = Double.NEGATIVE_INFINITY;
		}
		final Double value2 = getDoubleForComparison(o2);

		return value1.compareTo(value2);
	}
}
