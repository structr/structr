/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.property.StringProperty;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.parser.DatePropertyParser;

/**
 *
 *
 */
public abstract class Function<S, T> extends Hint {

	protected static final Logger logger = LoggerFactory.getLogger(Functions.class.getName());

	public abstract T apply(ActionContext ctx, Object caller, S[] sources) throws FrameworkException;
	public abstract String usage(boolean inJavaScriptContext);

	/**
	 * Basic logging for functions called with wrong parameter count
	 *
	 * @param caller The element that caused the error
	 * @param parameters The function parameters
	 * @param inJavaScriptContext Has the function been called from a JavaScript context?
	 */
	protected void logParameterError(final Object caller, final Object[] parameters, final boolean inJavaScriptContext) {
		logger.warn("{}: unsupported parameter combination/count in \"{}\". Parameters: {}. {}", new Object[] { getName(), caller, getParametersAsString(parameters), usage(inJavaScriptContext) });
	}

	/**
	 * Logging of an Exception in a function with a simple message outputting the name and call parameters of the function
	 *
	 * @param caller The element that caused the error
	 * @param t The thrown Exception
	 * @param parameters The method parameters
	 */
	protected void logException (final Object caller, final Throwable t, final Object[] parameters) {
		logException(t, "{}: Exception in \"{}\" for parameters: {}", new Object[] { getName(), caller, getParametersAsString(parameters) });
	}

	/**
	 * Logging of an Exception in a function with custom message and message parameters.
	 *
	 * @param t The thrown Exception
	 * @param msg The message to be printed
	 * @param messageParams The parameters for the message
	 */
	protected void logException (final Throwable t, final String msg, final Object[] messageParams) {
		logger.error(msg, messageParams, t);
	}

	protected String getParametersAsString (final Object[] sources) {
		return Arrays.toString(sources);
	}

	/**
	 * Test if the given object array has a minimum length and all its elements are not null.
	 *
	 * @param array
	 * @param minLength If null, don't do length check
	 * @return true if array has min length and all elements are not null
	 * @throws IllegalArgumentException in case of wrong number of parameters
	 */
	protected boolean arrayHasMinLengthAndAllElementsNotNull(final Object[] array, final Integer minLength) throws IllegalArgumentException  {

		if (array == null) {
			return false;
		}

		if (minLength != null) {

			if (array.length < minLength) {
				throw new IllegalArgumentException();
			}

			for (final Object element : array) {

				if (element == null) {
					return false;
				}

			}

			return true;
		}

		return false;
	}

	/**
	 * Test if the given object array has a minimum length and all its elements are not null.
	 *
	 * @param array
	 * @param minLength
	 * @param maxLength
	 * @return true if array has min length and all elements are not null
	 * @throws IllegalArgumentException in case of wrong number of parameters
	 */
	protected boolean arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(final Object[] array, final int minLength, final int maxLength) throws IllegalArgumentException {

		if (array == null) {
			return false;
		}

		if (array.length < minLength || array.length > maxLength) {

			throw new IllegalArgumentException();

		}

		for (final Object element : array) {

			if (element == null) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Test if the given object array has exact the given length and all its elements are not null.
	 *
	 * @param array
	 * @param length
	 * @return true if array has exact length and all elements are not null
	 * @throws IllegalArgumentException in case of wrong number of parameters
	 */
	protected boolean arrayHasLengthAndAllElementsNotNull(final Object[] array, final int length) throws IllegalArgumentException {

		if (array == null) {
			return false;
		}

		if (array.length != length) {
			throw new IllegalArgumentException();
		}

		for (final Object element : array) {

			if (element == null) {
				return false;
			}
		}

		return true;
	}

	protected Double getDoubleOrNull(final Object obj) {

		try {

			if (obj instanceof Date) {

				return (double)((Date)obj).getTime();

			} else if (obj instanceof Number) {

				return ((Number)obj).doubleValue();

			} else {

				Date date = DatePropertyParser.parseISO8601DateString(obj.toString());

				if (date != null) {

					return (double)(date).getTime();
				}

				return Double.parseDouble(obj.toString());

			}

		} catch (Throwable t) {

			logException(t, "{}: Exception parsing \"1\"", new Object[] { getName(), obj });
		}

		return null;
	}

	protected Integer parseInt(final Object source) {

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

	protected String encodeURL(final String source) {

		try {
			return URLEncoder.encode(source, "UTF-8");

		} catch (UnsupportedEncodingException ex) {
		}

		// fallback, unencoded
		return source;
	}

	protected boolean valueEquals(final Object obj1, final Object obj2) {

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

				logException(t, "{}: Exception parsing \"1\"", new Object[] { getName(), obj });
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

	protected File getServerlogFile() throws IOException {

		final String basePath = Settings.getBasePath();

		if (!basePath.isEmpty()) {

			boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp");

			final String logPath = basePath.endsWith(File.separator) ? basePath.concat("logs" + File.separator) : basePath.concat(File.separator + "logs" + File.separator);

			File logFile = new File(logPath.concat(isDebug ? "debug.log" : "server.log"));
			if (logFile.exists()) {

				return logFile;

			} else if (!isDebug) {

				// special handling for .deb installation
				logFile = new File("/var/log/structr.log");
				if (logFile.exists()) {
					return logFile;
				}
			}

			logger.warn("Could not locate logfile");

		} else {

			logger.warn("Unable to determine base.path from structr.conf, no data input/output possible.");
		}

		return null;
	}

	protected static String serialize(final Gson gson, final Map<String, Object> map) {
		return gson.toJson(map, new TypeToken<Map<String, String>>() {
		}.getType());
	}

	protected static Map<String, Object> deserialize(final Gson gson, final String source) {
		return gson.fromJson(source, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	protected static void recursivelyConvertMapToGraphObjectMap(final GraphObjectMap destination, final Map<String, Object> source, final int depth) {

		if (depth > 20) {
			return;
		}

		for (final Map.Entry<String, Object> entry : source.entrySet()) {

			final ConfigurationProvider provider = StructrApp.getConfiguration();
			final String key                     = entry.getKey();
			final Object value                   = entry.getValue();

			if (value instanceof Map) {

				final Map<String, Object> map = (Map<String, Object>)value;
				final GraphObjectMap obj = new GraphObjectMap();

				destination.put(provider.getPropertyKeyForJSONName(obj.getClass(), key), obj);

				recursivelyConvertMapToGraphObjectMap(obj, map, depth + 1);

			} else if (value instanceof Collection) {

				final List list = new LinkedList();
				final Collection collection = (Collection)value;

				for (final Object obj : collection) {

					if (obj instanceof Map) {

						final GraphObjectMap container = new GraphObjectMap();
						list.add(container);

						recursivelyConvertMapToGraphObjectMap(container, (Map<String, Object>)obj, depth + 1);

					} else {

						list.add(obj);
					}
				}

				destination.put(provider.getPropertyKeyForJSONName(list.getClass(), key), list);

			} else {

				destination.put(value != null ? provider.getPropertyKeyForJSONName(value.getClass(), key) : new StringProperty(key), value);
			}
		}
	}

	public static GraphObjectMap toGraphObjectMap(final Map<String, Object> src) {

		final GraphObjectMap dest = new GraphObjectMap();

		recursivelyConvertMapToGraphObjectMap(dest, src, 0);

		return dest;
	}

	public static Object toGraphObject (final Object sourceObject, final Integer outputDepth) {

		if (sourceObject instanceof GraphObject) {

			return sourceObject;

		} else if (sourceObject instanceof List) {

			final List list = (List)sourceObject;
			final List<GraphObject> res = new ArrayList<>();

			for(final Object o : list){

				if (o instanceof Map) {

					final GraphObjectMap newObj = new GraphObjectMap();

					Function.recursivelyConvertMapToGraphObjectMap(newObj, (Map)o, outputDepth);

					res.add(newObj);

				} else if (o instanceof GraphObject) {

					res.add((GraphObject)o);

				} else if (o instanceof String) {

					res.add(Function.wrapStringInGraphObjectMap((String)o));

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

		} else if (sourceObject instanceof String) {

			return Function.wrapStringInGraphObjectMap((String)sourceObject);

		}

		return null;

	}

	public static GraphObjectMap wrapStringInGraphObjectMap (final String str) {

		final GraphObjectMap stringWrapperObject = new GraphObjectMap();
		stringWrapperObject.put(new StringProperty("value"), str);
		return stringWrapperObject;

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

				return compareBooleanString((Boolean)o1, (String)o2) == 0;

			} else if (o1 instanceof String && o2 instanceof Boolean) {

				return compareStringBoolean((String)o1, (Boolean)o2) == 0;

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

		final String value1 = DatePropertyParser.format((Date)o1, Settings.DefaultDateFormat.getValue());
		final String value2 = (String)o2;

		return value1.compareTo(value2);
	}

	private int compareStringDate(final Object o1, final Object o2) {

		final String value1 = (String)o1;
		final String value2 = DatePropertyParser.format((Date)o2, Settings.DefaultDateFormat.getValue());

		return value1.compareTo(value2);
	}

	private int compareBooleanString(final Boolean o1, final String o2) {
		return o1.compareTo(Boolean.valueOf(o2));
	}

	private int compareStringBoolean(final String o1, final Boolean o2) {
		return Boolean.valueOf(o1).compareTo(o2);
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
