package org.structr.core.parser.function;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class NumberFormatFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_NUMBER_FORMAT    = "Usage: ${number_format(value, ISO639LangCode, pattern)}. Example: ${number_format(12345.6789, 'en', '#,##0.00')}";
	public static final String ERROR_MESSAGE_NUMBER_FORMAT_JS = "Usage: ${{Structr.number_format(value, ISO639LangCode, pattern)}}. Example: ${{Structr.number_format(12345.6789, 'en', '#,##0.00')}}";

	@Override
	public String getName() {
		return "number_format()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources == null || sources != null && sources.length != 3) {
			return usage(ctx.isJavaScriptContext());
		}

		if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

			if (StringUtils.isBlank(sources[0].toString())) {
				return "";
			}

			try {

				Double val = Double.parseDouble(sources[0].toString());
				String langCode = sources[1].toString();
				String pattern = sources[2].toString();

				return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.forLanguageTag(langCode))).format(val);

			} catch (Throwable t) {
			}
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_NUMBER_FORMAT_JS : ERROR_MESSAGE_NUMBER_FORMAT);
	}

	@Override
	public String shortDescription() {
		return "Formats the given value using the given number format string";
	}

}
