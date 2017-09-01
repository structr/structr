/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.rest.common;

import com.opencsv.CSVParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.logger;
import org.structr.core.JsonInput;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.TransactionCommand;
import static org.structr.rest.servlet.CsvServlet.DEFAULT_FIELD_SEPARATOR_COLLECTION_CONTENTS;
import static org.structr.rest.servlet.CsvServlet.DEFAULT_QUOTE_CHARACTER_COLLECTION_CONTENTS;

public class CsvHelper {

	public static Iterable<JsonInput> cleanAndParseCSV(final SecurityContext securityContext, final Reader input, final Class type, final char fieldSeparator, final char quoteCharacter) throws FrameworkException, IOException {
		return cleanAndParseCSV(securityContext, input, type, fieldSeparator, quoteCharacter, null);
	}

	public static Iterable<JsonInput> cleanAndParseCSV(final SecurityContext securityContext, final Reader input, final Class type, final char fieldSeparator, final char quoteCharacter, final Map<String, String> propertyMapping) throws FrameworkException, IOException {

		final BufferedReader reader  = new BufferedReader(input);
		final String headerLine      = reader.readLine();
		final CSVParser parser       = new CSVParser(fieldSeparator, quoteCharacter);
		final String[] propertyNames = parser.parseLine(headerLine);

		return new Iterable<JsonInput>() {

			@Override
			public Iterator<JsonInput> iterator() {

				return new Iterator<JsonInput>() {

					String line = null;

					@Override
					public boolean hasNext() {

						// return true if the line has not yet been consumed
						// (calling hasNext() more than once may not alter the
						// result of the next next() call!)
						if (line != null) {
							return true;
						}

						try {

							line = reader.readLine();

							return StringUtils.isNotBlank(line);

						} catch (IOException ioex) {
							logger.warn("", ioex);
						}

						return false;
					}

					@Override
					public JsonInput next() {

						try {

							if (StringUtils.isNotBlank(line)) {

								final JsonInput jsonInput = new JsonInput();
								final String[] columns    = parser.parseLine(line);
								final int len             = columns.length;

								for (int i=0; i<len; i++) {

									final String key = propertyNames[i];
									String targetKey = key;

									// map key name to its transformed name
									if (propertyMapping != null && propertyMapping.containsKey(key)) {
										targetKey = propertyMapping.get(key);
									}

									if (StructrApp.getConfiguration().getPropertyKeyForJSONName(type, targetKey).isCollection()) {

										// if the current property is a collection, split it into its parts
										jsonInput.add(key, extractArrayContentsFromArray(columns[i], key));

									} else {
										jsonInput.add(key, columns[i]);
									}
								}

								return jsonInput;
							}

						} catch (IOException ioex) {
							logger.warn("Exception in CSV line: {}", line);
							logger.warn("", ioex);

							final Map<String, Object> data = new LinkedHashMap();
							data.put("type", "CSV_IMPORT_ERROR");
							data.put("title", "CSV Import Error");
							data.put("text", "Error occured with dataset: " + line);
							data.put("username", securityContext.getUser(false).getName());
							TransactionCommand.simpleBroadcastGenericMessage(data);

						} finally {

							// mark line as "consumed"
							line = null;
						}

						return null;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException("Removal not supported.");
					}

				};
			}
		};
	}

	// ----- private methods -----
	private static ArrayList<String> extractArrayContentsFromArray (final String value, final String propertyName) throws IOException {

		final CSVParser arrayParser              = new CSVParser(DEFAULT_FIELD_SEPARATOR_COLLECTION_CONTENTS, DEFAULT_QUOTE_CHARACTER_COLLECTION_CONTENTS);
		final ArrayList<String> extractedStrings = new ArrayList();

		extractedStrings.addAll(Arrays.asList(arrayParser.parseLine(stripArrayBracketsFromString(value, propertyName))));

		return extractedStrings;
	}

	private static String stripArrayBracketsFromString (final String value, final String propertyName) {

		if (value.length() > 0) {

			if (value.charAt(0) != '[' || value.charAt(value.length() - 1) != ']') {

				logger.warn("Missing opening/closing brackets for array {}: {} ", propertyName, value);
				return value;

			} else {

				return value.substring(1, value.length() - 1);

			}

		} else {
			return "";
		}

	}

}
