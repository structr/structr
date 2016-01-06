package org.structr.core.parser.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.mozilla.javascript.ScriptableObject;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class DateFormatFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_DATE_FORMAT    = "Usage: ${date_format(value, pattern)}. Example: ${date_format(this.creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";
	public static final String ERROR_MESSAGE_DATE_FORMAT_JS = "Usage: ${{Structr.date_format(value, pattern)}}. Example: ${{Structr.date_format(Structr.get('this').creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}}";

	@Override
	public String getName() {
		return "date_format()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources == null || sources != null && sources.length != 2) {
			return usage(ctx.isJavaScriptContext());
		}

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			Date date = null;

			if (sources[0] instanceof Date) {

				date = (Date)sources[0];

			} else if (sources[0] instanceof Number) {

				date = new Date(((Number)sources[0]).longValue());

			} else if (sources[0] instanceof ScriptableObject) {

			} else {

				try {

					// parse with format from IS
					date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(sources[0].toString());

				} catch (ParseException ex) {
					ex.printStackTrace();
				}

			}

			// format with given pattern
			return new SimpleDateFormat(sources[1].toString()).format(date);
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DATE_FORMAT_JS : ERROR_MESSAGE_DATE_FORMAT);
	}

	@Override
	public String shortDescription() {
		return "Formats the given value as a date string with the given format string";
	}

}
