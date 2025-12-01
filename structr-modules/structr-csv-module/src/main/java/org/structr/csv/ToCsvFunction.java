/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.csv;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.api.util.ResultStream;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.property.PropertyKey;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.rest.servlet.CsvServlet;
import org.structr.schema.action.ActionContext;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ToCsvFunction extends CsvFunction {

	@Override
	public String getName() {
		return "toCsv";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("nodes, propertiesOrView [, delimiterChar, quoteChar, recordSeparator, includeHeader, localizeHeader, headerLocalizationDomain ]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 8);

			if ( !(sources[0] instanceof Iterable) ) {
				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return "ERROR: First parameter must be a collection!".concat(usage(ctx.isJavaScriptContext()));
			}

			final List<GraphObject> nodes           = Iterables.toList((Iterable)sources[0]);
			String delimiterChar                    = ";";
			String quoteChar                        = "\"";
			String recordSeparator                  = "\n";
			boolean includeHeader                   = true;
			boolean localizeHeader                  = false;
			String headerLocalizationDomain         = null;
			String propertyView                     = null;
			List<String> properties                 = null;

			switch (sources.length) {
				case 8: headerLocalizationDomain = (String)sources[7];
				case 7: localizeHeader           = (Boolean)sources[6];
				case 6: includeHeader            = (Boolean)sources[5];
				case 5: recordSeparator          = (String)sources[4];
				case 4: quoteChar                = (String)sources[3];
				case 3: delimiterChar            = (String)sources[2];
				case 2: {

					if (sources[1] instanceof String) {

						propertyView = (String)sources[1];

					} else if (sources[1] instanceof List) {

						properties = (List)sources[1];

						if (properties.size() == 0) {

							logger.info("toCsv(): Unable to create CSV if list of properties is empty - returning empty CSV");
							return "";
						}

					} else {

						logParameterError(caller, sources, ctx.isJavaScriptContext());
						return "ERROR: Second parameter must be a collection of property names or a single property view!".concat(usage(ctx.isJavaScriptContext()));
					}
				}
			}

			// if we are using a propertyView, we need extract the property names from the first object which can not work without objects
			if (nodes.size() == 0 && propertyView != null) {

				logger.info("toCsv(): Can not create CSV if no nodes are given - returning empty CSV");
				return "";
			}

			// validate/fix quoteChar parameter
			if (quoteChar.length() == 0) {

				throw new IllegalArgumentException("quoteChar is empty - unable to create CSV");

			} else if (quoteChar.length() > 1) {

				logger.info("toCsv(): quoteChar is more than one character ('{}'), first character will be used ('{}')", quoteChar, quoteChar.charAt(0));
			}

			// validate/fix delimiterChar parameter
			if (delimiterChar.length() == 0) {

				throw new IllegalArgumentException("delimiterChar is empty - unable to create CSV");

			} else if (delimiterChar.length() > 1) {

				logger.info("toCsv(): delimiterChar is more than one character ('{}'), first character will be used ('{}')", delimiterChar, delimiterChar.charAt(0));
			}

			try {

				final StringWriter writer = new StringWriter();

				writeCsv(nodes, writer, propertyView, properties, quoteChar.charAt(0), delimiterChar.charAt(0), recordSeparator, includeHeader, localizeHeader, headerLocalizationDomain, ctx.getLocale());

				return writer.toString();

			} catch (Throwable t) {

				logger.warn("toCsv(): Exception occurred", t);
				return "";
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${toCsv(nodes, propertiesOrView[, delimiterChar[, quoteChar[, recordSeparator[, includeHeader[, localizeHeader[, headerLocalizationDomain]]]])}. Example: ${toCsv(find('Page'), 'ui')}"),
			Usage.javaScript("Usage: ${{Structr.toCsv(nodes, propertiesOrView[, delimiterChar[, quoteChar[, recordSeparator[, includeHeader[, localizeHeader[, headerLocalizationDomain]]]])}}. Example: ${{Structr.toCsv(Structr.find('Page'), 'ui'))}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a CSV representation of the given nodes.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	public static void writeCsv(
			final ResultStream result,
			final Writer out,
			final String propertyView,
			final List<String> properties,
			final char quoteChar,
			final char delimiterChar,
			final String recordSeparator,
			final boolean includeHeader,
			final boolean localizeHeader,
			final String headerLocalizationDomain,
			final Locale locale
	) throws IOException {

		final List<GraphObject> list = Iterables.toList(result);

		writeCsv(list, out, propertyView, properties, quoteChar, delimiterChar, recordSeparator, includeHeader, localizeHeader, headerLocalizationDomain, locale);
	}

	public static void writeCsv(
			final List list,
			final Writer out,
			final String propertyView,
			final List<String> properties,
			final char quoteChar,
			final char delimiterChar,
			final String recordSeparator,
			final boolean includeHeader,
			final boolean localizeHeader,
			final String headerLocalizationDomain,
			final Locale locale
	) throws IOException {

		final StringBuilder row = new StringBuilder();

		if (includeHeader) {

			row.setLength(0);

			boolean isFirstCol = true;

			if (propertyView != null) {

				final Object obj = list.get(0);

				if (obj instanceof GraphObject) {
					for (PropertyKey key : ((GraphObject)obj).getPropertyKeys(propertyView)) {
						String value = key.dbName();
						if (localizeHeader) {
							try {
								value = LocalizeFunction.getLocalization(locale, value, headerLocalizationDomain);
							} catch (FrameworkException fex) {
								logger.warn("toCsv(): Exception", fex);
							}
						}

						isFirstCol = appendColumnString(row, value, isFirstCol, quoteChar, delimiterChar);
					}
				} else {
					row.append("Error: Object is not of type GraphObject, can not determine properties of view for header row");
				}

			} else if (properties != null) {

				for (final String colName : properties) {
					String value = colName;
					if (localizeHeader) {
						try {
							value = LocalizeFunction.getLocalization(locale, value, headerLocalizationDomain);
						} catch (FrameworkException fex) {
							logger.warn("toCsv(): Exception", fex);
						}
					}

					isFirstCol = appendColumnString(row, value, isFirstCol, quoteChar, delimiterChar);
				}
			}

			out.append(row).append(recordSeparator).flush();
		}

		for (final Object obj : list) {

			row.setLength(0);

			boolean isFirstCol = true;

			if (propertyView != null) {

				if (obj instanceof GraphObject) {

					for (PropertyKey key : ((GraphObject)obj).getPropertyKeys(propertyView)) {

						final Object value = ((GraphObject)obj).getProperty(key);
						isFirstCol = appendColumnString(row, value, isFirstCol, quoteChar, delimiterChar);
					}
				} else {
					row.append("Error: Object is not of type GraphObject, can not determine properties of object");
				}

			} else if (properties != null) {

				if (obj instanceof GraphObjectMap) {

					final Map convertedMap = ((GraphObjectMap)obj).toMap();

					for (final String colName : properties) {
						final Object value = convertedMap.get(colName);
						isFirstCol = appendColumnString(row, value, isFirstCol, quoteChar, delimiterChar);
					}

				} else if (obj instanceof GraphObject graphObj) {

					for (final String colName : properties) {

						final PropertyKey key = graphObj.getTraits().key(colName);
						final Object value    = graphObj.getProperty(key);

						isFirstCol = appendColumnString(row, value, isFirstCol, quoteChar, delimiterChar);
					}

				} else if (obj instanceof Map) {

					final Map map = (Map)obj;

					for (final String colName : properties) {
						final Object value = map.get(colName);
						isFirstCol = appendColumnString(row, value, isFirstCol, quoteChar, delimiterChar);
					}
				}
			}

			// Replace \r and \n so we dont get multi-line CSV (needs to be four backslashes because regex)
			final String rowWithoutRecordSeparator = row.toString().replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r");

			out.append(rowWithoutRecordSeparator).append(recordSeparator).flush();
		}

	}

	private static boolean appendColumnString (final StringBuilder row, final Object value, boolean isFirstColumn, final char quoteChar, final char delimiter) {
		if (!isFirstColumn) {
			row.append(delimiter);
		}
		row.append(escapeForCsv(value, quoteChar));

		return false;
	}

	private static String escapeForCsv(final Object value, final char quoteChar) {

		final String result = CsvServlet.escapeForCsv(value, quoteChar);

		// post-process escaped string
		return "".concat(""+quoteChar).concat(StringUtils.replace(StringUtils.replace(result, "\r\n", "\\n"), "\r", "\\n")).concat(""+quoteChar);
	}

}
