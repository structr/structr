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
package org.structr.core.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.structr.api.util.FixedSizeCache;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Localization;
import org.structr.core.property.StringProperty;
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
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final Locale ctxLocale  = ctx.getLocale();

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

			if (sources[0] instanceof List) {

				final List keyList = (List)sources[0];

				if (sources.length == 1) {
					return getLocalizedList(ctxLocale, keyList);
				} else {
					return getLocalizedList(ctxLocale, keyList, sources[1].toString());
				}

			} else {

				final String name = sources[0].toString();

				if (sources.length == 1) {
					return getLocalization(ctxLocale, name);
				} else {
					return getLocalization(ctxLocale, name, sources[1].toString());
				}

			}

		} else if (sources.length == 1 || sources.length == 2) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			// silently ignore null values
			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

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

	public static List getLocalizedList(final Locale locale, final List<String> keyList) throws FrameworkException {
		return getLocalizedList(locale, keyList, null);
	}

	public static List getLocalizedList(final Locale locale, final List<String> keyList, final String domain) throws FrameworkException {

		final ArrayList<GraphObjectMap> resultList = new ArrayList();

		for (final String key : keyList) {

			final GraphObjectMap localizedEntry = new GraphObjectMap();
			resultList.add(localizedEntry);

			localizedEntry.put(new StringProperty("name"), key);

			if (domain == null) {
				localizedEntry.put(new StringProperty("localizedName"), getLocalization(locale, key));
			} else {
				localizedEntry.put(new StringProperty("localizedName"), getLocalization(locale, key, domain));
			}

		}

		return resultList;
	}

	public static String getLocalization (final Locale locale, final String key) throws FrameworkException {
		return getLocalization(locale, key, null);
	}

	public static String getLocalization (final Locale locale, final String requestedKey, final String requestedDomain) throws FrameworkException {

		final String fullLocale  = locale.toString();
		final String lang        = locale.getLanguage();
		final String cacheKey    = cacheKey(fullLocale, requestedKey, requestedDomain);
		final String finalDomain = (requestedDomain == null) ? "" : requestedDomain;
		String value             = getCachedValue(cacheKey);

		// find localization with exact key, domain and (full) locale
		if (value == null) { value = getLocalizedNameFromDatabase(requestedKey, finalDomain, fullLocale); }

		// find localization with key, NO domain and (full) locale
		if (value == null && !finalDomain.equals("")) { value = getLocalizedNameFromDatabase(requestedKey, "", fullLocale); }

		// find localization with key, domain and language only
		if (value == null) { value = getLocalizedNameFromDatabase(requestedKey, finalDomain, lang); }

		// find localization with key, domain and language only
		if (value == null && !finalDomain.equals("")) { value = getLocalizedNameFromDatabase(requestedKey, "", lang); }

		// only cache if resolution was successful
		if (value == null) {

			value = requestedKey;

		} else {

			cacheValue(cacheKey, value);
		}

		return value;
	}

	// ----- caching -----
	private static final FixedSizeCache<String, String> localizationCache = new FixedSizeCache<>(10000);

	public static synchronized void invalidateCache() {
		localizationCache.clear();
	}

	private static String cacheKey(final String locale, final String key, final String domain) {

		final StringBuilder buf = new StringBuilder(locale);

		buf.append("||").append(key);

		if (domain != null && !domain.equals("")) {
			buf.append("||").append(domain);
		}

		return buf.toString();
	}

	private static synchronized String getCachedValue(final String cacheKey) {
		return localizationCache.get(cacheKey);
	}

	private static synchronized void cacheValue(final String cacheKey, final String value) {
		localizationCache.put(cacheKey, value);
	}

	private static String getLocalizedNameFromDatabase(final String key, final String domain, final String locale) throws FrameworkException {

		final List<Localization> localizations = StructrApp.getInstance().nodeQuery(Localization.class)
			.and(Localization.name,   key)
			.and(Localization.domain, domain)
			.and(Localization.locale, locale)
			.getAsList();

		// nothing found
		if (localizations.isEmpty()) {
			return null;
		}

		// too many
		if (localizations.size() > 1) {

			// Ambiguous localization found
			logger.warn("Found ambiguous localization for locale \"{}\", key \"{}\" and domain \"{}\". Please fix. Parameters: {}", new Object[] { locale, key, domain });
		}

		// return first
		return localizations.get(0).getProperty(Localization.localizedName);
	}
}
