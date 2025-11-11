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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FromCsvFunction extends CsvFunction {


	@Override
	public String getName() {
		return "from_csv";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("source [, delimiterChar = ';' [, quoteChar = '\"' [, recordSeparator = '\\n' [, header [, escapeChar = '\\\\' ]]]]]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 5);

			try {

				final List<Map<String, String>> objects = new LinkedList<>();
				final String source                     = sources[0].toString();
				String delimiter                        = ";";
				String quoteChar                        = "\"";
				String recordSeparator                  = "\n";
				String escape                           = "\\";
				boolean customColumnNamesSupplied       = false;

				switch (sources.length) {

					case 6: escape = (String)sources[5];
					case 5: customColumnNamesSupplied = (sources[4] instanceof Collection);
					case 4: recordSeparator = (String)sources[3];
					case 3: quoteChar = (String)sources[2];
					case 2: delimiter = (String)sources[1];
						break;
				}

				CSVFormat format = CSVFormat.newFormat(delimiter.charAt(0));

				if (customColumnNamesSupplied) {
					format = format.withHeader(((Collection<String>)sources[4]).toArray(new String[]{ })).withSkipHeaderRecord(false);
				} else {
					format = format.withHeader().withSkipHeaderRecord(true);
				}

				format = format.withQuote(quoteChar.charAt(0));
				format = format.withRecordSeparator(recordSeparator);
				format = format.withIgnoreEmptyLines(true);
				format = format.withIgnoreSurroundingSpaces(true);
				format = format.withQuoteMode(QuoteMode.ALL);
				format = format.withEscape(escape.charAt(0));

				CSVParser parser = new CSVParser(new StringReader(source), format);
				for (final CSVRecord record : parser.getRecords()) {

					objects.add(record.toMap());
				}

				return objects;

			} catch (Throwable t) {

				logException(t, "{}(): Encountered exception '{}' for input: {}", new Object[] { getName(), t.getMessage(), getParametersAsString(sources) });
			}

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${from_csv(source[, delimiterChar [, quoteChar [, recordSeparator [, header [, escapeChar ]]]]])}. Example: ${from_csv('COL1;COL2;COL3\none;two;three')}"),
			Usage.javaScript("Usage: ${{Structr.fromCsv(source [, delimiterChar [, quoteChar [, recordSeparator [, header [, escapeChar ]]]]])}}. Example: ${{Structr.from_csv('COL1;COL2;COL3\none;two;three')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Parses the given CSV string and returns a list objects";
	}
}
