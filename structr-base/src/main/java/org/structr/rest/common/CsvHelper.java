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
package org.structr.rest.common;

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.RFC4180Parser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.RangesIterator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.Traits;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static org.structr.rest.servlet.CsvServlet.DEFAULT_FIELD_SEPARATOR_COLLECTION_CONTENTS;
import static org.structr.rest.servlet.CsvServlet.DEFAULT_QUOTE_CHARACTER_COLLECTION_CONTENTS;

public class CsvHelper {

	private static final Logger logger = LoggerFactory.getLogger(CsvHelper.class);

	public static Iterable<JsonInput> cleanAndParseCSV(final SecurityContext securityContext, final Reader input, final String type, final Character fieldSeparator, final Character quoteCharacter, final String range) throws FrameworkException, IOException {
		return cleanAndParseCSV(securityContext, input, type, fieldSeparator, quoteCharacter, range, null, false, true);
	}

	public static Iterable<JsonInput> cleanAndParseCSV(final SecurityContext securityContext, final Reader input, final String type, final Character fieldSeparator, final Character quoteCharacter, final String range, final Map<String, String> propertyMapping, final boolean rfc4180Mode, final boolean strictQuotes) throws FrameworkException, IOException {

		final CSVReader reader;

		if (rfc4180Mode) {

			reader = new CSVReader(input, 0, new RFC4180Parser());

		} else if (quoteCharacter == null) {

			reader = new CSVReader(input, fieldSeparator);

		} else {

			reader = new CSVReader(input, fieldSeparator, quoteCharacter, strictQuotes);
		}

		final String[] propertyNames = reader.readNext();

		CsvHelper.checkPropertyNames(securityContext, propertyNames);

		return new Iterable<JsonInput>() {

			@Override
			public Iterator<JsonInput> iterator() {

				final Iterator<JsonInput> iterator = new CsvIterator(reader,  propertyNames, propertyMapping, type, securityContext.getCachedUserName());

				if (StringUtils.isNotBlank(range)) {

					return new RangesIterator<>(iterator, range);

				} else {

					return iterator;
				}
			}
		};
	}

	public static Iterable<JsonInput> cleanAndParseCSV(final SecurityContext securityContext, final Reader input, final Character fieldSeparator, final Character quoteCharacter, final String range, final Map<String, String> propertyMapping, final boolean strictQuotes) throws FrameworkException, IOException {

		final CSVReader reader;

		if (quoteCharacter == null) {

			reader = new CSVReader(input, fieldSeparator);

		} else {

			reader = new CSVReader(input, fieldSeparator, quoteCharacter, strictQuotes);
		}

		final String[] propertyNames = reader.readNext();

		CsvHelper.checkPropertyNames(securityContext, propertyNames);

		return new Iterable<JsonInput>() {

			@Override
			public Iterator<JsonInput> iterator() {

				final Iterator<JsonInput> iterator = new MixedCsvIterator(reader,  propertyNames, propertyMapping, securityContext.getCachedUserName());

				if (StringUtils.isNotBlank(range)) {

					return new RangesIterator<>(iterator, range);

				} else {

					return iterator;
				}
			}
		};
	}

	private static void checkPropertyNames(final SecurityContext securityContext, final String[] propertyNames) throws FrameworkException {

		final int len = propertyNames.length;

		for (int i=0; i<len; i++) {

			final String key = propertyNames[i];

			if (StringUtils.isBlank(key)) {

				final String message = "Property name in header is empty  - maybe a problem with the field quoting?";
				logger.warn(message);
				final Map<String, Object> data = new LinkedHashMap();

				data.put("type",     "CSV_IMPORT_WARNING");
				data.put("title",    "CSV Import Warning");
				data.put("text",     message);
				data.put("username", securityContext.getCachedUserName());

				TransactionCommand.simpleBroadcastGenericMessage(data);
			}
		}
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

	// ----- nested classes -----
	private static class CsvIterator implements Iterator<JsonInput> {

		private Map<String, String> propertyMapping = null;
		private CSVReader reader                    = null;
		private String[] propertyNames              = null;
		private String userName                     = null;
		private String[] fields                     = null;
		private String type                         = null;

		public CsvIterator(final CSVReader reader, final String[] propertyNames, final Map<String, String> propertMapping, final String type, final String userName) {

			this.propertyMapping = propertMapping;
			this.propertyNames   = propertyNames;
			this.userName        = userName;
			this.reader          = reader;
			this.type            = type;
		}

		@Override
		public boolean hasNext() {

			// return true if the line has not yet been consumed
			// (calling hasNext() more than once may not alter the
			// result of the next next() call!)
			if (fields != null) {
				return true;
			}

			try {

				fields = reader.readNext();

				return fields != null && fields.length > 0;

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}

			return false;
		}

		@Override
		public JsonInput next() {

			try {
				final Traits traits       = Traits.of(type);
				final JsonInput jsonInput = new JsonInput();
				final int len             = fields.length;

				if (fields.length > propertyNames.length) {
					throw new FrameworkException(422, "Line contains more fields than columns - maybe a problem with the field quoting?");
				}

				for (int i=0; i<len; i++) {

					final String key = propertyNames[i];
					String targetKey = key;

					// map key name to its transformed name
					if (propertyMapping != null && propertyMapping.containsKey(key)) {
						targetKey = propertyMapping.get(key);
					}

					if (traits.key(targetKey).isCollection()) {

						// if the current property is a collection, split it into its parts
						jsonInput.add(key, extractArrayContentsFromArray(fields[i], key));

					} else {

						jsonInput.add(key, fields[i]);
					}
				}

				return jsonInput;

			} catch (Throwable t) {

				final String lineInfo                 = Arrays.toString(fields);
				final String atMostFirst100Characters = lineInfo.substring(0, Math.min(100, lineInfo.length()));
				logger.warn("Exception in CSV line: {}", atMostFirst100Characters);
				logger.warn("", t);

				final Map<String, Object> data = new LinkedHashMap();

				data.put("type",     "CSV_IMPORT_ERROR");
				data.put("title",    "CSV Import Error");
				data.put("text",     "Error occured with dataset: " + atMostFirst100Characters);
				data.put("username", userName);

				TransactionCommand.simpleBroadcastGenericMessage(data);

			} finally {

				// mark line as "consumed"
				fields = null;
			}

			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Removal not supported.");
		}
	}

	private static class MixedCsvIterator implements Iterator<JsonInput> {

		private Map<String, String> propertyMapping = null;
		private CSVReader reader                    = null;
		private String[] propertyNames              = null;
		private String userName                     = null;
		private String[] fields                     = null;

		public MixedCsvIterator(final CSVReader reader, final String[] propertyNames, final Map<String, String> propertMapping, final String userName) {

			this.propertyMapping = propertMapping;
			this.propertyNames   = propertyNames;
			this.userName        = userName;
			this.reader          = reader;
		}

		@Override
		public boolean hasNext() {

			// return true if the line has not yet been consumed
			// (calling hasNext() more than once may not alter the
			// result of the next next() call!)
			if (fields != null) {
				return true;
			}

			try {

				fields = reader.readNext();

				return fields != null && fields.length > 0;

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}

			return false;
		}

		@Override
		public JsonInput next() {

			try {
				final JsonInput jsonInput = new JsonInput();
				final int len             = fields.length;

				for (int i=0; i<len; i++) {

					final String key = propertyNames[i];
					String targetKey = key;

					// map key name to its transformed name
					if (propertyMapping != null && propertyMapping.containsKey(key)) {
						targetKey = propertyMapping.get(key);
					}

					jsonInput.add(targetKey, fields[i]);
				}

				return jsonInput;

			} catch (Throwable t) {

				logger.warn("Exception in CSV line: {}", (Object)fields);
				logger.warn("", t);

				final Map<String, Object> data = new LinkedHashMap();

				data.put("type",     "CSV_IMPORT_ERROR");
				data.put("title",    "CSV Import Error");
				data.put("text",     "Error occured with dataset: " + fields);
				data.put("username", userName);

				TransactionCommand.simpleBroadcastGenericMessage(data);

			} finally {

				// mark line as "consumed"
				fields = null;
			}

			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Removal not supported.");
		}
	}
}
