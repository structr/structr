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
package org.structr.core.function;

import org.structr.api.config.Settings;
import org.structr.api.util.FixedSizeCache;
import org.structr.common.AccessMode;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.LocalizationTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocalizeFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_LOCALIZE    = "Usage: ${localize(key[, domain])}. Example ${localize('HELLO_WORLD', 'myDomain')}";
	public static final String ERROR_MESSAGE_LOCALIZE_JS = "Usage: ${{Structr.localize(key[, domain])}}. Example ${{Structr.localize('HELLO_WORLD', 'myDomain')}}";

	@Override
	public String getName() {
		return "localize";
	}

	@Override
	public String getSignature() {
		return "key [, domain ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String domain = (sources.length == 1) ? null : sources[1].toString();

			if (sources[0] instanceof List) {

				final List toLocalizeList = (List)sources[0];

				return getLocalizedList(ctx, caller, toLocalizeList, domain);

			} else {

				final String toLocalize = sources[0].toString();

				return getLocalization(ctx, caller, toLocalize, domain);

			}

		} catch (ArgumentNullException pe) {

			if (sources[0] == null) {

				// silently ignore case which can happen for localize(current.propertyThatCanBeNull[, domain])
				return "";

			} else if (sources.length <= 2) {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				return "";

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());

				// only show the error message for wrong parameter count
				return usage(ctx.isJavaScriptContext());
			}

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

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
		return "Returns a (cached) Localization result for the given key and optional domain";
	}

	public static List getLocalizedList(final ActionContext ctx, final Object caller, final List<String> keyList, final String domain) throws FrameworkException {

		final ArrayList<GraphObjectMap> resultList = new ArrayList();

		for (final String key : keyList) {

			final GraphObjectMap localizedEntry = new GraphObjectMap();
			resultList.add(localizedEntry);

			localizedEntry.put(new StringProperty("name"), key);
			localizedEntry.put(new StringProperty("localizedName"), getLocalization(ctx, caller, key, domain));
		}

		return resultList;
	}

	public static String getLocalization (final Locale locale, final String requestedKey, final String requestedDomain) throws FrameworkException {

		return getLocalization(locale, requestedKey, requestedDomain, false);
	}

	public static String getLocalization (final Locale locale, final String requestedKey, final String requestedDomain, final boolean isFallbackLookup) throws FrameworkException {

		/*
			OLD VERSION - make sure to keep functionality compatible when making changes!
		*/

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

		// find localization with key, NO domain and language only
		if (value == null && !finalDomain.equals("")) { value = getLocalizedNameFromDatabase(requestedKey, "", lang); }

		// prevent further fallback lookups and also caching in fallback mode
		if (isFallbackLookup == false) {

			// only cache if resolution was successful
			if (value != null) {

				cacheValue(cacheKey, value);

			} else {

				if (Settings.logMissingLocalizations.getValue()) {
					logger.warn("Missing localization: Key: '{}' Locale: '{}' Domain: '{}'", requestedKey, fullLocale, requestedDomain);
				}

				// try fallback locale, if active ...
				if (Settings.useFallbackLocale.getValue()) {

					final Locale fallbackLocale     = Locale.forLanguageTag(Settings.fallbackLocale.getValue().trim().replaceAll("_", "-"));
					final String fullFallbackLocale = fallbackLocale.toString();

					// ... and fallback locale is not empty and is different from current locale
					if (!fullFallbackLocale.equals("") && !fullLocale.equals(fullFallbackLocale)) {

						final String fallbackValue = getLocalization(fallbackLocale, requestedKey, requestedDomain, true);

						if (fallbackValue != null) {

							value = fallbackValue;

						} else if (Settings.logMissingLocalizations.getValue()) {

							logger.warn("Fallback localization also missing: Key: '{}' Locale: '{}' Domain: '{}'", requestedKey, fullFallbackLocale, requestedDomain);
						}
					}
				}

				if (value == null) {

					value = requestedKey;
				}

				cacheValue(cacheKey, value);
			}
		}

		return value;
	}

	public static String getLocalization (final ActionContext ctx, final Object caller, final String requestedKey, final String requestedDomain) throws FrameworkException {

		final Locale locale      = ctx.getLocale();

		// If we are accessing the function via the frontend access mode, we use the "regular" function
		if (AccessMode.Frontend.equals(ctx.getSecurityContext().getAccessMode())) {

			return getLocalization(locale, requestedKey, requestedDomain);
		}

		// otherwise we do not use the cache so we retrieve the database object every time

		final String fullLocale   = locale.toString();
		final String lang         = locale.getLanguage();
		final String finalDomain  = (requestedDomain == null) ? "" : requestedDomain;
		NodeInterface result      = null;

		// find localization with exact key, domain and (full) locale
		if (result == null) { result = getLocalizationFromDatabase(requestedKey, finalDomain, fullLocale); }

		// find localization with key, NO domain and (full) locale
		if (result == null && !finalDomain.equals("")) { result = getLocalizationFromDatabase(requestedKey, "", fullLocale); }

		// find localization with key, domain and language only
		if (result == null) { result = getLocalizationFromDatabase(requestedKey, finalDomain, lang); }

		// find localization with key, NO domain and language only
		if (result == null && !finalDomain.equals("")) { result = getLocalizationFromDatabase(requestedKey, "", lang); }

		String value = requestedKey;

		if (result != null) {

			value = result.getProperty(result.getTraits().key(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY));
		}

		ctx.getContextStore().addRequestedLocalization(caller, requestedKey, finalDomain, fullLocale, result);

		return value;
	}


	// ----- caching -----
	private static final FixedSizeCache<String, String> localizationCache = new FixedSizeCache<>("Localization cache", 10000);

	public static synchronized void invalidateCache() {
		localizationCache.clear();
	}

	public static synchronized Map getCacheInfo() {
		return localizationCache.getCacheInfo();
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

	private static NodeInterface getLocalizationFromDatabase(final String key, final String domain, final String locale) throws FrameworkException {

		final Traits traits                 = Traits.of(StructrTraits.LOCALIZATION);
		final PropertyKey<String> nameKey   = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<String> domainKey = traits.key(LocalizationTraitDefinition.DOMAIN_PROPERTY);
		final PropertyKey<String> localeKey = traits.key(LocalizationTraitDefinition.LOCALE_PROPERTY);

		final List<NodeInterface> localizations = StructrApp.getInstance().nodeQuery(StructrTraits.LOCALIZATION)
			.key(nameKey,   key)
			.key(domainKey, domain)
			.key(localeKey, locale)
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
		return localizations.get(0);
	}

	private static String getLocalizedNameFromDatabase(final String key, final String domain, final String locale) throws FrameworkException {

		final NodeInterface localization = getLocalizationFromDatabase(key, domain, locale);

		// nothing found
		if (localization == null) {
			return null;
		}

		// return first
		return localization.getProperty(localization.getTraits().key(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY));
	}

}
