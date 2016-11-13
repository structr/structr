/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.function;

import java.util.List;
import java.util.Locale;
import org.structr.api.util.FixedSizeCache;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Localization;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LocalizeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_LOCALIZE    = "Usage: ${localize(key[, domain])}. Example ${localize('HELLO_WORLD', 'myDomain')}";
	public static final String ERROR_MESSAGE_LOCALIZE_JS = "Usage: ${{Structr.localize(key[, domain])}}. Example ${{Structr.localize('HELLO_WORLD', 'myDomain')}}";

	@Override
	public String getName() {
		return "localize()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

			final String cacheKey = cacheKey(sources);
			String value          = getCachedValue(cacheKey);

			if (value == null) {

				final SecurityContext superUserSecurityContext = SecurityContext.getSuperUserInstance();
				final String locale                            = ctx.getLocale().toString();
				final String name                              = sources[0].toString();

				Query query = StructrApp.getInstance(superUserSecurityContext).nodeQuery(Localization.class).and(Localization.locale, locale).and(Localization.name, name);
				List<Localization> localizations;

				final Locale ctxLocale  = ctx.getLocale();
				final String fullLocale = ctxLocale.toString();
				final String lang       = ctxLocale.getLanguage();

				if (sources.length == 2) {

					final String domain = sources[1].toString();

					// with domain
					query.and(Localization.domain, domain);

					localizations = query.getAsList();

					if (localizations.isEmpty() && fullLocale.contains("_")) {

						// no language-specific localization found, try language code only

						query = StructrApp.getInstance(superUserSecurityContext)
							.nodeQuery(Localization.class)
							.and(Localization.locale, lang)
							.and(Localization.name, name)
							.and(Localization.domain, domain);

						localizations = query.getAsList();
					}


				} else {

					// without domain
					query.blank(Localization.domain);

					localizations = query.getAsList();

					if (localizations.isEmpty() && fullLocale.contains("_")) {

						// no language-specific localization found, try language code only

						query = StructrApp.getInstance(superUserSecurityContext)
							.nodeQuery(Localization.class)
							.and(Localization.locale, lang)
							.and(Localization.name, name)
							.blank(Localization.domain);

						localizations = query.getAsList();
					}

				}

				if (localizations.size() > 1) {

					// Ambiguous localization found
					if (sources.length > 1) {

						logger.warn("Found ambiguous localization for key \"{}\" and domain \"{}\". Please fix. Parameters: {}", new Object[] { sources[0].toString(), sources[1].toString(), getParametersAsString(sources) });

					} else {

						logger.warn("Found ambiguous localization for key \"{}\". Please fix. Parameters: {}", new Object[] { sources[0].toString(), getParametersAsString(sources) });
					}
				}

				// return first localization
				if (localizations.isEmpty()) {

					// no localization found - return the key
					value = name;

				} else {

					value = localizations.get(0).getProperty(Localization.localizedName);
				}

				cacheValue(cacheKey, value);
			}

			return value;

		} else if (sources.length == 1 || sources.length == 2) {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

			// silently ignore null values
			return "";

		} else {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

			// only show the error message for wrong parameter count
			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_LOCALIZE_JS : ERROR_MESSAGE_LOCALIZE);
	}

	@Override
	public String shortDescription() {
		return "";
	}

	// ----- caching -----
	private static final FixedSizeCache<String, String> localizationCache = new FixedSizeCache<>(10000);

	public static synchronized void invalidateCache() {
		localizationCache.clear();
	}

	private String cacheKey(final Object[] sources) {

		final StringBuilder buf = new StringBuilder();

		for (final Object src : sources) {
			buf.append("||");
			buf.append(src);
		}

		return buf.toString();
	}

	private synchronized String getCachedValue(final String cacheKey) {
		return localizationCache.get(cacheKey);
	}

	private synchronized void cacheValue(final String cacheKey, final String value) {
		localizationCache.put(cacheKey, value);
	}
}
