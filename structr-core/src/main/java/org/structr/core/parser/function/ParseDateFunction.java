package org.structr.core.parser.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ParseDateFunction extends Function<Object, Object> {

	private static final Logger logger = Logger.getLogger(ParseDateFunction.class.getName());

	public static final String ERROR_MESSAGE_PARSE_DATE    = "Usage: ${parse_date(value, pattern)}. Example: ${parse_format(\"2014-01-01\", \"yyyy-MM-dd\")}";
	public static final String ERROR_MESSAGE_PARSE_DATE_JS = "Usage: ${{Structr.parse_date(value, pattern)}}. Example: ${{Structr.parse_format(\"2014-01-01\", \"yyyy-MM-dd\")}}";

	@Override
	public String getName() {
		return "parse_date()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources == null || sources != null && sources.length != 2) {
			return usage(ctx.isJavaScriptContext());
		}

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			String dateString = sources[0].toString();

			if (StringUtils.isBlank(dateString)) {
				return "";
			}

			String pattern = sources[1].toString();

			try {
				// parse with format from IS
				return new SimpleDateFormat(pattern).parse(dateString);

			} catch (ParseException ex) {
				logger.log(Level.WARNING, "Could not parse date " + dateString + " and format it to pattern " + pattern, ex);
			}

		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PARSE_DATE_JS : ERROR_MESSAGE_PARSE_DATE);
	}

	@Override
	public String shortDescription() {
		return "Parses the given date string using the given format string";
	}

}
