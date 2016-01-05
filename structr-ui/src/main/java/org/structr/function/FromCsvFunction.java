package org.structr.function;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class FromCsvFunction extends UiFunction {

	public static final String ERROR_MESSAGE_FROM_CSV    = "Usage: ${from_csv(source [, delimiter, quoteChar, recordSeparator])}. Example: ${from_csv('one;two;three')}";
	public static final String ERROR_MESSAGE_FROM_CSV_JS = "Usage: ${{Structr.from_csv(src [, delimiter, quoteChar, recordSeparator])}}. Example: ${{Structr.from_csv('one;two;three')}}";

	@Override
	public String getName() {
		return "from_csv()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length > 0) {

			if (sources[0] != null) {

				try {

					final List<Map<String, String>> objects = new LinkedList<>();
					final String source                     = sources[0].toString();
					String delimiter                        = ";";
					String quoteChar                        = "\"";
					String recordSeparator                  = "\n";

					switch (sources.length) {

						case 4: recordSeparator = (String)sources[3];
						case 3: quoteChar = (String)sources[2];
						case 2: delimiter = (String)sources[1];
							break;
					}

					CSVFormat format = CSVFormat.newFormat(delimiter.charAt(0)).withHeader();
					format = format.withQuote(quoteChar.charAt(0));
					format = format.withRecordSeparator(recordSeparator);
					format = format.withIgnoreEmptyLines(true);
					format = format.withIgnoreSurroundingSpaces(true);
					format = format.withSkipHeaderRecord(true);
					format = format.withQuoteMode(QuoteMode.ALL);

					CSVParser parser = new CSVParser(new StringReader(source), format);
					for (final CSVRecord record : parser.getRecords()) {

						objects.add(record.toMap());
					}

					return objects;

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_FROM_CSV_JS : ERROR_MESSAGE_FROM_CSV);
	}

	@Override
	public String shortDescription() {
		return "Parses the given CSV string and returns a list objects";
	}

}
