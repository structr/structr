package org.structr.function;

import java.util.Locale;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class IsLocaleFunction extends UiFunction {

	public static final String ERROR_MESSAGE_IS_LOCALE    = "Usage: ${is_locale(locales...)}";
	public static final String ERROR_MESSAGE_IS_LOCALE_JS = "Usage: ${{Structr.isLocale(locales...}}. Example ${{Structr.isLocale('de_DE', 'de_AT', 'de_CH')}}";

	@Override
	public String getName() {
		return "is_locale()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final Locale locale = ctx.getLocale();
		if (locale != null) {

			final String localeString = locale.toString();
			if (sources != null && sources.length > 0) {

				final int len = sources.length;
				for (int i = 0; i < len; i++) {

					if (sources[i] != null && localeString.equals(sources[i].toString())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_IS_LOCALE_JS : ERROR_MESSAGE_IS_LOCALE);
	}

	@Override
	public String shortDescription() {
		return "Returns true if the current user locale is equal to the given argument";
	}

}
